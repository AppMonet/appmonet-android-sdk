package com.monet.bidder

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.core.content.ContextCompat
import com.monet.bidder.WebViewUtils.quote
import com.monet.bidder.adview.AdView
import com.monet.bidder.adview.AdViewClient
import com.monet.bidder.adview.AdViewContext
import com.monet.bidder.adview.AdViewManager
import com.monet.bidder.adview.AdViewManager.AdViewState.AD_RENDERED
import com.monet.bidder.adview.AdViewManager.Companion
import com.monet.bidder.adview.AdViewPoolManagerCallback
import com.monet.bidder.auction.AuctionManagerCallback
import com.monet.bidder.bid.BidResponse
import com.monet.bidder.bid.Pixel
import com.monet.bidder.callbacks.ReadyCallbackManager
import com.monet.bidder.threading.BackgroundThread
import com.monet.bidder.threading.InternalRunnable
import com.monet.bidder.threading.UIThread
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotSame
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.reflect.Whitebox
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.Future

@RunWith(RobolectricTestRunner::class)
class AdViewManagerTest {
  @Test
  fun `Given adViewEnvironment and containerView parent is null return "RENDER_ENV"`() {
    val containerView = mockk<AppMonetViewLayout>(relaxed = true)
    every { containerView.parent } returns null
    val adViewManagerComponent = adViewManagerSetup(containerView = containerView)
    assertEquals(adViewManagerComponent.adViewManager.adViewEnvironment, "RENDER_ENV")
  }

  @Test
  fun `Given adViewEnvironment and containerView parent is not null return "LOADING_ENV"`() {
    val containerView = mockk<AppMonetViewLayout>(relaxed = true)
    every { containerView.parent } returns mockk()
    val adViewManagerComponent = adViewManagerSetup(containerView = containerView)
    assertEquals(adViewManagerComponent.adViewManager.adViewEnvironment, "LOADING_ENV")
  }

  @Test
  fun `Given executeJs is called and adViewReadyCallback is ready webview executeJS is called`() {
    val adViewManagerComponent = adViewManagerSetup()
    adViewManagerComponent.adViewReadyCallback
        .executeReady(adViewManagerComponent.adView)
    adViewManagerComponent.adViewManager.executeJs("", "")
    verify(exactly = 1) { adViewManagerComponent.adViewManager.adView.executeJs("", "") }
  }

  @Test
  fun `Given adView visibility is VISIBLE adViewVisibility will return visible`() {
    val adViewManagerComponent = adViewManagerSetup()
    val adView = adViewManagerComponent.adView
    every { adView.visibility } answers { View.VISIBLE }
    assertEquals(adViewManagerComponent.adViewManager.adViewVisibility, "visible")
  }

  @Test
  fun `Given adView visibility is INVISIBLE adViewVisibility will return invisible`() {
    val adViewManagerComponent = adViewManagerSetup()
    val adView = adViewManagerComponent.adView
    every { adView.visibility } answers { View.INVISIBLE }
    assertEquals(adViewManagerComponent.adViewManager.adViewVisibility, "invisible")
  }

  @Test
  fun `Given adView visibility is GONE adViewVisibility will return gone`() {
    val adViewManagerComponent = adViewManagerSetup()
    val adView = adViewManagerComponent.adView
    every { adView.visibility } answers { View.GONE }
    assertEquals(adViewManagerComponent.adViewManager.adViewVisibility, "gone")
  }

  @Test
  fun `Given adView visibility returns something weird adViewVisibility will return unknown`() {
    val adViewManagerComponent = adViewManagerSetup()
    val adView = adViewManagerComponent.adView
    every { adView.visibility } answers { 0x00000010 }
    assertEquals(adViewManagerComponent.adViewManager.adViewVisibility, "unknown")
  }

  @Test
  fun `Given ready is called isLoaded should be set to true and AdViewReadyCallback executeReady should be called`() {
    val auctionManagerComponent = adViewManagerSetup(adViewReadyCallback = mockk(relaxed = true))
    auctionManagerComponent.adViewManager.ready()
    verify(exactly = 1) { auctionManagerComponent.adViewReadyCallback.executeReady(any()) }
    assertEquals(auctionManagerComponent.adViewManager.isLoaded, true)
  }

