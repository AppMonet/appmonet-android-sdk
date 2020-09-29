package com.monet.bidder;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.monet.bidder.auction.AuctionRequest;

class AdRequestFactory {
  private AdRequestFactory() {
  }

  @NonNull
  static AdServerAdRequest fromAuctionRequest(boolean isPublisherAdView,
      AuctionRequest auctionRequest) {
    if (isPublisherAdView) {
      return DFPAdRequest.fromAuctionRequest(auctionRequest);
    }
    return DFPAdViewRequest.fromAuctionRequest(auctionRequest);
  }

  @NonNull
  static AdServerAdRequest createEmptyRequest(boolean isPublisherAdView) {
    if (isPublisherAdView) {
      return new DFPAdRequest();
    }
    return new DFPAdViewRequest();
  }

  @NonNull
  static AdServerAdRequest fromMediationRequest(boolean isPublisherAdView,
      @NonNull MediationAdRequest adRequest) {
    if (isPublisherAdView) {
      return new DFPAdRequest(adRequest);
    }

    return new DFPAdViewRequest(adRequest);
  }
}
