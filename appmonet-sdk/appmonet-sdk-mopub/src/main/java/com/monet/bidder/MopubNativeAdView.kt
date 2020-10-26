package com.monet.bidder

import com.monet.AdServerAdRequest
import com.monet.AdServerAdView
import com.monet.AdType
import com.monet.AdType.NATIVE
import com.mopub.nativeads.MoPubNative

internal class MopubNativeAdView(
  val mopubNative: MoPubNative,
  override var adUnitId: String = ""
) : AdServerAdView {
  override var type: AdType = NATIVE

  override fun loadAd(request: AdServerAdRequest?) {}
}