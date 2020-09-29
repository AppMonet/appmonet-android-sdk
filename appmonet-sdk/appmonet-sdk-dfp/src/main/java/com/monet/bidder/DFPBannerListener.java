package com.monet.bidder;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.View;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.customevent.CustomEventBannerListener;
import com.monet.bidder.threading.InternalRunnable;
import com.monet.bidder.threading.UIThread;

/**
 * This wraps the DFP {@link CustomEventBannerListener}. It's instantiated
 * in the CustomEventBanner when we want to render an ad.
 *
 * @see {@link CustomEventBanner#requestBannerAd(Context, CustomEventBannerListener, String, AdSize, MediationAdRequest, Bundle)}
 */
class DFPBannerListener implements AdServerBannerListener {
  private static final Logger sLogger = new Logger("DFPBannerListener");

  private final CustomEventBannerListener mListener;
  private final AppMonetViewListener viewListener;
  private final  UIThread uiThread;

  DFPBannerListener(@NonNull CustomEventBannerListener listener,
                    @NonNull AppMonetViewListener viewListener,
                    @NonNull UIThread uiThread) {
    this.uiThread = uiThread;
    mListener = listener;
    this.viewListener = viewListener;
  }

  @Override
  public void onAdClicked() {
    mListener.onAdClicked();
  }

  /**
   * Convert an AppMonet ErrorCode into the approximate DFP equivalent
   *
   * @param errorCode an AdServerBannerListener$ErrorCode
   * @return a DFP integer constant error code
   */
  private int errorCodeToAdRequestError(ErrorCode errorCode) {
    switch (errorCode) {
      case NO_FILL:
        return AdRequest.ERROR_CODE_NO_FILL;
      case BAD_REQUEST:
        return AdRequest.ERROR_CODE_INVALID_REQUEST;
      case TIMEOUT:
        return AdRequest.ERROR_CODE_NETWORK_ERROR;
      case INTERNAL_ERROR:
      case UNKNOWN:
      default:
        return AdRequest.ERROR_CODE_INTERNAL_ERROR;
    }
  }

  /**
   * Indicate an error in loading the ad.
   *
   * @param errorCode an the type of error encountered while loading/rendering.
   */
  @Override
  public void onAdError(ErrorCode errorCode) {
    mListener.onAdFailedToLoad(
        errorCodeToAdRequestError(errorCode));
  }

  @Override
  public void onAdRefreshed(View view) {
    viewListener.onAdRefreshed(view);
  }

  /**
   * Indicate that the opened ad has closed (e.g. landing page was open)
   */
  @Override
  public void onAdClosed() {
    mListener.onAdClosed();
  }

  /**
   * Indicate that the ad is loaded & the impression can be counted
   *
   * @param view the view in which the ad was rendered. It will be added to the PublisherAdView
   * @return boolean indicating if the load was successful, or if another error was encountered.
   */
  @Override
  public boolean onAdLoaded(final View view) {
    uiThread.run(new InternalRunnable() {
      @Override
      public void runInternal() {
        AppMonetViewLayout viewLayout = (AppMonetViewLayout) view;
        AppMonetViewLayout currentView = viewListener.getCurrentView();
        if (viewLayout.isAdRefreshed()) {
          currentView.swapViews(viewLayout, DFPBannerListener.this);
          return;
        }

        sLogger.debug("DFP: Ad Loaded - Indicating to DFP");
        mListener.onAdLoaded(view);
      }

      @Override
      public void catchException(Exception e) {
        sLogger.error("Exception caught: " + e);
      }
    });
    return true;
  }

  /**
   * Indicate the landing page was opened (in AdActivity)
   */
  @Override
  public void onAdOpened() {
    mListener.onAdOpened();
  }
}
