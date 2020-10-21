package com.monet.bidder

import com.monet.bidder.adview.AdViewPoolManager
import com.monet.bidder.auction.AuctionManagerCallback
import com.monet.bidder.bid.BidManager
import com.monet.BidResponse
import com.monet.bidder.threading.BackgroundThread
import java.util.PriorityQueue

internal data class BidManagerTestComponent(
  val bidManager: BidManager,
  val pubSubService: PubSubService,
  val backgroundThread: BackgroundThread,
  val adViewPoolManager: AdViewPoolManager,
  val auctionManagerCallback: AuctionManagerCallback,
  val store: MutableMap<String?, PriorityQueue<BidResponse>?>,
  val seenBids: MutableMap<String, String?>,
  val adUnitNameMapping: MutableMap<String, String?>,
  val bidsById: MutableMap<String, BidResponse?>,
  val bidIdsByAdView: MutableMap<String, MutableList<String?>?>,
  val usedBids: MutableMap<String, String?>
)
