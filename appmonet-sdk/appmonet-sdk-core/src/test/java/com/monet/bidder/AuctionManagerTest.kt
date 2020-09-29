package com.monet.bidder

import android.webkit.ValueCallback
import com.monet.bidder.adview.AdViewContext
import com.monet.bidder.adview.AdViewManager
import com.monet.bidder.adview.AdViewPoolManager
import com.monet.bidder.auction.AuctionManager
import com.monet.bidder.bid.BidManager
import com.monet.bidder.bid.BidResponse
import com.monet.bidder.callbacks.ReadyCallbackManager
import com.monet.bidder.threading.BackgroundThread
import com.monet.bidder.threading.InternalRunnable
import com.monet.bidder.threading.UIThread
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuctionManagerTest {

  @Test
  fun `Given executeCode is called and webView is ready call executeJsCode on webView`() {
    val auctionManagerComponent =
      auctionManagerSetup(auctionManagerReadyCallbacks = ReadyCallbackManager())
    auctionManagerComponent.auctionManagerReadyCallbacks
        .executeReady(auctionManagerComponent.appMonetWebView)
    auctionManagerComponent.auctionManager.executeCode("")
    verify(exactly = 1) { auctionManagerComponent.appMonetWebView.executeJsCode(any()) }
  }

  @Test
  fun `Given executeJs with method and args is called and webView is ready call executeJs on webView`() {
    val auctionManagerComponent =
      auctionManagerSetup(auctionManagerReadyCallbacks = ReadyCallbackManager())
    auctionManagerComponent.auctionManagerReadyCallbacks
        .executeReady(auctionManagerComponent.appMonetWebView)
    auctionManagerComponent.auctionManager.executeJs("", "")
    verify(exactly = 1) { auctionManagerComponent.appMonetWebView.executeJs("", "") }
  }

  @Test
  fun `Given executeJs with timeout, method, callback and args is called and webView is ready call executeJs on webView`() {
    val auctionManagerComponent = auctionManagerSetup()
    auctionManagerComponent.auctionManagerReadyCallbacks.executeReady(
        auctionManagerComponent.appMonetWebView
    )
    auctionManagerComponent.auctionManager.executeJs(0, "", null, "")
    verify(exactly = 1) {
      auctionManagerComponent.appMonetWebView.executeJs(
          any(), any(),
          null, *anyVararg()
      )
    }
  }

  @Test
  fun `Given getAdvertisingInfo and webView is ready deviceData getAdClientInfo is called`() {
    val auctionmanagerComponent = auctionManagerSetup()
    auctionmanagerComponent.auctionManagerReadyCallbacks.executeReady(
        auctionmanagerComponent.appMonetWebView
    )
    auctionmanagerComponent.auctionManager.getAdvertisingInfo()
    verify(exactly = 1) { auctionmanagerComponent.deviceData.getAdClientInfo(any()) }
  }

  @Test
  fun `Given getDeviceData and webView is ready deviceData is returned`() {
    val auctionManagerComponent = auctionManagerSetup()
    auctionManagerComponent.auctionManagerReadyCallbacks.executeReady(
        auctionManagerComponent.appMonetWebView
    )
    val deviceData = auctionManagerComponent.auctionManager.getDeviceData()
    assertEquals(auctionManagerComponent.deviceData, deviceData)
  }

  @Test
  fun `Given getMonetWebView and webView is ready AppMonetWebView is returned`() {
    val auctionManagerComponent =
      auctionManagerSetup(auctionManagerReadyCallbacks = ReadyCallbackManager())
    auctionManagerComponent.auctionManagerReadyCallbacks.executeReady(
        auctionManagerComponent.appMonetWebView
    )
    val appMonetWebView = auctionManagerComponent.auctionManager.getMonetWebView()
    assertEquals(auctionManagerComponent.appMonetWebView, appMonetWebView)
  }

  @Test
  fun `Given onInit and webView is ready execute setup`() {
    val mock = spyk(ReadyCallbackManager<AppMonetWebView>(), recordPrivateCalls = true)
    val auctionManagerComponent = auctionManagerSetup(auctionManagerReadyCallbacks = mock)
    auctionManagerComponent.auctionWebViewCreatedCallbacks
        .executeReady(auctionManagerComponent.appMonetWebView)
    auctionManagerComponent.auctionManager.onInit()
    verify(exactly = 2) { auctionManagerComponent.appMonetWebView.executeJs(any(), *anyVararg()) }
    verify(exactly = 1) { auctionManagerComponent.addBidsManager.executeReady() }
    verify(exactly = 1) { mock.executeReady(auctionManagerComponent.appMonetWebView) }
  }

  @Test
  fun `Given removeHelper and webView is ready adViewPoolManager remove is called`() {
    val auctionManagerComponent = auctionManagerSetup()
    auctionManagerComponent.auctionManagerReadyCallbacks
        .executeReady(auctionManagerComponent.appMonetWebView)
    auctionManagerComponent.auctionManager.removeHelper("uuid")
    verify(exactly = 1) {
      auctionManagerComponent.adViewPoolManager
          .remove("uuid", true)
    }
  }

  @Test
  fun `Given requestHelperDestroy and webView is ready adViewPoolManager requestDestroy is called`() {
    val auctionManagerComponent = auctionManagerSetup()
    auctionManagerComponent.auctionManagerReadyCallbacks
        .executeReady(auctionManagerComponent.appMonetWebView)
    auctionManagerComponent.auctionManager.requestHelperDestroy("uuid")
    verify(exactly = 1) { auctionManagerComponent.adViewPoolManager.requestDestroy("uuid") }
  }

  @Test
  fun `Given trackEvent and webView is ready webView trackEvent is called`() {
    val auctionManagerComponent = auctionManagerSetup()
    auctionManagerComponent.auctionManagerReadyCallbacks.executeReady(
        auctionManagerComponent.appMonetWebView
    )
    auctionManagerComponent.auctionManager.trackEvent(
        "", "", "", 0F,
        0L
    )
    verify(exactly = 1) {
      auctionManagerComponent.appMonetWebView.trackEvent(
          any(), any(),
          any(), any(), any()
      )
    }
  }

  @Test
  fun `Given trackRequest and webView is ready webView executeJs is called`() {
    val auctionManagerComponent = auctionManagerSetup()
    auctionManagerComponent.auctionManagerReadyCallbacks.executeReady(
        auctionManagerComponent.appMonetWebView
    )
    auctionManagerComponent.auctionManager.trackRequest("", "")
    verify(exactly = 1) {
      auctionManagerComponent.appMonetWebView
          .executeJs(Constants.JSMethods.TRACK_REQUEST, "''", "''")
    }
  }

  @Test
  fun `Given addBids with AdServerAdView and AdServerAdRequest AppMonetBidder addBids is called`() {
    val auctionManagerComponent = auctionManagerSetup()
    auctionManagerComponent.auctionManager.addBids(
        mockk(relaxed = true),
        mockk(relaxed = true)
    )
    verify(exactly = 1) { auctionManagerComponent.appMonetBidder.addBids(any(), any()) }
  }

  @Test
  fun `Given addBids with AdServerAdView, AdServerAdRequest, remainingTime, and valueCallback AppMonetBidder addBids is called`() {
    val auctionManagerComponent = auctionManagerSetup()
    auctionManagerComponent.auctionManager.addBids(
        mockk(relaxed = true),
        mockk(relaxed = true), 0, mockk(relaxed = true)
    )
    verify(exactly = 1) {
      auctionManagerComponent.appMonetBidder.addBids(
          any(), any(),
          0, any()
      )
    }
  }

  @Test
  fun `Given disableBidCleaner BidManager disableIntervalCleaner is called`() {
    val auctionManagerComponent = auctionManagerSetup()
    auctionManagerComponent.auctionManager.disableBidCleaner()
    verify(exactly = 1) { auctionManagerComponent.bidManager.disableIntervalCleaner() }
  }

  @Test
  fun `Given enableBidCleaner BidManager enableBidManager is called`() {
    val auctionManagerComponent = auctionManagerSetup()
    auctionManagerComponent.auctionManager.enableBidCleaner()
    verify(exactly = 1) { auctionManagerComponent.bidManager.enableIntervalCleaner() }
  }

  @Test
  fun `Given getBidById BidManager enableBidManager is getBidById`() {
    val auctionManagerComponent = auctionManagerSetup()
    auctionManagerComponent.auctionManager.getBidById("")
    verify(exactly = 1) { auctionManagerComponent.bidManager.getBidById(any()) }
  }

  @Test
  fun `Given indicateRequest and webView is ready executeJs is called`() {
    val auctionManagerComponent = auctionManagerSetup()
    auctionManagerComponent.auctionManagerReadyCallbacks.executeReady(
        auctionManagerComponent.appMonetWebView
    )
    auctionManagerComponent.auctionManager.indicateRequest(
        "", mockk(relaxed = true),
        mockk(relaxed = true), 0.0
    )
    verify(exactly = 1) {
      auctionManagerComponent.auctionManager.executeJs(
          Constants.JSMethods.FETCH_BIDS_BLOCKING, *anyVararg()
      )
    }
  }

  @Test
  fun `Given indicateRequestAsync and webView is ready executeJs is called`() {
    val auctionManagerComponent = auctionManagerSetup()
    auctionManagerComponent.auctionManagerReadyCallbacks
        .executeReady(auctionManagerComponent.appMonetWebView)
    auctionManagerComponent.auctionManager.indicateRequestAsync(
        "", 0, mockk(relaxed = true),
        mockk(relaxed = true), 0.0, mockk(relaxed = true)
    )
    verify(exactly = 1) {
      auctionManagerComponent.auctionManager.executeJs(
          0,
          Constants.JSMethods.FETCH_BIDS_BLOCKING, any(), *anyVararg()
      )
    }
  }

  @Test
  fun `Given logState, BidManager and AdViewPoolManager logState is called`() {
    val auctionManagerComponent = auctionManagerSetup()
    auctionManagerComponent.auctionManager.logState()
    verify(exactly = 1) { auctionManagerComponent.bidManager.logState() }
    verify(exactly = 1) { auctionManagerComponent.adViewPoolManager.logState() }
  }

  @Test
  fun `Given prefetchAdUnits and webView is ready executeJs is called`() {
    val auctionManagerComponent = auctionManagerSetup()
    auctionManagerComponent.auctionManagerReadyCallbacks
        .executeReady(auctionManagerComponent.appMonetWebView)
    val adUnits = listOf("test1", "test2", "test3")
    auctionManagerComponent.auctionManager.prefetchAdUnits(adUnits)
    verify(exactly = 1) {
      auctionManagerComponent.appMonetWebView.executeJs(
          Constants.JSMethods.PREFETCH_UNITS,
          "'test1','test2','test3'"
      )
    }
  }

  @Test
  fun `Given registerFloatingAd and webView is ready executeJs is called`() {
    val auctionManagerComponent = auctionManagerSetup()
    auctionManagerComponent.auctionManagerReadyCallbacks
        .executeReady(auctionManagerComponent.appMonetWebView)
    auctionManagerComponent.auctionManager.registerFloatingAd(mockk(relaxed = true))
    verify(exactly = 1) {
      auctionManagerComponent.appMonetWebView.executeJs(
          Constants.JSMethods.REGISTER_FLOATING_AD,
          *anyVararg()
      )
    }
  }

  @Test
  fun `Given syncLogger and webView is ready executeJs is called`() {
    val auctionManagerComponent = auctionManagerSetup()
    auctionManagerComponent.auctionManagerReadyCallbacks
        .executeReady(auctionManagerComponent.appMonetWebView)
    auctionManagerComponent.auctionManager.syncLogger()
    verify(exactly = 1) {
      auctionManagerComponent.appMonetWebView.executeJs(
          Constants.JSMethods.SET_LOG_LEVEL,
          *anyVararg()
      )
    }
  }

  @Test
  fun `Given testMode and webView is ready executeJs is called`() {
    val auctionManagerComponent = auctionManagerSetup()
    auctionManagerComponent.auctionManagerReadyCallbacks
        .executeReady(auctionManagerComponent.appMonetWebView)
    auctionManagerComponent.auctionManager.testMode()
    verify(exactly = 1) {
      auctionManagerComponent.appMonetWebView.executeJs("testMode")
    }
  }

  @Test
  fun `Given timedCallback AddBidsManager onReady is called`() {
    val auctionManagerComponent = auctionManagerSetup()
    auctionManagerComponent.auctionManager.timedCallback(0, mockk(relaxed = true))
    verify(exactly = 1) {
      auctionManagerComponent.addBidsManager.onReady(0, any())
    }
  }

  @Test
  fun `Given trackAppState and webView is ready executeJs is called`() {
    val auctionManagerComponent = auctionManagerSetup()
    auctionManagerComponent.auctionManagerReadyCallbacks
        .executeReady(auctionManagerComponent.appMonetWebView)
    auctionManagerComponent.auctionManager.trackAppState("appState", "id")
    verify(exactly = 1) {
      auctionManagerComponent.appMonetWebView
          .executeJs(Constants.JSMethods.TRACK_APP_STATE, "'appState'", "'id'")
    }
  }

  @Test
  fun `Given markBidAsUsed 'bidManager#markused' should be called`() {
    val bid = mockk<BidResponse>()
    val auctionManagerComponent = auctionManagerSetup()
    auctionManagerComponent.auctionManager.markBidAsUsed(bid)
    verify(exactly = 1) { auctionManagerComponent.bidManager.markUsed(bid) }
  }

  @Test
  fun `Given invalidateBidsForAdView 'bidManager#invalidate' should be called`() {
    val auctionManagerComponent = auctionManagerSetup()
    auctionManagerComponent.auctionManager.invalidateBidsForAdView("uuid")
    verify(exactly = 1) { auctionManagerComponent.bidManager.invalidate("uuid") }
  }

  @Test
  fun `Given getSdkConfigurations 'baseManager#sdkConfigurations' is called`() {
    val baseManager = mockk<BaseManager>(relaxed = true)
    val sdkConfig = mockk<SdkConfigurations>(relaxed = true)
    every { baseManager.sdkConfigurations } answers { sdkConfig }
    val auctionManagerComponent = auctionManagerSetup(baseManager = baseManager)
    auctionManagerComponent.auctionManager.getSdkConfigurations()
    assertEquals(auctionManagerComponent.baseManager.sdkConfigurations, sdkConfig)
  }

  @Test
  fun `Given removeBid 'bidManager#removeBid' is called`() {
    val auctionManagerComponent = auctionManagerSetup()
    auctionManagerComponent.auctionManager.removeBid("bidId")
    verify { auctionManagerComponent.bidManager.removeBid("bidId") }
  }

  @Test
  fun `Given helperCreated and auctionManager is ready call webView executeJs`() {
    val auctionManagerComponent = auctionManagerSetup()
    auctionManagerComponent.auctionManagerReadyCallbacks.executeReady(
        auctionManagerComponent.appMonetWebView
    )
    auctionManagerComponent.auctionManager.helperCreated("", "")
    verify { auctionManagerComponent.appMonetWebView.executeJs("helperCreated", any(), any()) }
  }

  @Test
  fun `Given helperDestroyed and auctionManager is ready call webView executeJs`() {
    val auctionManagerComponent = auctionManagerSetup()
    auctionManagerComponent.auctionManagerReadyCallbacks.executeReady(
        auctionManagerComponent.appMonetWebView
    )
    auctionManagerComponent.auctionManager.helperDestroy("")
    verify { auctionManagerComponent.appMonetWebView.executeJs("helperDestroy", any()) }
  }

  @Test
  fun `Given helperLoaded and auctionManager is ready call webView executeJs`() {
    val auctionManagerComponent = auctionManagerSetup()
    auctionManagerComponent.auctionManagerReadyCallbacks.executeReady(
        auctionManagerComponent.appMonetWebView
    )
    auctionManagerComponent.auctionManager.helperLoaded("")
    verify { auctionManagerComponent.appMonetWebView.executeJs("helperLoaded", any()) }
  }

  @Test
  fun `Given loadHelper and we are on uithread adViewPoolManager request is called and adviewmanager has not loaded call load and callback onReceive`() {
    val auctionManagerComponent = auctionManagerSetup()
    every { auctionManagerComponent.uiThread.run(any()) } answers {
      firstArg<InternalRunnable>().runInternal()
    }
    val adViewManager = mockk<AdViewManager>(relaxed = true)
    every { adViewManager.isLoaded } answers { false }
    val callbackCaptor = mockk<ValueCallback<AdViewManager>>(relaxed = true)
    every {
      auctionManagerComponent.adViewPoolManager.request(
          any<AdViewContext>()
      )
    } answers { adViewManager }
    auctionManagerComponent.auctionManager.loadHelper("", "", "", 0, 0, "", callbackCaptor)
    verify(exactly = 1) { auctionManagerComponent.adViewPoolManager.request(any<AdViewContext>()) }
    verify(exactly = 1) { adViewManager.load() }
  }

  @Test
  fun `Given cancelRequest appMonetBidder cancel request is called`() {
    val auctionManagerComponent = auctionManagerSetup()
    auctionManagerComponent.auctionManager.cancelRequest("", mockk(), mockk())
    verify(exactly = 1) {
      auctionManagerComponent.appMonetBidder.cancelRequest(
          any(), any(), any()
      )
    }

  }

  private fun auctionManagerSetup(
    addBidsManager: AddBidsManager = mockk(relaxed = true),
    adViewPoolManager: AdViewPoolManager = mockk(relaxed = true),
    appMonetBidder: AppMonetBidder = mockk(relaxed = true),
    appMonetContext: AppMonetContext = mockk(relaxed = true),
    auctionManagerReadyCallbacks: ReadyCallbackManager<AppMonetWebView>
    = ReadyCallbackManager(),
    auctionWebView: AppMonetWebView = mockk(relaxed = true),
    auctionWebViewCreatedCallbacks: ReadyCallbackManager<AppMonetWebView>
    = ReadyCallbackManager(),
    backgroundThread: BackgroundThread = mockk(relaxed = true),
    baseManager: BaseManager = mockk(relaxed = true),
    bidManager: BidManager = mockk(relaxed = true),
    uiThread: UIThread = mockk(relaxed = true),
    deviceData: DeviceData = mockk(relaxed = true),
    mediationManager: MediationManager = mockk(relaxed = true)
  )
      : AuctionManagerTestComponent {
    val auctionManager = AuctionManager(
        addBidsManager, adViewPoolManager,
        appMonetBidder, appMonetContext, auctionManagerReadyCallbacks,
        auctionWebView, auctionWebViewCreatedCallbacks, backgroundThread, baseManager,
        bidManager, mediationManager, uiThread, deviceData
    )

    return AuctionManagerTestComponent(
        addBidsManager, adViewPoolManager,
        auctionWebView, appMonetBidder, auctionManager, auctionManagerReadyCallbacks,
        auctionWebViewCreatedCallbacks, baseManager, bidManager, deviceData, uiThread
    )
  }
}
