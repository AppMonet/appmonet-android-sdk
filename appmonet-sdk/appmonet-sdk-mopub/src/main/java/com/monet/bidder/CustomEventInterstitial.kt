package com.monet.bidder

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.monet.bidder.AdType.INTERSTITIAL
import com.monet.bidder.Constants.APPMONET_BROADCAST
import com.monet.bidder.Constants.APPMONET_BROADCAST_MESSAGE
import com.monet.bidder.Constants.BIDS_KEY
import com.monet.bidder.Constants.Configurations.DEFAULT_MEDIATION_FLOOR
import com.monet.bidder.Constants.INTERSTITIAL_ACTIVITY_BROADCAST
import com.monet.bidder.Constants.INTERSTITIAL_ACTIVITY_CLOSE
import com.monet.bidder.Constants.INTERSTITIAL_HEIGHT
import com.monet.bidder.Constants.INTERSTITIAL_WIDTH
import com.monet.bidder.Constants.Interstitial.AD_CONTENT_INTERSTITIAL
import com.monet.bidder.Constants.Interstitial.AD_UUID_INTERSTITIAL
import com.monet.bidder.Constants.Interstitial.BID_ID_INTERSTITIAL
import com.monet.bidder.CustomEventUtil.getAdUnitId
import com.monet.bidder.adview.AdViewManager.AdViewState.AD_RENDERED
import com.monet.bidder.bid.BidRenderer
import com.monet.bidder.bid.BidResponse
import com.monet.bidder.bid.BidResponse.Mapper.from
import com.mopub.common.LifecycleListener
import com.mopub.common.logging.MoPubLog
import com.mopub.common.logging.MoPubLog.AdLogEvent.SHOW_SUCCESS
import com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED
import com.mopub.common.logging.MoPubLog.ConsentLogEvent.CUSTOM
import com.mopub.mobileads.AdData
import com.mopub.mobileads.BaseAd
import com.mopub.mobileads.MoPubErrorCode
import com.mopub.mobileads.MoPubErrorCode.INTERNAL_ERROR
import com.mopub.mobileads.MoPubErrorCode.NETWORK_NO_FILL
import org.json.JSONObject

class CustomEventInterstitial : BaseAd() {
  private var mAdView: AppMonetViewLayout? = null
  private var bidResponse: BidResponse? = null
  private val interstitialContent: String? = null
  private var mContext: Context? = null
  private var sdkManager: SdkManager? = null
  private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
    private fun onActivityClosed(context: Context) {
      val i: Intent = Intent(INTERSTITIAL_ACTIVITY_BROADCAST)
      i.putExtra("message", INTERSTITIAL_ACTIVITY_CLOSE)
      LocalBroadcastManager.getInstance(context).sendBroadcast(i)
    }

