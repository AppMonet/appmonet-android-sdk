package com.monet.bidder

import com.monet.bidder.AdServerWrapper.Type
import com.monet.bidder.AdServerWrapper.Type.INTERSTITIAL
import com.monet.bidder.AdServerWrapper.Type.NATIVE
import com.monet.bidder.auction.AuctionRequest

internal class MoPubAdServerWrapper : AdServerWrapper {
  override fun newAdRequest(auctionRequest: AuctionRequest): AdServerAdRequest? {
    return null
  }

  override fun newAdRequest(
    auctionRequest: AuctionRequest,
    type: Type
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