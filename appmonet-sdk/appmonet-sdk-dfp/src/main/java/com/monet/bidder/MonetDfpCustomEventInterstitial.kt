package com.monet.bidder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.mediation.MediationAdRequest
import com.google.android.gms.ads.mediation.customevent.CustomEventInterstitial
import com.google.android.gms.ads.mediation.customevent.CustomEventInterstitialListener
import com.monet.AdServerBannerListener
import com.monet.AdType.INTERSTITIAL
import com.monet.bidder.Constants.APPMONET_BROADCAST
import com.monet.bidder.Constants.APPMONET_BROADCAST_MESSAGE
import com.monet.bidder.Constants.INTERSTITIAL_ACTIVITY_BROADCAST
import com.monet.bidder.Constants.INTERSTITIAL_ACTIVITY_CLOSE
import com.monet.bidder.Constants.INTERSTITIAL_HEIGHT
import com.monet.bidder.Constants.INTERSTITIAL_WIDTH
import com.monet.bidder.Constants.Interstitial.AD_CONTENT_INTERSTITIAL
import com.monet.bidder.Constants.Interstitial.AD_UUID_INTERSTITIAL
import com.monet.bidder.Constants.Interstitial.BID_ID_INTERSTITIAL
import com.monet.bidder.bid.BidRenderer
import com.monet.BidResponse
import com.monet.BidResponse.Mapper.from
import com.monet.MediationCallback
import com.monet.adview.AdSize
import com.monet.bidder.callbacks.Callback
import com.monet.toMap

open class MonetDfpCustomEventInterstitial : CustomEventInterstitial {
  private var customEventInterstitialListener: CustomEventInterstitialListener? = null
  private var mAdView: AppMonetViewLayout? = null
  private var mContext: Context? = null
  private var bidResponse: BidResponse? = null
  private var sdkManager: SdkManager? = null
  private var mAdServerListener: AdServerBannerListener<View?>? = null
  private fun onActivityClosed(context: Context) {
    val i = Intent(INTERSTITIAL_ACTIVITY_BROADCAST)
    i.putExtra("message", INTERSTITIAL_ACTIVITY_CLOSE)
    LocalBroadcastManager.getInstance(context).sendBroadcast(i)
    onDestroy()
  }

  private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(
      context: Context,
      intent: Intent
    ) {
      val message = intent.getStringExtra(APPMONET_BROADCAST_MESSAGE)
      when (message) {
        "interstitial_dismissed" -> {
          customEventInterstitialListener?.onAdClosed()
          onActivityClosed(context)
        }
        else -> {
          customEventInterstitialListener?.onAdFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR)
          onActivityClosed(context)
        }
      }
      logger.debug("receiver", "Got message: $message")
    }
  }

  override fun requestInterstitialAd(
    context: Context,
    listener: CustomEventInterstitialListener,
    serverParameter: String?,
    mediationAdRequest: MediationAdRequest,
    customEventExtras: Bundle?
  ) {
    customEventInterstitialListener = listener
    val adSize = AdSize(INTERSTITIAL_WIDTH, INTERSTITIAL_HEIGHT)
    val adUnitId = DfpRequestHelper.getAdUnitID(customEventExtras, serverParameter, adSize)
    sdkManager = SdkManager.get()
    mContext = context
    if (sdkManager == null) {
      logger.warn("AppMonet SDK Has not been initialized. Unable to serve ads.")
      customEventInterstitialListener?.onAdFailedToLoad(AdRequest.ERROR_CODE_NO_FILL)
      return
    }
    if (adUnitId == null || adUnitId.isEmpty()) {
      logger.debug("no adUnit/tagId: floor line item configured incorrectly")
      customEventInterstitialListener?.onAdFailedToLoad(AdRequest.ERROR_CODE_NO_FILL)
      return
    }
    sdkManager?.auctionManager?.trackRequest(
        adUnitId, WebViewUtils.generateTrackingSource(INTERSTITIAL)
    )
    var bid: BidResponse? = null
    if (serverParameter != null && serverParameter != adUnitId) {
      bid = DfpRequestHelper.getBidFromRequest(customEventExtras?.toMap())
    }
    if (bid != null && sdkManager?.auctionManager?.bidManager?.isValid(bid) == true
        && customEventInterstitialListener != null
    ) {
      logger.debug("bid from bundle is valid. Attaching!")
      setupBid(context, bid, customEventInterstitialListener!!)
      return
    }
    val floorCpm = DfpRequestHelper.getCpm(serverParameter)
    if (bid == null || bid.id.isEmpty()) {
      bid = sdkManager!!.auctionManager
          .mediationManager
          .getBidForMediation(adUnitId, floorCpm)
    }
    val configurationTimeOut =
      sdkManager!!.auctionManager.getSdkConfigurations().getAdUnitTimeout(adUnitId)
    val mediationManager = sdkManager!!.auctionManager.mediationManager
    mediationManager.getBidReadyForMediationAsync(
        bid, adUnitId, adSize, INTERSTITIAL,
        floorCpm,
        top@{ response, error ->
          if (error != null) {
            customEventInterstitialListener!!.onAdFailedToLoad(AdRequest.ERROR_CODE_NO_FILL)
            return@top
          }
          if (response != null) {
            setupBid(context, response, customEventInterstitialListener!!)
          }
        }, configurationTimeOut, 4000
    )
  }

  private fun setupBid(
    context: Context,
    bid: BidResponse,
    customEventInterstitial: CustomEventInterstitialListener
  ) {
    try {
      LocalBroadcastManager.getInstance(context).registerReceiver(
          mMessageReceiver,
          IntentFilter(APPMONET_BROADCAST)
      )
      bidResponse = bid
      mAdServerListener = MonetDfpInterstitialListener(customEventInterstitial, context)
      if (bidResponse!!.interstitial != null && bidResponse!!.interstitial!!.trusted) {
        mAdServerListener?.onAdLoaded(null)
        return
      }
      mAdView =
        BidRenderer.renderBid(mContext!!, sdkManager!!, bidResponse!!, null, mAdServerListener!!)
      if (mAdView == null) {
        logger.error("unexpected: could not generate the adView")
        customEventInterstitialListener!!.onAdFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR)
      }
    } catch (e: Exception) {
      logger.error("failed to render bid: " + e.localizedMessage)
      customEventInterstitialListener!!.onAdFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR)
    }
  }

  /**
   * This methods is called when an interstitial is requested to be displayed.
   */
  override fun showInterstitial() {
    sdkManager!!.preferences.setPreference(AD_CONTENT_INTERSTITIAL, bidResponse!!.adm)
    sdkManager!!.preferences.setPreference(BID_ID_INTERSTITIAL, bidResponse!!.id)
    val uuid = if (mAdView != null) mAdView!!.uuid else null
    sdkManager!!.preferences.setPreference(AD_UUID_INTERSTITIAL, uuid)
    MonetDfpActivity.start(mContext!!, sdkManager!!, uuid, bidResponse!!.adm)
  }

  override fun onDestroy() {
    if (mAdView != null) {
      mAdView!!.destroyAdView(true)
    }
    mContext?.let {
      LocalBroadcastManager.getInstance(it).unregisterReceiver(mMessageReceiver)
    }

  }

  override fun onPause() {}
  override fun onResume() {}

  companion object {
    private val logger = Logger("MonetDfpCustomEventInterstitial")
  }
}