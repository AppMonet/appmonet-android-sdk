package com.monet.bidder

import android.location.Location
import android.os.Bundle
import com.monet.bidder.Constants.BIDS_KEY
import com.monet.bidder.Constants.Dfp.ADUNIT_KEYWORD_KEY
import com.monet.bidder.MoPubRequestUtil.getKeywords
import com.monet.bidder.MoPubRequestUtil.mergeKeywords
import com.monet.bidder.auction.AuctionRequest
import com.monet.BidResponse
import com.monet.BidResponse.Mapper.from
import com.monet.BidResponse.Mapper.fromBidKey
import com.monet.BidResponse.Mapper.toJsonString
import com.mopub.mobileads.MoPubInterstitial
import org.json.JSONException
import org.json.JSONObject
import java.util.ArrayList
import java.util.Date

/**
 * Created by nbjacob on 6/26/17.
 */
internal class MoPubInterstitialAdRequest : AdServerAdRequest {
  private val mLocalExtras: MutableMap<String, Any>
  private val moPubInterstitial: MoPubInterstitial?
  private var mBid: BidResponse? = null

  constructor(moPubInterstitial: MoPubInterstitial) {
    mLocalExtras = moPubInterstitial.getLocalExtras().toMutableMap()
    this.moPubInterstitial = moPubInterstitial
    mLocalExtras[BIDS_KEY]?.let {
      try {
        mBid = from(it as String)
      } catch (e: JSONException) {
        //do nothing
      }
    }
  }

  constructor() {
    mLocalExtras = mutableMapOf()
    moPubInterstitial = null
  }

  override fun hasBid(): Boolean {
    return mBid != null
  }

  override val bid: BidResponse?
    get() = mBid

  override val location: Location?
    get() = moPubInterstitial?.getLocation()

  override val birthday: Date?
    get() = if (mLocalExtras.containsKey("birthday")) {
      mLocalExtras["birthday"] as Date
    } else null

  override val contentUrl: String?
    get() = if (mLocalExtras.containsKey("content_url")) {
      mLocalExtras["content_url"] as String
    } else null

  override val gender: String?
    get() = mLocalExtras["gender"] as String

  override fun apply(
    instance: AuctionRequest,
    adView: AdServerAdView
  ): AuctionRequest {
    instance.targeting.putAll(
        filterTargeting(customTargeting)
    )
    return instance
  }

  override val publisherProvidedId: String?
    get() = null

  override val customTargeting: Bundle
    // turn our map into targeting
    get() {
      val bundle = Bundle()
      for ((key, value1) in mLocalExtras) {
        val value = value1 ?: continue
        try {
          if (value is List<*>) {
            try {
              bundle.putStringArrayList(
                  key, value1 as ArrayList<String?>?
              )
            } catch (e: Exception) {
              sLogger.warn("failed to set custom targeting", e.message)
            }
            continue
          }
          if (value is Bundle) {
            bundle.putBundle(key, value)
            continue
          }
          bundle.putString(key, value.toString())
        } catch (e: Exception) {
          // do nothing
        }
      }
      return bundle
    }

  fun applyToView(adView: MoPubInterstitialAdView) {
    // apply the targeting to the view, as keywords
    val view = adView.moPubView
    mLocalExtras[BIDS_KEY] = toJsonString(mBid)
    mLocalExtras[ADUNIT_KEYWORD_KEY] = adView.adUnitId
    view.setLocalExtras(mLocalExtras)
    var keywords = getKeywords(mLocalExtras)
    if (view.getKeywords() != null) {
      keywords = mergeKeywords(view.getKeywords()!!, keywords)
    }
    view.setKeywords(keywords)
  }

  companion object {
    const val CE_AD_WIDTH = "com_mopub_ad_width"
    const val CE_AD_HEIGHT = "com_mopub_ad_height"
    const val CE_AD_FORMAT = "__ad_format"
    fun fromAuctionRequest(request: AuctionRequest): MoPubInterstitialAdRequest {
      val adRequest = MoPubInterstitialAdRequest()
      for (key in request.targeting.keySet()) {
        request.targeting[key]?.let {
          adRequest.mLocalExtras[key] = it
        }
      }
      if (request.bid != null) {
        adRequest.mBid = request.bid
      }
      return adRequest
    }
  }
}