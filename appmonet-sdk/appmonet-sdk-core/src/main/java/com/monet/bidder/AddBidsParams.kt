package com.monet.bidder

import com.monet.Callback
import com.monet.AdServerAdRequest
import com.monet.AdServerAdView

data class AddBidsParams(
  val adView: AdServerAdView,
  val request: AdServerAdRequest,
  val timeout: Int,
  val callback: Callback<AdServerAdRequest>
)