package com.monet.bidder

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.monet.AdServerBannerListener
import com.monet.AdType.BANNER
import com.monet.bidder.adview.AdViewManager
import com.monet.bidder.adview.AdViewManager.AdViewState.AD_RENDERED
import com.monet.bidder.adview.AdViewPoolManagerCallback
import com.monet.bidder.auction.AuctionManagerCallback
import com.monet.BidResponse
import com.monet.BidResponse.Constant.FLOATING_AD_TYPE
import com.monet.IAppMonetViewLayout
import com.monet.MediationManager.NoBidsFoundException
import com.monet.MediationManager.NullBidException
import com.monet.adview.AdSize
import java.lang.ref.WeakReference

@SuppressLint("ViewConstructor")
class AppMonetViewLayout(
  context: Context,
  adViewPoolManagerCallback: AdViewPoolManagerCallback,
  auctionManagerCallback: AuctionManagerCallback,
  adViewManager: AdViewManager,
  adSize: AdSize
) : FrameLayout(context), IAppMonetViewLayout {
  private val adViewPoolManagerCallback: AdViewPoolManagerCallback
  private val auctionManager: AuctionManagerCallback
  private val adViewManager: WeakReference<AdViewManager> = WeakReference(adViewManager)
  private var innerHandler: Handler? = null
  private var runnable: Runnable? = null
  private var parent: ViewGroup? = null
  val uuid = adViewManager.uuid
  val state = adViewManager.state
  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    parent = getParent() as ViewGroup
  }

  override fun destroyAdView(invalidate: Boolean) {
    adViewManager.get()!!.destroy(invalidate)
    cleanup()
  }

  override fun removeAllViews() {
    super.removeAllViews()
    cleanup()
  }

  fun activateRefresh(
    bid: BidResponse,
    listener: AdServerBannerListener<View?>?
  ) {
    innerHandler = Handler(Looper.getMainLooper())
    if (FLOATING_AD_TYPE == bid.adType || bid.refresh <= 1000) {
      return
    }
    val adUnit = bid.adUnitId
    runnable = Runnable {
      val mediationBid = auctionManager.getBidWithFloorCpm(adUnit, 0.0)
      val mediationManager = auctionManager.mediationManager
      try {
        val adSize =
          if (mediationBid != null) AdSize(
              context.applicationContext, mediationBid.width, mediationBid.height
          ) else null
        val nextBid = mediationManager.getBidReadyForMediation(
            mediationBid, adUnit, adSize,
            BANNER, 0.0, true
        )
        val nextAdView = adViewPoolManagerCallback.request(nextBid)
        if (nextAdView == null) {
          innerHandler?.postDelayed(runnable, bid.refresh.toLong())
          return@Runnable
        }
        if (!nextAdView.isLoaded) {
          nextAdView.load()
        }
        auctionManager.markBidAsUsed(bid)
        nextAdView.isAdRefreshed = true
        nextAdView.bid = nextBid
        nextAdView.bidForTracking = nextBid
        nextAdView.setState(AD_RENDERED, listener, context)
        // this is always done after the state change
        nextAdView.inject(nextBid)
      } catch (e: NoBidsFoundException) {
        innerHandler?.postDelayed(runnable, bid.refresh.toLong())
      } catch (e: NullBidException) {
        innerHandler?.postDelayed(runnable, bid.refresh.toLong())
      }
    }
    innerHandler?.postDelayed(runnable, bid.refresh.toLong())
  }

  private fun getLayoutParams(adSize: AdSize): LayoutParams {
    return LayoutParams(
        adSize.getWidthInPixels(),
        adSize.getHeightInPixels(),
        Gravity.CENTER
    )
  }

  private fun cleanup() {
    if (innerHandler != null && runnable != null) {
      innerHandler?.removeCallbacks(runnable)
    }
    if (innerHandler != null) {
      innerHandler?.removeCallbacksAndMessages(null)
    }
    runnable = null
    innerHandler = null
    parent = null
  }

  fun swapViews(
    view: AppMonetViewLayout,
    listener: AdServerBannerListener<View?>
  ) {
    if (view !== this) {
      innerHandler?.removeCallbacksAndMessages(null)
      innerHandler?.removeCallbacks(runnable)
      view.parent = parent
      parent!!.removeAllViews()
      parent!!.addView(view)
      parent!!.removeView(this)
      destroyAdView(true)
      parent = null
    }
    listener.onAdRefreshed(view)
  }

  val isAdRefreshed: Boolean
    get() = adViewManager.get()!!.isAdRefreshed

  init {
    this.adViewPoolManagerCallback = adViewPoolManagerCallback
    auctionManager = auctionManagerCallback
    addView(adViewManager.adView, getLayoutParams(adSize))
  }
}