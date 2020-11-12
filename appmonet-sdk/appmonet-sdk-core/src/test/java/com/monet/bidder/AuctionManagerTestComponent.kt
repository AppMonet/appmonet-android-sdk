package com.monet.bidder

import com.monet.bidder.adview.AdViewPoolManager
import com.monet.bidder.auction.AuctionManager
import com.monet.bidder.bid.BidManager
import com.monet.bidder.callbacks.ReadyCallbackManager

internal data class AuctionManagerTestComponent(
  val addBidsManager: AddBidsManager,
  val adViewPoolManager: AdViewPoolManager,
  val appMonetWebView: AppMonetWebView,
  val appMonetBidder: AppMonetBidder,
  val auctionManager: AuctionManager,
  val auctionManagerReadyCallbacks: ReadyCallbackManager<AppMonetWebView>,
  val auctionWebViewCreatedCallbacks: ReadyCallbackManager<AppMonetWebView>,
  val baseManager: BaseManager,
  val bidManager: BidManager,
  val deviceData: DeviceData,
  val uiThread: UIThread
)
