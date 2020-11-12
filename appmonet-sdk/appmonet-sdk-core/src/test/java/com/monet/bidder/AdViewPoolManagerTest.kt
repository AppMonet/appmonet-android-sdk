package com.monet.bidder

import android.content.Context
import android.webkit.ValueCallback
import com.monet.bidder.adview.AdViewManager
import com.monet.bidder.adview.AdViewManager.AdViewState.AD_LOADING
import com.monet.bidder.adview.AdViewManager.AdViewState.NOT_FOUND
import com.monet.bidder.adview.AdViewPoolManager
import com.monet.bidder.auction.AuctionManagerCallback
import com.monet.BidResponse
import com.monet.bidder.threading.BackgroundThread
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.reflect.Whitebox
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment.application
import java.util.concurrent.ConcurrentHashMap

@RunWith(RobolectricTestRunner::class)
class AdViewPoolManagerTest {

  @Test
  fun `Given getReferenceCount return reference count from adViewRefCount given webView uuid`() {
    val adViewRefCount = ConcurrentHashMap<String, Int>().apply { this["uuid"] = 3 }
    val adViewPoolManagerTestComponent = adViewPoolManagerSetup(adViewRefCount = adViewRefCount)
    val count = adViewPoolManagerTestComponent.adViewPoolManager.getReferenceCount("uuid")
    assertEquals(count, 3)
  }

  @Test
  fun `Given adViewCreated helperCreated is called`() {
    val adViewPoolManagerTestComponent = adViewPoolManagerSetup()
    adViewPoolManagerTestComponent.adViewPoolManager.adViewCreated("uuid", mockk(relaxed = true))
    verify(exactly = 1) {
      adViewPoolManagerTestComponent.auctionManagerCallback.helperCreated(
          "uuid", any()
      )
    }
  }

  @Test
  fun `Given adViewLoaded helperLoaded is called`() {
    val adViewPoolManagerTestComponent = adViewPoolManagerSetup()
    adViewPoolManagerTestComponent.adViewPoolManager.adViewLoaded("uuid")
    verify(exactly = 1) {
      adViewPoolManagerTestComponent.auctionManagerCallback.helperLoaded("uuid")
    }
  }

  @Test
  fun `Given adViewResponse executeJs is called`() {
    val auctionManagerCallback = mockk<AuctionManagerCallback>(relaxed = true)
    val adViewPoolManagerTestComponent =
      adViewPoolManagerSetup(auctionManagerCallback = auctionManagerCallback)
    adViewPoolManagerTestComponent.adViewPoolManager.adViewResponse("uuid")
    verify(exactly = 1) {
      adViewPoolManagerTestComponent.auctionManagerCallback.executeJs("helperRespond", "uuid")
    }
  }

  @Test
  fun `Given canReleaseAdViewManager and adViewManagerCollection does not contain webView uuid then return true`() {
    val adViewManager = mockk<AdViewManager>(relaxed = true)
    every { adViewManager.uuid } answers { "uuid" }
    val adViewPoolManagerTestComponent = adViewPoolManagerSetup()
    assertTrue(
        adViewPoolManagerTestComponent.adViewPoolManager.canReleaseAdViewManager(adViewManager)
    )
  }

  @Test
  fun `Given canReleaseAdViewManager and adViewRefCount is 0 return true`() {
    val adViewManager = mockk<AdViewManager>(relaxed = true)
    val adViewManagerCollection =
      mutableMapOf<String, AdViewManager>().apply { this["uuid"] = adViewManager }
    val adViewRefCount = mutableMapOf<String, Int>().apply { this["uuid"] = 0 }
    every { adViewManager.uuid } answers { "uuid" }
    val adViewPoolManagerTestComponent =
      adViewPoolManagerSetup(
          adViewManagerCollection = adViewManagerCollection, adViewRefCount = adViewRefCount
      )
    assertTrue(
        adViewPoolManagerTestComponent.adViewPoolManager.canReleaseAdViewManager(adViewManager)
    )
  }

  @Test
  fun `Given canReleaseAdViewManager and adViewRefCount is gt 0 return false`() {
    val adViewManager = mockk<AdViewManager>(relaxed = true)
    val adViewManagerCollection =
      mutableMapOf<String, AdViewManager>().apply { this["uuid"] = adViewManager }
    val adViewRefCount = mutableMapOf<String, Int>().apply { this["uuid"] = 1 }
    every { adViewManager.uuid } answers { "uuid" }
    val adViewPoolManagerTestComponent =
      adViewPoolManagerSetup(
          adViewManagerCollection = adViewManagerCollection, adViewRefCount = adViewRefCount
      )
    assertFalse(
        adViewPoolManagerTestComponent.adViewPoolManager.canReleaseAdViewManager(adViewManager)
    )
  }

