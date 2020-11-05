package com.monet.bidder

import android.annotation.SuppressLint
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.monet.AdServerAdRequest
import com.monet.AdServerAdView
import com.monet.AdType
import com.monet.AdType.INTERSTITIAL
import com.monet.DFPAdViewRequest

internal class DFPInterstitialAdView(private val interstitialAd: InterstitialAd?) : AdServerAdView {
  override var adUnitId: String = interstitialAd?.adUnitId ?: ""
  override var type: AdType = INTERSTITIAL

  @SuppressLint("MissingPermission")
  override fun loadAd(request: AdServerAdRequest?) {
    request?.let {
      val adRequest = it as DFPAdViewRequest
      interstitialAd?.loadAd(adRequest.adRequest.request as AdRequest)
    }
  }
}