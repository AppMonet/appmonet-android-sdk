package com.monet.bidder

import android.content.Context
import com.monet.AdServerAdRequest
import com.monet.AdServerWrapper
import com.monet.AdType
import com.monet.adview.AdSize
import com.monet.auction.AuctionRequest
import com.monet.bidder.AdRequestFactory.createEmptyRequest
import com.monet.bidder.AdRequestFactory.fromAuctionRequest

/**
 * Created by nbjacob on 6/26/17.
 */
internal class DFPAdServerWrapper(private val context: Context) : AdServerWrapper {
  private var sdkManager: SdkManager? = null
  fun setSdkManager(sdkManager: SdkManager?) {
    this.sdkManager = sdkManager
  }

  override fun newAdRequest(auctionRequest: AuctionRequest): AdServerAdRequest {
    return fromAuctionRequest(sdkManager!!.isPublisherAdView, auctionRequest)
  }

  override fun newAdRequest(
    auctionRequest: AuctionRequest,
    type: AdType
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
    return AdSize(context, width, height)
  }
}