  @Test
  fun `Given getSdkConfigurations auctionManagerCallback getSdkConfigurations is called`() {
    val adViewPoolManagerTestComponent = adViewPoolManagerSetup()
    adViewPoolManagerTestComponent.adViewPoolManager.getSdkConfigurations()
    verify(
        exactly = 1
    ) { adViewPoolManagerTestComponent.auctionManagerCallback.getSdkConfigurations() }
  }

  @Test
  fun `Given impressionEnded executeJS is called`() {
    val adViewPoolManagerTestComponent = adViewPoolManagerSetup()
    adViewPoolManagerTestComponent.adViewPoolManager.impressionEnded()
    verify(exactly = 1) {
      adViewPoolManagerTestComponent.auctionManagerCallback.executeJs(
          "impressionEnded", any()
      )
    }
  }

  @Test
  fun `Given requestDestroy and adView is not found return false`() {
    val adViewManagerCollection = mutableMapOf<String, AdViewManager>()
    val adViewPoolManagerTestComponent =
      adViewPoolManagerSetup(adViewManagerCollection = adViewManagerCollection)
    assertFalse(adViewPoolManagerTestComponent.adViewPoolManager.requestDestroy("uuid"))
  }

  @Test
  fun `Given requestDestroy and adView is loading remove is called`() {
    val adViewManager = mockk<AdViewManager>(relaxed = true)
    every { adViewManager.state } answers { AD_LOADING }
    every { adViewManager.uuid } answers { "uuid" }
    val adViewsByContext = mutableMapOf<String, MutableList<AdViewManager>>()
    val adViewManagerCollection =
      mutableMapOf<String, AdViewManager>().apply { this["uuid"] = adViewManager }
    val adViewRefCount = mutableMapOf<String, Int>()
    val adViewPoolManagerTestComponent =
      adViewPoolManagerSetup(
          adViewManagerCollection = adViewManagerCollection, adViewRefCount = adViewRefCount,
          adViewsByContext = adViewsByContext
      )
    val spyAdViewPoolManager = spyk(adViewPoolManagerTestComponent.adViewPoolManager)
    spyAdViewPoolManager.requestDestroy("uuid")
    verify(exactly = 1) { spyAdViewPoolManager.remove("uuid", true) }
  }

  @Test
  fun `Given requestDestroy and adView is not loading and reference count is gt 0 return false`() {
    val adViewManager = mockk<AdViewManager>(relaxed = true)
    every { adViewManager.state } answers { NOT_FOUND }
    every { adViewManager.uuid } answers { "uuid" }
    val adViewManagerCollection =
      mutableMapOf<String, AdViewManager>().apply { this["uuid"] = adViewManager }
    val adViewRefCount = mutableMapOf<String, Int>()
    adViewRefCount["uuid"] = 10
    val adViewPoolManagerTestComponent =
      adViewPoolManagerSetup(
          adViewManagerCollection = adViewManagerCollection, adViewRefCount = adViewRefCount
      )
    val spyAdViewPoolManager = spyk(adViewPoolManagerTestComponent.adViewPoolManager)
    assertFalse(spyAdViewPoolManager.requestDestroy("uuid"))
    verify(
        exactly = 1
    ) { adViewPoolManagerTestComponent.auctionManagerCallback.invalidateBidsForAdView("uuid") }
  }

  @Test
  fun `Given requestDestroy and adView is not loading and reference count is  0 return true`() {
    val adViewManager = mockk<AdViewManager>(relaxed = true)
    every { adViewManager.state } answers { NOT_FOUND }
    every { adViewManager.uuid } answers { "uuid" }
    val adViewManagerCollection =
      mutableMapOf<String, AdViewManager>().apply { this["uuid"] = adViewManager }
    val adViewRefCount = mutableMapOf<String, Int>()
    adViewRefCount["uuid"] = 0
    val adViewPoolManagerTestComponent =
      adViewPoolManagerSetup(
          adViewManagerCollection = adViewManagerCollection, adViewRefCount = adViewRefCount
      )
    val spyAdViewPoolManager = spyk(adViewPoolManagerTestComponent.adViewPoolManager)
    assertTrue(spyAdViewPoolManager.requestDestroy("uuid"))
    verify(
        exactly = 1
    ) { adViewPoolManagerTestComponent.auctionManagerCallback.invalidateBidsForAdView("uuid") }
  }

  @Test
  fun `Given updateRefCount adViewRefCount gets updated for given uuid`() {
    val adViewRefCount = mutableMapOf<String, Int>()
    val adViewPoolManagerTestComponent = adViewPoolManagerSetup(adViewRefCount = adViewRefCount)
    adViewPoolManagerTestComponent.adViewPoolManager.updateRefCount("uuid", 10)
    assertEquals(adViewPoolManagerTestComponent.adViewRefCount["uuid"], 10)
  }

