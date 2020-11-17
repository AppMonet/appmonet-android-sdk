package com.monet

import com.monet.adview.AdSize
import com.monet.auction.AuctionManagerKMM

class CustomEventInterstitialAdapter(
  override val customEventExtras: Map<String, Any?>?,
  override val serverParameter: String?,
  override val adSize: AdSize,
  override val customEventListenerWrapper: AdServerBannerListener<*>?,
  override val sdkManager: IBaseManager?,
  override val auctionManager: AuctionManagerKMM?,
  override val bidManager: IBidManager?,
  override val mediationManager: MediationManager?,
  override val renderingAdapter: RenderingInterstitialAdapter
) : ICustomEventInterstitialAdapter {
  override var adView: IAppMonetViewLayout? = null
  override var currentBid: BidResponse? = null
  override val adUnitId: String?
    get() = DFPAdRequestHelper.getAdUnitID(customEventExtras, serverParameter, adSize)

}