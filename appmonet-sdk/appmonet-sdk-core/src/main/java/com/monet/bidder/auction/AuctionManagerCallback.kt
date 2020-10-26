package com.monet.bidder.auction

import com.monet.Callback
import com.monet.DeviceData
import com.monet.bidder.*
import com.monet.bidder.adview.AdViewManager
import com.monet.BidResponse
import com.monet.AdServerAdRequest
/**
 * Interface to be implemented for controlling and retrieving aspects of an auction.
 */
interface AuctionManagerCallback {

  /**
   * Retrieves an instance of [MediationManager].
   */
  val mediationManager: MediationManager

  /**
   * Device's advertising id.
   */
  var advertisingId: String

  /**
   * The implementation of this will cancel a particular request.
   *
   * @param adUnitId The ad unit of a particular request.
   * @param adRequest The adRequest to cancel.
   * @param bid The bid associated to the request to be canceled.
   */
  fun cancelRequest(
    adUnitId: String?,
    adRequest: AdServerAdRequest,
    bid: BidResponse?
  )

  /**
   * Executes dynamic JS code.
   *
   * @param code Code to be executed
   */
  fun executeCode(code: String)

  /**
   *  Execute a JS method with optional parameters.
   *
   *  @param method The method name to be executed in JS.
   *  @param args The optional arguments to be passed to JS.
   */
  fun executeJs(
    method: String,
    vararg args: String
  )

  /**
   *  Execute a JS method with optional parameters asynchronously.
   *  This includes a timeout of execution
   *
   *  @param timeout The timeout to be used for waiting for JS to come back.
   *  @param method The method name to be executed in JS.
   *  @param callback The callback containing a response back from JS.
   *  @param args The optional arguments to be passed to JS.
   */
  fun executeJs(
    timeout: Int,
    method: String,
    callback: Callback<String?>?,
    vararg args: String
  )

  /**
   * Async way to retrieve advertising information.
   */
  fun getAdvertisingInfo()

  /**
   * Retrieve a [BidResponse] using it's [BidResponse.id]
   *
   * @param bidId This is the id to be used to retrieve a [BidResponse].
   */
  fun getBidById(bidId: String): BidResponse?

  /**
   * Retrieve a [BidResponse] associated with an ad unit that has a particular cpm.
   *
   * @param adUnitId This is the ad unit id to be used to retrieve a [BidResponse].
   * @param floorCpm  This is the bottom cpm to be used to retrieve a [BidResponse].
   *
   * @return [BidResponse]
   */
  fun getBidWithFloorCpm(
    adUnitId: String,
    floorCpm: Double
  ): BidResponse?

  /**
   * Retrieves an instance of [DeviceData].
   *
   * @return [DeviceData]
   */
  fun getDeviceData(): DeviceData

  /**
   * Retrieves an instance of [AppMonetWebView].
   *
   * @return [AppMonetWebView]
   */
  fun getMonetWebView(): AppMonetWebView?

  /**
   * Retrieves an instance of [SdkConfigurations].
   *
   * @return [SdkConfigurations]
   */
  fun getSdkConfigurations(): SdkConfigurations

  /**
   * This method should inform the auction that an [AdView] was created
   *
   * @param uuid The UUID of the [AdView]
   * @param adViewContextJson adView context as a JSON string.
   */
  fun helperCreated(
    uuid: String,
    adViewContextJson: String
  )

  /**
   *  This method should inform the auction when an [AdView] is about to be destroyed.
   *
   *  @param wvUUID The UUID of the [AdView]
   */
  fun helperDestroy(wvUUID: String)

  /**
   * This method should inform the auction that an [AdView] loaded completely.
   *
   * @param uuid The UUID of the [AdView].
   */
  fun helperLoaded(uuid: String)

  /**
   * This method should invalidate the bids of a particular [AdView].
   *
   * @param uuid The UUID of the [AdView].
   */
  fun invalidateBidsForAdView(uuid: String)

  /**
   * This method should be called when requesting for an [AdView].
   *
   * @param url The [AdView] url to be loaded.
   * @param ua The user agent to be set by the [AdView].
   * @param html The html content to be loaded by the [AdView].
   * @param width The [AdView] width.
   * @param height The [AdView] height.
   * @param adUnitId The ad unit id related to the [AdView].
   */
  fun loadHelper(
    url: String,
    ua: String,
    html: String,
    width: Int,
    height: Int,
    adUnitId: String,
    onLoad: Callback<AdViewManager>
  )

  /**
   * The implementation of this method will be responsible of marking a bid as used.
   *
   * @param bid [BidResponse] to be marked as used.
   */
  fun markBidAsUsed(bid: BidResponse)

  /**
   * This method should be called when the auction has fully initialized.
   */
  fun onInit()
  fun removeHelper(uuid: String): Boolean
  fun requestHelperDestroy(uuid: String): Boolean
  fun trackEvent(
    event: String,
    detail: String,
    key: String,
    value: Float,
    currentTime: Long
  )

  fun trackRequest(
    adUnitId: String,
    source: String
  )

  fun removeBid(bidId: String): BidResponse?
}