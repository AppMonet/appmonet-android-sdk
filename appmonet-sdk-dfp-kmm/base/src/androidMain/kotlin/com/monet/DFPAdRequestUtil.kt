package com.monet

import android.location.Location
import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.android.gms.ads.doubleclick.PublisherAdRequest
import com.google.android.gms.ads.doubleclick.PublisherAdRequest.Builder
import com.google.android.gms.ads.mediation.MediationExtrasReceiver
import com.google.android.gms.ads.mediation.admob.AdMobExtras
import com.google.android.gms.ads.mediation.customevent.CustomEvent
import com.monet.auction.AuctionRequest

actual class DFPAdRequestUtil {
  actual companion object {
    const val ADMOB_EXTRAS = "admob_extras"
    const val CUSTOM_TARGETING = "custom_targeting"
    const val NETWORK_EXTRAS_BUNDLE = "network_extras_bundle"
    const val NETWORK_EXTRAS = "network_extras"

    actual fun apply(
      adRequestWrapper: RequestWrapper<*>,
      request: AuctionRequest,
      adView: AdServerAdView,
      adServerAdRequest: AdServerAdRequest
    ): AuctionRequest {
      try {
        val adMob = adRequestWrapper.networkExtrasBundle
        val admobExtras = request.admobExtras.toMutableMap()
        admobExtras.putAll(
            adServerAdRequest.filterTargeting(
                adRequestWrapper.networkExtras ?: adMob
            )
        )
        request.admobExtras = admobExtras
      } catch (e: Exception) {
        // do nothing
      }
      if (request.requestData == null) {
        request.requestData = RequestData(adServerAdRequest, adView)
      }
      val targeting = request.targeting.toMutableMap()
      targeting.putAll(
          adServerAdRequest.filterTargeting(
              adRequestWrapper.customTargeting
          )
      )
      request.targeting = targeting
      return request
    }

    fun fromAuctionRequest(
      request: AuctionRequest,
      monetCustomEventInterstitialClazz: Class<out CustomEvent>,
      customEventBannerClazz: Class<out CustomEvent>,
      customEventBannerReceiverClazz: Class<out MediationExtrasReceiver>,
      customEventInterstitialClazz: Class<out CustomEvent>
    ): DFPAdRequest {
      val bundleNetworkExtras = request.networkExtras.toBundle()
      val builder = Builder()
          .addCustomEventExtrasBundle(
              customEventBannerClazz, bundleNetworkExtras
          )
          .addCustomEventExtrasBundle(
              customEventInterstitialClazz, bundleNetworkExtras
          )
          .addCustomEventExtrasBundle(
              monetCustomEventInterstitialClazz, bundleNetworkExtras
          )
          .addNetworkExtrasBundle(
              customEventBannerReceiverClazz, bundleNetworkExtras
          )

      // copy over the targeting
      for ((key, value) in request.targeting) {
//      for (key in request.targeting.keySet()) {
//        val value = request.targeting[key] ?: continue
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
        builder.setLocation(
            request.requestData!!.location?.let {
              Location(it.provider).apply {
                longitude = it.lon
                latitude = it.lat
                accuracy = it.accuracy.toFloat()
              }
            })
        for ((key, value) in request.requestData!!.getStringMapFromKvp()) {
          builder.addCustomTargeting(key, value)
        }
      }

      // add in the admob extras
      val completeExtras = request.admobExtras.toMutableMap()
      completeExtras.putAll(request.targeting)
      request.admobExtras = completeExtras
      try {
        builder.addNetworkExtras(AdMobExtras(completeExtras.toBundle()))
      } catch (e: Exception) {
        // do nothing
      }
      val mediationRequest = builder.build()
      return DFPAdRequest(PublisherAdRequestWrapper(mediationRequest))
    }
  }

}

