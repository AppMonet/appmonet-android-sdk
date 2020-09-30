package com.monet.bidder;

import android.content.Context;

import java.util.List;

public interface AdServerAdView {
  AdServerWrapper.Type getType();

  String getAdUnitId();

  void setAdUnitId(String adUnitId);

  Context getContext();

  void loadAd(AdServerAdRequest request);
}