package com.monet.bidder;

import com.monet.bidder.auction.AuctionRequest;

/**
 * The AdServerWrapper implements a kind of delegate interface, that allows adserver-specific
 * implementations of the SDK to construct objects specific to that adserver (e.g. MopubAdRequest).
 *
 * @see SdkManager#adServerWrapper
 */
public interface AdServerWrapper {
  enum Type {
    INTERSTITIAL,
    BANNER,
    NATIVE
  }

  /**
   * Build an adServer-specific AdRequest from the more agnostic inner "AuctionRequest"
   * representation of this request for ads
   *
   * @param auctionRequest an AuctionRequest containing information about the request for ads
   * @return an AdServerAdRequest subclass specific to the configured AdServer
   */
  AdServerAdRequest newAdRequest(AuctionRequest auctionRequest);

  AdServerAdRequest newAdRequest(AuctionRequest auctionRequest, Type type);

  /**
   * Build an empty/new AdRequest without any attached AuctionRequest.
   *
   * @return an AdserverAdRequest subclass specific to the configured AdServer.
   */
  AdServerAdRequest newAdRequest();

  /**
   * Instantiate an AdSize subclass specific to this AdServer
   *
   * @param width  the width of the ad in device pixels (CSS pixels). E.g. "300"
   * @param height the height of the ad in device pixels (CSS pixels). E.g. "250"
   * @return an AdSize subclass specific to this AdServer
   */
  AdSize newAdSize(Integer width, Integer height);
}