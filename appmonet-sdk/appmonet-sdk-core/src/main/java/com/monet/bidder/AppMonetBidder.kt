package com.monet.bidder

import android.text.TextUtils
import com.monet.Callback
import com.monet.bidder.Constants.Configurations
import com.monet.bidder.Constants.JSMethods
import com.monet.bidder.Constants.TEST_MODE_WARNING
import com.monet.bidder.auction.AuctionManagerCallback
import com.monet.auction.AuctionRequest
import com.monet.auction.AuctionRequest.Companion.from
import com.monet.bidder.bid.BidManager
import com.monet.BidResponse
import com.monet.BidResponse.Constant.BID_BUNDLE_KEY
import com.monet.threading.BackgroundThread
import com.monet.bidder.threading.InternalRunnable
import com.monet.bidder.threading.UIThread
import com.monet.AdServerWrapper
import com.monet.AdType
import com.monet.AdServerAdView
import com.monet.AdServerAdRequest
import com.monet.RequestData

/**
 * Created by jose on 8/28/17.
 */
class AppMonetBidder(
  private val bidManager: BidManager,
  private val adServerWrapper: AdServerWrapper,
  private val auctionManagerCallback: AuctionManagerCallback,
  private val backgroundThread: BackgroundThread,
  private val uiThread: UIThread
) {
  private val adViews: MutableMap<String, AdServerAdView?> = mutableMapOf()
  private val extantExtras: MutableMap<String, AuctionRequest> = mutableMapOf()

  /**
   * Add header bids to the given PublisherAdRequest. This sets customTargeting
   * on the ad request, which will target line items set up in DFP.
   *
   * @param adView PublisherAdView instance that will be loading the ad request
   * @param adRequest PublisherAdRequest for this adUnit
   * @return a new PublisherAdRequest with the custom targeting required for header bidding
   */
  fun addBids(
    adView: AdServerAdView,
    adRequest: AdServerAdRequest
  ): AdServerAdRequest {
    return try {
      addBidsToPublisherAdRequest(adView, adRequest)
    } catch (e: Exception) {
      adRequest
    }
  }

  protected fun addBids(
    adView: AdServerAdView,
    adRequest: AdServerAdRequest,
    adUnitId: String?
  ): AdServerAdRequest? {
    adView.adUnitId = adUnitId!!
    return addBids(adView, adRequest)
  }

  /**
   * Add header bids to the given PublisherAdRequest, blocking for the given timeout to allow
   * AppMonetBidder time to fetch bids if no bids are present in the bid cache.
   *
   * @param adView PublisherAdView that will load the PublisherAdRequest
   * @param adRequest PublisherAdRequest request instance for the given adView
   * @param timeout int milliseconds to wait for a bid from AppMonetBidder bidder
   * @param onDone ValueCallback to receiev the request with bids attached
   */
  fun addBids(
    adView: AdServerAdView,
    adRequest: AdServerAdRequest,
    timeout: Int,
    onDone: Callback<AdServerAdRequest>
  ) {
    try {
      addBidsToPublisherAdRequest(adView, adRequest, timeout, onDone)
    } catch (e: Exception) {
      onDone(adRequest)
    }
  }

  protected fun addBids(
    adView: AdServerAdView,
    adRequest: AdServerAdRequest,
    adUnitId: String?,
    timeout: Int,
    onDone: Callback<AdServerAdRequest>
  ) {
    adView.adUnitId = adUnitId!!
    addBids(adView, adRequest, timeout, onDone)
  }

  private fun addBidsToPublisherAdRequest(
    adView: AdServerAdView,
    otherRequest: AdServerAdRequest
  ): AdServerAdRequest {
    // fetch a bid from our backend & attach it's KVPs to the
    // request
    if (BaseManager.isTestMode) {
      sLogger.warn(TEST_MODE_WARNING)
    }
    registerView(adView, otherRequest)
    val request = attachBid(adView, otherRequest)
    auctionManagerCallback.trackRequest(adView.adUnitId, "addBids")
    if (request == null) {
      addBidsNoFill(adView.adUnitId)
      sLogger.debug("no bid received")
      return otherRequest
    }
    return buildRequest(request, adView.type)
  }

  private fun addBidsToPublisherAdRequest(
    adView: AdServerAdView,
    otherRequest: AdServerAdRequest,
    timeout: Int,
    onDone: Callback<AdServerAdRequest>
  ) {
    registerView(adView, otherRequest)
    if (BaseManager.isTestMode) {
      sLogger.warn(TEST_MODE_WARNING)
    }
    backgroundThread.execute {
      attachBidAsync(
          adView, otherRequest, timeout
      ) { value: AuctionRequest? ->
        uiThread.run(object : InternalRunnable() {
          override fun runInternal() {
            auctionManagerCallback.trackRequest(adView.adUnitId, "addBidsAsync")
            if (value == null) {
              sLogger.info("no bid returned from js")
              addBidsNoFill(adView.adUnitId)
              onDone(otherRequest)
              return
            }
            val newRequest = buildRequest(value, adView.type)
            sLogger.debug("passing bid to main thread")
            addBidsNoFill(adView.adUnitId)
            onDone(newRequest)
          }

          override fun catchException(e: Exception?) {
            onDone(otherRequest)
          }
        })
      }
    }
  }

  fun cancelRequest(
    adUnitId: String?,
    adRequest: AdServerAdRequest,
    bid: BidResponse?
  ) {
    if (adUnitId == null || adRequest == null) {
      return
    }
    if (!adViews.containsKey(adUnitId)) {
      return
    }
    val adView = adViews[adUnitId]
    if (adView == null) {
      sLogger.warn("could not associate adview for next request")
      return
    }

    // the request that we get has minimal targeting.
    // we need to create a new request from merging that
    // with what we have in sExtantExtras
    var extant = extantExtras[adUnitId]
    extant = extant ?: from(adView, adRequest)
    val request = adServerWrapper.newAdRequest(adRequest.apply(extant, adView))
    auctionManagerCallback.trackRequest(adUnitId, "addBidRefresh")
    if (bid != null) {
      sLogger.info("attaching next bid", bid.toString())
      val req = addRawBid(adView, request, bid)
      req?.let {
        adView.loadAd(buildRequest(req, adView.type))
        return
      }
    }
    sLogger.debug("passing request")
    adView.loadAd(request)
  }

  /**
   * Given a JSON response from our AuctionManager WebView, retrieve the bids we have for that
   * adunit and attach them to the given adView/adRequest
   *
   * @param adView the adView where the ad will be rendered
   * @param adRequest the request for ad impressions
   * @return an AuctionRequest with the demand attached
   */
  fun attachBidResponse(
    adView: AdServerAdView,
    adRequest: AdServerAdRequest
  ): AuctionRequest? {
    val bid = bidManager.getBidForAdUnit(adView.adUnitId)
    return attachBid(adView, adRequest, bid)
  }

  /**
   * Add a bid to the given request/AdView pair.
   *
   * @param adView the view the bid will be rendered in
   * @param adRequest the request for an ad
   * @return an AuctionRequest representing the bid & the initial request (adRequest)
   */
  private fun attachBid(
    adView: AdServerAdView,
    adRequest: AdServerAdRequest
  ): AuctionRequest? {
    val bidResponse =
      if (!bidManager.needsNewBids(adView, adRequest)) adRequest.bid else bidManager.getLocalBid(
          adView.adUnitId
      )
    val data = RequestData(adRequest, adView)
    if (bidResponse != null) {
      sLogger.debug("(sync) attaching bids to request")
      sLogger.debug("\t[sync/request] attaching:\$bidResponse")
      return attachBid(adView, adRequest, bidResponse)
    }
    auctionManagerCallback.executeJs(
        JSMethods.FETCH_BIDS,
        WebViewUtils.quote(adView.adUnitId), data.toJson()
    )
    return attachBidResponse(adView, adRequest)
  }

  private fun attachBid(
    adView: AdServerAdView,
    adRequest: AdServerAdRequest,
    bidResponse: BidResponse?
  ): AuctionRequest? {
    if (bidResponse == null) {
      return null
    }
    val auctionRequest = from(adView, adRequest)
    val kwStrings = mutableListOf<String>()
    kwStrings.add(bidResponse.keywords)
    attachBidToNetworkExtras(auctionRequest.networkExtras, bidResponse)
    auctionRequest.bid = bidResponse
    val kwTargeting = keywordStringToMap(TextUtils.join(",", kwStrings))
    val targeting = auctionRequest.targeting.toMutableMap()
    targeting.putAll(kwTargeting)
    auctionRequest.targeting = targeting
    return auctionRequest
  }

  /**
   * Convert a formatted string into a bundle (of string:string mappings). The input string should
   * be in this format:
   *
   *
   * key1:value,key2:value2,key3:value3
   *
   * @param kwString a string of keywords in the expected format
   * @return a Bundle with string keys & String values
   */
  private fun keywordStringToMap(kwString: String): Map<String, String> {
    val map = mutableMapOf<String, String>()
    for (kvp in TextUtils.split(kwString, ",")) {
      val pair = TextUtils.split(kvp, ":")
      if (pair.size != 2) {
        continue
      }
      map[pair[0]] = pair[1]
    }
    return map
  }

  /**
   * Attach the bid to the network Extras bundle, eg the bundle that will be passed in the
   * customEventBanner.
   *
   * @param bundle the Bundle the bid should be attached to
   * @param bid the BidResponse to be passed into the adserver.
   */
  private fun attachBidToNetworkExtras(
    bundle: MutableMap<String, Any>?,
    bid: BidResponse?
  ) {
    if (bid == null || bundle == null) {
      return
    }
    bundle[BID_BUNDLE_KEY] = BidResponse.Mapper.toJsonString(bid)
  }

  private fun attachBidAsync(
    adView: AdServerAdView,
    adRequest: AdServerAdRequest,
    timeout: Int,
    callback: Callback<AuctionRequest?>
  ) {
    val adUnitId = adView.adUnitId
    val data = RequestData(adRequest, adView)
    if (!bidManager.needsNewBids(adView, adRequest)) {
      sLogger.debug("keeping current bids")
      callback(attachBid(adView, adRequest, adRequest.bid))
      return
    }
    val config = auctionManagerCallback.getSdkConfigurations()
    if (config.getBoolean(Configurations.SKIP_FETCH)
        && bidManager.hasLocalBids(adView.adUnitId)
    ) {
      sLogger.debug("Skipping fetch wait (latency reduction)")
      val bid = bidManager.getLocalBid(adView.adUnitId)
      callback(attachBid(adView, adRequest, bid))
    } else {
      // get the timeout
      val realTimeout = resolveTimeout(config, timeout)
      auctionManagerCallback.executeJs(
          realTimeout, JSMethods.FETCH_BIDS_BLOCKING,
          {
            val bid = bidManager.getLocalBid(adView.adUnitId)
            if (bid != null) {
              sLogger.debug("attaching bids to request")
            }
            callback(attachBid(adView, adRequest, bid))
          },
          WebViewUtils.quote(adUnitId), timeout.toString(),
          data.toJson(), "'addBids'"
      )
    }
  }

  /**
   * Get a raw bid available for this adunit
   */
  fun fetchRawBid(adUnitId: String?): BidResponse? {
    return bidManager.getLocalBid(adUnitId)
  }

  private fun resolveTimeout(
    config: SdkConfigurations?,
    timeout: Int
  ): Int {
    val realTimeout =
      if (config != null && config.hasKey(Configurations.FETCH_TIMEOUT_OVERRIDE)) config.getInt(
          Configurations.FETCH_TIMEOUT_OVERRIDE
      ) else timeout
    return if (realTimeout <= 0) timeout else realTimeout
  }

  private fun registerView(
    adView: AdServerAdView?,
    request: AdServerAdRequest?
  ) {
    if (adView == null) {
      return
    }
    val adUnitId = adView.adUnitId
    adViews[adUnitId] = adView
    if (request == null) {
      return
    }
    extantExtras[adUnitId] = from(adView, request)
  }

  private fun addRawBid(
    adView: AdServerAdView,
    baseRequest: AdServerAdRequest,
    bid: BidResponse
  ): AuctionRequest? {
    return attachBid(adView, baseRequest, bid)
  }

  private fun addBidsNoFill(adUnitId: String) {
    auctionManagerCallback.trackEvent(
        "addbids_nofill", "null",
        adUnitId, 0f, System.currentTimeMillis()
    )
  }

  private fun buildRequest(
    req: AuctionRequest,
    type: AdType
  ): AdServerAdRequest {
    return adServerWrapper.newAdRequest(req, type)
  }

  companion object {
    private val sLogger = Logger("Bdr")
  }

}