package com.monet

import com.monet.BidResponse.Mapper.from
import com.monet.BidResponse.Mapper.toJsonString
import com.monet.Constants.ADUNIT_KEYWORD_KEY
import com.monet.Constants.BIDS_KEY
import com.monet.auction.AuctionRequest
import com.monet.MoPubRequestUtil.getKeywords
import com.monet.MoPubRequestUtil.mergeKeywords

class MopubNativeAdRequest : AdServerAdRequest {
  private val mLocalExtras: MutableMap<String, Any>
  private val adUnitId: String
  private val requestParameters: MoPubNativeRequestParametersWrapper?
  var modifiedRequestParameters: MoPubNativeRequestParametersWrapper? = null

  constructor(
    adUnitId: String,
    requestParameters: MoPubNativeRequestParametersWrapper?
  ) {
    this.requestParameters = requestParameters
    mLocalExtras = mutableMapOf()
    this.adUnitId = adUnitId
    mLocalExtras[BIDS_KEY]?.let {
      bid = from(it as String)
    }
  }

  constructor(adUnitId: String) {
    requestParameters = MoPubNativeRequestParametersWrapper.Builder().build()
    mLocalExtras = mutableMapOf()
    this.adUnitId = adUnitId
  }

  override val location: LocationData?
    get() = requestParameters?.location

  override val birthday: Long?
    get() = null

  override val contentUrl: String?
    get() = null

  override val gender: String?
    get() = null

  override fun apply(
    request: AuctionRequest,
    adView: AdServerAdView
  ): AuctionRequest {
    return request
  }

  override val publisherProvidedId: String?
    get() = null

  override val customTargeting: Map<String, Any>
    get() = mapOf()

  fun applyToView(mopubNativeAdView: MopubNativeAdView) {
    // apply the targeting to the view, as keywords
    val nativeAd = mopubNativeAdView.viewWrapper
    bid?.let {
      mLocalExtras[BIDS_KEY] = toJsonString(it)
      mLocalExtras[ADUNIT_KEYWORD_KEY] = adUnitId
      nativeAd.getLocalExtras()
      nativeAd.setLocalExtras(mLocalExtras)
      var keywords = getKeywords(mLocalExtras)
      keywords = mergeKeywords(requestParameters?.keywords, keywords)
      modifiedRequestParameters = setKeywords(keywords)
    }
  }

  private fun setKeywords(keywords: String): MoPubNativeRequestParametersWrapper {
    return if (requestParameters != null) {
      MoPubNativeRequestParametersWrapper.Builder()
          .keywords(keywords)
          .location(requestParameters.location)
          .userDataKeywords(requestParameters.userDataKeywords).build()
    } else {
      MoPubNativeRequestParametersWrapper.Builder()
          .keywords(keywords)
          .build()
    }
  }

  companion object {
    fun fromAuctionRequest(request: AuctionRequest): MopubNativeAdRequest {
      val adRequest = MopubNativeAdRequest(request.adUnitId)
      for (key in request.targeting.keys) {
        val targeting = request.targeting[key]
        if (targeting != null) {
          adRequest.mLocalExtras[key] = targeting
        }
      }
      if (request.bid != null) {
        adRequest.bid = request.bid
      }
      return adRequest
    }
  }
}