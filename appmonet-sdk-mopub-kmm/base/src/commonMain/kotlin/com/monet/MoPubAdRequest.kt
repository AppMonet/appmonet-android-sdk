package com.monet

import com.monet.BidResponse.Mapper.from
import com.monet.BidResponse.Mapper.toJsonString
import com.monet.Constants.ADUNIT_KEYWORD_KEY
import com.monet.Constants.BIDS_KEY
import com.monet.MoPubRequestUtil.getKeywords
import com.monet.MoPubRequestUtil.mergeKeywords
import com.monet.auction.AuctionRequest

/**
 * Created by nbjacob on 6/26/17.
 */
class MoPubAdRequest : AdServerAdRequest {
  private val mLocalExtras: MutableMap<String, Any>
  private val mAdView: ViewWrapper<*>?

  constructor(adView: ViewWrapper<*>) {
    mLocalExtras = adView.getLocalExtras()?.toMutableMap() ?: mutableMapOf()
    mAdView = adView

    // set this data so we can read it later
    // on the local extras we get
    mLocalExtras[CE_AD_FORMAT] = adView.getAdFormat()
    // extract the targeting from the adview
    bid = mLocalExtras[BIDS_KEY]?.let { from(it as String) }
  }

  constructor() {
    mLocalExtras = mutableMapOf()
    mAdView = null
  }

  override val location: LocationData?
    get() = mAdView?.getLocation()

  override var birthday: Long? = null
    get() {
      return mLocalExtras["birthday"] as Long?
    }

  override val contentUrl: String?
    get() {
      return mLocalExtras["content_url"] as String?
    }

  override val gender: String?
    get() = mLocalExtras["gender"] as String?

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
          if (Util.isAdRequestExtra(bundle, key, value))
            continue
          bundle[key] = value.toString()
        } catch (e: Exception) {
          // do nothing
        }
      }
      return bundle
    }

  fun applyToView(adView: MoPubBannerAdView?) {
    // apply the targeting to the view, as keywords
    adView?.viewWrapper?.let { view ->
      bid?.let {
        mLocalExtras[BIDS_KEY] = toJsonString(it)
        mLocalExtras[ADUNIT_KEYWORD_KEY] = view.adUnit
        var keywords = getKeywords(mLocalExtras)
        keywords = mergeKeywords(view.getKeywords(), keywords)
        view.setKeywords(keywords)
        view.setLocation(location)
        view.getLocalExtras()?.toMutableMap()?.let { viewLocalExtras ->
          viewLocalExtras.putAll(mLocalExtras)
          view.setLocalExtras(viewLocalExtras)
        } ?: view.setLocalExtras(mLocalExtras)
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