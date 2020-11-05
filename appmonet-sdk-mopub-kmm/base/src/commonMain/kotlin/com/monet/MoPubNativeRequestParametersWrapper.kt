package com.monet

class MoPubNativeRequestParametersWrapper() {
  var location: LocationData? = null
  var userDataKeywords: String? = null
  var keywords: String? = null

  constructor(builder: Builder) : this() {
    this.location = builder.location
    this.userDataKeywords = builder.userDataKeywords
    this.keywords = builder.keywords
  }

  class Builder {

    var keywords: String? = null
      private set
    var location: LocationData? = null
      private set
    var userDataKeywords: String? = null
      private set

    fun keywords(keywords: String?) = apply { this.keywords = keywords }
    fun location(location: LocationData?) = apply { this.location = location }
    fun userDataKeywords(userDataKeywords: String?) =
      apply { this.userDataKeywords = userDataKeywords }

    fun build(): MoPubNativeRequestParametersWrapper = MoPubNativeRequestParametersWrapper(this)
  }
}