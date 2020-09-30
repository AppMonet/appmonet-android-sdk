package com.monet.bidder;

import android.content.Context;
import android.view.View;

import com.mopub.mobileads.MoPubInterstitial;

/**
 * Created by nbjacob on 6/26/17.
 */

class MopubInterstitialAdView implements AdServerAdView {
  private final MoPubInterstitial moPubInterstitial;
  private final View mContentView;
  private String mAdUnitId;

  MopubInterstitialAdView(MoPubInterstitial moPubInterstitial, String adUnitId) {
    this.moPubInterstitial = moPubInterstitial;
    mContentView = null;
    mAdUnitId = adUnitId;
  }

  MoPubInterstitial getMopubView() {
    return moPubInterstitial;
  }

  @Override
  public Context getContext() {
    return moPubInterstitial.getActivity();
  }


  @Override
  public AdServerWrapper.Type getType() {
    return AdServerWrapper.Type.INTERSTITIAL;
  }

  @Override
  public String getAdUnitId() {
    return mAdUnitId;
  }

  @Override
  public void setAdUnitId(String adUnitId) {
    mAdUnitId = adUnitId;
  }

  @Override
  public void loadAd(AdServerAdRequest request) {
  }
}
