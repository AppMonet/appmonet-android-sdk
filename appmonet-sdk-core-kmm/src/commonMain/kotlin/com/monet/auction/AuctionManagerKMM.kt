package com.monet.auction

import com.monet.AdServerAdRequest
import com.monet.BidResponse

interface AuctionManagerKMM {
  fun trackRequest(
    adUnitId: String,
    source: String
  )

  /**
   * The implementation of this will cancel a particular request.
   *
   * @param adUnitId The ad unit of a particular request.
   * @param adRequest The adRequest to cancel.
   * @param bid The bid associated to the request to be canceled.
   */
  fun cancelRequest(
    adUnitId: String?,
    adRequest: AdServerAdRequest,
    bid: BidResponse?
  )
}