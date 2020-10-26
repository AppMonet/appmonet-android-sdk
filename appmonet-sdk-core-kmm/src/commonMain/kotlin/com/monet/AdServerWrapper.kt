package com.monet

import com.monet.adview.AdSize
import com.monet.auction.AuctionRequest

/**
 * The AdServerWrapper implements a kind of delegate interface, that allows adserver-specific
 * implementations of the SDK to construct objects specific to that adserver (e.g. MoPubAdRequest).
 *
 */
interface AdServerWrapper {

  /**
   * Build an adServer-specific AdRequest from the more agnostic inner "AuctionRequest"
   * representation of this request for ads
   *
   * @param auctionRequest an AuctionRequest containing information about the request for ads
   * @return an AdServerAdRequest subclass specific to the configured AdServer
   */
  fun newAdRequest(auctionRequest: AuctionRequest): AdServerAdRequest {
    return AdServerAdRequestTemplate()
  }

  fun newAdRequest(
    auctionRequest: AuctionRequest,
    type: AdType
  ): AdServerAdRequest {
    return AdServerAdRequestTemplate()
  }

  /**
   * Build an empty/new AdRequest without any attached AuctionRequest.
   *
   * @return an AdserverAdRequest subclass specific to the configured AdServer.
   */
  fun newAdRequest(): AdServerAdRequest {
    return AdServerAdRequestTemplate()
  }

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

private class AdServerAdRequestTemplate : AdServerAdRequest() {
  override val customTargeting: Map<String, Any>
    get() = mapOf()
  override val birthday: Long?
    get() = null
  override val gender: String?
    get() = null
  override val location: LocationData?
    get() = null
  override val contentUrl: String?
    get() = null

  override fun apply(
    request: AuctionRequest,
    adView: AdServerAdView
  ): AuctionRequest {
    return request
  }

  override val publisherProvidedId: String?
    get() = null
}