  @Test
  fun `Given setBackground is called adview backgroundColor is called in UI Thread`() {
    val adViewManagerComponent = adViewManagerSetup()
    every { adViewManagerComponent.uiThread.run(any()) } answers {
      firstArg<InternalRunnable>().runInternal()
    }
    adViewManagerComponent.adViewManager.setBackgroundColor("#ffffff")
    verify(exactly = 1) { adViewManagerComponent.uiThread.run(any()) }
    verify(exactly = 1) {
      adViewManagerComponent.adView.setBackgroundColor(
          Color.parseColor("#ffffff")
      )
    }
  }

  @Test
  fun `Given destroy is called adView destroy is called and adviewPoolManagerCallback remove is called in a background thread`() {
    val future = mockk<Future<*>>(relaxed = true)
    val adViewManagerComponent = adViewManagerSetup()
    every { adViewManagerComponent.backgroundThread.execute(any()) } answers {
      firstArg<InternalRunnable>().runInternal()
      future
    }
    adViewManagerComponent.adViewManager.destroy()
    verify(exactly = 1) { adViewManagerComponent.adView.destroy() }

    verify {
      adViewManagerComponent.adViewPoolManagerCallback.remove(any(), false)
    }
  }

  @Test
  fun `Given checkOverride is called and adServerListener is null return false`() {
    val adViewManagerComponent = adViewManagerSetup()
    assertEquals(adViewManagerComponent.adViewManager.checkOverride(mockk(), mockk()), false)
  }

  @Test
  fun `Given checkOverride is called and adServerListener is not null and uri is 'monet finishLoad' but bidForTracking is null return false`() {
    val adViewManagerComponent = adViewManagerSetup()
    adViewManagerComponent.adViewReadyCallback.executeReady(adViewManagerComponent.adView)
    adViewManagerComponent.adViewManager.setState(AD_RENDERED, mockk(), mockk())
    adViewManagerComponent.adViewManager.bidForTracking = null
    assertEquals(
        adViewManagerComponent.adViewManager.checkOverride(
            mockk(), Uri.parse("monet://finishLoad")
        ), false
    )
  }

  @Test
  fun `Given checkOverride is called and adServerListener is not null and uri is 'monet finishLoad' and bidForTracking is not null and not nativeRender return true`() {
    val adViewManagerComponent = adViewManagerSetup()
    val adViewManager = spyk(adViewManagerComponent.adViewManager)
    val bidForTracking = mockk<BidResponse>(relaxed = true)
    every { bidForTracking.nativeRender } returns false
    adViewManagerComponent.adViewReadyCallback.executeReady(adViewManagerComponent.adView)
    adViewManager.setState(AD_RENDERED, mockk(), mockk())
    adViewManager.bidForTracking = bidForTracking
    assertEquals(
        adViewManager.checkOverride(
            mockk(), Uri.parse("monet://finishLoad")
        ), true
    )
    verify(exactly = 1) { adViewManager.callFinishLoad(bidForTracking) }
  }

  @Test
  fun `Given checkOverride is called and adServerListener is not null and uri is 'monet finishLoad' and bidForTracking is not null and nativeRender return true`() {
    val adViewManagerComponent = adViewManagerSetup()
    val bidForTracking = mockk<BidResponse>(relaxed = true)
    every { bidForTracking.nativeRender } returns true
    adViewManagerComponent.adViewReadyCallback.executeReady(adViewManagerComponent.adView)
    adViewManagerComponent.adViewManager.setState(AD_RENDERED, mockk(), mockk())
    adViewManagerComponent.adViewManager.bidForTracking = bidForTracking
    assertEquals(
        adViewManagerComponent.adViewManager.checkOverride(
            mockk(), Uri.parse("monet://finishLoad")
        ), true
    )
    verify(exactly = 0) {
      spyk(adViewManagerComponent.adViewManager).callFinishLoad(
          bidForTracking
      )
    }
  }

  @Test
  fun `Given checkOverride is called and adServerListener is not null and uri is 'monet failLoad' and bid is null return false`() {
    val adViewManagerComponent = adViewManagerSetup()
    adViewManagerComponent.adViewReadyCallback.executeReady(adViewManagerComponent.adView)
    adViewManagerComponent.adViewManager.setState(AD_RENDERED, mockk(), mockk())
    adViewManagerComponent.adViewManager.bid = null
    assertEquals(
        adViewManagerComponent.adViewManager.checkOverride(
            mockk(), Uri.parse("monet://failLoad")
        ), false
    )
  }

