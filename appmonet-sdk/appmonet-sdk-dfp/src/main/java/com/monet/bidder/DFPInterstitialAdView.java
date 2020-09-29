package com.monet.bidder;

import android.content.Context;

import com.google.android.gms.ads.InterstitialAd;

class DFPInterstitialAdView implements AdServerAdView {
  private final InterstitialAd interstitialAd;
  private String mAdUnitId;

  DFPInterstitialAdView(InterstitialAd interstitialAd) {
    this.interstitialAd = interstitialAd;
    mAdUnitId = interstitialAd.getAdUnitId();
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
  public Context getContext() {
    return null;
  }

  @Override
  public void loadAd(AdServerAdRequest request) {
    DFPAdViewRequest adRequest = (DFPAdViewRequest) request;
    if(interstitialAd != null){
      interstitialAd.loadAd(adRequest.getDFPRequest());
    }
  }
}
