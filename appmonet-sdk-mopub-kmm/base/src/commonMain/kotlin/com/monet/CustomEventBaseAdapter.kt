package com.monet

import com.monet.AdServerBannerListener.ErrorCode
import com.monet.AdServerBannerListener.ErrorCode.INTERNAL_ERROR
import com.monet.AdServerBannerListener.ErrorCode.NO_FILL
import com.monet.CustomEventUtil.getAdUnitId
import com.monet.adview.AdSize
import com.monet.auction.AuctionManagerKMM

typealias RenderingAdapter = (bid: BidResponse?, adSize: AdSize, customEventAdapter: CustomEventBaseAdapter) -> IAppMonetViewLayout?

abstract class CustomEventBaseAdapter(
  val adSize: AdSize,
  val sdkManager: IBaseManager?,
  val auctionManager: AuctionManagerKMM?,
  val mediationManager: MediationManager?,
  val adServerBannerListener: AdServerBannerListener<*>,
  val extras: Map<String, String?>,
  val mediationDefaultFloor: Double,
  val adType: AdType
) {
  val adUnitId: String?
    get() = getAdUnitId(extras, adSize)
  var adView: IAppMonetViewLayout? = null
  fun requestAd(
    renderingAdapter: RenderingAdapter,
    configurationTimeout: Int? = 0
  ) {
    if (sdkManager == null) {
      onError(INTERNAL_ERROR)
      return
    }
    if (adUnitId == null || adUnitId?.isEmpty() == true) {
//      sLogger.debug("no adUnit/tagId: floor line item configured incorrectly")
      onError(NO_FILL)
      return
    }
    auctionManager?.trackRequest(adUnitId!!, Util.generateTrackingSource(adType))
    var headerBiddingBid: BidResponse? = CustomEventUtil.getBid(extras)
    val floorCpm = CustomEventUtil.getServerExtraCpm(extras, mediationDefaultFloor)
    if (headerBiddingBid == null) {
      headerBiddingBid = mediationManager?.getBidForMediation(adUnitId, floorCpm)
    }
    getFinalBid(headerBiddingBid, adUnitId!!, floorCpm, configurationTimeout, renderingAdapter)
  }

  abstract fun getFinalBid(
    headerBiddingBid: BidResponse?,
    adUnitId: String,
    floorCpm: Double,
    configurationTimeout: Int?,
    renderingAdapter: RenderingAdapter
  )

  fun onError(error: ErrorCode) {
    adServerBannerListener.onAdError(error)
  }
}