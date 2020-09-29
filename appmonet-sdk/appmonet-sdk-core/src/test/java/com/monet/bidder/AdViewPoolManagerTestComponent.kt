package com.monet.bidder

import android.content.Context
import android.webkit.ValueCallback
import com.monet.bidder.adview.AdViewManager
import com.monet.bidder.adview.AdViewPoolManager
import com.monet.bidder.auction.AuctionManagerCallback
import com.monet.bidder.threading.BackgroundThread
import com.monet.bidder.threading.UIThread
import io.mockk.mockk

internal data class AdViewPoolManagerTestComponent(
  val adViewPoolManager: AdViewPoolManager,
  val adServerWrapper: AdServerWrapper,
  val adViewsByContext: MutableMap<String, MutableList<AdViewManager>>,
  val adViewsReadyState: MutableMap<String, Boolean>,
  val adViewRefCount: MutableMap<String, Int>,
  val adViewManagerCollection: MutableMap<String, AdViewManager>,
  val auctionManagerCallback: AuctionManagerCallback,
  val appMonetContext: AppMonetContext,
  val context: Context,
  val messageHandlers: MutableMap<String, List<ValueCallback<String>>>,
  val pubSubService: PubSubService,
  val backgroundThread: BackgroundThread,
  val uiThread: UIThread
)
