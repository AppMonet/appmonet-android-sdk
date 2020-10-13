package com.monet.bidder.bid;

import android.content.Context;
import androidx.annotation.NonNull;

import com.monet.bidder.AdServerBannerListener;
import com.monet.bidder.AdSize;
import com.monet.bidder.AppMonetViewLayout;
import com.monet.bidder.BaseManager;
import com.monet.bidder.Logger;
import com.monet.bidder.adview.AdViewManager;

import static com.monet.bidder.BaseManager.isTestMode;
import static com.monet.bidder.Constants.TEST_MODE_WARNING;

public class BidRenderer {
  private static final Logger sLogger = new Logger("Renderer");

  private BidRenderer() {
  }

  public static AppMonetViewLayout renderBid(@NonNull Context context,
      @NonNull BaseManager sdkManager,
      @NonNull BidResponse bidResponse, AdSize adSize, @NonNull AdServerBannerListener listener) {
    sLogger.info("Rendering bid:", bidResponse.toString());
    if (!sdkManager.getAuctionManager().getBidManager().isValid(bidResponse)) {
      sdkManager.getAuctionManager().trackEvent("bidRenderer",
          "invalid_bid", bidResponse.getId(), 0f, System.currentTimeMillis());
      return null;
    }

    AdViewManager adViewManager = sdkManager.getAuctionManager().getAdViewPoolManager().request(bidResponse);

    if (adViewManager == null) {
      sLogger.warn("fail to attach adView. Unable to serve");
      sdkManager.getAuctionManager().trackEvent("bidRenderer",
          "null_view", bidResponse.getId(), 0f, System.currentTimeMillis());
      return null;
    }

    if (!adViewManager.isLoaded()) {
      // load sdk.js
      sLogger.debug("Initializing AdView for injection");
      adViewManager.load();
    }
    sdkManager.getAuctionManager().getBidManager().markUsed(bidResponse);

    adViewManager.setBid(bidResponse);
    adViewManager.setBidForTracking(bidResponse);
    adViewManager.setState(AdViewManager.AdViewState.AD_RENDERED, listener, context);

    // this is always done after the state change
    sLogger.debug("injecting ad into view");
    adViewManager.inject(bidResponse);
    adViewManager.setShouldAdRefresh(false);
    if (adSize != null
        && adSize.getWidth() != 0
        && adSize.getHeight() != 0
        && bidResponse.getFlexSize()) {
      adViewManager.resize(adSize);
    }
    if (isTestMode) {
      sLogger.warn(TEST_MODE_WARNING);
    }
    return (AppMonetViewLayout) adViewManager.adView.getParent();
  }
}
