package com.monet.bidder

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.mediation.MediationAdRequest
import com.google.android.gms.ads.mediation.MediationAdapter
import com.google.android.gms.ads.mediation.customevent.CustomEventBanner
import com.google.android.gms.ads.mediation.customevent.CustomEventBannerListener
import com.monet.bidder.AdRequestFactory.fromMediationRequest
import com.monet.AdType.BANNER
import com.monet.bidder.MediationManager.NoBidsFoundException
import com.monet.bidder.MediationManager.NullBidException
import com.monet.bidder.bid.BidRenderer
import com.monet.BidResponse

class CustomEventBanner : CustomEventBanner, MediationAdapter, AppMonetViewListener {
  private var mAdView: AppMonetViewLayout? = null
  private var mListener: AdServerBannerListener? = null
  private var sdkManager: SdkManager? = null

  /**
   * Safely trigger an error on the listener. This will cause DFP
   * to cancel the rendering of this add and pass onto the next available ad.
   *
   * @param listener the DFP banner listener
   * @param errorCode the DFP AdRequest error indicating the type of error
   */
  private fun loadError(
    listener: CustomEventBannerListener?,
    errorCode: Int
  ) {
    if (listener == null) {
      return
    }
    listener.onAdFailedToLoad(errorCode)
  }

  /**
   * When an impression is rendered, we want to queue up another bid to be sent to the adserver
   * on the following refresh.
   *
   * @param bid the current bid being rendered
   * @param adUnitId the string adUnitId of the current rendering adUnit
   * @param adRequest the request used to get to this current render
   */
  private fun tryToAttachDemand(
    bid: BidResponse,
    adUnitId: String,
    adRequest: MediationAdRequest
  ) {
    if (!bid.nextQueue) {
      logger.debug("automatic refresh is disabled. Skipping queue next (clearing bids)")
      sdkManager!!.auctionManager.cancelRequest(
          adUnitId, fromMediationRequest(sdkManager!!.isPublisherAdView, adRequest), null
      )
      return
    }
    val nextBid = sdkManager!!.auctionManager.bidManager.peekNextBid(adUnitId)
    if (nextBid == null || !sdkManager!!.auctionManager.bidManager.isValid(nextBid)) {
      sdkManager!!.auctionManager.cancelRequest(
          adUnitId,
          fromMediationRequest(sdkManager!!.isPublisherAdView, adRequest), null
      )
    } else {
      sdkManager!!.auctionManager.cancelRequest(
          adUnitId,
          fromMediationRequest(sdkManager!!.isPublisherAdView, adRequest), nextBid
      )
    }
  }

  override fun requestBannerAd(
    context: Context,
    customEventBannerListener: CustomEventBannerListener,
    code: String?,
    adSize: com.google.android.gms.ads.AdSize,
    mediationAdRequest: MediationAdRequest,
    bundle: Bundle?
  ) {
    sdkManager = SdkManager.get()
    if (sdkManager == null) {
      loadError(customEventBannerListener, AdRequest.ERROR_CODE_INTERNAL_ERROR)
      return
    }
    try {
      requestBannerAdInner(
          context, customEventBannerListener, code, adSize,
          mediationAdRequest, bundle
      )
    } catch (e: Exception) {
      loadError(customEventBannerListener, AdRequest.ERROR_CODE_INTERNAL_ERROR)
    }
  }

  @SuppressLint("DefaultLocale") private fun requestBannerAdInner(
    context: Context,
    listener: CustomEventBannerListener,
    serverParameter: String?,
    adSize: com.google.android.gms.ads.AdSize,
    mediationAdRequest: MediationAdRequest,
    customEventExtras: Bundle?
  ) {
    val amAdSize = AdSize(adSize.width, adSize.height)
    val adUnitId = DfpRequestHelper.getAdUnitID(customEventExtras, serverParameter, amAdSize)
    if (adUnitId == null) {
      logger.warn("load failed: invalid bid data")
      loadError(listener, AdRequest.ERROR_CODE_INTERNAL_ERROR)
      return
    }
    sdkManager!!.auctionManager.trackRequest(
        adUnitId,
        WebViewUtils.generateTrackingSource(BANNER)
    )
    var bid = from(customEventExtras)
    val floorCpm = DfpRequestHelper.getCpm(serverParameter)
    if (bid == null || bid.id.isEmpty()) {
      bid = sdkManager!!.auctionManager.mediationManager.getBidForMediation(adUnitId, floorCpm)
    }
    val mediationManager = MediationManager(sdkManager!!, sdkManager!!.auctionManager.bidManager)
    try {
      bid = mediationManager.getBidReadyForMediation(
          bid, adUnitId, amAdSize, BANNER, floorCpm,
          true
      )

      // this will set adview
      try {
        tryToAttachDemand(bid, adUnitId, mediationAdRequest)
      } catch (e: Exception) {
        logger.warn("unable to attach upcoming demand", e.message)
      }
      mListener = DFPBannerListener(listener, this, sdkManager!!.uiThread)
      mAdView = BidRenderer.renderBid(context, sdkManager!!, bid, amAdSize, mListener!!)
      if (mAdView == null) {
        loadError(listener, AdRequest.ERROR_CODE_INTERNAL_ERROR)
      }
    } catch (e: NoBidsFoundException) {
      loadError(listener, AdRequest.ERROR_CODE_NO_FILL)
    } catch (e: NullBidException) {
      loadError(listener, AdRequest.ERROR_CODE_INTERNAL_ERROR)
    }
  }

  /**
   * When DFP destroyed the containing adunit, clean up our AdView.
   * Note: this method needs to change later when we allow for video ads through DFP,
   * since we want to call [AdView.destroy] to give the adView the opportunity
   * to return to the "loading" state.
   */
  override fun onDestroy() {
    if (mAdView != null) {
      try {
        mAdView!!.destroyAdView(true)
      } catch (e: Exception) {
        logger.warn("error destroying ceb - ", e.message)
      }
    }
  }

  override fun onPause() {}
  override fun onResume() {}
  override fun onAdRefreshed(view: View?) {
    mAdView = view as AppMonetViewLayout?
  }

  override val currentView: AppMonetViewLayout?
    get() = mAdView

  companion object {
    private val logger = Logger("CustomEventBanner")
  }
}