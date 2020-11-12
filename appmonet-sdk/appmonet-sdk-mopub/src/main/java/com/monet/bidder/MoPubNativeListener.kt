package com.monet.bidder

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.monet.bidder.AdServerBannerListener.ErrorCode
import com.monet.bidder.AdServerBannerListener.ErrorCode.BAD_REQUEST
import com.monet.bidder.AdServerBannerListener.ErrorCode.INTERNAL_ERROR
import com.monet.bidder.AdServerBannerListener.ErrorCode.NO_FILL
import com.monet.bidder.AdServerBannerListener.ErrorCode.TIMEOUT
import com.monet.bidder.adview.AdView
import com.monet.bidder.threading.InternalRunnable
import com.mopub.nativeads.CustomEventNative.CustomEventNativeListener
import com.mopub.nativeads.ImpressionTracker
import com.mopub.nativeads.NativeClickHandler
import com.mopub.nativeads.NativeErrorCode
import com.mopub.nativeads.NativeErrorCode.NETWORK_INVALID_STATE
import com.mopub.nativeads.NativeErrorCode.NETWORK_NO_FILL
import com.mopub.nativeads.NativeErrorCode.NETWORK_TIMEOUT
import com.mopub.nativeads.NativeErrorCode.UNEXPECTED_RESPONSE_CODE
import com.mopub.nativeads.NativeErrorCode.UNSPECIFIED

internal class MoPubNativeListener(
  private val context: Context,
  private val mListener: CustomEventNativeListener,
  private val serverExtras: Map<String, String>
) : AdServerBannerListener {
  private var staticNativeAd: AppMonetStaticNativeAd? = null
  override fun onAdError(errorCode: ErrorCode) {
    mListener.onNativeAdFailed(moPubErrorCode(errorCode))
  }

  override fun onAdRefreshed(view: View?) {
    staticNativeAd!!.media = view
  }

  override fun onAdOpened() {
    // Not implemented
  }

  override fun onAdClosed() {
    //Not implemented
  }

  override fun onAdClicked() {
    if (staticNativeAd != null) {
      staticNativeAd!!.onAdClicked()
    }
  }

  @SuppressLint("infer") override fun onAdLoaded(view: View?): Boolean {
    try {
      SdkManager.get()?.uiThread?.run top@{
        try {
          val viewLayout = view as AppMonetViewLayout
          if (staticNativeAd != null && viewLayout.isAdRefreshed) {
            staticNativeAd!!.swapViews(viewLayout, this@MoPubNativeListener)
            return@top
          }
          staticNativeAd = AppMonetStaticNativeAd(
              serverExtras, view, ImpressionTracker(context),
              NativeClickHandler(context), mListener, object : AppMonetNativeEventCallback {
            override fun destroy(view: View?) {
              val adView = (view as ViewGroup?)!!.getChildAt(0) as AdView
              adView?.destroy(true)
            }

            override fun onClick(view: View?) {
            }
          })
          staticNativeAd!!.loadAd()
        } catch (e: Exception) {
          sLogger.warn("failed to finish on view: ", e.message)
          mListener.onNativeAdFailed(UNEXPECTED_RESPONSE_CODE)
        }
      }
    } catch (e: Exception) {
      sLogger.error("error while loading into MoPub", e.message)
      onAdError(INTERNAL_ERROR)
      return false
    }
    return true
  }

  companion object {
    private val sLogger = Logger("MoPubNativeListener")
    private fun moPubErrorCode(errorCode: ErrorCode): NativeErrorCode {
      return when (errorCode) {
        INTERNAL_ERROR -> UNEXPECTED_RESPONSE_CODE
        NO_FILL -> NETWORK_NO_FILL
        TIMEOUT -> NETWORK_TIMEOUT
        BAD_REQUEST -> NETWORK_INVALID_STATE
        else -> UNSPECIFIED
      }
    }
  }
}