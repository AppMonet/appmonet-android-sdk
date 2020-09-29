package com.monet.bidder

import android.webkit.ValueCallback

data class AddBidsParams(
  val adView: AdServerAdView,
  val request: AdServerAdRequest,
  val timeout: Int,
  val callback: ValueCallback<AdServerAdRequest>
)