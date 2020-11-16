package com.monet.bidder

import android.view.View
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.mediation.customevent.CustomEventBannerListener
import com.monet.AdServerBannerListener
import com.monet.AppMonetViewListener
import com.monet.AdServerBannerListener.ErrorCode
import com.monet.AdServerBannerListener.ErrorCode.BAD_REQUEST
import com.monet.AdServerBannerListener.ErrorCode.INTERNAL_ERROR
import com.monet.AdServerBannerListener.ErrorCode.NO_FILL
import com.monet.AdServerBannerListener.ErrorCode.TIMEOUT
import com.monet.AdServerBannerListener.ErrorCode.UNKNOWN
import com.monet.threading.UIThread

/**
 * This wraps the DFP [CustomEventBannerListener]. It's instantiated
 * in the CustomEventBanner when we want to render an ad.
 *
 * @see {@link CustomEventBanner.requestBannerAd
 */
internal class DFPBannerListener(
  private val mListener: CustomEventBannerListener,
  private val viewListener: AppMonetViewListener<View>,
  private val uiThread: UIThread
) : AdServerBannerListener<View?> {

  override fun onAdClicked() {
    mListener.onAdClicked()
  }

  /**
   * Convert an AppMonet ErrorCode into the approximate DFP equivalent
   *
   * @param errorCode an AdServerBannerListener$ErrorCode
   * @return a DFP integer constant error code
   */
  private fun errorCodeToAdRequestError(errorCode: ErrorCode): Int {
    return when (errorCode) {
      NO_FILL -> AdRequest.ERROR_CODE_NO_FILL
      BAD_REQUEST -> AdRequest.ERROR_CODE_INVALID_REQUEST
      TIMEOUT -> AdRequest.ERROR_CODE_NETWORK_ERROR
      INTERNAL_ERROR, UNKNOWN -> AdRequest.ERROR_CODE_INTERNAL_ERROR
      else -> AdRequest.ERROR_CODE_INTERNAL_ERROR
    }
  }

  /**
   * Indicate an error in loading the ad.
   *
   * @param errorCode an the type of error encountered while loading/rendering.
   */
  override fun onAdError(errorCode: ErrorCode) {
    mListener.onAdFailedToLoad(
        errorCodeToAdRequestError(errorCode)
    )
  }

  override fun onAdRefreshed(view: View?) {
    viewListener.onAdRefreshed(view)
  }

  /**
   * Indicate that the opened ad has closed (e.g. landing page was open)
   */
  override fun onAdClosed() {
    mListener.onAdClosed()
  }

  /**
   * Indicate that the ad is loaded & the impression can be counted
   *
   * @param view the view in which the ad was rendered. It will be added to the PublisherAdView
   * @return boolean indicating if the load was successful, or if another error was encountered.
   */
  override fun onAdLoaded(view: View?): Boolean {
    uiThread.run top@{
      val viewLayout = view as AppMonetViewLayout?
      val currentView = viewListener.currentView as AppMonetViewLayout?
      if (viewLayout?.isAdRefreshed == true) {
        currentView?.swapViews(viewLayout, this@DFPBannerListener)
        return@top
      }
      sLogger.debug("DFP: Ad Loaded - Indicating to DFP")
      mListener.onAdLoaded(view)
    }
    return true
  }

  /**
   * Indicate the landing page was opened (in AdActivity)
   */
  override fun onAdOpened() {
    mListener.onAdOpened()
  }

  companion object {
    private val sLogger = Logger("DFPBannerListener")
  }

  override fun onLeftApplication() {
    mListener.onAdLeftApplication()
  }
}