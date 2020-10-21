package com.monet.bidder

import android.location.Location
import android.os.Bundle
import com.monet.bidder.auction.AuctionRequest
import com.monet.BidResponse
import java.util.Date

abstract class AdServerAdRequest {
  abstract val customTargeting: Bundle
  abstract val birthday: Date?
  abstract val gender: String?
  abstract val bid: BidResponse?

  open fun hasBid(): Boolean {
    return false
  }

  abstract val location: Location?
  abstract val contentUrl: String?

  abstract fun apply(
    request: AuctionRequest,
    adView: AdServerAdView
  ): AuctionRequest

  abstract val publisherProvidedId: String?

  private fun shouldRemoveKey(
    dynamicKeyPrefix: String?,
    key: String?
  ): Boolean {
    return if (key == null) {
      false
    } else key.startsWith(
        Constants.KW_KEY_PREFIX
    ) || dynamicKeyPrefix != null && key.startsWith(dynamicKeyPrefix)
  }

  fun filterTargeting(targeting: Bundle?): Bundle {
    if (targeting == null) {
      return Bundle()
    }
    val filteredBundle = Bundle()
    filteredBundle.putAll(targeting)
    val dynamicKeyPrefix = targeting.getString(Constants.CUSTOM_KW_PREFIX_KEY)
    for (key in targeting.keySet()) {
      if (shouldRemoveKey(dynamicKeyPrefix, key)) {
        filteredBundle.remove(key)
      }
    }
    return filteredBundle
  }

  companion object {
    val sLogger = Logger("AdRequest")
  }
}