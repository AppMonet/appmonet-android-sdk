package com.monet.bidder

import android.annotation.SuppressLint
import com.google.android.gms.ads.InterstitialAd
import com.monet.bidder.AdServerWrapper.Type
import com.monet.bidder.AdServerWrapper.Type.INTERSTITIAL

internal class DFPInterstitialAdView(private val interstitialAd: InterstitialAd?) : AdServerAdView {
  override var adUnitId: String = interstitialAd?.adUnitId ?: ""
  override val type: Type
    get() = INTERSTITIAL

  @SuppressLint("MissingPermission")
  override fun loadAd(request: AdServerAdRequest?) {
    request?.let {
      val adRequest = it as DFPAdViewRequest
      interstitialAd?.loadAd(adRequest.dfpRequest)
    }
  }
}