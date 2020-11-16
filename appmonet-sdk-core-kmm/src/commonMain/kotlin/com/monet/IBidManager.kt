package com.monet

interface IBidManager {
  fun isValid(bid: BidResponse?): Boolean
  fun peekNextBid(unitId: String?): BidResponse?
  fun markUsed(bid: BidResponse)
  fun invalidate(wvUUID: String)
  fun cleanBids()
  fun logState()

}