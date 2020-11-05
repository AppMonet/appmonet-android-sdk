package com.monet

import com.monet.AdType.INTERSTITIAL

/**
 * Created by nbjacob on 6/26/17.
 */
class MoPubInterstitialAdView(
  val viewWrapper: ViewWrapper<*>,
  override var adUnitId: String = ""
) : AdServerAdView {

  override var type: AdType = INTERSTITIAL

  override fun loadAd(request: AdServerAdRequest?) {
  }
}