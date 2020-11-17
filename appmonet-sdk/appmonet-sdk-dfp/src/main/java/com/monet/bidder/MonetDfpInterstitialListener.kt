package com.monet.bidder

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.mediation.customevent.CustomEventInterstitialListener
import com.monet.AdServerBannerListener
import com.monet.AdServerBannerListener.ErrorCode
import com.monet.AdServerBannerListener.ErrorCode.BAD_REQUEST
import com.monet.AdServerBannerListener.ErrorCode.INTERNAL_ERROR
import com.monet.AdServerBannerListener.ErrorCode.NO_FILL
import com.monet.AdServerBannerListener.ErrorCode.TIMEOUT
import com.monet.AdServerBannerListener.ErrorCode.UNKNOWN
import com.monet.bidder.Constants.APPMONET_BROADCAST
import com.monet.bidder.Constants.APPMONET_BROADCAST_MESSAGE
import java.lang.ref.WeakReference

internal class MonetDfpInterstitialListener(
  private val mListener: CustomEventInterstitialListener,
  context: Context?
) : AdServerBannerListener<View?> {
  private val context: WeakReference<Context?> = WeakReference(context)
  override fun onAdClosed() {
    if (context.get() != null) {
      LocalBroadcastManager.getInstance(context.get()!!).sendBroadcast(
          Intent(APPMONET_BROADCAST)
              .putExtra(
                  APPMONET_BROADCAST_MESSAGE,
                  "interstitial_dismissed"
              )
      )
    }
  }

  override fun onAdOpened() {}
  override fun onAdLoaded(view: View?): Boolean {
    mListener.onAdLoaded()
    return true
  }

  override fun onAdClicked() {
    mListener.onAdClicked()
  }

  override fun onAdError(errorCode: ErrorCode) {
    mListener.onAdFailedToLoad(errorCodeToAdRequestError(errorCode))
  }

  override fun onAdRefreshed(view: View?) {}
  override fun onLeftApplication() {
    mListener.onAdLeftApplication()
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
}