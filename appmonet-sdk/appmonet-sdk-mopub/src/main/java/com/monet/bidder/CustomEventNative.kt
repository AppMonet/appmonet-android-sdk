package com.monet.bidder

import android.content.Context
import com.monet.AdType.NATIVE
import com.monet.bidder.Constants.BIDS_KEY
import com.monet.bidder.CustomEventUtil.getAdUnitId
import com.monet.bidder.CustomEventUtil.getServerExtraCpm
import com.monet.bidder.MediationManager.NoBidsFoundException
import com.monet.bidder.MediationManager.NullBidException
import com.monet.bidder.bid.BidRenderer
import com.monet.BidResponse
import com.monet.BidResponse.Mapper.from
import com.monet.adview.AdSize
import com.mopub.nativeads.CustomEventNative
import com.mopub.nativeads.NativeErrorCode.NETWORK_NO_FILL
import com.mopub.nativeads.NativeErrorCode.UNSPECIFIED
import org.json.JSONException
import org.json.JSONObject

/**
 * Class called by Mopub which triggers our logic for serving ads.
 */
open class CustomEventNative : CustomEventNative() {
  override fun loadNativeAd(
    context: Context,
    customEventNativeListener: CustomEventNativeListener,
    localExtras: Map<String, Any>,
    serverExtras: MutableMap<String, String>
  ) {
    logger.debug("Loading Native Ad")
    val adSize = AdSize(context.applicationContext, 320, 250)
    val adUnitId = getAdUnitId(serverExtras, localExtras, adSize)
    val sdkManager = SdkManager.get()
    if (sdkManager == null) {
      customEventNativeListener.onNativeAdFailed(UNSPECIFIED)
      return
    }
    if (adUnitId == null || adUnitId.isEmpty()) {
      customEventNativeListener.onNativeAdFailed(NETWORK_NO_FILL)
      return
    }
    sdkManager.auctionManager.trackRequest(
        adUnitId,
        WebViewUtils.generateTrackingSource(NATIVE)
    )
    var headerBiddingBid: BidResponse? = null
    try {
      localExtras[BIDS_KEY]?.let {
        headerBiddingBid = from(it as String)
      }
    } catch (e: JSONException) {
      // Exception
    }
    val serverExtraCpm = getServerExtraCpm(serverExtras, 0.0)
    if (headerBiddingBid == null) {
      headerBiddingBid = sdkManager.auctionManager.mediationManager
          .getBidForMediation(adUnitId, serverExtraCpm)
    }
    val mediationManager = MediationManager(sdkManager, sdkManager.auctionManager.bidManager)
    try {
      val bid = mediationManager.getBidReadyForMediation(
          headerBiddingBid, adUnitId, adSize,
          NATIVE, serverExtraCpm, true
      )
      if (bid.extras.isNotEmpty()) {
        for ((key, value1) in bid.extras) {
          val value = value1 ?: continue

          // add all the data into server extras
          serverExtras[key] = value.toString()
        }
      }
      val mListener: AdServerBannerListener =
        MoPubNativeListener(context, customEventNativeListener, serverExtras)
      val mAdView = BidRenderer.renderBid(context, sdkManager, bid, null, mListener)
      if (mAdView == null) {
        customEventNativeListener.onNativeAdFailed(UNSPECIFIED)
      }
    } catch (e: NoBidsFoundException) {
      customEventNativeListener.onNativeAdFailed(NETWORK_NO_FILL)
    } catch (e: NullBidException) {
      customEventNativeListener.onNativeAdFailed(UNSPECIFIED)
    }
  }

  companion object {
    private val logger = Logger("CustomEventNative")
  }
}