package com.monet

import android.location.Location
import android.os.Bundle
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdRequest.Builder
import com.google.android.gms.ads.mediation.MediationExtrasReceiver
import com.google.android.gms.ads.mediation.customevent.CustomEvent
import com.monet.auction.AuctionRequest

actual class DFPAdViewRequestUtil {
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
      // transfer admob extras if they're there
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
      return request
    }

    fun fromAuctionRequest(
      request: AuctionRequest,
      monetCustomEventInterstitialClazz: Class<out CustomEvent>,
      customEventBannerClazz: Class<out CustomEvent>,
      customEventBannerReceiverClazz: Class<out MediationExtrasReceiver>,
      customEventInterstitialClazz: Class<out CustomEvent>
    ):  DFPAdViewRequest{
      val builder = Builder()
          .addCustomEventExtrasBundle(
              customEventBannerClazz, request.networkExtras[NETWORK_EXTRAS] as Bundle?
          )
          .addCustomEventExtrasBundle(
              customEventInterstitialClazz, request.networkExtras[NETWORK_EXTRAS] as Bundle?
          )
          .addCustomEventExtrasBundle(
              monetCustomEventInterstitialClazz, request.networkExtras[NETWORK_EXTRAS] as Bundle?
          )
          .addNetworkExtrasBundle(
              customEventBannerReceiverClazz, request.networkExtras[NETWORK_EXTRAS] as Bundle?
          )

      // add in the admob extras
      val completeExtras = request.admobExtras.toMutableMap()
      completeExtras.putAll(request.targeting)
      request.admobExtras = completeExtras
      try {
        builder.addNetworkExtrasBundle(AdMobAdapter::class.java, completeExtras.toBundle())
      } catch (e: Exception) {
        // do nothing
      }
      if (request.requestData != null) {
        if (!request.requestData!!.contentURL.isNullOrEmpty()) {
          builder.setContentUrl(request.requestData!!.contentURL)
        }
        request.requestData?.location?.let {
          builder.setLocation(Location(it.provider).apply {
            longitude = it.lon
            latitude = it.lat
            accuracy = it.accuracy.toFloat()
          })
        }
      }
      val adRequest = builder.build()
      return DFPAdViewRequest(AdRequestWrapper(adRequest))
    }
  }

}

