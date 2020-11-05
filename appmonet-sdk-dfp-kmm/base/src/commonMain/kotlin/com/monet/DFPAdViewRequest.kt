package com.monet

import com.monet.auction.AuctionRequest

class DFPAdViewRequest : AdServerAdRequest {
  val adRequest: RequestWrapper<*>


  constructor(adRequest: RequestWrapper<*>) {
    this.adRequest = adRequest
  }

  private val adMobExtras: Map<String, Any>?
    get() = adRequest.networkExtrasBundle

  override val customTargeting: Map<String, Any>
    get() {
      return adRequest.customTargeting
    }

  override val birthday: Long?
    get() = adRequest.birthday

  override val gender: String?
    get() = adRequest.gender

  override val location: LocationData?
    get() = adRequest.location

  override val contentUrl: String?
    get() = adRequest.contentUrl

  override fun apply(
    request: AuctionRequest,
    adView: AdServerAdView
  ): AuctionRequest {
    return DFPAdViewRequestUtil.apply(adRequest, request, adView, this)
  }

  override val publisherProvidedId: String?
    get() = null

  companion object{}

}