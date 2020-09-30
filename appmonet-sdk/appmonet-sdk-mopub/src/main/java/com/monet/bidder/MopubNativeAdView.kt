package com.monet.bidder

import com.monet.bidder.AdServerWrapper.Type
import com.monet.bidder.AdServerWrapper.Type.NATIVE
import com.mopub.nativeads.MoPubNative

internal class MopubNativeAdView(
  val mopubNative: MoPubNative,
  override var adUnitId: String = ""
) : AdServerAdView {
  override val type: Type
    get() = NATIVE

  override fun loadAd(request: AdServerAdRequest?) {}
}