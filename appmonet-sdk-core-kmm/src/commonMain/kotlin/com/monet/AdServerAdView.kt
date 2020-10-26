package com.monet

interface AdServerAdView {
  var type: AdType
  var adUnitId: String
  fun loadAd(request: AdServerAdRequest?)
}