package com.monet.bidder;

import com.monet.bidder.auction.AuctionRequest;

class MopubAdServerWrapper implements AdServerWrapper {

  @Override
  public AdServerAdRequest newAdRequest(AuctionRequest auctionRequest) {
    return null;
  }

  @Override
  public AdServerAdRequest newAdRequest(AuctionRequest auctionRequest, Type type) {
    if (type == Type.INTERSTITIAL) {
      return MopubInterstitialAdRequest.fromAuctionRequest(auctionRequest);
    } else if (type == Type.NATIVE) {
      return MopubNativeAdRequest.fromAuctionRequest(auctionRequest);
    } else {
      return MopubAdRequest.fromAuctionRequest(auctionRequest);
    }
  }

  @Override
  public AdServerAdRequest newAdRequest() {
    return new MopubAdRequest();
  }

  @Override
  public AdSize newAdSize(Integer width, Integer height) {
    return new AdSize(width, height);
  }
}