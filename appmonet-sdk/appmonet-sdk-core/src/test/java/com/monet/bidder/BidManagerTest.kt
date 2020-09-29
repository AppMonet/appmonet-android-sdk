package com.monet.bidder

import com.monet.bidder.adview.AdViewPoolManager
import com.monet.bidder.auction.AuctionManagerCallback
import com.monet.bidder.bid.BidManager
import com.monet.bidder.bid.BidResponse
import com.monet.bidder.bid.BidResponse.Interstitial
import com.monet.bidder.threading.BackgroundThread
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import junit.framework.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.reflect.Whitebox
import org.robolectric.RobolectricTestRunner
import java.util.PriorityQueue
import java.util.concurrent.ScheduledFuture

@RunWith(RobolectricTestRunner::class)
class BidManagerTest {

  @Test fun `Given store does not contain adUnitId return 0`() {
    val bidManagerComponent = bidManagerSetup()
    val count = bidManagerComponent.bidManager.countBids("")
    assertEquals(count, 0)
  }

  @Test fun `Given store does contain adUnitId return correct bid count`() {
    val store = mutableMapOf<String?, PriorityQueue<BidResponse>?>()
    val pq = PriorityQueue<BidResponse>()
    pq.add(mockk())
    store["adunit"] = pq
    val bidManagerComponent = bidManagerSetup(store = store)
    val count = bidManagerComponent.bidManager.countBids("adunit")
    assertEquals(count, 1)
  }

  @Test fun `Given there are no bids for adUnitId return false`() {
    val bidManagerComponent = bidManagerSetup()
    val hasLocalBids = bidManagerComponent.bidManager.hasLocalBids("")
    assertEquals(hasLocalBids, false)
  }

  @Test fun `Given there are bids for adUnitId return true`() {
    val store = mutableMapOf<String?, PriorityQueue<BidResponse>?>()
    val pq = PriorityQueue<BidResponse>()
    pq.add(mockk())
    store["adunit"] = pq
    val bidManagerComponent = bidManagerSetup(store = store)
    val hasLocalBids = bidManagerComponent.bidManager.hasLocalBids("adunit")
    assertEquals(hasLocalBids, true)
  }

  @Test fun `Given there are no bids for adUnitId return null bid`() {
    val bidManagerComponent = bidManagerSetup()
    val bid = bidManagerComponent.bidManager.getLocalBid("")
    assertEquals(bid, null)
  }

  @Test
  fun `Given there are bids for adUnitId return top most bid and validate it was removed from queue`() {
    val store = mutableMapOf<String?, PriorityQueue<BidResponse>?>()
    val pq = PriorityQueue<BidResponse>()
    val validBidResponse = bidBuilder()
    pq.add(validBidResponse)
    store["adunit"] = pq
    val bidManagerComponent = bidManagerSetup(store = store)
    val bid = bidManagerComponent.bidManager.getLocalBid("adunit")
    assertEquals(bid?.id, validBidResponse.id)
    assertEquals(pq.size, 0)
  }

  @Test fun `Given addBids is called and two bids are the same only add one`() {
    val bidOne = bidBuilder();
    val bidTwo = bidBuilder();
    val listOfBids = mutableListOf<BidResponse>().apply {
      add(bidOne)
      add(bidTwo)
    }
    val store = mutableMapOf<String?, PriorityQueue<BidResponse>?>()
    val seenBids = mutableMapOf<String, String?>()
    val bidsById = mutableMapOf<String, BidResponse?>()
    val bidsByAdView = mutableMapOf<String, MutableList<String?>?>()
    val bidManagerComponent =
      bidManagerSetup(
          store = store, seenBids = seenBids, bidsById = bidsById, bidIdsByAdView = bidsByAdView
      )
    bidManagerComponent.bidManager.addBids(listOfBids)
    assertEquals(store.size, 1)
    assertEquals(store[bidOne.adUnitId]?.size, 1)
  }

