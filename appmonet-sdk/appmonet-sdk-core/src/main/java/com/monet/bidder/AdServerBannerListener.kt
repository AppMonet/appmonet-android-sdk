package com.monet.bidder

import android.view.View

interface AdServerBannerListener {
  fun onAdClosed()
  fun onAdOpened()
  fun onAdLoaded(view: View?): Boolean
  fun onAdClicked()
  fun onAdError(errorCode: ErrorCode)
  fun onAdRefreshed(view: View?)
  enum class ErrorCode {
    NO_FILL,
    INTERNAL_ERROR,
    TIMEOUT,
    UNKNOWN,
    BAD_REQUEST
  }
}