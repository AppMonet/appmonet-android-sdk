package com.monet.bidder

import android.app.Activity
import android.content.Context
import android.view.View
import com.monet.bidder.AdType.BANNER
import com.monet.bidder.Constants.BIDS_KEY
import com.monet.bidder.Constants.Configurations.DEFAULT_MEDIATION_FLOOR
import com.monet.bidder.CustomEventUtil.getAdUnitId
import com.monet.bidder.CustomEventUtil.getServerExtraCpm
import com.monet.bidder.MediationManager.NoBidsFoundException
import com.monet.bidder.MediationManager.NullBidException
import com.monet.bidder.adview.AdViewManager.AdViewState.AD_RENDERED
import com.monet.bidder.bid.BidRenderer
import com.monet.bidder.bid.BidResponse
import com.monet.bidder.bid.BidResponse.Mapper.from
import com.mopub.common.LifecycleListener
import com.mopub.common.logging.MoPubLog
import com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED
import com.mopub.common.logging.MoPubLog.ConsentLogEvent.CUSTOM
import com.mopub.mobileads.AdData
import com.mopub.mobileads.BaseAd
import com.mopub.mobileads.MoPubErrorCode
import com.mopub.mobileads.MoPubErrorCode.INTERNAL_ERROR
import com.mopub.mobileads.MoPubErrorCode.NETWORK_NO_FILL
import org.json.JSONObject

/**
 * Created by jose on 2/1/18.
 */
open class CustomEventBanner : BaseAd(), AppMonetViewListener {
  private var mAdView: AppMonetViewLayout? = null
  private var listener: MoPubBannerListener? = null
  private var adUnitID = "ZONE_ID"
  override fun onInvalidate() {
    if (mAdView != null) {
      if (mAdView!!.adViewState !== AD_RENDERED) {
        sLogger.warn("attempt to remove loading adview..")
      }
      mAdView!!.destroyAdView(true)
      MoPubLog.log(adNetworkId, CUSTOM, ADAPTER_NAME, "Banner destroyed")
    }
    if (listener != null && listener!!.moPubAdViewContainer != null && listener!!.moPubAdViewContainer is FloatingAdView) {
      (listener!!.moPubAdViewContainer as FloatingAdView).removeAllViews()
    }
  }

  override fun getLifecycleListener(): LifecycleListener? {
    return null
  }

  override fun getAdNetworkId(): String {
    return adUnitID
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
    val extras: Map<String, String?> = adData.extras
    val adSize = AdSize(
        (if (adData.adWidth != null) adData.adWidth!! else 0),
        (if (adData.adHeight != null) adData.adHeight!! else 0)
    )
    adUnitID = getAdUnitId(extras, adSize)!!
    val sdkManager = SdkManager.get()
    //    // check if it's null first
    if (sdkManager == null) {
      sLogger.warn("AppMonet SDK Has not been initialized. Unable to serve ads.")
      onMoPubError(INTERNAL_ERROR)
      return
    }
    if (adUnitID.isEmpty()) {
      sLogger.debug("no adUnit/tagId: floor line item configured incorrectly")
      onMoPubError(NETWORK_NO_FILL)
      return
    }
    sdkManager.auctionManager.trackRequest(
        adUnitID,
        WebViewUtils.generateTrackingSource(BANNER)
    )
    val configurations = sdkManager.sdkConfigurations

    //    // try to get the bid from the localExtras
    //    // thanks to localExtras we don't need to serialize/deserialize
    var headerBiddingBid: BidResponse? = null
    if (extras.containsKey(BIDS_KEY) && extras[BIDS_KEY] != null) {
      headerBiddingBid = from(JSONObject(extras[BIDS_KEY]))
    }
    val floorCpm = getServerExtraCpm(
        extras, configurations.getDouble(DEFAULT_MEDIATION_FLOOR)
    )
    if (headerBiddingBid == null) {
      sLogger.debug("checking store for precached bids")
      headerBiddingBid =
        sdkManager.auctionManager.mediationManager.getBidForMediation(adUnitID, floorCpm)
    }
    val mediationManager = sdkManager.auctionManager.mediationManager
    try {
      val bid = mediationManager.getBidReadyForMediation(
          headerBiddingBid, adUnitID, adSize,
          BANNER, floorCpm, true
      )
      listener = MoPubBannerListener(
          sdkManager, mLoadListener, mInteractionListener, bid, adUnitID,
          this
      )
      // this will set adview
      mAdView = BidRenderer.renderBid(context, sdkManager, bid, adSize, listener!!)
      if (mAdView == null) {
        sLogger.error("unexpected: could not generate the adView")
        onMoPubError(INTERNAL_ERROR)
      }
    } catch (e: NoBidsFoundException) {
      onMoPubError(NETWORK_NO_FILL)
    } catch (e: NullBidException) {
      onMoPubError(INTERNAL_ERROR)
    }
  }

  override fun getAdView(): View? {
    return mAdView
  }

  override fun onAdRefreshed(view: View?) {
    mAdView = view as AppMonetViewLayout?
  }

  override val currentView: AppMonetViewLayout?
    get() = mAdView

  private fun onMoPubError(error: MoPubErrorCode) {
    MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, error.intCode, error)
    if (mLoadListener != null) {
      mLoadListener.onAdLoadFailed(error)
    }
  }

  companion object {
    private val sLogger = Logger("CustomEventBanner")
    @JvmField val ADAPTER_NAME = CustomEventBanner::class.java.simpleName
  }
}