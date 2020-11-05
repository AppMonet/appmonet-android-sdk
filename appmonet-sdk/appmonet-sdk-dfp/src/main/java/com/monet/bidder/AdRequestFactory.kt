package com.monet.bidder

import com.google.android.gms.ads.mediation.MediationAdRequest
import com.monet.AdServerAdRequest
import com.monet.DFPAdRequest
import com.monet.DFPAdViewRequest
import com.monet.PublisherAdRequestWrapper
import com.monet.auction.AuctionRequest
import com.google.android.gms.ads.doubleclick.PublisherAdRequest.Builder
import com.monet.AdRequestWrapper
import com.monet.fromAuctionRequest

internal object AdRequestFactory {
  fun fromAuctionRequest(
    isPublisherAdView: Boolean,
    auctionRequest: AuctionRequest
  ): AdServerAdRequest {
    return if (isPublisherAdView) {
      DFPAdRequest.fromAuctionRequest(
          auctionRequest, MonetDfpCustomEventInterstitial::class.java,
          CustomEventBanner::class.java, CustomEventBanner::class.java,
          CustomEventInterstitial::class.java
      )
    } else DFPAdViewRequest.fromAuctionRequest(
        auctionRequest, MonetDfpCustomEventInterstitial::class.java,
        CustomEventBanner::class.java, CustomEventBanner::class.java,
        CustomEventInterstitial::class.java
    )
  }

  fun createEmptyRequest(isPublisherAdView: Boolean): AdServerAdRequest {
    return if (isPublisherAdView) {
      DFPAdRequest(PublisherAdRequestWrapper(Builder().build()))
    } else DFPAdViewRequest(
        AdRequestWrapper(com.google.android.gms.ads.AdRequest.Builder().build())
    )
  }

  fun fromMediationRequest(
    isPublisherAdView: Boolean,
    adRequest: MediationAdRequest
  ): AdServerAdRequest {
    return if (isPublisherAdView) {
      val dfpRequest = Builder()
          .setBirthday(adRequest.birthday)
          .setGender(adRequest.gender)
          .setLocation(adRequest.location)
          .build()
      DFPAdRequest(PublisherAdRequestWrapper(dfpRequest))
    } else {
      val req = com.google.android.gms.ads.AdRequest.Builder()
          .setBirthday(adRequest.birthday)
          .setGender(adRequest.gender)
          .setLocation(adRequest.location)
          .build()
      DFPAdViewRequest(AdRequestWrapper(req))
    }
  }
}