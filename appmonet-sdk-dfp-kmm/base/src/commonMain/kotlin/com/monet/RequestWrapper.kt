package com.monet

interface RequestWrapper<T> {
  val request: T
  val publisherProvidedId: String?
  val location: LocationData?
  val contentUrl: String?
  val gender: String
  val birthday: Long?
  val customTargeting: Map<String, Any>
  val networkExtrasBundle: Map<String, Any>
  val networkExtras: Map<String, Any>?
}