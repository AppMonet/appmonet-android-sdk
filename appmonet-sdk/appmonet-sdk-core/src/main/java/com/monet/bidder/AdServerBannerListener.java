package com.monet.bidder;

import android.view.View;

public interface AdServerBannerListener {
//    protected static final Logger sLogger = new Logger("BannerListener");

  void onAdClosed();

  void onAdOpened();

  boolean onAdLoaded(final View view);

  void onAdClicked();

  void onAdError(ErrorCode errorCode);

  void onAdRefreshed(View view);

  enum ErrorCode {
    NO_FILL,
    INTERNAL_ERROR,
    TIMEOUT,
    UNKNOWN,
    BAD_REQUEST
  }
}