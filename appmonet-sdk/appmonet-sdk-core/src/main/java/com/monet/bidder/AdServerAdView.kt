package com.monet.bidder

import com.monet.bidder.AdServerWrapper.Type

interface AdServerAdView {
  val type: Type
  var adUnitId: String
  fun loadAd(request: AdServerAdRequest?)
}