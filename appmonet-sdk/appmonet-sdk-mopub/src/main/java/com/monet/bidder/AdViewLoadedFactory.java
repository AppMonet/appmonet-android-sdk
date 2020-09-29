package com.monet.bidder;

import android.view.View;

import com.monet.bidder.bid.BidResponse;
import com.mopub.mobileads.MoPubView;

class AdViewLoadedFactory {
  View getAdView(SdkManager manager, View originalView, BidResponse bid, String adUnit) {
    final MoPubView mopubView = manager.getMopubAdView(adUnit);
    if (manager.currentActivity != null && manager.getFloatingAdPosition(adUnit) != null
        && mopubView != null) {
      FloatingAdView.Params params = new FloatingAdView.Params(manager, originalView, bid,
          mopubView.getAdWidth(), mopubView.getAdHeight(), adUnit);
      return new FloatingAdView(manager, params, originalView.getContext());
    } else if(manager.currentActivity!=null && manager.getFloatingAdPosition(adUnit) != null){
      FloatingAdView.Params params = new FloatingAdView.Params(manager, originalView, bid,
          null, null, adUnit);
      return new FloatingAdView(manager, params, originalView.getContext());
    }
    return originalView;
  }
}