  @Test fun `Given addBids is called and two bids (with nativeRender) are valid two are added`() {
    val bidOne = bidBuilder();
    val bidTwo = bidBuilder(id = "bidTwo");
    val listOfBids = mutableListOf<BidResponse>().apply {
      add(bidOne)
      add(bidTwo)
    }
    val store = mutableMapOf<String?, PriorityQueue<BidResponse>?>()
    val seenBids = mutableMapOf<String, String?>()
    val bidsById = mutableMapOf<String, BidResponse?>()
    val bidsByAdView = mutableMapOf<String, MutableList<String?>?>()
    val bidManagerComponent =
      bidManagerSetup(
          store = store, seenBids = seenBids, bidsById = bidsById, bidIdsByAdView = bidsByAdView
      )

    bidManagerComponent.bidManager.addBids(listOfBids)
    assertEquals(store[bidOne.adUnitId]?.size, 2)
    assertEquals(seenBids.size, 2)
    assertEquals(bidsById.size, 2)

    verify(exactly = 2) {
      bidManagerComponent.pubSubService.addMessageToQueue(any())
      bidManagerComponent.pubSubService.broadcast()
    }
  }

  @Test fun `Given getBidById is called with a null id return null BidResponse`() {
    val bidsById = mutableMapOf<String, BidResponse?>()
    val bidManagerComponent =
      bidManagerSetup(
          bidsById = bidsById
      )
    assertEquals(bidManagerComponent.bidManager.getBidById(null), null)
  }

  @Test fun `Given getBidById is called with and id is not in map return null BidResponse`() {
    val bidsById = mutableMapOf<String, BidResponse?>()
    val bidManagerComponent =
      bidManagerSetup(
          bidsById = bidsById
      )
    assertEquals(bidManagerComponent.bidManager.getBidById("hello"), null)
  }

  @Test fun `Given getBidById is called with and id is present in map return BidResponse`() {
    val bidsById = mutableMapOf<String, BidResponse?>()
    val validBid = bidBuilder()
    bidsById["test"] = validBid
    val bidManagerComponent =
      bidManagerSetup(
          bidsById = bidsById
      )
    assertEquals(bidManagerComponent.bidManager.getBidById("test"), validBid)
  }

  @Test
  fun `Given getBidForAdUnit is called with adUnit that has not been seen return null BidResponse`() {
    val bidManagerComponent =
      bidManagerSetup()
    assertEquals(bidManagerComponent.bidManager.getBidForAdUnit("test"), null)
  }

  @Test
  fun `Given getBidForAdUnit is called with valid adUnit return BidResponse and remove it from queue`() {
    val store = mutableMapOf<String?, PriorityQueue<BidResponse>?>()
    val pq = PriorityQueue<BidResponse>()
    val validBid = bidBuilder()
    pq.add(validBid)
    store["test"] = pq
    val bidManagerComponent =
      bidManagerSetup(store = store)
    assertEquals(bidManagerComponent.bidManager.getBidForAdUnit("test"), validBid)
    assertEquals(pq.size, 0)
  }

  @Test
  fun `Given disableIntervalCleaner and intervalFuture is not null then cancel is called and its set to null`() {
    val backgroundThread = mockk<BackgroundThread>(relaxed = true)
    val intervalFuture = mockk<ScheduledFuture<*>>(relaxed = true)
    every { backgroundThread.scheduleAtFixedRate(any(), any(), any()) } answers { intervalFuture }
    val bidManagerComponent = bidManagerSetup(backgroundThread = backgroundThread)
    bidManagerComponent.bidManager.disableIntervalCleaner()
    verify(exactly = 1) { intervalFuture.cancel(any()) }
    assertEquals(Whitebox.getInternalState(bidManagerComponent.bidManager, "intervalFuture"), null)
  }

