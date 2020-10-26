package com.monet.bidder

import com.google.android.gms.ads.mediation.MediationAdRequest
import com.monet.AdServerAdRequest
import com.monet.auction.AuctionRequest

internal object AdRequestFactory {
  fun fromAuctionRequest(
    isPublisherAdView: Boolean,
    auctionRequest: AuctionRequest
  ): AdServerAdRequest {
    return if (isPublisherAdView) {
      DFPAdRequest.fromAuctionRequest(auctionRequest)
    } else DFPAdViewRequest.fromAuctionRequest(auctionRequest)
  }

  fun createEmptyRequest(isPublisherAdView: Boolean): AdServerAdRequest {
    return if (isPublisherAdView) {
      DFPAdRequest()
    } else DFPAdViewRequest()
  }

  fun fromMediationRequest(
    isPublisherAdView: Boolean,
    adRequest: MediationAdRequest
  ): AdServerAdRequest {
    return if (isPublisherAdView) {
      DFPAdRequest(adRequest)
    } else DFPAdViewRequest(adRequest)
  }
}