package com.monet

import com.monet.AdServerBannerListener.ErrorCode.INTERNAL_ERROR
import com.monet.AdServerBannerListener.ErrorCode.NO_FILL
import com.monet.AdType.INTERSTITIAL
import com.monet.adview.AdSize
import com.monet.auction.AuctionManagerKMM

class CustomEventInterstitialAdapter(
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
  var bidResponse: BidResponse? = null

  override fun getFinalBid(
    headerBiddingBid: BidResponse?,
    adUnitId: String,
    floorCpm: Double,
    configurationTimeout: Int?,
    renderingAdapter: RenderingAdapter
  ) {
    mediationManager?.getBidReadyForMediationAsync(
        headerBiddingBid, adUnitId, adSize,
        INTERSTITIAL, floorCpm, top@{ response, error ->
      if (error != null) {
        onError(NO_FILL)
      }
      bidResponse = response
      adView = renderingAdapter(response, adSize, this)
      if (adView == null) {
//        logger.error("unexpected: could not generate the adView")
        onError(INTERNAL_ERROR)
      }
      if (bidResponse!!.interstitial != null && bidResponse!!.interstitial!!
              .trusted && Platform.platform == ANDROID
      ) {
        adServerBannerListener.onAdLoaded(null)
        return@top
      }

    }, configurationTimeout, 4000
    )
  }

}