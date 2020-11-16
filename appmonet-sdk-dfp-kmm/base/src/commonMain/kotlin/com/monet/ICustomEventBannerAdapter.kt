package com.monet

import com.monet.AdServerBannerListener.ErrorCode
import com.monet.AdType.BANNER
import com.monet.MediationManager.NoBidsFoundException
import com.monet.MediationManager.NullBidException
import com.monet.adview.AdSize
import com.monet.auction.AuctionManagerKMM

typealias RenderingAdapter = (bid: BidResponse, adSize: AdSize) -> IAppMonetViewLayout?

interface ICustomEventBannerAdapter {
  val adSize: AdSize
  val baseManager: IBaseManager?
  val extras: Map<String, Any?>?
  val serverParameter: String?
  val mediationManager: MediationManager?
  val bidManager: IBidManager?
  val auctionManager: AuctionManagerKMM?
  val customEventListenerWrapper: AdServerBannerListener<*>
  val bannerRequest: Any
  val isDfpRequest: Boolean
  val renderingAdapter: RenderingAdapter
  var adView: IAppMonetViewLayout?
  fun requestBanner() {
    if (baseManager == null) {
      loadError(customEventListenerWrapper,ErrorCode.INTERNAL_ERROR)
      return
    }
    val adUnitId = DFPAdRequestHelper.getAdUnitID(extras, serverParameter, adSize)
    if (adUnitId == null) {
//      logger.warn("load failed: invalid bid data")
      loadError(customEventListenerWrapper, ErrorCode.INTERNAL_ERROR)
      return
    }

    auctionManager?.trackRequest(adUnitId, Util.generateTrackingSource(BANNER))
    var bid = DFPAdRequestHelper.getBidFromRequest(extras)
    val floorCpm = DFPAdRequestHelper.getCpm(serverParameter)
    if (bid == null || bid.id.isEmpty()) {
      bid = mediationManager?.getBidForMediation(adUnitId, floorCpm)
    }
    try {
      bid = mediationManager?.getBidReadyForMediation(
          bid, adUnitId, adSize, BANNER, floorCpm, true
      )

      // this will set adview
      try {
        tryToAttachDemand(bid, adUnitId, bannerRequest)
      } catch (e: Exception) {
//        logger.warn("unable to attach upcoming demand", e.message)
      }
      bid?.let {
        adView = renderingAdapter(it, adSize)
      }
      if (adView == null) {
        loadError(customEventListenerWrapper, ErrorCode.INTERNAL_ERROR)
      }
    } catch (e: NoBidsFoundException) {
      loadError(customEventListenerWrapper, ErrorCode.NO_FILL)
    } catch (e: NullBidException) {
      loadError(customEventListenerWrapper, ErrorCode.INTERNAL_ERROR)
    }

  }

  fun destroy()

  fun tryToAttachDemand(
    bid: BidResponse?,
    adUnitId: String,
    adRequest: Any
  )

  private fun loadError(
    listener: AdServerBannerListener<*>?,
    errorCode: ErrorCode
  ) {
    if (listener == null) {
      return
    }
    listener.onAdError(errorCode)
  }
}