package com.monet

interface IBidManager {
  fun isValid(bid: BidResponse?): Boolean
  fun peekNextBid(unitId: String?): BidResponse?
}