package com.monet

import com.mopub.nativeads.MoPubNative

class MoPubNativeViewWrapper(
  override val view: MoPubNative,
  override val adUnit: String = ""
) : ViewWrapper<MoPubNative> {
  override fun loadAd() {

  }

  override fun getLocalExtras(): Map<String, Any>? {
    return null
  }

  override fun getAdFormat(): AdType {
    return AdType.NATIVE
  }

  override fun getLocation(): LocationData? {
    return null
  }

  override fun setLocalExtras(extras: Map<String, Any>?) {
    val localExtras = mutableMapOf<String, Any>()
    extras?.map {
      localExtras[it.key] = it.value
    }
    view.setLocalExtras(localExtras)
  }

  override fun getKeywords(): String? {
    return null
  }

  override fun setKeywords(keywords: String?) {
  }

  override fun setLocation(location: LocationData?) {
    //not implemented
  }
}