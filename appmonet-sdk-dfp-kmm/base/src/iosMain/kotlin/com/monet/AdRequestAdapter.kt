package com.monet

import cocoapods.GoogleMobileAds.DFPRequest
import cocoapods.GoogleMobileAds.GADCustomEventRequest
import cocoapods.GoogleMobileAds.GADRequest

actual object AdRequestAdapter {
  actual fun fromMediationRequest(
    isPublisherAdView: Boolean,
    adRequest: Any
  ): AdServerAdRequest {
    val gadCustomEventRequest = adRequest as GADCustomEventRequest
    return if (isPublisherAdView) {
      DFPAdRequest(DFPAdRequestWrapper(DFPRequest().apply {
        this.birthday = gadCustomEventRequest.userBirthday
        this.gender = gadCustomEventRequest.userGender
        this.setLocationWithLatitude(
            gadCustomEventRequest.userLatitude, gadCustomEventRequest.userLongitude,
            gadCustomEventRequest.userLocationAccuracyInMeters
        )
      }))
    } else {
      DFPAdViewRequest(GADAdRequestWrapper(GADRequest().apply {
        this.birthday = gadCustomEventRequest.userBirthday
        this.gender = gadCustomEventRequest.userGender
        this.setLocationWithLatitude(
            gadCustomEventRequest.userLatitude, gadCustomEventRequest.userLongitude,
            gadCustomEventRequest.userLocationAccuracyInMeters
        )
      }))
    }
  }
}