package com.monet

import com.monet.AdServerBannerListener.ErrorCode.INTERNAL_ERROR
import com.monet.AdServerBannerListener.ErrorCode.NO_FILL
import com.monet.AdType.BANNER
import com.monet.MediationManager.NoBidsFoundException
import com.monet.MediationManager.NullBidException
import com.monet.adview.AdSize
import com.monet.auction.AuctionManagerKMM

class CustomEventBannerAdapter(
  adSize: AdSize,
  sdkManager: IBaseManager?,
  auctionManager: AuctionManagerKMM?,
  mediationManager: MediationManager?,
  adServerBannerListener: AdServerBannerListener<*>,
  extras: Map<String, String?>,
  mediationDefaultFloor: Double,
  adType: AdType
) : CustomEventBaseAdapter(
    adSize, sdkManager, auctionManager, mediationManager, adServerBannerListener, extras,
    mediationDefaultFloor, adType
) {

  override fun getFinalBid(
    headerBiddingBid: BidResponse?,
    adUnitId: String,
    floorCpm: Double,
    configurationTimeout: Int?,
    renderingAdapter: RenderingAdapter
  ) {
    try {
      val bid = mediationManager?.getBidReadyForMediation(
          headerBiddingBid, adUnitId, adSize,
          BANNER, floorCpm, true
      )
      adView = renderingAdapter(bid, adSize, this)
      if (adView == null) {
//        sLogger.error("unexpected: could not generate the adView")
        onError(INTERNAL_ERROR)
      }
    } catch (e: NoBidsFoundException) {
      onError(NO_FILL)
    } catch (e: NullBidException) {
      onError(INTERNAL_ERROR)
    }

  }

}