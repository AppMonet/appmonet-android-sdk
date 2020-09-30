package com.monet.bidder

import android.annotation.SuppressLint
import com.google.android.gms.ads.AdView
import com.monet.bidder.AdServerWrapper.Type
import com.monet.bidder.AdServerWrapper.Type.BANNER

internal class DFPAdView(private val adView: AdView?) : AdServerAdView {
  override var adUnitId: String = adView?.adUnitId ?: ""
  override val type: Type
    get() = BANNER

  @SuppressLint("MissingPermission")
  override fun loadAd(request: AdServerAdRequest?) {
    request?.let {
      val adRequest = it as DFPAdViewRequest
      adView?.loadAd(adRequest.dfpRequest)
    }
  }
}