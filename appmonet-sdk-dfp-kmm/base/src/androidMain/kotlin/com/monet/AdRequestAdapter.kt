package com.monet

import com.google.android.gms.ads.doubleclick.PublisherAdRequest.Builder
import com.google.android.gms.ads.mediation.MediationAdRequest

actual object AdRequestAdapter {
  actual fun fromMediationRequest(
    isPublisherAdView: Boolean,
    adRequest: Any
  ): AdServerAdRequest {
    val mediationRequest = adRequest as MediationAdRequest
    return if (isPublisherAdView) {
      val dfpRequest = Builder()
          .setBirthday(mediationRequest.birthday)
          .setGender(mediationRequest.gender)
          .setLocation(mediationRequest.location)
          .build()
      DFPAdRequest(PublisherAdRequestWrapper(dfpRequest))
    } else {
      val req = com.google.android.gms.ads.AdRequest.Builder()
          .setBirthday(mediationRequest.birthday)
          .setGender(mediationRequest.gender)
          .setLocation(mediationRequest.location)
          .build()
      DFPAdViewRequest(AdRequestWrapper(req))
    }
  }
}