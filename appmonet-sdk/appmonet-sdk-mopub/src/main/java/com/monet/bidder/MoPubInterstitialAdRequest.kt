package com.monet.bidder

import android.location.Location
import android.os.Bundle
import com.monet.BidResponse
import com.monet.BidResponse.Mapper.from
import com.monet.BidResponse.Mapper.toJsonString
import com.monet.bidder.Constants.BIDS_KEY
import com.monet.bidder.Constants.Dfp.ADUNIT_KEYWORD_KEY
import com.monet.bidder.MoPubRequestUtil.getKeywords
import com.monet.bidder.MoPubRequestUtil.mergeKeywords
import com.monet.auction.AuctionRequest
import com.mopub.mobileads.MoPubInterstitial
import org.json.JSONException
import java.util.ArrayList
import java.util.Date
import com.monet.AdServerAdRequest
import com.monet.AdServerAdView
import com.monet.LocationData

/**
 * Created by nbjacob on 6/26/17.
 */
internal class MoPubInterstitialAdRequest : AdServerAdRequest {
  private val mLocalExtras: MutableMap<String, Any>
  private val moPubInterstitial: MoPubInterstitial?

  constructor(moPubInterstitial: MoPubInterstitial) {
    mLocalExtras = moPubInterstitial.getLocalExtras().toMutableMap()
    this.moPubInterstitial = moPubInterstitial
    mLocalExtras[BIDS_KEY]?.let {
      try {
        bid = from(it as String)
      } catch (e: JSONException) {
        //do nothing
      }
    }
  }

  constructor() {
    mLocalExtras = mutableMapOf()
    moPubInterstitial = null
  }

  override val location: LocationData?
    get() {
      return moPubInterstitial?.getLocation()?.let {
        LocationData(it.latitude, it.longitude, it.accuracy.toDouble(), it.provider)
      }
    }

  override val birthday: Long?
    get() = if (mLocalExtras.containsKey("birthday")) {
      mLocalExtras["birthday"] as Long
    } else null

  override val contentUrl: String?
    get() = if (mLocalExtras.containsKey("content_url")) {
      mLocalExtras["content_url"] as String
    } else null

  override val gender: String?
    get() = mLocalExtras["gender"] as String

  override fun apply(
    request: AuctionRequest,
    adView: AdServerAdView
  ): AuctionRequest {
    val targeting = request.targeting.toMutableMap()
    targeting.putAll(
        filterTargeting(customTargeting)
    )
    request.targeting = targeting
    return request
  }

  override val publisherProvidedId: String?
    get() = null

  override val customTargeting: Map<String, Any>
    // turn our map into targeting
    get() {
      val bundle = mutableMapOf<String, Any>()
      for ((key, value1) in mLocalExtras) {
        val value = value1 ?: continue
        try {
          if (value is List<*>) {
            try {
              bundle[key] = value1
            } catch (e: Exception) {
//              sLogger.warn("failed to set custom targeting", e.message)
            }
            continue
          }
          if (value is Bundle) {
            bundle[key] = value
            continue
          }
          bundle[key] = value.toString()
        } catch (e: Exception) {
          // do nothing
        }
      }
      return bundle
    }

  fun applyToView(adView: MoPubInterstitialAdView) {
    // apply the targeting to the view, as keywords
    val view = adView.moPubView
    mLocalExtras[BIDS_KEY] = toJsonString(bid)
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
      for (key in request.targeting.keys) {
        request.targeting[key]?.let {
          adRequest.mLocalExtras[key] = it
        }
      }
      request.bid?.let {
        adRequest.bid = request.bid
      }
      return adRequest
    }
  }
}