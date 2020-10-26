package com.monet.bidder

import android.location.Location
import android.os.Bundle
import com.monet.AdServerAdRequest
import com.monet.AdServerAdView
import com.monet.BidResponse
import com.monet.BidResponse.Mapper.from
import com.monet.BidResponse.Mapper.toJsonString
import com.monet.LocationData
import com.monet.auction.AuctionRequest
import com.monet.bidder.Constants.BIDS_KEY
import com.monet.bidder.Constants.Dfp.ADUNIT_KEYWORD_KEY
import com.monet.bidder.MoPubRequestUtil.getKeywords
import com.monet.bidder.MoPubRequestUtil.mergeKeywords
import com.mopub.mobileads.MoPubView
import org.json.JSONException
import java.util.HashMap

/**
 * Created by nbjacob on 6/26/17.
 */
internal class MoPubAdRequest : AdServerAdRequest {
  private val mLocalExtras: MutableMap<String, Any>
  private val mAdView: MoPubView?

  constructor(adView: MoPubView) {
    mLocalExtras = adView.getLocalExtras().toMutableMap()
    mAdView = adView

    // set this data so we can read it later
    // on the local extras we get
    mLocalExtras[CE_AD_FORMAT] = adView.getAdFormat()
    // extract the targeting from the adview
    try {
      bid = mLocalExtras[BIDS_KEY]?.let { from(it as String) }
    } catch (e: JSONException) {
      //do nothing
    }
  }

  constructor() {
    mLocalExtras = HashMap()
    mAdView = null
  }

  override val location: LocationData?
    get() = mAdView?.getLocation()?.let {
      LocationData(it.longitude, it.longitude, it.accuracy.toDouble(), it.provider)
    }

  override var birthday: Long? = null
    get() {
      return if (mLocalExtras.containsKey("birthday")) {
        mLocalExtras["birthday"] as Long
      } else null
    }

  override val contentUrl: String?
    get() {
      return if (mLocalExtras.containsKey("content_url")) {
        mLocalExtras["content_url"] as String
      } else null
    }

  override val gender: String?
    get() = mLocalExtras["gender"] as String?

  override fun apply(
    instance: AuctionRequest,
    adView: AdServerAdView
  ): AuctionRequest {
    val targeting = instance.targeting.toMutableMap()
    targeting.putAll(
        filterTargeting(customTargeting)
    )
    instance.targeting = targeting
    return instance
  }

  override val publisherProvidedId: String?
    get() = null

  override val customTargeting: Map<String, Any>
    get() {
      // turn our map into targeting
      val bundle = mutableMapOf<String, Any>()
      for ((key, extra) in mLocalExtras) {
        val value = extra ?: continue
        try {
          if (value is List<*>) {
            try {
              bundle[key] = extra
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

  fun applyToView(adView: MoPubAdView?) {
    // apply the targeting to the view, as keywords
    if (adView == null) {
      return
    }
    val view = adView.getMoPubView()
    view?.let { v ->
      if (bid != null) {
        mLocalExtras[BIDS_KEY] = toJsonString(bid)
      }
      mLocalExtras[ADUNIT_KEYWORD_KEY] = v.getAdUnitId() ?: ""
      v.setLocalExtras(mLocalExtras)
      var keywords = getKeywords(mLocalExtras)
      keywords = mergeKeywords(v.getKeywords(), keywords)
      v.setKeywords(keywords)
      location?.let {
        val targetLocation = Location(it.provider).apply {
          latitude = it.lat
          longitude = it.lon
        }
        v.setLocation(targetLocation)
      }
    }
  }

  companion object {
    const val CE_AD_FORMAT = "__ad_format"
    fun fromAuctionRequest(request: AuctionRequest): AdServerAdRequest {
      val adRequest = MoPubAdRequest()
      for (key in request.targeting.keys) {
        request.targeting[key]?.let {
          adRequest.mLocalExtras[key] = it
        }
      }
      if (request.bid != null) {
        adRequest.bid = request.bid // just pass the bids along
      }
      return adRequest
    }
  }
}