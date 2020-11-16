package com.monet

expect object AdRequestAdapter {
  fun fromMediationRequest(
    isPublisherAdView: Boolean,
    adRequest: Any
  ): AdServerAdRequest
}