package com.monet

interface ViewWrapper<T> {
  val view : T
  val adUnit: String
  fun getAdFormat(): AdType
  fun getLocalExtras(): Map<String, Any>?
  fun loadAd()
  fun getLocation(): LocationData?
  fun setLocalExtras(extras: Map<String, Any>?)
  fun getKeywords(): String?
  fun setKeywords(keywords: String?)
  fun setLocation(location: LocationData?)
}