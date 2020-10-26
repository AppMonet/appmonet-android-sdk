package com.monet.auction

import co.touchlab.stately.freeze
import com.monet.AdServerAdRequest
import com.monet.AdServerAdView
import com.monet.BidResponse
import com.monet.Constants
import com.monet.RequestData

data class AuctionRequest internal constructor(
  val networkExtras: MutableMap<String, Any> = mutableMapOf(),
  var targeting: Map<String, Any> = mapOf(),
  val admobExtras: Map<String, String> = mapOf(),
  var requestData: RequestData? = null,
  var bid: BidResponse? = null,
  var adUnitId: String = ""
) {

  fun freezeAuctionRequest() {
    this.freeze()
  }

  companion object {
    fun from(
      adServerAdView: AdServerAdView,
      adServerAdRequest: AdServerAdRequest
    ): AuctionRequest {
      val auctionRequest = AuctionRequest().apply {
        networkExtras[Constants.ADUNIT_KEYWORD_KEY] = adServerAdView.adUnitId;
        requestData = RequestData(adServerAdRequest, adServerAdView)
        adUnitId = adServerAdView.adUnitId
      }
      return adServerAdRequest.apply(auctionRequest, adServerAdView)
    }
  }
}
