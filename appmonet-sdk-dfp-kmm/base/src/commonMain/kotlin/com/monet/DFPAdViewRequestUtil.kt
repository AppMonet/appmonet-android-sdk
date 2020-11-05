package com.monet

import com.monet.auction.AuctionRequest

expect class DFPAdViewRequestUtil {
  companion object {
    fun apply(
      adRequestWrapper: RequestWrapper<*>,
      request: AuctionRequest,
      adView: AdServerAdView,
      adServerAdRequest: AdServerAdRequest
    ): AuctionRequest
  }
}