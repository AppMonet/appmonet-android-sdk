package com.monet

import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.android.gms.ads.mediation.MediationExtrasReceiver
import com.google.android.gms.ads.mediation.customevent.CustomEvent
import com.monet.auction.AuctionRequest

fun Map<String, Any?>.toBundle(): Bundle = bundleOf(*this.toList().toTypedArray())
fun Bundle.toMap(): Map<String, Any?> {
  val map = mutableMapOf<String, Any?>()
  for (key: String in this.keySet()) {
    map[key] = this.get(key)
  }
  return map
}

fun DFPAdRequest.Companion.fromAuctionRequest(
  request: AuctionRequest,
  monetCustomEventInterstitialClazz: Class<out CustomEvent>,
  customEventBannerClazz: Class<out CustomEvent>,
  customEventBannerReceiverClazz: Class<out MediationExtrasReceiver>,
  customEventInterstitialClazz: Class<out CustomEvent>
): DFPAdRequest {
  return DFPAdRequestUtil.fromAuctionRequest(
      request, monetCustomEventInterstitialClazz, customEventBannerClazz,
      customEventBannerReceiverClazz, customEventInterstitialClazz
  )
}

fun DFPAdViewRequest.Companion.fromAuctionRequest(
  request: AuctionRequest,
  monetCustomEventInterstitialClazz: Class<out CustomEvent>,
  customEventBannerClazz: Class<out CustomEvent>,
  customEventBannerReceiverClazz: Class<out MediationExtrasReceiver>,
  customEventInterstitialClazz: Class<out CustomEvent>
): DFPAdViewRequest {
  return DFPAdViewRequestUtil.fromAuctionRequest(
      request, monetCustomEventInterstitialClazz, customEventBannerClazz,
      customEventBannerReceiverClazz, customEventInterstitialClazz
  )
}