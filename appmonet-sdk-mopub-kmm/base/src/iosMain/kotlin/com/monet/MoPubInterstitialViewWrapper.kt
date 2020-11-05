package com.monet

import cocoapods.MoPub.*
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocation

class MoPubInterstitialViewWrapper(
  override val view: MPInterstitialAdController
) :
    ViewWrapper<MPInterstitialAdController> {
  override val adUnit: String = view.adUnitId ?: ""
  override fun loadAd() {
    view.loadAd()
  }

  override fun getLocalExtras(): Map<String, Any>? {
    val localExtras = mutableMapOf<String, Any>()
    view.localExtras?.map {
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
    return AdType.BANNER
  }

  override fun getLocation(): LocationData? {
    return view.location()?.let {
      LocationData(
          it.coordinate.useContents { latitude }, it.coordinate.useContents { longitude },
          it.courseAccuracy, ""
      )
    }
  }

  override fun setLocalExtras(extras: Map<String, Any>?) {
    val localExtras = mutableMapOf<Any?, Any?>()
    extras?.map {
      localExtras[it.key] = it.value
    }
    view.setLocalExtras(localExtras)
  }

  override fun getKeywords(): String? {
    return view.keywords
  }

  override fun setKeywords(keywords: String?) {
    view.setKeywords(keywords)
  }

  override fun setLocation(location: LocationData?) {
    location?.let {
      val targetLocation = CLLocation(it.lat, it.lon)
      view.setLocation(targetLocation)
    }
  }
}