package com.monet.bidder

import com.monet.AdServerAdRequest
import com.monet.AdServerAdView
import com.monet.AdType
import com.monet.AdType.BANNER
import com.mopub.mobileads.MoPubView

/**
 * Created by nbjacob on 6/26/17.
 */
internal class MoPubAdView(private val moPubView: MoPubView?) : AdServerAdView {
  override var adUnitId: String = moPubView?.getAdUnitId() ?: ""

  override var type: AdType = BANNER

  fun getMoPubView(): MoPubView? {
    return moPubView
  }

  override fun loadAd(request: AdServerAdRequest?) {
    val adRequest = request as MoPubAdRequest?
    adRequest?.applyToView(this)
    moPubView?.loadAd()
  }
}