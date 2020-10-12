package com.monet.bidder

import com.monet.bidder.threading.InternalRunnable
import com.mopub.mobileads.DefaultBannerAdListener
import com.mopub.mobileads.MoPubErrorCode
import com.mopub.mobileads.MoPubView
import com.mopub.mobileads.MoPubView.BannerAdListener

/**
 * Created by nbjacob on 9/2/17.
 */
internal class MopubBannerAdListener(
  private val mAdUnitId: String,
  originalListener: BannerAdListener?,
  private val manager: SdkManager
) : BannerAdListener {
  private val mOriginalListener: BannerAdListener = originalListener ?: DefaultBannerAdListener()
  private var mRefreshTimer: Runnable? = null
  fun setBannerRefreshTimer(banner: MoPubView?) {
    if (mRefreshTimer != null) {
      manager.uiThread.run(mRefreshTimer!!)
    }
    mRefreshTimer = object : InternalRunnable() {
      override fun runInternal() {
        sLogger.debug("Attaching next bid (after load)")
        manager.addBids(banner, mAdUnitId)
      }

      override fun catchException(e: Exception?) {}
    }

    manager.uiThread.runDelayed(mRefreshTimer!!, REFRESH_TRY_DELAY.toLong())
  }

  override fun onBannerLoaded(banner: MoPubView) {
    sLogger.debug("banner loaded. Attaching next bid")
    setBannerRefreshTimer(banner)
    mOriginalListener.onBannerLoaded(banner)
  }

  override fun onBannerFailed(
    banner: MoPubView,
    errorCode: MoPubErrorCode
  ) {
    sLogger.debug("banner failed. Attaching new bid")
    manager.addBids(banner, mAdUnitId)
    mOriginalListener.onBannerFailed(banner, errorCode)
  }

  override fun onBannerClicked(banner: MoPubView) {
    mOriginalListener.onBannerClicked(banner)
  }

  override fun onBannerExpanded(banner: MoPubView) {
    mOriginalListener.onBannerExpanded(banner)
  }

  override fun onBannerCollapsed(banner: MoPubView) {
    mOriginalListener.onBannerCollapsed(banner)
  }

  companion object {
    private const val REFRESH_TRY_DELAY = 4000
    private val sLogger = Logger("BannerAdListener")
  }

}