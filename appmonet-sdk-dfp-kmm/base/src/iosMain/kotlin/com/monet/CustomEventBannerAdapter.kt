package com.monet


import com.monet.AdRequestAdapter.fromMediationRequest
import com.monet.adview.AdSize
import com.monet.auction.AuctionManagerKMM

class CustomEventBannerAdapter(
  override val adSize: AdSize,
  override val baseManager: IBaseManager?,
  override val extras: Map<String, Any?>?,
  override val serverParameter: String?,
  override val mediationManager: MediationManager?,
  override val bidManager: IBidManager?,
  override val auctionManager: AuctionManagerKMM?,
  override val customEventListenerWrapper: AdServerBannerListener<*>,
  override val isDfpRequest: Boolean,
  override val bannerRequest: Any,
  override val renderingAdapter: RenderingAdapter,
) : ICustomEventBannerAdapter {

  override var adView: IAppMonetViewLayout? = null

  override fun destroy() {
    if (adView != null) {
      try {
        adView?.destroyAdView(true)
      } catch (e: Exception) {
//        logger.warn("error destroying ceb - ", e.message)
      }
    }
  }

  override fun tryToAttachDemand(
    bid: BidResponse?,
    adUnitId: String,
    adRequest: Any
  ) {
    if (bid?.nextQueue == false) {
//      logger.debug("automatic refresh is disabled. Skipping queue next (clearing bids)")
      auctionManager?.cancelRequest(
          adUnitId, fromMediationRequest(isDfpRequest, adRequest), null
      )
      return
    }
    val nextBid = bidManager?.peekNextBid(adUnitId)
    if (nextBid == null || bidManager?.isValid(nextBid) == false) {
      auctionManager?.cancelRequest(
          adUnitId,
          fromMediationRequest(isDfpRequest, adRequest), null
      )
    } else {
      auctionManager?.cancelRequest(
          adUnitId,
          fromMediationRequest(isDfpRequest, adRequest), nextBid
      )
    }
  }
}