  @Test
  fun `Given checkOverride is called and adServerListener is not null and uri is 'monet failLoad' and bid is not null and not nativeRender and has not finish loading return true and trigger error`() {
    val adViewManagerComponent = adViewManagerSetup()
    val bid = mockk<BidResponse>(relaxed = true)
    val adServerListener = mockk<AdServerBannerListener>(relaxed = true)
    every { bid.nativeRender } returns false
    adViewManagerComponent.adViewReadyCallback.executeReady(adViewManagerComponent.adView)
    adViewManagerComponent.adViewManager.setState(AD_RENDERED, adServerListener, mockk())
    adViewManagerComponent.adViewManager.bid = bid
    assertEquals(
        adViewManagerComponent.adViewManager.checkOverride(
            mockk(), Uri.parse("monet://failLoad")
        ), true
    )

    verify(exactly = 1) { adServerListener.onAdError(any()) }
  }

//  @Test
//  fun `Given checkOverride is called and adServerListener is not null and uri is 'monet failLoad' and bid is not null and not nativeRender and has finished loading return true and do not trigger error`() {
//    val adViewManagerComponent = adViewManagerSetup()
//    val adViewManager = spyk(adViewManagerComponent.adViewManager, recordPrivateCalls = true)
//    every { adViewManager setProperty "mHasCalledFinishLoad" value true } just runs
////    every { adViewManager getProperty "mHasCalledFinishLoad" } propertyType Boolean::class answers { true }
//    val bid = mockk<BidResponse>(relaxed = true)
//    val adServerListener = mockk<AdServerBannerListener>(relaxed = true)
//    every { bid.nativeRender } returns false
//    adViewManagerComponent.adViewReadyCallback.executeReady(adViewManagerComponent.adView)
//    adViewManager.setState(AD_RENDERED, adServerListener, mockk())
//    adViewManager.bid = bid
//    assertEquals(
//        adViewManager.checkOverride(
//            mockk(), Uri.parse("monet://failLoad")
//        ), true
//    )
//
//    verify(exactly = 0) { adServerListener.onAdError(any()) }
//  }

  @Test
  fun `Given checkOverride is called and adServerListener is not null and uri is http and it was clicked return true`() {
    val adViewManagerComponent = adViewManagerSetup()
    val adServerListener = mockk<AdServerBannerListener>(relaxed = true)
    val adViewManager = spyk(adViewManagerComponent.adViewManager, recordPrivateCalls = true)
    every { adViewManager.wasAdViewClicked } returns true
    adViewManagerComponent.adViewReadyCallback.executeReady(adViewManagerComponent.adView)
    adViewManager.setState(AD_RENDERED, adServerListener, mockk())
    assertEquals(
        adViewManager.checkOverride(
            mockk(), Uri.parse("http://url.com")
        ), true
    )
    verify(exactly = 1) { adViewManager.handleAdInteraction(any(), any()) }
  }

  @Test
  fun `Given checkOverride is called and adServerListener is not null and uri is http and it was not clicked return false`() {
    val adViewManagerComponent = adViewManagerSetup()
    val adServerListener = mockk<AdServerBannerListener>(relaxed = true)
    val adViewManager = spyk(adViewManagerComponent.adViewManager, recordPrivateCalls = true)
    every { adViewManager.wasAdViewClicked } returns false
    adViewManagerComponent.adViewReadyCallback.executeReady(adViewManagerComponent.adView)
    adViewManager.setState(AD_RENDERED, adServerListener, mockk())
    assertEquals(
        adViewManager.checkOverride(
            mockk(), Uri.parse("http://url.com")
        ), false
    )
    verify(exactly = 0) { adViewManager.handleAdInteraction(any(), any()) }
  }

  @Test
  fun `Given checkOverride is called and adServerListener is not null and uri is market loadAppStoreUrl should be called`() {
    val adViewManagerComponent = adViewManagerSetup()
    val adServerListener = mockk<AdServerBannerListener>(relaxed = true)
    val adViewManager = spyk(adViewManagerComponent.adViewManager, recordPrivateCalls = true)
    every { adViewManager.wasAdViewClicked } returns false
    every { adViewManager.loadAppStoreUrl(any(), any()) } answers { true }
    adViewManagerComponent.adViewReadyCallback.executeReady(adViewManagerComponent.adView)
    adViewManager.setState(AD_RENDERED, adServerListener, mockk())
    adViewManager.checkOverride(
        mockk(), Uri.parse("market://play.store")
    )
    verify(exactly = 1) { adViewManager.loadAppStoreUrl(any(), any()) }
  }

