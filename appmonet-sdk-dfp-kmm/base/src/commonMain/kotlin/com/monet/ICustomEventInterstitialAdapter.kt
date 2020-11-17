package com.monet

import com.monet.AdServerBannerListener.ErrorCode.INTERNAL_ERROR
import com.monet.AdServerBannerListener.ErrorCode.NO_FILL
import com.monet.AdType.INTERSTITIAL
import com.monet.adview.AdSize
import com.monet.auction.AuctionManagerKMM

typealias RenderingInterstitialAdapter = (bid: BidResponse?) -> RenderingInterstitialAdapterResponse

data class RenderingInterstitialAdapterResponse(
  val view: IAppMonetViewLayout?,
  val ignoreAdView: Boolean = false
)

interface ICustomEventInterstitialAdapter {
  val customEventExtras: Map<String, Any?>?
  val serverParameter: String?
  val adSize: AdSize
  val customEventListenerWrapper: AdServerBannerListener<*>?
  val sdkManager: IBaseManager?
  val auctionManager: AuctionManagerKMM?
  val bidManager: IBidManager?
  val renderingAdapter: RenderingInterstitialAdapter
  val mediationManager: MediationManager?
  var adView: IAppMonetViewLayout?
  var currentBid: BidResponse?
  val adUnitId: String?
  fun requestAd(configurationTimeOut: Int?) {
    if (sdkManager == null) {
//      logger.warn("AppMonet SDK Has not been initialized. Unable to serve ads.")
      customEventListenerWrapper?.onAdError(NO_FILL)
      return
    }

    if (adUnitId == null || adUnitId!!.isEmpty()) {
//      logger.debug("no adUnit/tagId: floor line item configured incorrectly")
      customEventListenerWrapper?.onAdError(NO_FILL)
      return
    }

    auctionManager?.trackRequest(adUnitId!!, Util.generateTrackingSource(INTERSTITIAL))
    var bid: BidResponse? = null
    if (serverParameter != null && serverParameter != adUnitId) {
      bid = DFPAdRequestHelper.getBidFromRequest(customEventExtras)
    }
    if (bid != null && bidManager?.isValid(bid) == true
        && customEventListenerWrapper != null
    ) {
//      logger.debug("bid from bundle is valid. Attaching!")
      setupBid(bid)
      return
    }
    val floorCpm = DFPAdRequestHelper.getCpm(serverParameter)
    if (bid == null || bid.id.isEmpty()) {
      bid = mediationManager?.getBidForMediation(adUnitId, floorCpm)
    }
    mediationManager?.getBidReadyForMediationAsync(
        bid, adUnitId!!, adSize, INTERSTITIAL,
        floorCpm,
        top@{ response, error ->
          if (error != null) {
            customEventListenerWrapper!!.onAdError(NO_FILL)
            return@top
          }
          if (response != null) {
            setupBid(response)
          }
        }, configurationTimeOut, 4000
    )
  }

  private fun setupBid(bid: BidResponse) {
    currentBid = bid
    val response = renderingAdapter(bid)
    adView = response.view
    if (!response.ignoreAdView && adView == null) {
//      logger.error("unexpected: could not generate the adView")
      customEventListenerWrapper?.onAdError(INTERNAL_ERROR)
    }
  }
}