  @Test
  fun `Given getAdViewByUuid return AdViewManager`() {
    val adViewManager = mockk<AdViewManager>()
    val adViewManagerCollection =
      mutableMapOf<String, AdViewManager>().apply { this["uuid"] = adViewManager }
    val adViewPoolManagerTestComponent =
      adViewPoolManagerSetup(adViewManagerCollection = adViewManagerCollection)
    adViewPoolManagerTestComponent.adViewPoolManager.getAdViewByUuid("uuid")
    assertEquals(adViewPoolManagerTestComponent.adViewManagerCollection["uuid"], adViewManager)
  }

  @Test
  fun `Given executeInContext uuid not in adViewManagerCollection return false`() {
    val adViewManager = mockk<AdViewManager>()
    val adViewManagerCollection =
      mutableMapOf<String, AdViewManager>().apply { this["uuid"] = adViewManager }
    val adViewPoolManagerTestComponent =
      adViewPoolManagerSetup(adViewManagerCollection = adViewManagerCollection)
    assertFalse(
        adViewPoolManagerTestComponent.adViewPoolManager.executeInContext("not_uuid", "message")
    )
  }

  @Test
  fun `Given executeInContext uuid in adViewManagerCollection and executeJs is called return true`() {
    val adViewManager = mockk<AdViewManager>(relaxed = true)
    val adViewManagerCollection =
      mutableMapOf<String, AdViewManager>().apply { this["uuid"] = adViewManager }
    val adViewPoolManagerTestComponent =
      adViewPoolManagerSetup(adViewManagerCollection = adViewManagerCollection)
    assertTrue(
        adViewPoolManagerTestComponent.adViewPoolManager.executeInContext("uuid", "message")
    )
    verify(exactly = 1) { adViewManager.executeJs("__a", "message") }
  }

  @Test
  fun `Given addRef and uuid is not in adViewRefCount then one is added`() {
    val adViewRefCount = mutableMapOf<String, Int>()
    val adViewPoolManagerTestComponent = adViewPoolManagerSetup(adViewRefCount = adViewRefCount)
    adViewPoolManagerTestComponent.adViewPoolManager.addRef("uuid")
    assertEquals(adViewRefCount["uuid"], 1)
  }

  @Test
  fun `Given addRef and uuid is in adViewRefCount then plus one is added`() {
    val adViewRefCount = mutableMapOf<String, Int>().apply { this["uuid"] = 10 }
    val adViewPoolManagerTestComponent = adViewPoolManagerSetup(adViewRefCount = adViewRefCount)
    adViewPoolManagerTestComponent.adViewPoolManager.addRef("uuid")
    assertEquals(adViewRefCount["uuid"], 11)
  }

  @Test
  fun `Given getState with uuid return adViewManager state value`() {
    val adViewManager = mockk<AdViewManager>(relaxed = true)
    every { adViewManager.state } answers { AD_LOADING }
    val adViewManagerCollection =
      mutableMapOf<String, AdViewManager>().apply { this["uuid"] = adViewManager }
    val adViewPoolManagerTestComponent =
      adViewPoolManagerSetup(adViewManagerCollection = adViewManagerCollection)
    assertEquals(
        adViewPoolManagerTestComponent.adViewPoolManager.getState("uuid"), AD_LOADING.toString()
    )
  }

  @Test
  fun `Given getState with not found uuid return AdViewState#NOT_FOUND`() {
    val adViewManagerCollection = mutableMapOf<String, AdViewManager>()
    val adViewPoolManagerTestComponent =
      adViewPoolManagerSetup(adViewManagerCollection = adViewManagerCollection)
    assertEquals(
        adViewPoolManagerTestComponent.adViewPoolManager.getState("uuid"), NOT_FOUND.toString()
    )
  }

  @Test
  fun `Given removeBid and adViewManager is not null and removeCached is true bid is marked invalid`() {
    val adViewManager = mockk<AdViewManager>(relaxed = true)
    val adViewRefCount = mutableMapOf<String, Int>()
    val bid = mockk<BidResponse>(relaxed = true)
    every { bid.wvUUID } answers { "uuid" }
    val adViewManagerCollection =
      mutableMapOf<String, AdViewManager>().apply { this["uuid"] = adViewManager }
    val adViewPoolManagerTestComponent =
      adViewPoolManagerSetup(
          adViewManagerCollection = adViewManagerCollection, adViewRefCount = adViewRefCount
      )
    val mockedAdViewPoolManager = spyk(adViewPoolManagerTestComponent.adViewPoolManager)
    mockedAdViewPoolManager.removeBid(bid, true)
    verify(exactly = 1) { mockedAdViewPoolManager.removeRef("uuid") }
    verify(exactly = 1) { adViewManager.markBidInvalid(any()) }
  }

