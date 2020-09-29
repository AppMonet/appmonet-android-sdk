package com.monet.bidder;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.monet.bidder.threading.InternalRunnable;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.AdLifecycleListener;
import com.mopub.mobileads.MoPubErrorCode;

import java.lang.ref.WeakReference;

import static com.monet.bidder.Constants.APPMONET_BROADCAST;
import static com.monet.bidder.Constants.APPMONET_BROADCAST_MESSAGE;
import static com.monet.bidder.CustomEventInterstitial.ADAPTER_NAME;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.LOAD_SUCCESS;


/**
 * Created by jose on 3/14/18.
 */

class MonetInterstitialListener implements AdServerBannerListener {
  private static final Logger sLogger = new Logger("MonetInterstitialListener");

  private final WeakReference<Context> context;
  private final SdkManager sdkManager;
  private final AdLifecycleListener.LoadListener mLoadListener;
  private final AdLifecycleListener.InteractionListener mInteractionListener;
  private final String adUnitId;


  public MonetInterstitialListener(@Nullable AdLifecycleListener.LoadListener mLoadListener,
                                   @Nullable AdLifecycleListener.InteractionListener mInteractionListener,
                                   @NonNull String adUnitId,
                                   @NonNull Context context, @NonNull SdkManager sdkManager) {
    this.sdkManager = sdkManager;
    this.mLoadListener = mLoadListener;
    this.mInteractionListener = mInteractionListener;
    this.adUnitId = adUnitId;
    this.context = new WeakReference<>(context);
  }

  @Override
  public void onAdClosed() {
    if (this.context.get() != null) {
      LocalBroadcastManager.getInstance(this.context.get()).sendBroadcast(
          new Intent(APPMONET_BROADCAST)
              .putExtra(APPMONET_BROADCAST_MESSAGE,
                  "interstitial_dismissed"));
    }
  }

  @Override
  public void onAdOpened() {
    //no implementation
  }

  @Override
  public boolean onAdLoaded(View view) {
    try {
      sdkManager.getUiThread().run(new InternalRunnable() {
        @Override
        public void runInternal() {
          MoPubLog.log(adUnitId, LOAD_SUCCESS, ADAPTER_NAME);
          if (mLoadListener != null) {
            mLoadListener.onAdLoaded();
          }
        }

        @Override
        public void catchException(Exception e) {
          sLogger.warn("failed to finish on view: ", e.getMessage());
          onAdError(ErrorCode.INTERNAL_ERROR);
        }
      });
    } catch (Exception e) {
      sLogger.error("error while loading into MoPub", e.getMessage());
      onAdError(ErrorCode.INTERNAL_ERROR);
      return false;
    }
    return true;
  }

  @Override
  public void onAdClicked() {
    MoPubLog.log(adUnitId, CLICKED, ADAPTER_NAME);
    if (mInteractionListener != null) {
      mInteractionListener.onAdClicked();
    }
  }

  @Override
  public void onAdError(ErrorCode errorCode) {
    MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, moPubErrorCode(errorCode).getIntCode(),
        moPubErrorCode(errorCode));
    if (mInteractionListener != null) {
      mInteractionListener.onAdFailed(moPubErrorCode(errorCode));
    }
  }

  @Override
  public void onAdRefreshed(View view) {
    //no implementation
  }

  private static MoPubErrorCode moPubErrorCode(ErrorCode errorCode) {
    switch (errorCode) {
      case INTERNAL_ERROR:
        return MoPubErrorCode.INTERNAL_ERROR;
      case NO_FILL:
        return MoPubErrorCode.NETWORK_NO_FILL;
      case TIMEOUT:
        return MoPubErrorCode.NETWORK_TIMEOUT;
      case BAD_REQUEST:
        return MoPubErrorCode.NETWORK_INVALID_STATE;
      default:
        return MoPubErrorCode.UNSPECIFIED;
    }
  }
}
