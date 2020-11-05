package com.monet

import com.monet.AdType.BANNER

/**
 * Created by nbjacob on 6/26/17.
 */


class MoPubBannerAdView(val viewWrapper: ViewWrapper<*>) : AdServerAdView {
  override var adUnitId: String = viewWrapper.adUnit

  override var type: AdType = BANNER

  override fun loadAd(request: AdServerAdRequest?) {
    val adRequest = request as MoPubAdRequest?
    adRequest?.applyToView(this)
    viewWrapper.loadAd()
  }
}