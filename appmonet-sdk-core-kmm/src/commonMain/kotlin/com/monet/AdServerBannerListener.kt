package com.monet

interface AdServerBannerListener<T> {
  fun onAdClosed()
  fun onAdOpened()
  fun onLeftApplication()
  fun onAdLoaded(view: T?): Boolean
  fun onAdClicked()
  fun onAdError(errorCode: ErrorCode)
  fun onAdRefreshed(view: T?)
  enum class ErrorCode {
    NO_FILL,
    INTERNAL_ERROR,
    TIMEOUT,
    UNKNOWN,
    BAD_REQUEST
  }
}