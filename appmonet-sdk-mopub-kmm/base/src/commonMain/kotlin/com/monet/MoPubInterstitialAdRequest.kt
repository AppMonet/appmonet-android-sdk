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
class MoPubInterstitialAdRequest : AdServerAdRequest {
  private val mLocalExtras: MutableMap<String, Any>
  private val moPubInterstitial: ViewWrapper<*>?

  constructor(moPubInterstitial: ViewWrapper<*>) {
    mLocalExtras = moPubInterstitial.getLocalExtras()?.toMutableMap() ?: mutableMapOf()
    this.moPubInterstitial = moPubInterstitial
    mLocalExtras[BIDS_KEY]?.let {
      bid = from(it as String)
    }
  }

  constructor() {
    mLocalExtras = mutableMapOf()
    moPubInterstitial = null
  }

  override val location: LocationData?
    get() = moPubInterstitial?.getLocation()

  override val birthday: Long?
    get() = mLocalExtras["birthday"] as Long?

  override val contentUrl: String?
    get() = mLocalExtras["content_url"] as String?

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
          if (Util.isAdRequestExtra(bundle, key, value)) {
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
    val view = adView.viewWrapper
    bid?.let {
      mLocalExtras[BIDS_KEY] = toJsonString(it)
      mLocalExtras[ADUNIT_KEYWORD_KEY] = adView.adUnitId
      var keywords = getKeywords(mLocalExtras)
      if (view.getKeywords() != null) {
        keywords = mergeKeywords(view.getKeywords()!!, keywords)
      }
      view.setKeywords(keywords)
      view.setLocation(location)
      view.getLocalExtras()?.toMutableMap()?.let { viewLocalExtras ->
        viewLocalExtras.putAll(mLocalExtras)
        view.setLocalExtras(viewLocalExtras)
      } ?: view.setLocalExtras(mLocalExtras)
    }
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