  @Test
  fun `Given handleAdInteraction is called adServerListener null nothing should be called`() {
    val adViewManagerComponent = adViewManagerSetup()
    val adViewManager = spyk(adViewManagerComponent.adViewManager)
    adViewManagerComponent.adViewManager.bid = mockk()
    adViewManagerComponent.adViewManager.handleAdInteraction(mockk(relaxed = true), "")
    verify(exactly = 0) { adViewManager.executeJs(any(), any()) }
  }

  @Test
  fun `Given handleAdInteraction is called bid null nothing should be called`() {
    val adViewManagerComponent = adViewManagerSetup()
    val adViewManager = spyk(adViewManagerComponent.adViewManager)
    adViewManagerComponent.adViewManager.setState(AD_RENDERED, mockk(), mockk())
    adViewManagerComponent.adViewManager.handleAdInteraction(mockk(relaxed = true), "")
    verify(exactly = 0) { adViewManager.executeJs(any(), any()) }
  }

  @Test
  fun `Given handleAdInteraction is called url equals adviewUrl nothing should be called`() {
    val adViewManagerComponent =
      adViewManagerSetup(adViewContext = AdViewContext("url", "", 0, 0, ""))
    val adViewManager = spyk(adViewManagerComponent.adViewManager)
    adViewManagerComponent.adViewManager.bid = mockk()
    adViewManagerComponent.adViewManager.setState(AD_RENDERED, mockk(), mockk())
    adViewManagerComponent.adViewManager.handleAdInteraction(mockk(relaxed = true), "url")
    verify(exactly = 0) { adViewManager.executeJs(any(), any()) }
  }

  @Test
  fun `Given handleAdInteraction is called url does not equal adviewUrl executeJS navigation start should be called`() {
    val adViewManagerComponent =
      adViewManagerSetup(adViewContext = AdViewContext("url_context", "", 0, 0, ""))
    adViewManagerComponent.adViewReadyCallback
        .executeReady(adViewManagerComponent.adView)
//    val adViewManager = spyk(adViewManagerComponent.adViewManager)
    adViewManagerComponent.adViewManager.bid = mockk()
    adViewManagerComponent.adViewManager.setState(
        AD_RENDERED, mockk(relaxed = true), mockk(relaxed = true)
    )
    adViewManagerComponent.adViewManager.handleAdInteraction(mockk(relaxed = true), "url")
    verify(exactly = 1) {
      adViewManagerComponent.adViewManager.adView.executeJs(
          Constants.JSMethods.NAVIGATION_START, quote("url")
      )
    }
  }

  @Test
  fun `Given handleAdInteraction is called url is sane adview was clicked and browser is not opening adView stopLoading is called and openUrlInBrowser is called`() {
    val adViewManagerComponent =
      adViewManagerSetup(adViewContext = AdViewContext("url_context", "", 0, 0, ""))
    val adViewManager = spyk(adViewManagerComponent.adViewManager)
    adViewManagerComponent.adViewReadyCallback
        .executeReady(adViewManagerComponent.adView)
    adViewManager.bid = mockk(relaxed = true)
    adViewManager.setState(
        AD_RENDERED, mockk(relaxed = true), mockk(relaxed = true)
    )
    adViewManager.wasAdViewClicked = true
    adViewManager.isBrowserOpening = false
    val webView: AdView = mockk(relaxed = true)
    adViewManager.handleAdInteraction(
        webView, "http://test.com"
    )
    verify(exactly = 1) { webView.stopLoading() }
    verify(exactly = 1) { adViewManager.openUrlInBrowser(any()) }
  }

  @Test
  fun `Given handleAdInteraction is called url is marketplace stopLoading is called and loadAppStoreUrl`() {
    val adViewManagerComponent =
      adViewManagerSetup(adViewContext = AdViewContext("url_context", "", 0, 0, ""))
    val adViewManager = spyk(adViewManagerComponent.adViewManager)
    adViewManagerComponent.adViewReadyCallback
        .executeReady(adViewManagerComponent.adView)
    adViewManager.bid = mockk(relaxed = true)
    adViewManager.setState(
        AD_RENDERED, mockk(relaxed = true), mockk(relaxed = true)
    )
    val webView: AdView = mockk(relaxed = true)
    adViewManager.handleAdInteraction(
        webView, "market://market"
    )
    verify(exactly = 1) { webView.stopLoading() }
    verify(exactly = 1) { adViewManager.loadAppStoreUrl(any(), any()) }
  }

