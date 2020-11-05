package com.monet

import com.monet.AdType.NATIVE

class MopubNativeAdView(
  val viewWrapper: ViewWrapper<*>,
  override var adUnitId: String = ""
) : AdServerAdView {
  override var type: AdType = NATIVE

  override fun loadAd(request: AdServerAdRequest?) {}
}