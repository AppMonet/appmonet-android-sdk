package com.monet.bidder

import android.location.Location
import android.os.Bundle
import com.monet.bidder.Constants.BIDS_KEY
import com.monet.bidder.Constants.Dfp.ADUNIT_KEYWORD_KEY
import com.monet.bidder.MoPubRequestUtil.getKeywords
import com.monet.bidder.MoPubRequestUtil.mergeKeywords
import com.monet.bidder.auction.AuctionRequest
import com.monet.bidder.bid.BidResponse
import com.monet.bidder.bid.BidResponse.Mapper.from
import com.monet.bidder.bid.BidResponse.Mapper.toJson
import com.mopub.mobileads.MoPubView
import org.json.JSONException
import org.json.JSONObject
import java.util.ArrayList
import java.util.Date
import java.util.HashMap

/**
 * Created by nbjacob on 6/26/17.
 */
internal class MoPubAdRequest : AdServerAdRequest {
  private val mLocalExtras: MutableMap<String, Any>
  private val mAdView: MoPubView?
  private var mBid: BidResponse? = null

  constructor(adView: MoPubView) {
    mLocalExtras = adView.getLocalExtras().toMutableMap()
    mAdView = adView

    // set this data so we can read it later
    // on the local extras we get
    mLocalExtras[CE_AD_FORMAT] = adView.getAdFormat()
    // extract the targeting from the adview
    if (mLocalExtras.containsKey(BIDS_KEY)) {
      try {
        mBid = from(JSONObject(mLocalExtras[BIDS_KEY] as String? ?: ""))
      } catch (e: JSONException) {
        //do nothing
      }
    }
  }

  constructor() {
    mLocalExtras = HashMap()
    mAdView = null
  }

  override fun hasBid(): Boolean {
    return mBid != null
  }

  override val bid: BidResponse?
    get() = mBid

  override val location: Location?
    get() = mAdView?.getLocation()

  override val birthday: Date?
    get() {
      return if (mLocalExtras.containsKey("birthday")) {
        mLocalExtras["birthday"] as Date
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
    instance.targeting.putAll(
        filterTargeting(customTargeting)
    )
    return instance
  }

  override val publisherProvidedId: String?
    get() = null

  override val customTargeting: Bundle
    get() {
      // turn our map into targeting
      val bundle = Bundle()
      for ((key, extra) in mLocalExtras) {
        val value = extra ?: continue
        try {
          if (value is List<*>) {
            try {
              bundle.putStringArrayList(
                  key, extra as ArrayList<String?>?
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

  fun applyToView(adView: MoPubAdView?) {
    // apply the targeting to the view, as keywords
    if (adView == null) {
      return
    }
    val view = adView.getMoPubView()
    view?.let { v ->
      if (mBid != null) {
        mLocalExtras[BIDS_KEY] = toJson(mBid).toString()
      }
      mLocalExtras[ADUNIT_KEYWORD_KEY] = v.getAdUnitId() ?: ""
      v.setLocalExtras(mLocalExtras)
      var keywords = getKeywords(mLocalExtras)
      keywords = mergeKeywords(v.getKeywords(), keywords)
      v.setKeywords(keywords)
      v.setLocation(location)
    }
  }

  companion object {
    const val CE_AD_FORMAT = "__ad_format"
    fun fromAuctionRequest(request: AuctionRequest): AdServerAdRequest {
      val adRequest = MoPubAdRequest()
      for (key in request.targeting.keySet()) {
        request.targeting[key]?.let {
          adRequest.mLocalExtras[key] = it
        }
      }
      if (request.bid != null) {
        adRequest.mBid = request.bid // just pass the bids along
      }
      return adRequest
    }
  }
}