  @Test
  fun `Given enableIntervalCleaner and intervalFuture is null  then setup interval execution`() {
    val backgroundThread = mockk<BackgroundThread>(relaxed = true)
    val intervalFuture = mockk<ScheduledFuture<*>>(relaxed = true)
    every { backgroundThread.scheduleAtFixedRate(any(), any(), any()) } answers { intervalFuture }
    val bidManagerComponent = bidManagerSetup(backgroundThread = backgroundThread)
    bidManagerComponent.bidManager.enableIntervalCleaner()
    verify(exactly = 1) {
      bidManagerComponent.backgroundThread.scheduleAtFixedRate(
          any(), any(), any()
      )
    }
    assertEquals(
        Whitebox.getInternalState(bidManagerComponent.bidManager, "intervalFuture"), intervalFuture
    )
  }

  @Test
  fun `Given invalidReason and bid has been used return 'bid used' `() {
    val bid = mockk<BidResponse>(relaxed = true)
    every { bid.uuid } answers { "test" }
    every { bid.id } answers { "id" }
    val usedBids = mutableMapOf<String, String?>().apply { this["test"] = bid.id }
    val bidManagerComponent = bidManagerSetup(usedBids = usedBids)
    assertEquals(bidManagerComponent.bidManager.invalidReason(bid), "bid used")
  }

  @Test
  fun `Given markUsed bid should be in usedBids map`() {
    val bid = mockk<BidResponse>(relaxed = true)
    every { bid.uuid } answers { "test" }
    every { bid.id } answers { "id" }
    val usedBids = mutableMapOf<String, String?>()
    val bidManagerComponent = bidManagerSetup(usedBids = usedBids)
    bidManagerComponent.bidManager.markUsed(bid)
    assertEquals(usedBids[bid.uuid], bid.id)
  }

  private fun bidManagerSetup(
    pubSubService: PubSubService = mockk(relaxed = true),
    backgroundThread: BackgroundThread = mockk(relaxed = true),
    adViewPoolManager: AdViewPoolManager = mockk(relaxed = true),
    auctionManagerCallback: AuctionManagerCallback = mockk(relaxed = true),
    store: MutableMap<String?, PriorityQueue<BidResponse>?> = mutableMapOf(),
    seenBids: MutableMap<String, String?> = mockk(relaxed = true),
    adUnitNameMapping: MutableMap<String, String?> = mockk(relaxed = true),
    bidsById: MutableMap<String, BidResponse?> = mockk(relaxed = true),
    bidIdsByAdView: MutableMap<String, MutableList<String?>?> = mockk(relaxed = true),
    usedBids: MutableMap<String, String?> = mockk(relaxed = true)
  ): BidManagerTestComponent {
    val bidManager = BidManager(
        pubSubService, backgroundThread, adViewPoolManager, auctionManagerCallback, store, seenBids,
        adUnitNameMapping, bidsById, bidIdsByAdView, usedBids
    )
    return BidManagerTestComponent(
        bidManager, pubSubService, backgroundThread, adViewPoolManager, auctionManagerCallback,
        store, seenBids, adUnitNameMapping, bidsById, bidIdsByAdView, usedBids
    )
  }

  private fun bidBuilder(
    adm: String = "adm",
    id: String = "validBid",
    code: String = "code",
    width: Int = 0,
    height: Int = 0,
    createdAt: Long = 0L,
    cpm: Double = 0.0,
    bidder: String = "bidder",
    adUnitId: String = "adUnitId",
    keyWords: String = "keywords",
    renderPixel: String = "",
    clickPixel: String = "",
    u: String = "",
    uuid: String = "",
    cool: Int = 0,
    nativeRender: Boolean = true,
    wvUUID: String = "",
    duration: Int = 0,
    expiration: Long = System.currentTimeMillis() * 2,
    mega: Boolean = false,
    adType: String = "",
    refresh: Int = 0,
    interstitial: Interstitial? = null,
    extras: Map<String, Any> = hashMapOf(),
    nativeInvalidated: Boolean = false,
    queueNext: Boolean = true,
    flexSize: Boolean = false,
    url: String = ""
  ): BidResponse {
    return BidResponse(
        adm, id, code, width, height, createdAt, cpm, bidder, adUnitId, keyWords, renderPixel,
        clickPixel, u, uuid, cool, nativeRender, wvUUID, duration, expiration, mega, adType,
        refresh, interstitial
    )
  }
}