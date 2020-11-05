package com.monet

import com.mopub.mobileads.MoPubInterstitial

class MoPubInterstitialViewWrapper(
  override val view: MoPubInterstitial
) : ViewWrapper<MoPubInterstitial> {
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
    return AdType.INTERSTITIAL
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
    //not implemented
  }
}