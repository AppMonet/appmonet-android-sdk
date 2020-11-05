package com.monet

import android.location.Location
import com.mopub.common.AdFormat.INTERSTITIAL
import com.mopub.common.AdFormat.NATIVE
import com.mopub.mobileads.MoPubView

class MoPubViewWrapper(
  override val view: MoPubView
) : ViewWrapper<MoPubView> {
  override val adUnit: String = view.getAdUnitId() ?: ""
  override fun loadAd() {
    view.loadAd()
  }

  override fun getLocalExtras(): Map<String, Any>? {
    val localExtras = mutableMapOf<String, Any>()
    view.getLocalExtras().map {
      localExtras[it.key] = it.value
    }
    return localExtras
  }

  override fun getAdFormat(): AdType {
    return when (view.getAdFormat()) {
      INTERSTITIAL -> AdType.INTERSTITIAL
      NATIVE -> AdType.NATIVE
      else -> AdType.BANNER
    }
  }

  override fun getLocation(): LocationData? {
    return view.getLocation()?.let {
      LocationData(it.longitude, it.longitude, it.accuracy.toDouble(), it.provider)
    }
  }

  override fun setLocalExtras(extras: Map<String, Any>?) {
    val localExtras = mutableMapOf<String, Any>()
    extras?.map {
      localExtras[it.key] = it.value
    }
    view.setLocalExtras(localExtras)
  }

  override fun getKeywords(): String? {
    return view.getKeywords()
  }

  override fun setKeywords(keywords: String?) {
    view.setKeywords(keywords)
  }

  override fun setLocation(location: LocationData?) {
    location?.let {
      val targetLocation = Location(it.provider).apply {
        latitude = it.lat
        longitude = it.lon
      }
      view.setLocation(targetLocation)
    }
  }
}