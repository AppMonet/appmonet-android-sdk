package com.monet.bidder

import android.view.View
import com.monet.bidder.AdServerBannerListener.ErrorCode
import com.monet.bidder.AdServerBannerListener.ErrorCode.BAD_REQUEST
import com.monet.bidder.AdServerBannerListener.ErrorCode.INTERNAL_ERROR
import com.monet.bidder.AdServerBannerListener.ErrorCode.NO_FILL
import com.monet.bidder.AdServerBannerListener.ErrorCode.TIMEOUT
import com.monet.BidResponse
import com.monet.BidResponse.Constant.FLOATING_AD_TYPE
import com.monet.bidder.threading.InternalRunnable
import com.mopub.common.logging.MoPubLog
import com.mopub.common.logging.MoPubLog.AdLogEvent.CLICKED
import com.mopub.common.logging.MoPubLog.AdLogEvent.CUSTOM
import com.mopub.common.logging.MoPubLog.AdLogEvent.LOAD_SUCCESS
import com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED
import com.mopub.mobileads.AdLifecycleListener.InteractionListener
import com.mopub.mobileads.AdLifecycleListener.LoadListener
import com.mopub.mobileads.MoPubErrorCode
import com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE
import com.mopub.mobileads.MoPubErrorCode.NETWORK_NO_FILL
import com.mopub.mobileads.MoPubErrorCode.NETWORK_TIMEOUT
import com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED

internal class MoPubBannerListener(
  private val sdkManager: SdkManager,
  private val mListener: LoadListener?,
  private val mInteractionListener: InteractionListener?,
  private val mBid: BidResponse,
  private val adUnitId: String,
  private val viewListener: AppMonetViewListener
) : AdServerBannerListener {
  var moPubAdViewContainer: View? = null
  override fun onAdError(errorCode: ErrorCode) {
    MoPubLog.log(
        LOAD_FAILED, CustomEventBanner.ADAPTER_NAME, moPubErrorCode(errorCode).intCode,
        moPubErrorCode(errorCode)
    )
    mListener?.onAdLoadFailed(moPubErrorCode(errorCode))
  }

  override fun onAdRefreshed(view: View?) {
    viewListener.onAdRefreshed(view)
  }

  override fun onAdOpened() {
    MoPubLog.log(adUnitId, CUSTOM, CustomEventBanner.ADAPTER_NAME, "Banner opened fullscreen")
    mInteractionListener?.onAdExpanded()
  }

  override fun onAdClosed() {
    MoPubLog.log(adUnitId, CUSTOM, CustomEventBanner.ADAPTER_NAME, "Banner closed fullscreen")
    mInteractionListener?.onAdCollapsed()
  }

  override fun onAdClicked() {
    mInteractionListener?.onAdClicked()
    MoPubLog.log(adUnitId, CLICKED, CustomEventBanner.ADAPTER_NAME)
  }

  override fun onAdLoaded(view: View?): Boolean {

    view?.let {
      try {
        sdkManager.uiThread.run top@{
          try {
            val viewLayout = view as AppMonetViewLayout?
            val currentView = viewListener.currentView
            if (viewLayout?.isAdRefreshed == true) {
              currentView?.swapViews(viewLayout, this@MoPubBannerListener)
              return@top
            }
            if (sdkManager.currentActivity == null && FLOATING_AD_TYPE == mBid.adType) {
              onAdError(NO_FILL)
              return@top
            }
            val factory = AdViewLoadedFactory()
            moPubAdViewContainer = factory.getAdView(
                sdkManager.currentActivity?.get(), sdkManager, view, mBid, adUnitId
            )
            mListener?.onAdLoaded()
            MoPubLog.log(adUnitId, LOAD_SUCCESS, CustomEventBanner.ADAPTER_NAME)
          } catch (e: Exception) {
            sLogger.warn("failed to finish on view: ", e!!.message)
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
    onAdError(INTERNAL_ERROR)
    return false
  }

  companion object {
    private val sLogger = Logger("MoPubBannerListener")
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
}