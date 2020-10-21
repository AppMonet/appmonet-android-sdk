package com.monet.bidder

import com.monet.Callback

data class AddBidsParams(
  val adView: AdServerAdView,
  val request: AdServerAdRequest,
  val timeout: Int,
  val callback: Callback<AdServerAdRequest>
)