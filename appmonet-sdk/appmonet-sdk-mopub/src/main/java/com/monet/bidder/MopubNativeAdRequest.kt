package com.monet.bidder

import com.monet.AdServerAdRequest
import com.monet.AdServerAdView
import com.monet.BidResponse.Mapper.from
import com.monet.BidResponse.Mapper.toJsonString
import com.monet.LocationData
import com.monet.auction.AuctionRequest
import com.monet.bidder.Constants.BIDS_KEY
import com.monet.bidder.Constants.Dfp.ADUNIT_KEYWORD_KEY
import com.monet.bidder.MoPubRequestUtil.getKeywords
import com.monet.bidder.MoPubRequestUtil.mergeKeywords
import com.mopub.nativeads.MoPubNative
import com.mopub.nativeads.RequestParameters
import com.mopub.nativeads.RequestParameters.Builder
import org.json.JSONException
import java.util.HashMap

internal class MopubNativeAdRequest : AdServerAdRequest {
  private val mLocalExtras: MutableMap<String, Any>
  private val adUnitId: String
  private var moPubNative: MoPubNative? = null
  private val requestParameters: RequestParameters?
  var modifiedRequestParameters: RequestParameters? = null

  constructor(
    moPubNative: MoPubNative?,
    adUnitId: String,
    requestParameters: RequestParameters?
  ) {
    this.requestParameters = requestParameters
    mLocalExtras = mutableMapOf()
    this.moPubNative = moPubNative
    this.adUnitId = adUnitId
    mLocalExtras[BIDS_KEY]?.let {
      try {
        bid = from(it as String)
      } catch (e: JSONException) {
        //do nothing
      }
    }
  }

  constructor(adUnitId: String) {
    requestParameters = Builder().build()
    mLocalExtras = HashMap()
    this.adUnitId = adUnitId
  }

  override val location: LocationData?
    get() = requestParameters?.location?.let {
      LocationData(
          it.latitude, it.longitude, it.accuracy.toDouble(), it.provider
      )
    }

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
    val nativeAd = mopubNativeAdView.mopubNative
    mLocalExtras[BIDS_KEY] = toJsonString(bid)
    mLocalExtras[ADUNIT_KEYWORD_KEY] = adUnitId
    nativeAd.setLocalExtras(mLocalExtras)
    var keywords = getKeywords(mLocalExtras)
    keywords = mergeKeywords(requestParameters?.keywords, keywords)
    modifiedRequestParameters = setKeywords(keywords)
  }

  private fun setKeywords(keywords: String): RequestParameters {
    return if (requestParameters != null) {
      Builder()
          .keywords(keywords)
          .location(requestParameters.location)
          .userDataKeywords(requestParameters.userDataKeywords).build()
    } else {
      Builder()
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