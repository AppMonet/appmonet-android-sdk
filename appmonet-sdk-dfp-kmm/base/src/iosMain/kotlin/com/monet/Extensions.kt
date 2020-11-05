package com.monet

import com.monet.auction.AuctionRequest

fun DFPAdRequest.fromAuctionRequest(
  request: AuctionRequest,
  adServerLabel: String,
  currentExtrasLabel: String
): DFPAdRequest {
  return DFPAdRequestUtil.fromAuctionRequest(
      request, adServerLabel, currentExtrasLabel
  )
}

fun DFPAdViewRequest.fromAuctionRequest(
  request: AuctionRequest,
  adServerLabel: String,
  currentExtrasLabel: String
): DFPAdViewRequest {
  return DFPAdViewRequestUtil.fromAuctionRequest(
      request, adServerLabel, currentExtrasLabel
  )
}