  @Test
  fun `Given handleAdInteraction is called url is sane adview was clicked and browser is  opening adView stopLoading is called`() {
    val adViewManagerComponent =
      adViewManagerSetup(adViewContext = AdViewContext("url_context", "", 0, 0, ""))
    val adViewManager = spyk(adViewManagerComponent.adViewManager)
    adViewManagerComponent.adViewReadyCallback
        .executeReady(adViewManagerComponent.adView)
    adViewManager.bid = mockk(relaxed = true)
    adViewManager.setState(
        AD_RENDERED, mockk(relaxed = true), mockk(relaxed = true)
    )
    adViewManager.wasAdViewClicked = true
    adViewManager.isBrowserOpening = true
    val webView: AdView = mockk(relaxed = true)
    adViewManager.handleAdInteraction(
        webView, "http://test.com"
    )
    verify(exactly = 1) { webView.stopLoading() }
    verify(exactly = 0) { adViewManager.openUrlInBrowser(any()) }
  }

//  @Test
//  fun `Given isAdViewAttachedToLayout is called and SDK_INT gt KITKAT containerView isAttachedToWindow return attached_window`() {
//    val containerView = mockk<AppMonetViewLayout>(relaxed = true)
//    every { containerView.isAttachedToWindow } answers { true }
//    val adViewManagerComponent = adViewManagerSetup(containerView = containerView)
//    Whitebox.setInternalState(Build.VERSION::class.java, "SDK_INT", 23)
//    assertEquals(adViewManagerComponent.adViewManager.isAdViewAttachedToLayout(), "attached_window")
//  }

  @Test
  fun `Given isAdViewAttachedToLayout is called and SDK_INT lt KITKAT containerView windowVisibility GONE return window_gone`() {
    val adView = mockk<AdView>(relaxed = true)
    every { adView.windowVisibility } answers { GONE }
    val adViewManagerComponent = adViewManagerSetup(adView = adView)
    Whitebox.setInternalState(Build.VERSION::class.java, "SDK_INT", 9)
    assertEquals(adViewManagerComponent.adViewManager.isAdViewAttachedToLayout(), "window_gone")
  }

  @Test
  fun `Given isAdViewAttachedToLayout is called and SDK_INT lt KITKAT containerView windowVisibility INVISIBLE return window_invisible`() {
    val adView = mockk<AdView>(relaxed = true)
    every { adView.windowVisibility } answers { INVISIBLE }
    val adViewManagerComponent = adViewManagerSetup(adView = adView)
    Whitebox.setInternalState(Build.VERSION::class.java, "SDK_INT", 9)
    assertEquals(
        adViewManagerComponent.adViewManager.isAdViewAttachedToLayout(), "window_invisible"
    )
  }

  @Test
  fun `Given isAdViewAttachedToLayout is called and SDK_INT lt KITKAT containerView windowVisibility VISIBLE return window_visible`() {
    val adView = mockk<AdView>(relaxed = true)
    every { adView.windowVisibility } answers { VISIBLE }
    val adViewManagerComponent = adViewManagerSetup(adView = adView)
    Whitebox.setInternalState(Build.VERSION::class.java, "SDK_INT", 9)
    assertEquals(adViewManagerComponent.adViewManager.isAdViewAttachedToLayout(), "window_visible")
  }

  @Test
  fun `Given markBidRendered and bid != null bidForTracking = bid`() {
    val auctionManagerCallback = mockk<AuctionManagerCallback>()
    val bid = mockk<BidResponse>()
    every { auctionManagerCallback.removeBid("bidId") } answers { bid }
    val adViewManagerComponent = adViewManagerSetup(auctionManagerCallback = auctionManagerCallback)
    adViewManagerComponent.adViewManager.markBidRendered("bidId")
    verify(exactly = 1) { auctionManagerCallback.removeBid("bidId") }
    assertEquals(adViewManagerComponent.adViewManager.bidForTracking, bid)
  }

