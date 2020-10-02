package com.monet.bidder;

import androidx.annotation.NonNull;
import com.monet.bidder.auction.AuctionRequest;

/**
 * Created by nbjacob on 6/26/17.
 */

class DFPAdServerWrapper implements AdServerWrapper {
  private SdkManager sdkManager;

  public void setSdkManager(SdkManager sdkManager) {
    this.sdkManager = sdkManager;
  }

  @NonNull
  @Override
  public AdServerAdRequest newAdRequest(@NonNull AuctionRequest auctionRequest) {
    return AdRequestFactory.fromAuctionRequest(sdkManager.isPublisherAdView, auctionRequest);
  }

  @NonNull
  @Override
  public AdServerAdRequest newAdRequest(@NonNull AuctionRequest auctionRequest,
      @NonNull Type type) {
    return newAdRequest(auctionRequest);
  }

  @NonNull
  @Override
  public AdServerAdRequest newAdRequest() {
    return AdRequestFactory.createEmptyRequest(sdkManager.isPublisherAdView);
  }

  @NonNull
  @Override
  public AdSize newAdSize(Integer width, Integer height) {
    return new AdSize(width, height);
  }
}
