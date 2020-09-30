package com.monet.bidder

import com.monet.bidder.AdServerWrapper.Type
import com.monet.bidder.AdServerWrapper.Type.INTERSTITIAL
import com.mopub.mobileads.MoPubInterstitial

/**
 * Created by nbjacob on 6/26/17.
 */
internal class MoPubInterstitialAdView(
  val moPubView: MoPubInterstitial,
  override var adUnitId: String = ""
) : AdServerAdView {

  override val type: Type
    get() = INTERSTITIAL

  override fun loadAd(request: AdServerAdRequest?) {
  }
}