package com.monet.bidder

import android.app.Activity
import android.view.View
import com.monet.bidder.FloatingAdView.Params
import com.monet.bidder.bid.BidResponse

internal class AdViewLoadedFactory {
  fun getAdView(
    activity: Activity?,
    manager: SdkManager,
    originalView: View,
    bid: BidResponse,
    adUnit: String
  ): View {
    val moPubView = manager.getMopubAdView(adUnit)
    if (activity != null && manager.getFloatingAdPosition(
            adUnit
        ) != null && moPubView != null
    ) {
      val params = Params(
          manager, originalView, bid,
          moPubView.getAdWidth(), moPubView.getAdHeight(), adUnit
      )
      return FloatingAdView(
          activity, manager, params, originalView.context
      )
    } else if (activity != null && manager.getFloatingAdPosition(
            adUnit
        ) != null
    ) {
      val params = Params(
          manager, originalView, bid,
          null, null, adUnit
      )
      return FloatingAdView(activity, manager, params, originalView.context)
    }
    return originalView
  }
}