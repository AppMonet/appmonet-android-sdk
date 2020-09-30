package com.monet.bidder

import com.monet.bidder.AdServerWrapper.Type
import com.monet.bidder.AdServerWrapper.Type.BANNER
import com.mopub.mobileads.MoPubView

/**
 * Created by nbjacob on 6/26/17.
 */
internal class MoPubAdView(private val moPubView: MoPubView?) : AdServerAdView {
  override var adUnitId: String = moPubView?.getAdUnitId() ?: ""

  override val type: Type
    get() = BANNER

  fun getMoPubView(): MoPubView? {
    return moPubView
  }

  override fun loadAd(request: AdServerAdRequest?) {
    val adRequest = request as MoPubAdRequest?
    adRequest?.applyToView(this)
    moPubView?.loadAd()
  }
}