package com.monet.bidder

import android.annotation.SuppressLint
import com.google.android.gms.ads.doubleclick.PublisherAdView
import com.monet.bidder.AdServerWrapper.Type
import com.monet.bidder.AdServerWrapper.Type.BANNER

/**
 * Created by nbjacob on 6/26/17.
 */
internal class DFPPublisherAdView : AdServerAdView {
  private val mAdView: PublisherAdView?
  override var adUnitId: String

  constructor(adView: PublisherAdView) {
    mAdView = adView
    adUnitId = adView.adUnitId
  }

  constructor(adUnitId: String) {
    this.adUnitId = adUnitId
    mAdView = null
  }

  override val type: Type
    get() = BANNER

  @SuppressLint("MissingPermission")
  override fun loadAd(request: AdServerAdRequest?) {
    request?.let {
      val adRequest = it as DFPAdRequest
      mAdView?.loadAd(adRequest.dfpRequest)
    }
  }
}