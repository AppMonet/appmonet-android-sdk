package com.monet.bidder

import android.annotation.SuppressLint
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.monet.AdServerAdRequest
import com.monet.AdServerAdView
import com.monet.AdType
import com.monet.AdType.BANNER
import com.monet.DFPAdViewRequest

class DFPAdView(private val adView: AdView?) : AdServerAdView {
  override var adUnitId: String = adView?.adUnitId ?: ""
  override var type: AdType = BANNER

  @SuppressLint("MissingPermission")
  override fun loadAd(request: AdServerAdRequest?) {
    request?.let {
      val adRequest = it as DFPAdViewRequest
      adView?.loadAd(adRequest.adRequest.request as AdRequest)
    }
  }
}