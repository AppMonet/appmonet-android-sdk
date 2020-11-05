package com.monet

import co.touchlab.stately.freeze
import com.monet.auction.AuctionRequest

abstract class AdServerAdRequest {
  open val customTargeting: Map<String, Any> = mapOf()
  open val birthday: Long? = null
  open val gender: String? = null
  var bid: BidResponse? = null

  fun hasBid(): Boolean {
    return bid != null
  }

  abstract val location: LocationData?
  abstract val contentUrl: String?

  abstract fun apply(
    request: AuctionRequest,
    adView: AdServerAdView
  ): AuctionRequest

  abstract val publisherProvidedId: String?

  private fun shouldRemoveKey(
    dynamicKeyPrefix: String?,
    key: Any?
  ): Boolean {
    return if (key == null || key !is String) {
      false
    } else key.startsWith(
        Constants.KW_KEY_PREFIX
    ) || dynamicKeyPrefix != null && key.startsWith(dynamicKeyPrefix)
  }

  fun freezeRequest() {
    this.freeze()
  }

  fun filterTargeting(targeting: Map<String, Any>?): Map<String, Any> {
    if (targeting == null) {
      return mapOf()
    }
    val filteredMap = mutableMapOf<String, Any>()
    filteredMap.putAll(targeting)
    val dynamicKeyPrefix = targeting[Constants.CUSTOM_KW_PREFIX_KEY] as String?
    for (key in targeting.keys) {
      if (shouldRemoveKey(dynamicKeyPrefix, key)) {
        filteredMap.remove(key)
      }
    }
    return filteredMap
  }

  companion object {
//    val sLogger = Logger("AdRequest")
  }
}