package com.monet.bidder.bid

import android.content.Context
import com.monet.bidder.AdServerBannerListener
import com.monet.bidder.AdSize
import com.monet.bidder.AppMonetViewLayout
import com.monet.bidder.BaseManager
import com.monet.bidder.Constants.TEST_MODE_WARNING
import com.monet.bidder.Logger
import com.monet.bidder.adview.AdViewManager.AdViewState.AD_RENDERED
import com.monet.BidResponse
object BidRenderer {
  private val sLogger = Logger("Renderer")
  @JvmStatic fun renderBid(
    context: Context,
    sdkManager: BaseManager,
    bidResponse: BidResponse,
    adSize: AdSize?,
    listener: AdServerBannerListener
  ): AppMonetViewLayout? {
    sLogger.info("Rendering bid:", bidResponse.toString())
    if (!sdkManager.auctionManager.bidManager.isValid(bidResponse)) {
      sdkManager.auctionManager.trackEvent(
          "bidRenderer",
          "invalid_bid", bidResponse.id, 0f, System.currentTimeMillis()
      )
      return null
    }
    val adViewManager = sdkManager.auctionManager.adViewPoolManager.request(bidResponse)
    if (adViewManager == null) {
      sLogger.warn("fail to attach adView. Unable to serve")
      sdkManager.auctionManager.trackEvent(
          "bidRenderer",
          "null_view", bidResponse.id, 0f, System.currentTimeMillis()
      )
      return null
    }
    if (!adViewManager.isLoaded) {
      // load sdk.js
      sLogger.debug("Initializing AdView for injection")
      adViewManager.load()
    }
    sdkManager.auctionManager.bidManager.markUsed(bidResponse)
    adViewManager.bid = bidResponse
    adViewManager.bidForTracking = bidResponse
    adViewManager.setState(AD_RENDERED, listener, context)

    // this is always done after the state change
    sLogger.debug("injecting ad into view")
    adViewManager.inject(bidResponse)
    adViewManager.shouldAdRefresh = false
    if (adSize != null && adSize.width != 0 && adSize.height != 0 && bidResponse.flexSize) {
      adViewManager.resize(adSize)
    }
    if (BaseManager.isTestMode) {
      sLogger.warn(TEST_MODE_WARNING)
    }
    return adViewManager.adView.parent as AppMonetViewLayout
  }
}