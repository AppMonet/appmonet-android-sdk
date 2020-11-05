package com.monet

import cocoapods.MoPub.MPNativeAdRequest
import cocoapods.MoPub.MPNativeAdRequestTargeting
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocation

fun MoPubNativeRequestParametersWrapper(request: MPNativeAdRequest?): MoPubNativeRequestParametersWrapper {
  val builder = MoPubNativeRequestParametersWrapper.Builder()
  request?.targeting?.keywords?.let { builder.keywords(it) }
  request?.targeting?.location?.let {
    builder.location(
        LocationData(
            it.coordinate.useContents { latitude },
            it.coordinate.useContents { longitude }, it.courseAccuracy,
            ""
        )
    )
  }
  request?.targeting?.userDataKeywords?.let { builder.userDataKeywords(it) }
  return MoPubNativeRequestParametersWrapper(builder)
}

fun MoPubNativeRequestParametersWrapper.request(): MPNativeAdRequest {

  val builder = MPNativeAdRequest()
  val targeting  = MPNativeAdRequestTargeting()
  location?.let {
    targeting.setLocation(CLLocation(it.lat, it.lon))
  }
  keywords?.let {
    targeting.setKeywords(it)
  }

  userDataKeywords?.let {
    targeting.setUserDataKeywords(it)
  }
  builder.setTargeting(targeting)
  return builder
}