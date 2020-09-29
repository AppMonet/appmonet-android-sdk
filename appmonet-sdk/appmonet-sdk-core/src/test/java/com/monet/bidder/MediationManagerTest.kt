package com.monet.bidder

import android.webkit.ValueCallback
import com.monet.bidder.auction.AuctionManager
import com.monet.bidder.bid.BidManager
import com.monet.bidder.bid.BidResponse
import com.monet.bidder.threading.InternalRunnable
import com.monet.bidder.threading.UIThread
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.reflect.Whitebox
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediationManagerTest {
  private lateinit var mediationManager: MediationManager;
  private val sdkManager = mockk<BaseManager>(relaxed = true)
  private val bidManager = mockk<BidManager>(relaxed = true)

  private val validBidResponse = BidResponse(
      "", "id", "", 0, 0, 0L, 10.0,
      "", "", "", "", "", "", "",
      0, true, "", 0, 0L, true,
      "", 0, null
  )

  //
  @Before
  fun setup() {
    mediationManager = MediationManager(sdkManager, bidManager)
  }

  //
//    //getBidReadyForMediation
//
  @Test
  fun `Given indicateRequest is true make sure sdkmanager indicateRequest is called once`() {
    try {
      mediationManager.getBidReadyForMediation(
          mockk(relaxed = true), "test_ad_unit",
          AdSize(300, 250), AdType.BANNER, 10.00, true
      )
    } catch (e: Exception) {
    }
    verify(exactly = 1) { sdkManager.indicateRequest(any(), any(), any(), any()) }
  }

  @Test(expected = MediationManager.NullBidException::class)
  fun `Given bid is null throw NullBidException`() {
    mediationManager.getBidReadyForMediation(
        null, "test_ad_unit",
        AdSize(300, 250), AdType.BANNER, 10.00, false
    )
  }

  @Test(expected = MediationManager.NullBidException::class)
  fun `Given bid id is null throw NullBidException`() {
    val bid = BidResponse(
        "", "", "", 0, 0, 0L, 0.0,
        "", "", "", "", "", "", "",
        0, true, "", 0, 0L, true,
        "", 0, null
    )
    mediationManager.getBidReadyForMediation(
        bid, "", AdSize(300, 250),
        AdType.BANNER, 10.00, false
    )
  }

  @Test
  fun `Given bid is valid return back bid`() {
    val bid = spyk(validBidResponse)
    every { bidManager.isValid(bid) } answers { true }

    val mediationBid = mediationManager.getBidReadyForMediation(
        bid, "", AdSize(300, 250),
        AdType.BANNER, 10.00, false
    )
    assertEquals(mediationBid, bid)
  }

  @Test
  fun `Given bid is invalid then will peek for a nextBid`() {
    val bid = spyk(validBidResponse)
    every { bidManager.isValid(bid) } answers { false }

    try {
      mediationManager.getBidReadyForMediation(
          bid, "", AdSize(300, 250),
          AdType.BANNER, 10.00, false
      )
    } catch (e: Exception) {
    }
    verify(exactly = 1) { bidManager.peekNextBid(any()) }
  }

  @Test(expected = MediationManager.NoBidsFoundException::class)
  fun `Given bid is invalid and peekedBid is null throw NoBidsFoundException`() {
    val bid = spyk(validBidResponse)
    every { bidManager.isValid(bid) } answers { false }
    every { bidManager.peekNextBid(any()) } answers { null }
    mediationManager.getBidReadyForMediation(
        bid, "", AdSize(300, 250),
        AdType.BANNER, 10.00, false
    )
  }

  @Test(expected = MediationManager.NoBidsFoundException::class)
  fun `Given bid is invalid and peekedBid is invalid throw NoBidsFoundException`() {
    val bid = spyk(validBidResponse)
    every { bidManager.isValid(bid) } answers { false }
    val peekedBid = mockk<BidResponse>()
    every { bidManager.isValid(peekedBid) } answers { false }
    every { bidManager.peekNextBid(any()) } answers { peekedBid }
    mediationManager.getBidReadyForMediation(
        bid, "", AdSize(300, 250),
        AdType.BANNER, 10.00, false
    )
  }

  @Test(expected = MediationManager.NoBidsFoundException::class)
  fun `Given bid is invalid and peekedBid is valid but cpm is lower than original bid throw NoBidsFoundException`() {
    val bid = spyk(validBidResponse)
    every { bidManager.isValid(bid) } answers { false }
    val peekedBid = spyk(createLowCpmBid())
    every { bidManager.isValid(peekedBid) } answers { true }
    every { bidManager.peekNextBid(any()) } answers { peekedBid }
    mediationManager.getBidReadyForMediation(
        bid, "", AdSize(300, 250),
        AdType.BANNER, 10.00, false
    )
  }

  @Test
  fun `Given bid is invalid and peekedBid is valid and cpm is greater than or equal to original bid return peekedBid`() {
    val bid = spyk(validBidResponse)
    every { bidManager.isValid(bid) } answers { false }
    val peekedBid = spyk(validBidResponse)
    every { bidManager.isValid(peekedBid) } answers { true }
    every { bidManager.peekNextBid(any()) } answers { peekedBid }
    val mediationBid = mediationManager.getBidReadyForMediation(
        bid, "", AdSize(300, 250),
        AdType.BANNER, 10.00, false
    )
    assertEquals(mediationBid, peekedBid)
  }

  //getBidReadyForMediationAsync
  @Test
  fun `Given a bid is ready for mediation callback on success with bid`() {
    val bid = spyk(validBidResponse)
    every { bidManager.isValid(bid) } answers { true }
    val bidResponseCallback = mockk<Callback<BidResponse>>(relaxed = true)
    mediationManager.getBidReadyForMediationAsync(
        bid, "",
        AdSize(300, 250), AdType.BANNER, 10.00, bidResponseCallback
    )
    verify(exactly = 1) { bidResponseCallback.onSuccess(bid) }
  }

  @Test
  fun `Given there is no bids and no timeout callback on error and indicate request is called`() {
    val bidResponseCallback = mockk<Callback<BidResponse>>(relaxed = true)
    setTimeout(0)
    mediationManager.getBidReadyForMediationAsync(
        null, "",
        AdSize(300, 250), AdType.BANNER, 10.00, bidResponseCallback
    )
    verify(exactly = 1) { bidResponseCallback.onError() }
    verify(exactly = 1) {
      (sdkManager.indicateRequest(any(), any(), any(), any()))
    }
  }

  @Test
  fun `Given there is no bids and there is a timeout indicateRequestAsync is called`() {
    val bidResponseCallback = mockk<Callback<BidResponse>>(relaxed = true)
    setTimeout(1)
    mediationManager.getBidReadyForMediationAsync(
        null, "",
        AdSize(300, 250), AdType.BANNER, 10.00, bidResponseCallback
    )
    verify(exactly = 1) {
      sdkManager
          .indicateRequestAsync(any(), any(), any(), any(), any(), any())
    }
  }

  @Test
  fun `Given there is no bids and indicateRequestAsync is called return value is null so callback on error`() {
    val bidResponseCallback = mockk<Callback<BidResponse>>(relaxed = true)
    val callbackCaptor = mockk<ValueCallback<String>>(relaxed = true)
    every { callbackCaptor.onReceiveValue("") }
    setTimeout(1)
    setHandlerCaptor()
    every { sdkManager.uiThread.run(any()) } answers {
      firstArg<InternalRunnable>().runInternal()
    }
    every {
      sdkManager.indicateRequestAsync(any(), any(), any(), any(), any(), any())
    } answers {
      lastArg<ValueCallback<String>>().onReceiveValue("")
    }
    mediationManager.getBidReadyForMediationAsync(
        null, "",
        AdSize(300, 250), AdType.BANNER, 10.00, bidResponseCallback
    )
    verify(exactly = 1) {
      sdkManager.indicateRequestAsync(any(), any(), any(), any(), any(), any())
    }
    verify(exactly = 1) { bidResponseCallback.onError() }
  }

  @Test
  fun `Given there is no bids and indicateRequestAsync is called and there is an exception callback on error`() {
    val bidResponseCallback = mockk<Callback<BidResponse>>(relaxed = true)
    setTimeout(1)
    setHandlerCaptor()
    every { sdkManager.uiThread.run(any()) } answers {
      firstArg<InternalRunnable>().catchException(mockk(relaxed = true))
    }
    every {
      sdkManager.indicateRequestAsync(any(), any(), any(), any(), any(), any())
    } answers {
      lastArg<ValueCallback<String>>().onReceiveValue("")
    }
    mediationManager.getBidReadyForMediationAsync(
        null, "",
        AdSize(300, 250), AdType.BANNER, 10.00, bidResponseCallback
    )
    verify(exactly = 1) {
      sdkManager.indicateRequestAsync(any(), any(), any(), any(), any(), any())
    }
    verify(exactly = 1) { bidResponseCallback.onError() }
  }

  @Test
  fun `Given there is no bids and indicateRequestAsync is called return value is valid but there are still not bid so callback on error`() {
    val bidResponseCallback = mockk<Callback<BidResponse>>(relaxed = true)
    setTimeout(1)
    setHandlerCaptor()
    every { sdkManager.uiThread.run(any()) } answers {
      firstArg<InternalRunnable>().catchException(mockk(relaxed = true))
    }
    every {
      sdkManager.indicateRequestAsync(any(), any(), any(), any(), any(), any())
    } answers {
      lastArg<ValueCallback<String>>().onReceiveValue("")
    }
    mediationManager.getBidReadyForMediationAsync(
        null, "",
        AdSize(300, 250), AdType.BANNER, 10.00, bidResponseCallback
    )
    verify(exactly = 1) {
      sdkManager.indicateRequestAsync(any(), any(), any(), any(), any(), any())
    }
    verify(exactly = 1) { bidResponseCallback.onError() }
  }

  @Test
  fun `Given there is no bids and indicateRequestAsync is called return value is valid and there are bids so callback on success`() {
    val bidResponseCallback = mockk<Callback<BidResponse>>(relaxed = true)
    val mediation = spyk(mediationManager)
    setTimeout(1)
    setHandlerCaptor()
    every { sdkManager.uiThread.run(any()) } answers {
      firstArg<InternalRunnable>().runInternal()
    }
    every {
      sdkManager.indicateRequestAsync(any(), any(), any(), any(), any(), any())
    } answers {
      lastArg<ValueCallback<String>>().onReceiveValue("success")
    }
    val bid = spyk(validBidResponse)
    every { bidManager.isValid(bid) } answers { true }
    every { mediation.getBidForMediation(any(), any()) } answers { bid }

    mediation.getBidReadyForMediationAsync(
        null, "id",
        AdSize(300, 250), AdType.BANNER, 10.00, bidResponseCallback
    )
    verify(exactly = 1) {
      sdkManager.indicateRequestAsync(any(), any(), any(), any(), any(), any())
    }
//    verify(exactly = 1) { sdkManager.uiThread.run(any()) }
    verify(exactly = 1) { bidResponseCallback.onSuccess(bid) }
  }

  //private methods
  private fun createLowCpmBid(): BidResponse {
    return BidResponse(
        "", "id", "", 0, 0, 0L, 0.0,
        "", "", "", "", "", "", "",
        0, true, "", 0, 0L, true,
        "", 0, null
    )
  }

  private fun setTimeout(timeout: Int) {
    val configuration = mockk<SdkConfigurations>(relaxed = true)
    every { configuration.getAdUnitTimeout(any()) } answers { timeout }
    val auctionManager = mockk<AuctionManager>(relaxed = true)
    Whitebox.setInternalState(sdkManager, "auctionManager", auctionManager)
    every { sdkManager.auctionManager.getSdkConfigurations() } answers { configuration }
  }

  private fun setHandlerCaptor() {
    val handler = mockk<UIThread>(relaxed = true)
    Whitebox.setInternalState(sdkManager, "uiThread", handler)
  }
}