    override fun onReceive(
      context: Context,
      intent: Intent
    ) {
      val message = intent.getStringExtra(APPMONET_BROADCAST_MESSAGE)
      when (message) {
        MonetActivity.INTERSTITIAL_SHOWN -> {
          if (mInteractionListener != null) {
            mInteractionListener.onAdShown()
            mInteractionListener.onAdImpression()
          }
          MoPubLog.log(
              adNetworkId, SHOW_SUCCESS, ADAPTER_NAME,
              "AppMonet interstitial ad has been shown"
          )
        }
        "interstitial_dismissed" -> {
          MoPubLog.log(
              adNetworkId, CUSTOM, ADAPTER_NAME,
              "AppMonet interstitial ad has been dismissed"
          )
          if (mInteractionListener != null) {
            mInteractionListener.onAdDismissed()
          }
          onActivityClosed(context)
        }
        else -> {
          onMoPubError(INTERNAL_ERROR)
          onActivityClosed(context)
        }
      }
      logger.debug("receiver", "Got message: $message")
    }
  }
  private var adUnitId = "ZONE_ID"
  override fun show() {
    sdkManager!!.preferences.setPreference(AD_CONTENT_INTERSTITIAL, bidResponse!!.adm)
    sdkManager!!.preferences.setPreference(BID_ID_INTERSTITIAL, bidResponse!!.id)
    val uuid = if (mAdView != null) mAdView!!.adViewUUID else null
    sdkManager!!.preferences.setPreference(AD_UUID_INTERSTITIAL, uuid)
    MonetActivity.start(mContext!!, sdkManager!!, uuid, bidResponse!!.adm)
  }

  override fun onInvalidate() {
    if (mAdView != null) {
      if (mAdView!!.adViewState !== AD_RENDERED) {
        logger.warn("attempt to remove loading adview..")
      }
      mAdView!!.destroyAdView(true)
      MoPubLog.log(adNetworkId, CUSTOM, ADAPTER_NAME, "Interstitial destroyed")
    }
    LocalBroadcastManager.getInstance(mContext!!).unregisterReceiver(mMessageReceiver)
  }

  override fun getLifecycleListener(): LifecycleListener? {
    return null
  }

  override fun getAdNetworkId(): String {
    return adUnitId
  }

  @Throws(
      Exception::class
  ) override fun checkAndInitializeSdk(
    launcherActivity: Activity,
    adData: AdData
  ): Boolean {
    return false
  }

  @Throws(Exception::class) override fun load(
    context: Context,
    adData: AdData
  ) {
    LocalBroadcastManager.getInstance(context).registerReceiver(
        mMessageReceiver,
        IntentFilter(APPMONET_BROADCAST)
    )
    sdkManager = SdkManager.get()
    if (sdkManager == null) {
      logger.warn("AppMonet SDK Has not been initialized. Unable to serve ads.")
      onMoPubError(NETWORK_NO_FILL)
      return
    }
    val extras: Map<String, String?> = adData.extras
    val adSize = AdSize(INTERSTITIAL_WIDTH, INTERSTITIAL_HEIGHT)
    adUnitId = getAdUnitId(extras, adSize)!!
    if (adUnitId == null || adUnitId.isEmpty()) {
      logger.debug("no adUnit/tagId: floor line item configured incorrectly")
      onMoPubError(NETWORK_NO_FILL)
      return
    }
    sdkManager!!.auctionManager.trackRequest(
        adUnitId,
        WebViewUtils.generateTrackingSource(INTERSTITIAL)
    )
    val configurations = sdkManager!!.sdkConfigurations
    var headerBiddingBid: BidResponse? = null
    if (extras.containsKey(BIDS_KEY) && extras[BIDS_KEY] != null) {
      headerBiddingBid = from(JSONObject(extras[BIDS_KEY]))
    }
    val floorCpm = getServerExtraCpm(extras, configurations.getDouble(DEFAULT_MEDIATION_FLOOR))
    if (headerBiddingBid == null) {
      headerBiddingBid =
        sdkManager!!.auctionManager.mediationManager.getBidForMediation(adUnitId, floorCpm)
    }
    val mediationManager = sdkManager!!.auctionManager.mediationManager
    mediationManager.getBidReadyForMediationAsync(headerBiddingBid, adUnitId, adSize,
        INTERSTITIAL, floorCpm, object : Callback<BidResponse> {
      override fun onSuccess(response: BidResponse) {
        mContext = context
        bidResponse = response
        val listener: AdServerBannerListener = MonetInterstitialListener(
            mLoadListener, mInteractionListener, adUnitId,
            context, sdkManager!!
        )
        if (bidResponse!!.interstitial != null && bidResponse!!.interstitial!!
                .trusted
        ) {
          listener.onAdLoaded(null)
          return
        }
        mAdView = BidRenderer.renderBid(context, sdkManager!!, bidResponse!!, null, listener)
        if (mAdView == null) {
          logger.error("unexpected: could not generate the adView")
          onMoPubError(INTERNAL_ERROR)
        }
      }

      override fun onError() {
        onMoPubError(NETWORK_NO_FILL)
      }
    })
  }

  private fun onMoPubError(error: MoPubErrorCode) {
    MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, error.intCode, error)
    if (mLoadListener != null) {
      mLoadListener.onAdLoadFailed(error)
    }
  }

  private fun getServerExtraCpm(
    serverExtras: Map<String, String?>?,
    defaultValue: Double
  ): Double {
    if (serverExtras == null || !serverExtras.containsKey(SERVER_EXTRA_CPM_KEY)) {
      return defaultValue
    }
    try {
      return serverExtras[SERVER_EXTRA_CPM_KEY]!!
          .toDouble()
    } catch (e: NumberFormatException) {
      // do nothing
    }
    return defaultValue
  }

  companion object {
    private val logger = MonetLogger("CustomEventInterstitial")
    private const val SERVER_EXTRA_CPM_KEY = "cpm"
    @JvmField val ADAPTER_NAME = CustomEventInterstitial::class.java.simpleName
  }
}