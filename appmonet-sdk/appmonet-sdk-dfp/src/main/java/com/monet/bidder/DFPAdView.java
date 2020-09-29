package com.monet.bidder;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

class DFPAdView implements AdServerAdView {
  private final com.google.android.gms.ads.AdView mAdView;
  private String mAdUnitId;
  private Context mContext;

  DFPAdView(com.google.android.gms.ads.AdView adView) {
    mAdView = adView;
    mContext = adView.getContext();
    mAdUnitId = adView.getAdUnitId();
  }

  @Override
  public AdServerWrapper.Type getType() {
    return AdServerWrapper.Type.BANNER;
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
    return mContext;
  }

  @Override
  public void loadAd(AdServerAdRequest request) {
    DFPAdViewRequest adRequest = (DFPAdViewRequest) request;
    if (mAdView != null) {
      mAdView.loadAd(adRequest.getDFPRequest());
    }
  }
}
