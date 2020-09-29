package com.monet.bidder;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.monet.bidder.bid.BidResponse;
import com.monet.bidder.threading.InternalRunnable;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.AdLifecycleListener;
import com.mopub.mobileads.MoPubErrorCode;

import static com.monet.bidder.CustomEventBanner.ADAPTER_NAME;
import static com.monet.bidder.bid.BidResponse.Constant.FLOATING_AD_TYPE;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;

class MopubBannerListener implements AdServerBannerListener {
  private static final Logger sLogger = new Logger("MopubBannerListener");

  private final AppMonetViewListener viewListener;
  private final SdkManager sdkManager;
  View moPubAdViewContainer;
  private final String mAdUnitId;
  private final BidResponse mBid;
  private final AdLifecycleListener.LoadListener mListener;
  private final AdLifecycleListener.InteractionListener mInteractionListener;

  MopubBannerListener(@NonNull SdkManager sdkManager,
                      @Nullable AdLifecycleListener.LoadListener mLoadListener,
                      @Nullable AdLifecycleListener.InteractionListener mInteractionListener,
                      @NonNull BidResponse bid, @NonNull String adUnitId,
                      @NonNull AppMonetViewListener viewListener) {
    this.sdkManager = sdkManager;
    this.mInteractionListener = mInteractionListener;
    mAdUnitId = adUnitId;
    mListener = mLoadListener;
    mBid = bid;
    this.viewListener = viewListener;
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

  @Override
  public void onAdError(ErrorCode errorCode) {
    MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, moPubErrorCode(errorCode).getIntCode(), moPubErrorCode(errorCode));
    if (mListener != null) {
      mListener.onAdLoadFailed(moPubErrorCode(errorCode));
    }
  }

  @Override
  public void onAdRefreshed(View view) {
    viewListener.onAdRefreshed(view);
  }

  @Override
  public void onAdOpened() {
    MoPubLog.log(mAdUnitId, CUSTOM, ADAPTER_NAME, "Banner opened fullscreen");
    if (mInteractionListener != null) {
      mInteractionListener.onAdExpanded();
    }
  }

  @Override
  public void onAdClosed() {
    MoPubLog.log(mAdUnitId, CUSTOM, ADAPTER_NAME, "Banner closed fullscreen");
    if (mInteractionListener != null) {
      mInteractionListener.onAdCollapsed();
    }
  }

  @Override
  public void onAdClicked() {
    if (mInteractionListener != null) {
      mInteractionListener.onAdClicked();
    }
    MoPubLog.log(mAdUnitId, CLICKED, ADAPTER_NAME);
  }

  @Override
  public boolean onAdLoaded(final View view) {
    try {
      sdkManager.getUiThread().run(new InternalRunnable() {
        @Override
        public void runInternal() {
          AppMonetViewLayout viewLayout = (AppMonetViewLayout) view;
          AppMonetViewLayout currentView = viewListener.getCurrentView();
          if (viewLayout.isAdRefreshed()) {
            currentView.swapViews(viewLayout, MopubBannerListener.this);
            return;
          }

          if (sdkManager.currentActivity == null &&FLOATING_AD_TYPE.equals(mBid.getAdType())) {
            onAdError(ErrorCode.NO_FILL);
            return;
          }
          AdViewLoadedFactory factory = new AdViewLoadedFactory();
          moPubAdViewContainer = factory.getAdView(sdkManager, view, mBid, mAdUnitId);
          if (mListener != null) {
            mListener.onAdLoaded();
          }
          MoPubLog.log(mAdUnitId, LOAD_SUCCESS, ADAPTER_NAME);
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
}
