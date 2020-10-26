package com.monet.bidder

import com.monet.bidder.AdRequestFactory.createEmptyRequest
import com.monet.bidder.AdRequestFactory.fromAuctionRequest
import com.monet.bidder.AdServerWrapper.Type

/**
 * Created by nbjacob on 6/26/17.
 */
internal class DFPAdServerWrapper : AdServerWrapper {
  private var sdkManager: SdkManager? = null
  fun setSdkManager(sdkManager: SdkManager?) {
    this.sdkManager = sdkManager
  }

  override fun newAdRequest(auctionRequest: AuctionRequest): AdServerAdRequest {
    return fromAuctionRequest(sdkManager!!.isPublisherAdView, auctionRequest)
  }

  override fun newAdRequest(
    auctionRequest: AuctionRequest,
    type: Type
  ): AdServerAdRequest {
    return newAdRequest(auctionRequest)
  }

  override fun newAdRequest(): AdServerAdRequest {
    return createEmptyRequest(sdkManager!!.isPublisherAdView)
  }

  override fun newAdSize(
    width: Int,
    height: Int
  ): AdSize {
    return AdSize(width, height)
  }
}