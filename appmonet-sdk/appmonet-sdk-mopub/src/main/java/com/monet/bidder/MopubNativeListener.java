package com.monet.bidder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.monet.bidder.adview.AdView;
import com.monet.bidder.threading.InternalRunnable;
import com.mopub.nativeads.ImpressionTracker;
import com.mopub.nativeads.NativeClickHandler;
import com.mopub.nativeads.NativeErrorCode;

import java.util.Map;

class MopubNativeListener implements AdServerBannerListener {
  private static final Logger sLogger = new Logger("MopubNativeListener");

  private final CustomEventNative.CustomEventNativeListener mListener;
  private final Map<String, String> serverExtras;
  private final Context context;
  private AppMonetStaticNativeAd staticNativeAd;

  MopubNativeListener(Context context, CustomEventNative.CustomEventNativeListener listener,
                      Map<String, String> serverExtras) {
    this.context = context;
    mListener = listener;
    this.serverExtras = serverExtras;
  }

  private static NativeErrorCode moPubErrorCode(ErrorCode errorCode) {
    switch (errorCode) {
      case INTERNAL_ERROR:
        return NativeErrorCode.UNEXPECTED_RESPONSE_CODE;
      case NO_FILL:
        return NativeErrorCode.NETWORK_NO_FILL;
      case TIMEOUT:
        return NativeErrorCode.NETWORK_TIMEOUT;
      case BAD_REQUEST:
        return NativeErrorCode.NETWORK_INVALID_STATE;
      default:
        return NativeErrorCode.UNSPECIFIED;
    }
  }

  @Override
  public void onAdError(ErrorCode errorCode) {
    mListener.onNativeAdFailed(moPubErrorCode(errorCode));
  }

  @Override
  public void onAdRefreshed(View view) {
    staticNativeAd.setMedia(view);
  }

  @Override
  public void onAdOpened() {
    // Not implemented
  }

  @Override
  public void onAdClosed() {
    //Not implemented
  }

  @Override
  public void onAdClicked() {
    if (staticNativeAd != null) {
      staticNativeAd.onAdClicked();
    }
  }
  @SuppressLint("infer")
  @Override
  public boolean onAdLoaded(final View view) {
    try {
      SdkManager.get().getUiThread().run(new InternalRunnable() {
        @Override
        public void runInternal() {
          AppMonetViewLayout viewLayout = (AppMonetViewLayout) view;
          if(staticNativeAd != null && viewLayout.isAdRefreshed()){
            staticNativeAd.swapViews(viewLayout, MopubNativeListener.this);
            return;
          }
          staticNativeAd = new AppMonetStaticNativeAd(serverExtras, view, new ImpressionTracker(context),
              new NativeClickHandler(context), mListener, new AppMonetNativeEventCallback() {
            @Override
            public void destroy(View view) {
              AdView adView = (AdView) ((ViewGroup) view).getChildAt(0);
              if (adView != null) {
                adView.destroy(true);
              }
            }

            @Override
            public void onClick(View view) {
              // Obtain MotionEvent object
              float x = view.getWidth() / 2F;
              float y = view.getHeight() / 2F;
              int metaState = 0;
              MotionEvent actionDown = MotionEvent.obtain(SystemClock.uptimeMillis(),
                  SystemClock.uptimeMillis() + 100, MotionEvent.ACTION_DOWN, x, y, metaState);
              view.dispatchTouchEvent(actionDown);

              MotionEvent actionUp = MotionEvent.obtain(SystemClock.uptimeMillis() + 150,
                  SystemClock.uptimeMillis() + 250, MotionEvent.ACTION_UP, x, y, metaState);
              view.dispatchTouchEvent(actionUp);
            }
          });
          staticNativeAd.loadAd();
        }

        @Override
        public void catchException(Exception e) {
          sLogger.warn("failed to finish on view: ", e.getMessage());
          mListener.onNativeAdFailed(NativeErrorCode.UNEXPECTED_RESPONSE_CODE);
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
