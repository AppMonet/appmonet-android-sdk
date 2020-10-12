package com.monet.bidder

import com.monet.bidder.auction.AuctionRequest

/**
 * The AdServerWrapper implements a kind of delegate interface, that allows adserver-specific
 * implementations of the SDK to construct objects specific to that adserver (e.g. MoPubAdRequest).
 *
 */
interface AdServerWrapper {
  enum class Type {
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
  fun newAdRequest(auctionRequest: AuctionRequest): AdServerAdRequest

  fun newAdRequest(
    auctionRequest: AuctionRequest,
    type: Type
  ): AdServerAdRequest

  /**
   * Build an empty/new AdRequest without any attached AuctionRequest.
   *
   * @return an AdserverAdRequest subclass specific to the configured AdServer.
   */
  fun newAdRequest(): AdServerAdRequest

  /**
   * Instantiate an AdSize subclass specific to this AdServer
   *
   * @param width  the width of the ad in device pixels (CSS pixels). E.g. "300"
   * @param height the height of the ad in device pixels (CSS pixels). E.g. "250"
   * @return an AdSize subclass specific to this AdServer
   */
  fun newAdSize(
    width: Int,
    height: Int
  ): AdSize
}