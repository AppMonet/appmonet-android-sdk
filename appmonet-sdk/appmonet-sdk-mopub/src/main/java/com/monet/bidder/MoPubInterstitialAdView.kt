package com.monet.bidder

import com.monet.AdServerAdRequest
import com.monet.AdServerAdView
import com.monet.AdType
import com.monet.AdType.INTERSTITIAL
import com.mopub.mobileads.MoPubInterstitial

/**
 * Created by nbjacob on 6/26/17.
 */
internal class MoPubInterstitialAdView(
  val moPubView: MoPubInterstitial,
  override var adUnitId: String = ""
) : AdServerAdView {

  override var type: AdType = INTERSTITIAL

  override fun loadAd(request: AdServerAdRequest?) {
  }
}