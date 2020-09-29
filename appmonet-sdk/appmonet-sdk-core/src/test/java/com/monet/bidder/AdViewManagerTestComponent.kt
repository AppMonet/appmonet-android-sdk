package com.monet.bidder

import android.content.Context
import com.monet.bidder.adview.AdView
import com.monet.bidder.adview.AdViewClient
import com.monet.bidder.adview.AdViewContext
import com.monet.bidder.adview.AdViewManager
import com.monet.bidder.adview.AdViewPoolManagerCallback
import com.monet.bidder.auction.AuctionManagerCallback
import com.monet.bidder.callbacks.ReadyCallbackManager
import com.monet.bidder.threading.BackgroundThread
import com.monet.bidder.threading.UIThread

internal data class AdViewManagerTestComponent(val adSize: AdSize,
                                               val adServerWrapper: AdServerWrapper,
                                               val adView: AdView,
                                               val adViewClient: AdViewClient,
                                               val adViewContext: AdViewContext,
                                               val adViewHtml: String,
                                               val adViewManager: AdViewManager,
                                               val adViewPoolManagerCallback: AdViewPoolManagerCallback,
                                               val adViewReadyCallback: ReadyCallbackManager<AdView>,
                                               val adViewState: AdViewManager.AdViewState,
                                               val appMonetContext: AppMonetContext,
                                               val auctionManagerCallback: AuctionManagerCallback,
                                               val backgroundThread: BackgroundThread,
                                               val containerView: AppMonetViewLayout,
                                               val context: Context,
                                               val injectionDelay: Int,
                                               val pubSubService: PubSubService,
                                               val uiThread: UIThread,
                                               val uuid: String)