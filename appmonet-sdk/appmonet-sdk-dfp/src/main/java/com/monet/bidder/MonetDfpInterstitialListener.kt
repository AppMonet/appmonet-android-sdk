package com.monet.bidder

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.ads.mediation.customevent.CustomEventInterstitialListener
import com.monet.bidder.AdServerBannerListener.ErrorCode
import com.monet.bidder.Constants.APPMONET_BROADCAST
import com.monet.bidder.Constants.APPMONET_BROADCAST_MESSAGE
import java.lang.ref.WeakReference

internal class MonetDfpInterstitialListener(
  private val mListener: CustomEventInterstitialListener,
  context: Context?
) : AdServerBannerListener {
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

  override fun onAdError(errorCode: ErrorCode) {}
  override fun onAdRefreshed(view: View) {}

}