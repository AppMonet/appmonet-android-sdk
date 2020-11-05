package com.monet.bidder

import android.annotation.SuppressLint
import com.google.android.gms.ads.doubleclick.PublisherAdRequest
import com.google.android.gms.ads.doubleclick.PublisherInterstitialAd
import com.monet.AdServerAdRequest
import com.monet.AdServerAdView
import com.monet.AdType
import com.monet.AdType.INTERSTITIAL
import com.monet.DFPAdRequest

class DFPPublisherInterstitialAdView(private val interstitialAd: PublisherInterstitialAd?) :
    AdServerAdView {
  override var adUnitId = interstitialAd?.adUnitId ?: ""

  override var type: AdType = INTERSTITIAL

  @SuppressLint("MissingPermission")
  override fun loadAd(request: AdServerAdRequest?) {
    request?.let {
      val adRequest = it as DFPAdRequest
      interstitialAd?.loadAd(adRequest.dfpRequest.request as PublisherAdRequest)
    }
  }
}