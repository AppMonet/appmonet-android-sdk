package com.monet.bidder;

import android.content.Context;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.view.View;

import com.google.android.gms.ads.mediation.customevent.CustomEventInterstitialListener;

import java.lang.ref.WeakReference;

import static com.monet.bidder.Constants.APPMONET_BROADCAST;
import static com.monet.bidder.Constants.APPMONET_BROADCAST_MESSAGE;

class MonetDfpInterstitialListener implements AdServerBannerListener {
  private final CustomEventInterstitialListener mListener;
  private final WeakReference<Context> context;

  MonetDfpInterstitialListener(CustomEventInterstitialListener listener, Context context) {
    mListener = listener;
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
  }

  @Override
  public boolean onAdLoaded(View view) {
    mListener.onAdLoaded();
    return true;
  }

  @Override
  public void onAdClicked() {
    mListener.onAdClicked();
  }

  @Override
  public void onAdError(ErrorCode errorCode) {
  }

  @Override
  public void onAdRefreshed(View view) {

  }
}
