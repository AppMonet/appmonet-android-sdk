package com.monet.bidder

import android.annotation.SuppressLint
import com.google.android.gms.ads.doubleclick.PublisherInterstitialAd
import com.monet.bidder.AdServerWrapper.Type
import com.monet.bidder.AdServerWrapper.Type.INTERSTITIAL

class DFPPublisherInterstitialAdView(private val interstitialAd: PublisherInterstitialAd?) :
    AdServerAdView {
  override var adUnitId = interstitialAd?.adUnitId ?: ""

  override val type: Type
    get() = INTERSTITIAL

  @SuppressLint("MissingPermission")
  override fun loadAd(request: AdServerAdRequest?) {
    request?.let {
      val adRequest = it as DFPAdRequest
      interstitialAd?.loadAd(adRequest.dfpRequest)
    }
  }
}