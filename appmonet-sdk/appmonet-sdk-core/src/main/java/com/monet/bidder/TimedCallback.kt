package com.monet.bidder

interface TimedCallback {
  fun execute(remainingTime: Int)
  fun timeout()
}