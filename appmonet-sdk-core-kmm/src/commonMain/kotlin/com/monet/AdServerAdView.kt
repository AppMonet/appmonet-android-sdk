package com.monet

import co.touchlab.stately.freeze

interface AdServerAdView {
  var type: AdType
  var adUnitId: String
  fun loadAd(request: AdServerAdRequest?)

  fun freezeAdServerAdView() {
    this.freeze()
  }
}