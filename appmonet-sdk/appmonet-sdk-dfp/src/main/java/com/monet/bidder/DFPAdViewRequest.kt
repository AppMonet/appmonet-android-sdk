package com.monet.bidder

import android.location.Location
import android.os.Bundle
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdRequest.Builder
import com.google.android.gms.ads.doubleclick.PublisherAdRequest
import com.google.android.gms.ads.mediation.MediationAdRequest
import com.google.android.gms.ads.mediation.admob.AdMobExtras
import com.monet.bidder.auction.AuctionRequest
import java.util.Date

internal class DFPAdViewRequest : AdServerAdRequest {
  val dfpRequest: AdRequest

  constructor() {
    dfpRequest = Builder().build()
  }

  constructor(adRequest: AdRequest) {
    dfpRequest = adRequest
  }

  constructor(mediationAdRequest: MediationAdRequest) {
    dfpRequest = Builder()
        .setBirthday(mediationAdRequest.birthday)
        .setGender(mediationAdRequest.gender)
        .setLocation(mediationAdRequest.location)
        .build()
  }

  private val adMobExtras: Bundle
    get() {
      try {
        val extras = dfpRequest.getNetworkExtrasBundle(
            AdMobAdapter::class.java
        )
        if (extras != null) {
          return extras
        }
      } catch (e: Exception) {
        //do nothing
      }
      return Bundle()
    }

  override fun getCustomTargeting(): Bundle? {
    // also get the admob extras and merge it here
    val extras = adMobExtras

    // create a bundle merging both
    val merged = Bundle()
    merged.putAll(extras)
    return merged
  }

  public override fun getBirthday(): Date {
    return dfpRequest.birthday
  }

  public override fun getGender(): String {
    return when (dfpRequest.gender) {
      PublisherAdRequest.GENDER_FEMALE -> "female"
      PublisherAdRequest.GENDER_MALE -> "male"
      else -> "unknown"
    }
  }

  public override fun getLocation(): Location {
    return dfpRequest.location
  }

  public override fun getContentUrl(): String {
    return dfpRequest.contentUrl
  }

  override fun apply(
    request: AuctionRequest,
    adView: AdServerAdView
  ): AuctionRequest {
    // transfer admob extras if they're there
    try {
      val adMob = dfpRequest.getNetworkExtrasBundle(
          AdMobAdapter::class.java
      )
      val adMobExtras = dfpRequest.getNetworkExtras(
          AdMobExtras::class.java
      )
      request.admobExtras.putAll(
          filterTargeting(
              if (adMobExtras != null) adMobExtras.extras else adMob
          )
      )
    } catch (e: Exception) {
      // do nothing
    }
    if (request.requestData == null) {
      request.requestData = RequestData(this, adView)
    }
    return request
  }

  public override fun getPublisherProvidedId(): String {
    return ""
  }

  companion object {
    fun fromAuctionRequest(request: AuctionRequest): DFPAdViewRequest {
      val builder = Builder()
          .addCustomEventExtrasBundle(CustomEventBanner::class.java, request.networkExtras)
          .addCustomEventExtrasBundle(CustomEventInterstitial::class.java, request.networkExtras)
          .addCustomEventExtrasBundle(
              MonetDfpCustomEventInterstitial::class.java,
              request.networkExtras
          )
          .addNetworkExtrasBundle(CustomEventBanner::class.java, request.networkExtras)

      // add in the admob extras
      val completeExtras = request.admobExtras
      completeExtras.putAll(request.targeting)
      try {
        builder.addNetworkExtrasBundle(AdMobAdapter::class.java, completeExtras)
      } catch (e: Exception) {
        sLogger.error("excetion $e")
        // do nothing
      }
      if (request.requestData != null) {
        builder.setContentUrl(request.requestData!!.contentURL)
        builder.setLocation(request.requestData!!.location)
      }
      val adRequest = builder.build()
      return DFPAdViewRequest(adRequest)
    }
  }
}