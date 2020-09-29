package com.monet.bidder;

import android.webkit.ValueCallback;

class AddBidsParams {
  private final ValueCallback<AdServerAdRequest> callback;
  private final AdServerAdRequest request;
  private final int timeout;
  private final AdServerAdView adView;

  AddBidsParams(AdServerAdView adView, AdServerAdRequest request, int timeout,
                ValueCallback<AdServerAdRequest> callback) {
    this.adView = adView;
    this.request = request;
    this.timeout = timeout;
    this.callback = callback;
  }

  public ValueCallback<AdServerAdRequest> getCallback() {
    return callback;
  }

  public AdServerAdRequest getRequest() {
    return request;
  }

  public int getTimeout() {
    return timeout;
  }

  public AdServerAdView getAdView() {
    return adView;
  }
}
