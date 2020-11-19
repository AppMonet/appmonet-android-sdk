package com.monet.bidder

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.monet.AdServerBannerListener
import com.monet.AdType.INTERSTITIAL
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
import com.monet.CustomEventUtil.getAdUnitId
import com.monet.bidder.bid.BidRenderer
import com.monet.BidResponse
import com.monet.BidResponse.Mapper.from
import com.monet.CustomEventBaseAdapter
import com.monet.CustomEventInterstitialAdapter
import com.monet.adview.AdSize
import com.monet.adview.AdViewState.AD_RENDERED
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

class CustomEventInterstitial : BaseAd() {
  private val interstitialContent: String? = null
  private var mContext: Context? = null
  private var sdkManager: SdkManager? = null
  private var customEventInterstitialAdapter: CustomEventInterstitialAdapter? = null
  private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
    private fun onActivityClosed(context: Context) {
      val i = Intent(INTERSTITIAL_ACTIVITY_BROADCAST)
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

  override fun show() {
    customEventInterstitialAdapter?.let {
      sdkManager!!.preferences.setPreference(AD_CONTENT_INTERSTITIAL, it.bidResponse!!.adm)
      sdkManager!!.preferences.setPreference(BID_ID_INTERSTITIAL, it.bidResponse!!.id)
      val uuid = if (it.adView != null) it.adView!!.uuid else null
      sdkManager!!.preferences.setPreference(AD_UUID_INTERSTITIAL, uuid)
      MonetActivity.start(mContext!!, sdkManager!!, uuid, it.bidResponse!!.adm)
    }

  }

  override fun onInvalidate() {
    customEventInterstitialAdapter?.adView?.let {
      if (it.state !== AD_RENDERED) {
        logger.warn("attempt to remove loading adview..")
      }
      it.destroyAdView(true)
      MoPubLog.log(adNetworkId, CUSTOM, ADAPTER_NAME, "Interstitial destroyed")
    }
    LocalBroadcastManager.getInstance(mContext!!).unregisterReceiver(mMessageReceiver)
  }

  override fun getLifecycleListener(): LifecycleListener? {
    return null
  }

  override fun getAdNetworkId(): String {
    return customEventInterstitialAdapter?.adUnitId ?: "ZONE_ID"
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
    val adSize = AdSize(context.applicationContext, INTERSTITIAL_WIDTH, INTERSTITIAL_HEIGHT)
    val listener: AdServerBannerListener<View?> = MonetInterstitialListener(
        mLoadListener, mInteractionListener,
        context, sdkManager!!
    )
    val configurations = sdkManager!!.sdkConfigurations
    val floorCpm = getServerExtraCpm(extras, configurations.getDouble(DEFAULT_MEDIATION_FLOOR))

    customEventInterstitialAdapter = CustomEventInterstitialAdapter(
        adSize, sdkManager, sdkManager?.auctionManager,
        sdkManager?.auctionManager?.mediationManager, listener,
        extras, floorCpm, INTERSTITIAL
    )
    val configurationTimeOut =
      sdkManager!!.auctionManager.getSdkConfigurations()
          .getAdUnitTimeout(customEventInterstitialAdapter?.adUnitId)

    customEventInterstitialAdapter?.requestAd(
        { bid: BidResponse?, adSize: AdSize, customEventAdapter: CustomEventBaseAdapter ->
          mContext = context
          BidRenderer.renderBid(context, sdkManager!!, bid!!, null, listener)
        }, configurationTimeOut
    )

//    val adUnitFromExtras = getAdUnitId(extras, adSize)
//    if (adUnitFromExtras == null || adUnitFromExtras.isEmpty()) {
//      logger.debug("no adUnit/tagId: floor line item configured incorrectly")
//      onMoPubError(NETWORK_NO_FILL)
//      return
//    }
//    adUnitId = adUnitFromExtras
//    sdkManager!!.auctionManager.trackRequest(
//        adUnitId,
//        WebViewUtils.generateTrackingSource(INTERSTITIAL)
//    )
//    val configurations = sdkManager!!.sdkConfigurations
//    var headerBiddingBid: BidResponse? = null
//    if (extras.containsKey(BIDS_KEY) && extras[BIDS_KEY] != null) {
//      headerBiddingBid = extras[BIDS_KEY]?.let { from(it) }
//    }
//    val floorCpm = getServerExtraCpm(extras, configurations.getDouble(DEFAULT_MEDIATION_FLOOR))
//    if (headerBiddingBid == null) {
//      headerBiddingBid =
//        sdkManager!!.auctionManager.mediationManager.getBidForMediation(adUnitId, floorCpm)
//    }
//    val mediationManager = sdkManager!!.auctionManager.mediationManager
//    val configurationTimeOut =
//      sdkManager!!.auctionManager.getSdkConfigurations().getAdUnitTimeout(adUnitId)
//    mediationManager.getBidReadyForMediationAsync(
//        headerBiddingBid, adUnitId, adSize,
//        INTERSTITIAL, floorCpm, top@{ response, error ->
//      if (error != null) {
//        onMoPubError(NETWORK_NO_FILL)
//      }
//      mContext = context
//      bidResponse = response
//      val listener: AdServerBannerListener<View?> = MonetInterstitialListener(
//          mLoadListener, mInteractionListener, adUnitId,
//          context, sdkManager!!
//      )
//      if (bidResponse!!.interstitial != null && bidResponse!!.interstitial!!
//              .trusted
//      ) {
//        listener.onAdLoaded(null)
//        return@top
//      }
//      mAdView = BidRenderer.renderBid(context, sdkManager!!, bidResponse!!, null, listener)
//      if (mAdView == null) {
//        logger.error("unexpected: could not generate the adView")
//        onMoPubError(INTERNAL_ERROR)
//      }
//    }, configurationTimeOut, 4000
//    )
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
      return serverExtras[SERVER_EXTRA_CPM_KEY]?.toDouble() ?: defaultValue
    } catch (e: NumberFormatException) {
      // do nothing
    }
    return defaultValue
  }

  companion object {
    private val logger = Logger("CustomEventInterstitial")
    private const val SERVER_EXTRA_CPM_KEY = "cpm"
    @JvmField val ADAPTER_NAME = CustomEventInterstitial::class.java.simpleName
  }
}