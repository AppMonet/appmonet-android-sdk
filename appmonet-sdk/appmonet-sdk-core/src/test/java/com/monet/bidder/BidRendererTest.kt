package com.monet.bidder

import com.monet.bidder.adview.AdView
import com.monet.bidder.adview.AdViewManager
import com.monet.bidder.adview.AdViewManager.AdViewState.AD_RENDERED
import com.monet.bidder.adview.AdViewPoolManager
import com.monet.bidder.auction.AuctionManager
import com.monet.bidder.auction.AuctionWebView
import com.monet.bidder.bid.BidManager
import com.monet.bidder.bid.BidRenderer
import com.monet.BidResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.reflect.Whitebox
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment.application

@RunWith(RobolectricTestRunner::class)
class BidRendererTest {
  private lateinit var bidManager: BidManager

  //
  private lateinit var sdkManager: BaseManager

  private lateinit var bidResponse: BidResponse
  private lateinit var adSize: AdSize
  private lateinit var listener: AdServerBannerListener
  private lateinit var auctionWebView: AuctionWebView;
  private lateinit var adViewPoolManager: AdViewPoolManager;
  private lateinit var adViewManager: AdViewManager
  private lateinit var auctionManager: AuctionManager

  //
  @Before
  fun setup() {
    sdkManager = mockk(relaxed = true)
    auctionWebView = mockk(relaxed = true)
    adViewPoolManager = mockk(relaxed = true)
    adViewManager = mockk(relaxed = true)
    bidManager = mockk(relaxed = true)
    auctionManager = mockk(relaxed = true)
    Whitebox.setInternalState(sdkManager, "auctionManager", auctionManager)
    Whitebox.setInternalState(auctionManager, "bidManager", bidManager)
    Whitebox.setInternalState(auctionManager, "adViewPoolManager", adViewPoolManager)
    bidResponse = mockk(relaxed = true)
    adSize = mockk(relaxed = true)
    listener = mockk(relaxed = true)
    every { bidResponse.toString() } answers { "" }
  }

  @Test
  fun `Given bid is invalid return null and verify trackEvent is called`() {
    val auctionManager = sdkManager.auctionManager

    every { auctionManager.bidManager.isValid(bidResponse) } answers { false }
    val view = BidRenderer.renderBid(
        application, sdkManager, bidResponse,
        adSize, listener
    )

    verify(exactly = 1) {
      auctionManager.trackEvent(
          eq("bidRenderer"),
          eq("invalid_bid"), any(), any(), any()
      )
    }
    assertEquals(view, null)
  }

  //
  @Test
  fun `Given bid is valid and adView is null return null and verify track event is called`() {
    val auctionManager = sdkManager.auctionManager
    every { auctionManager.bidManager.isValid(bidResponse) } answers { true }
    every { auctionManager.adViewPoolManager.request(bidResponse) } answers { null }
    val view = BidRenderer.renderBid(
        application, sdkManager, bidResponse,
        adSize, listener
    )
    verify(exactly = 1) {
      auctionManager.trackEvent(
          eq("bidRenderer"),
          eq("null_view"), any(), any(), any()
      )
    }
    assertEquals(view, null)
  }

  @Test
  fun `Given bid is valid and adView is not null if adView is not loaded call load`() {
    every { sdkManager.auctionManager.bidManager.isValid(bidResponse) } answers { true }
    val adView = mockk<AdView>(relaxed = true)
    every { adView.parent } returns mockk<AppMonetViewLayout>()
    Whitebox.setInternalState(adViewManager, "adView", adView)
    every {
      sdkManager.auctionManager.adViewPoolManager.request(
          bidResponse
      )
    } answers { adViewManager }
    every { adViewManager.isAdRefreshed } answers { false }
    BidRenderer.renderBid(
        application, sdkManager, bidResponse,
        adSize, listener
    )
    verify(exactly = 1) { adViewManager.load() }
  }

  @Test
  fun `Given adView is not null make sure setup methods are called for rendering`() {
    every { sdkManager.auctionManager.bidManager.isValid(bidResponse) } answers { true }
    val adView = mockk<AdView>(relaxed = true)
    every { adView.parent } returns mockk<AppMonetViewLayout>()
    Whitebox.setInternalState(adViewManager, "adView", adView)
    every {
      sdkManager.auctionManager.adViewPoolManager.request(
          bidResponse
      )
    } answers { adViewManager }
    every { adViewManager.isLoaded } answers { false }
    BidRenderer.renderBid(
        application, sdkManager, bidResponse,
        adSize, listener
    )
    verify { sdkManager.auctionManager.bidManager.markUsed(bidResponse) }
    verify { adViewManager.setState(AD_RENDERED, listener, application) }
    verify { adViewManager.inject(bidResponse) }
  }

  @Test
  fun `Given adView is not null and a valid adSize is passed and flexSize is true adView resize is called`() {
    every { sdkManager.auctionManager.bidManager.isValid(bidResponse) } answers { true }
    val adView = mockk<AdView>(relaxed = true)
    every { adView.parent } returns mockk<AppMonetViewLayout>()
    Whitebox.setInternalState(adViewManager, "adView", adView)
    every {
      sdkManager.auctionManager.adViewPoolManager.request(
          bidResponse
      )
    } answers { adViewManager }
    every { adViewManager.isLoaded } answers { true }
    every { bidResponse.flexSize } answers { true }
    BidRenderer.renderBid(
        application, sdkManager, bidResponse,
        AdSize(300, 250), listener
    )
    verify(exactly = 1) { adViewManager.resize(any()) }
  }

  @Test
  fun `Given everything went well we should get the adView parent (AppMonetViewLayout)`() {
    val parentLayout = mockk<AppMonetViewLayout>()
    val adView = mockk<AdView>(relaxed = true)

    every { sdkManager.auctionManager.bidManager.isValid(bidResponse) } answers { true }
    every {
      sdkManager.auctionManager.adViewPoolManager.request(
          bidResponse
      )
    } answers { adViewManager }
    every { adViewManager.isLoaded } answers { true }
    every { adView.parent } returns parentLayout
    Whitebox.setInternalState(adViewManager, "adView", adView)

    val view = BidRenderer.renderBid(
        application, sdkManager, bidResponse,
        adSize, listener
    )
    assertEquals(view, parentLayout)
  }

  private val validBidResponse = BidResponse(
      "", "id", "", 0, 0, 0L, 10.0,
      "", "", "", "", "", "", "",
      0, true, "", 0, 0L, true,
      "", 0, null
  )
}