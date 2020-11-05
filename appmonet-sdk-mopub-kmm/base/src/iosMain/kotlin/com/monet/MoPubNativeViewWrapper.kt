package com.monet

import cocoapods.MoPub.MPNativeAdRequest
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocation

class MoPubNativeViewWrapper(
  override val view: MPNativeAdRequest,
  override val adUnit: String = ""
) : ViewWrapper<MPNativeAdRequest> {
  override fun loadAd() {

  }

  override fun getLocalExtras(): Map<String, Any>? {
    val localExtras = mutableMapOf<String, Any>()
    view.targeting?.localExtras?.map {
      it.key?.let { key ->
        if (key is String) {
          it.value?.let { value ->
            localExtras[key] = value
          }
        }
      }
    }
    return localExtras
  }

  override fun getAdFormat(): AdType {
    return AdType.NATIVE
  }

  override fun getLocation(): LocationData? {
    return view.targeting?.location()?.let {
      LocationData(
          it.coordinate.useContents { latitude }, it.coordinate.useContents { longitude },
          it.courseAccuracy, ""
      )
    }
  }

  override fun setLocalExtras(extras: Map<String, Any>?) {
    val localExtras = mutableMapOf<Any?, Any>()
    extras?.map {
      localExtras[it.key] = it.value
    }
    view.targeting?.setLocalExtras(localExtras)
  }

  override fun getKeywords(): String? {
    return view.targeting?.keywords
  }

  override fun setKeywords(keywords: String?) {
    view.targeting?.setKeywords(keywords)
  }

  override fun setLocation(location: LocationData?) {
    location?.let {
      view.targeting?.setLocation(CLLocation(it.lat, it.lon))
    }
  }
}