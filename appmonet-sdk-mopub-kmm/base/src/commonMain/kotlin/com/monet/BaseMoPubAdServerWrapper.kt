package com.monet

import com.monet.adview.AdSize
import com.monet.AdType.INTERSTITIAL
import com.monet.AdType.NATIVE
import com.monet.auction.AuctionRequest

interface BaseMoPubAdServerWrapper : AdServerWrapper {
  override fun newAdRequest(
    auctionRequest: AuctionRequest,
    type: AdType
  ): AdServerAdRequest {
    return when (type) {
      INTERSTITIAL -> {
        MoPubInterstitialAdRequest.fromAuctionRequest(auctionRequest)
      }
      NATIVE -> {
        MopubNativeAdRequest.fromAuctionRequest(auctionRequest)
      }
      else -> {
        MoPubAdRequest.fromAuctionRequest(auctionRequest)
      }
    }
  }

  override fun newAdRequest(): AdServerAdRequest {
    return MoPubAdRequest()
  }

  override fun newAdSize(
    width: Int,
    height: Int
  ): AdSize {
    return AdSize(width, height)
  }
}