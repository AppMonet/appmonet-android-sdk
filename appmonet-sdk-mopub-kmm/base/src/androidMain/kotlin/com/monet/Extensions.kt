package com.monet

import android.location.Location
import com.mopub.nativeads.RequestParameters
import com.mopub.nativeads.RequestParameters.Builder

fun MoPubNativeRequestParametersWrapper(request: RequestParameters?): MoPubNativeRequestParametersWrapper {
  val builder = MoPubNativeRequestParametersWrapper.Builder()
  request?.keywords?.let { builder.keywords(it) }
  request?.location?.let {
    builder.location(LocationData(it.latitude, it.longitude, it.accuracy.toDouble(), it.provider))
  }
  request?.userDataKeywords?.let { builder.userDataKeywords(it) }
  return MoPubNativeRequestParametersWrapper(builder)
}

fun MoPubNativeRequestParametersWrapper.request(): RequestParameters {
  val builder = Builder()
  location?.let {
    builder.location(Location(it.provider).apply {
      longitude = it.lon
      latitude = it.lat
      accuracy = it.accuracy.toFloat()
    })
  }
  keywords?.let {
    builder.keywords(it)
  }

  userDataKeywords?.let {
    builder.userDataKeywords(it)
  }

  return builder.build()
}