  @Test
  fun `Given markBidRendered and bid == null bidForTracking != bid`() {
    val auctionManagerCallback = mockk<AuctionManagerCallback>()
    val bid = mockk<BidResponse>()
    every { auctionManagerCallback.removeBid("bidId") } answers { null }
    val adViewManagerComponent = adViewManagerSetup(auctionManagerCallback = auctionManagerCallback)
    adViewManagerComponent.adViewManager.markBidRendered("bidId")
    verify(exactly = 1) { auctionManagerCallback.removeBid("bidId") }
    assertNotSame(adViewManagerComponent.adViewManager.bidForTracking, bid)
  }

  @Test
  fun `Given nativePlacement pubService is executed`() {
    val pubSubService = mockk<PubSubService>(relaxed = true)
    val adViewManagerComponent = adViewManagerSetup(pubSubService = pubSubService)
    adViewManagerComponent.adViewManager.nativePlacement("key", "value")
    verify(exactly = 1) { pubSubService.addMessageToQueue(any()) }
    verify(exactly = 1) { pubSubService.broadcast() }
  }

  @Test
  fun `Given onAdViewTouchEvent ACTION_DOWN hasAdVieTouchStarted = true return false`() {
    val adViewManagerComponent = adViewManagerSetup()
    val motionEvent = mockk<MotionEvent>()
    every { motionEvent.action } answers { MotionEvent.ACTION_DOWN }
    assertEquals(adViewManagerComponent.adViewManager.onAdViewTouchEvent(motionEvent), false)
  }

  @Test
  fun `Given onAdViewTouchEvent ACTION_UP return false`() {
    val adViewManagerComponent = adViewManagerSetup()
    val motionEvent = mockk<MotionEvent>()
    every { motionEvent.action } answers { MotionEvent.ACTION_UP }
    assertEquals(adViewManagerComponent.adViewManager.onAdViewTouchEvent(motionEvent), false)
  }

  @Test
  fun `Given callFinishLoad and adServerListener == null uiThread is not called`() {
    val adViewManagerComponent = adViewManagerSetup()
    val mock = spyk(adViewManagerComponent.adViewManager, recordPrivateCalls = true)

    mock.callFinishLoad(mockk(relaxed = true))
    verify(exactly = 1) {
      mock invoke "firePixel" withArguments listOf(
          "", Pixel.Events.IMPRESSION
      )
    }
    verify(exactly = 0) { adViewManagerComponent.uiThread.run(any()) }
  }

  private fun adViewManagerSetup(
    adSize: AdSize = mockk(relaxed = true),
    adServerWrapper: AdServerWrapper = mockk(relaxed = true),
    adView: AdView = mockk(relaxed = true),
    adViewClient: AdViewClient = mockk(relaxed = true),
    adViewContext: AdViewContext = mockk(relaxed = true),
    adViewHtml: String = "html",
    adViewPoolManagerCallback: AdViewPoolManagerCallback = mockk(relaxed = true),
    adViewReadyCallback: ReadyCallbackManager<AdView> = ReadyCallbackManager(),
    adViewState: AdViewManager.AdViewState = mockk(relaxed = true),
    appMonetContext: AppMonetContext = mockk(relaxed = true),
    auctionManagerCallback: AuctionManagerCallback = mockk(relaxed = true),
    backgroundThread: BackgroundThread = mockk(relaxed = true),
    containerView: AppMonetViewLayout = mockk(relaxed = true),
    context: Context = RuntimeEnvironment.application.applicationContext,
    injectionDelay: Int = 0,
    pubSubService: PubSubService = mockk(relaxed = true),
    uiThread: UIThread = mockk(relaxed = true),
    uuid: String = "uuid"
  ): AdViewManagerTestComponent {
    val adViewManager = AdViewManager(
        adSize, adServerWrapper, adView, adViewClient, adViewContext,
        adViewHtml, adViewPoolManagerCallback, adViewReadyCallback, adViewState, appMonetContext,
        auctionManagerCallback, backgroundThread, containerView, context, injectionDelay,
        pubSubService, uiThread, uuid
    )
    return AdViewManagerTestComponent(
        adSize, adServerWrapper, adView, adViewClient,
        adViewContext, adViewHtml, adViewManager, adViewPoolManagerCallback, adViewReadyCallback,
        adViewState, appMonetContext, auctionManagerCallback, backgroundThread,
        containerView, context, injectionDelay, pubSubService, uiThread, uuid
    )
  }
}