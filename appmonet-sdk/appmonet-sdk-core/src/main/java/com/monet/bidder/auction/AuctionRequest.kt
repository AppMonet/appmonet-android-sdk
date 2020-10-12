package com.monet.bidder.auction

import android.os.Bundle
import com.monet.bidder.AdServerAdRequest
import com.monet.bidder.AdServerAdView
import com.monet.bidder.Constants
import com.monet.bidder.RequestData
import com.monet.bidder.bid.BidResponse

data class AuctionRequest internal constructor(
  val networkExtras: Bundle = Bundle(),
  val targeting: Bundle = Bundle(),
  val admobExtras: Bundle = Bundle(),
  var requestData: RequestData? = null,
  var bid: BidResponse? = null,
  var adUnitId: String = ""
) {
  internal companion object {
    fun from(
      adServerAdView: AdServerAdView,
      adServerAdRequest: AdServerAdRequest
    ): AuctionRequest {
      val auctionRequest = AuctionRequest().apply {
        networkExtras.putString(Constants.Dfp.ADUNIT_KEYWORD_KEY, adServerAdView.adUnitId);
        requestData = RequestData(adServerAdRequest, adServerAdView)
        adUnitId = adServerAdView.adUnitId
      }
      return adServerAdRequest.apply(auctionRequest, adServerAdView)
    }
  }
}
