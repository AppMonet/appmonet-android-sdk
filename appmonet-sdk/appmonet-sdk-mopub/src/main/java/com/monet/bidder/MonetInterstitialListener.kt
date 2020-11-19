package com.monet.bidder

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.monet.AdServerBannerListener
import com.monet.AdServerBannerListener.ErrorCode
import com.monet.AdServerBannerListener.ErrorCode.BAD_REQUEST
import com.monet.AdServerBannerListener.ErrorCode.INTERNAL_ERROR
import com.monet.AdServerBannerListener.ErrorCode.NO_FILL
import com.monet.AdServerBannerListener.ErrorCode.TIMEOUT
import com.monet.bidder.Constants.APPMONET_BROADCAST
import com.monet.bidder.Constants.APPMONET_BROADCAST_MESSAGE
import com.mopub.common.logging.MoPubLog
import com.mopub.common.logging.MoPubLog.AdLogEvent.CLICKED
import com.mopub.common.logging.MoPubLog.AdLogEvent.LOAD_FAILED
import com.mopub.common.logging.MoPubLog.AdLogEvent.LOAD_SUCCESS
import com.mopub.mobileads.AdLifecycleListener.InteractionListener
import com.mopub.mobileads.AdLifecycleListener.LoadListener
import com.mopub.mobileads.MoPubErrorCode
import com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE
import com.mopub.mobileads.MoPubErrorCode.NETWORK_NO_FILL
import com.mopub.mobileads.MoPubErrorCode.NETWORK_TIMEOUT
import com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED
import java.lang.ref.WeakReference

/**
 * Created by jose on 3/14/18.
 */
internal class MonetInterstitialListener(
  private val mLoadListener: LoadListener?,
  private val mInteractionListener: InteractionListener?,
  context: Context,
  private val sdkManager: SdkManager
) : AdServerBannerListener<View?> {
  private val context: WeakReference<Context?> = WeakReference(context)
  var adUnitId: String? = null
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

  override fun onAdOpened() {
    //no implementation
  }

  override fun onAdLoaded(view: View?): Boolean {
    try {
      sdkManager.uiThread.run {
        try {
          MoPubLog.log(adUnitId, LOAD_SUCCESS, CustomEventInterstitial.ADAPTER_NAME)
          mLoadListener?.onAdLoaded()
        } catch (e: Exception) {
          sLogger.warn("failed to finish on view: ", e.message)
          onAdError(INTERNAL_ERROR)
        }
      }
    } catch (e: Exception) {
      sLogger.error("error while loading into MoPub", e.message)
      onAdError(INTERNAL_ERROR)
      return false
    }
    return true
  }

  override fun onAdClicked() {
    MoPubLog.log(adUnitId, CLICKED, CustomEventInterstitial.ADAPTER_NAME)
    mInteractionListener?.onAdClicked()
  }

  override fun onAdError(errorCode: ErrorCode) {
    MoPubLog.log(
        LOAD_FAILED, CustomEventInterstitial.ADAPTER_NAME, moPubErrorCode(errorCode).intCode,
        moPubErrorCode(errorCode)
    )
    mInteractionListener?.onAdFailed(moPubErrorCode(errorCode))
  }

  override fun onAdRefreshed(view: View?) {
    //no implementation
  }

  companion object {
    private val sLogger = Logger("MonetInterstitialListener")
    private fun moPubErrorCode(errorCode: ErrorCode): MoPubErrorCode {
      return when (errorCode) {
        INTERNAL_ERROR -> MoPubErrorCode.INTERNAL_ERROR
        NO_FILL -> NETWORK_NO_FILL
        TIMEOUT -> NETWORK_TIMEOUT
        BAD_REQUEST -> NETWORK_INVALID_STATE
        else -> UNSPECIFIED
      }
    }
  }

  override fun onLeftApplication() {
    TODO("Not yet implemented")
  }

}