  @Test
  fun `Given removeBid and adViewManager is not null and removeCached is false  bid is not marked invalid`() {
    val adViewManager = mockk<AdViewManager>(relaxed = true)
    val adViewRefCount = mutableMapOf<String, Int>()
    val bid = mockk<BidResponse>(relaxed = true)
    every { bid.wvUUID } answers { "uuid" }
    val adViewManagerCollection =
      mutableMapOf<String, AdViewManager>().apply { this["uuid"] = adViewManager }
    val adViewPoolManagerTestComponent =
      adViewPoolManagerSetup(
          adViewManagerCollection = adViewManagerCollection, adViewRefCount = adViewRefCount
      )
    val mockedAdViewPoolManager = spyk(adViewPoolManagerTestComponent.adViewPoolManager)
    mockedAdViewPoolManager.removeBid(bid, false)
    verify(exactly = 1) { mockedAdViewPoolManager.removeRef("uuid") }
    verify(exactly = 0) { adViewManager.markBidInvalid(any()) }
  }

  @Test
  fun `Given getRenderCount return adview manager render count`() {
    val adViewManager = mockk<AdViewManager>(relaxed = true)
    adViewManager.renderCount = 10
    val adViewManagerCollection =
      mutableMapOf<String, AdViewManager>().apply { this["uuid"] = adViewManager }
    val adViewPoolManagerTestComponent =
      adViewPoolManagerSetup(
          adViewManagerCollection = adViewManagerCollection
      )
    assertEquals(adViewPoolManagerTestComponent.adViewPoolManager.getRenderCount("uuid"), 10)
  }

  @Test
  fun `Given getRenderCount and uuid is not in adViewManagerCollection return 0`() {
    val adViewManagerCollection = mutableMapOf<String, AdViewManager>()
    val adViewPoolManagerTestComponent =
      adViewPoolManagerSetup(adViewManagerCollection = adViewManagerCollection)
    assertEquals(adViewPoolManagerTestComponent.adViewPoolManager.getRenderCount("uuid"), 0)
  }

  @Test
  fun `Given getUrl return adview manager url`() {
    val adViewManager = mockk<AdViewManager>(relaxed = true)
    Whitebox.setInternalState(adViewManager, "adViewUrl", "http")
    val adViewManagerCollection =
      mutableMapOf<String, AdViewManager>().apply { this["uuid"] = adViewManager }
    val adViewPoolManagerTestComponent =
      adViewPoolManagerSetup(
          adViewManagerCollection = adViewManagerCollection
      )
    assertEquals(adViewPoolManagerTestComponent.adViewPoolManager.getUrl("uuid"), "http")
  }

  @Test
  fun `Given getUrl and uuid is not in adViewManagerCollection return empty string`() {
    val adViewManagerCollection = mutableMapOf<String, AdViewManager>()
    val adViewPoolManagerTestComponent =
      adViewPoolManagerSetup(adViewManagerCollection = adViewManagerCollection)
    assertEquals(adViewPoolManagerTestComponent.adViewPoolManager.getUrl("uuid"), "")
  }

  private fun adViewPoolManagerSetup(
    adServerWrapper: AdServerWrapper = mockk(relaxed = true),
    adViewsByContext: MutableMap<String, MutableList<AdViewManager>> = mockk(relaxed = true),
    adViewsReadyState: MutableMap<String, Boolean> = mockk(relaxed = true),
    adViewRefCount: MutableMap<String, Int> = mockk(relaxed = true),
    adViewManagerCollection: MutableMap<String, AdViewManager> = mockk(relaxed = true),
    auctionManagerCallback: AuctionManagerCallback = mockk(relaxed = true),
    appMonetContext: AppMonetContext = mockk(relaxed = true),
    context: Context = application,
    messageHandlers: MutableMap<String, List<ValueCallback<String>>> = mockk(relaxed = true),
    pubSubService: PubSubService = mockk(relaxed = true),
    backgroundThread: BackgroundThread = mockk(relaxed = true),
    uiThread: UIThread = mockk(relaxed = true)
  ): AdViewPoolManagerTestComponent {
    val adViewPoolManager =
      AdViewPoolManager(
          adServerWrapper, adViewsByContext, adViewsReadyState, adViewManagerCollection,
          adViewRefCount, appMonetContext, auctionManagerCallback, context, messageHandlers,
          pubSubService, backgroundThread, uiThread
      )
    return AdViewPoolManagerTestComponent(
        adViewPoolManager, adServerWrapper, adViewsByContext, adViewsReadyState, adViewRefCount,
        adViewManagerCollection, auctionManagerCallback, appMonetContext, context, messageHandlers,
        pubSubService, backgroundThread, uiThread
    )
  }
}