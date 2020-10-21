package com.monet.bidder

import android.location.Location
import android.os.Bundle
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.doubleclick.PublisherAdRequest
import com.google.android.gms.ads.doubleclick.PublisherAdRequest.Builder
import com.google.android.gms.ads.mediation.MediationAdRequest
import com.google.android.gms.ads.mediation.admob.AdMobExtras
import com.monet.bidder.MonetDfpCustomEventInterstitial
import com.monet.bidder.auction.AuctionRequest
import com.monet.BidResponse
import java.util.Date

internal class DFPAdRequest : AdServerAdRequest {
  val dfpRequest: PublisherAdRequest

  /**
   * Build a DFPAdRequest from a PublisherAdRequest, the DFP representation of an ad request
   *
   * @param adRequest the request constructed by the publisher, to be sent to DFP
   */
  constructor(adRequest: PublisherAdRequest) {
    dfpRequest = adRequest
  }

  /**
   * Build a "new"/blank request, using an empty PublisherAdRequest
   */
  constructor() {
    dfpRequest = Builder().build()
  }

  /**
   * Build a DFPAdRequest from a MediationAdRequest. The MediationAdRequest
   * is what's passed to use in [CustomEventBanner.requestBannerAd].
   * We instantiate a DFPAdRequest there in order to queue up further bids.
   *
   * @param mediationRequest a MediationAdRequest representing the current request cycle in DFP
   */
  constructor(mediationRequest: MediationAdRequest) {
    dfpRequest = Builder()
        .setBirthday(mediationRequest.birthday)
        .setGender(mediationRequest.gender)
        .setLocation(mediationRequest.location)
        .build()
  }

  private val adMobExtras: Bundle
    get() {
      try {
        val extras = dfpRequest.getNetworkExtras(
            AdMobExtras::class.java
        )
        if (extras != null) {
          return extras.extras
        }
      } catch (e: Exception) {
      }
      return Bundle()
    }

  // also get the admob extras and merge it here
  override val customTargeting: Bundle
    // create a bundle merging both
    get() {
      // also get the admob extras and merge it here
      val extras = adMobExtras
      val targeting = dfpRequest.customTargeting

      // create a bundle merging both
      val merged = Bundle()
      merged.putAll(extras)
      merged.putAll(targeting)
      return merged
    }

  override val birthday: Date?
    get() = dfpRequest.birthday

  override val gender: String
    get() = when (dfpRequest.gender) {
      PublisherAdRequest.GENDER_FEMALE -> "female"
      PublisherAdRequest.GENDER_MALE -> "male"
      else -> "unknown"
    }

  override val bid: BidResponse?
    get() = null

  override val location: Location?
    get() = dfpRequest.location

  override val contentUrl: String?
    get() = dfpRequest.contentUrl

  override fun apply(
    request: AuctionRequest,
    adView: AdServerAdView
  ): AuctionRequest {
    // transfer admob extras if they're there
    try {
      val adMob = dfpRequest.getNetworkExtrasBundle(
          AdMobAdapter::class.java
      )
      val admobExtras = dfpRequest.getNetworkExtras(
          AdMobExtras::class.java
      )
      request.admobExtras.putAll(
          filterTargeting(
              if (admobExtras != null) admobExtras.extras else adMob
          )
      )
    } catch (e: Exception) {
      // do nothing
    }
    if (request.requestData == null) {
      request.requestData = RequestData(this, adView)
    }
    request.targeting.putAll(
        filterTargeting(
            dfpRequest.customTargeting
        )
    )
    return request
  }

  override val publisherProvidedId: String?
    get() = dfpRequest.publisherProvidedId

  companion object {
    fun fromAuctionRequest(request: AuctionRequest): DFPAdRequest {
      val builder = Builder()
          .addCustomEventExtrasBundle(CustomEventBanner::class.java, request.networkExtras)
          .addCustomEventExtrasBundle(CustomEventInterstitial::class.java, request.networkExtras)
          .addCustomEventExtrasBundle(
              MonetDfpCustomEventInterstitial::class.java,
              request.networkExtras
          )
          .addNetworkExtrasBundle(CustomEventBanner::class.java, request.networkExtras)

      // copy over the targeting
      for (key in request.targeting.keySet()) {
        val value = request.targeting[key] ?: continue
        if (value is List<*>) {
          val listValue = value as List<String>
          builder.addCustomTargeting(key, listValue)
        } else {
          builder.addCustomTargeting(key, value.toString())
        }
      }
      if (request.requestData != null) {
        if (!request.requestData!!.contentURL.isNullOrEmpty()) {
          builder.setContentUrl(request.requestData!!.contentURL)
        }
        builder.setLocation(request.requestData!!.location)
        for ((key, value) in request.requestData!!.additional) {
          builder.addCustomTargeting(key, value)
        }
      }

      // add in the admob extras
      val completeExtras = request.admobExtras
      completeExtras.putAll(request.targeting)
      try {
        builder.addNetworkExtras(AdMobExtras(completeExtras))
      } catch (e: Exception) {
        // do nothing
      }
      return DFPAdRequest(builder.build())
    }
  }
}