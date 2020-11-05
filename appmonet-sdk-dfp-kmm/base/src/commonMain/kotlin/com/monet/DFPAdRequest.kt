package com.monet

import com.monet.auction.AuctionRequest

class DFPAdRequest : AdServerAdRequest {
  val dfpRequest: RequestWrapper<*>

  /**
   * Build a DFPAdRequest from a PublisherAdRequest, the DFP representation of an ad request
   *
   * @param adRequest the request constructed by the publisher, to be sent to DFP
   */
  constructor(adRequest: RequestWrapper<*>) {
    dfpRequest = adRequest
  }

  // also get the admob extras and merge it here
  override val customTargeting: Map<String, Any>
    // create a bundle merging both
    get() = dfpRequest.customTargeting

  override val birthday: Long?
    get() = dfpRequest.birthday

  override val gender: String
    get() = dfpRequest.gender

  override val location: LocationData?
    get() = dfpRequest.location

  override val contentUrl: String?
    get() = dfpRequest.contentUrl

  override fun apply(
    request: AuctionRequest,
    adView: AdServerAdView
  ): AuctionRequest {
    return DFPAdRequestUtil.apply(dfpRequest, request, adView, this)
  }

  override val publisherProvidedId: String?
    get() = dfpRequest.publisherProvidedId

  companion object{}

}