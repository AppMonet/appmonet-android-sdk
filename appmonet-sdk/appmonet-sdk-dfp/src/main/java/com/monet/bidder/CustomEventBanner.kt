package com.monet.bidder

import android.content.Context
import android.os.Bundle
import android.view.View
import com.google.android.gms.ads.mediation.MediationAdRequest
import com.google.android.gms.ads.mediation.MediationAdapter
import com.google.android.gms.ads.mediation.customevent.CustomEventBanner
import com.google.android.gms.ads.mediation.customevent.CustomEventBannerListener
import com.monet.AdServerBannerListener
import com.monet.AppMonetViewListener
import com.monet.BidResponse
import com.monet.CustomEventBannerAdapter
import com.monet.IAppMonetViewLayout
import com.monet.adview.AdSize
import com.monet.bidder.bid.BidRenderer
import com.monet.toMap

class CustomEventBanner : CustomEventBanner, MediationAdapter, AppMonetViewListener<View> {
  //  private var mAdView: AppMonetViewLayout? = null
  private var mListener: AdServerBannerListener<View?>? = null
  private var sdkManager: SdkManager? = null
  private var customEventBannerAdapter: CustomEventBannerAdapter? = null
  override fun requestBannerAd(
    context: Context,
    customEventBannerListener: CustomEventBannerListener,
    code: String?,
    adSize: com.google.android.gms.ads.AdSize,
    mediationAdRequest: MediationAdRequest,
    bundle: Bundle?
  ) {
    val appMonetAdSize = AdSize(context.applicationContext, adSize.width, adSize.height)

    sdkManager = SdkManager.get()
    mListener = DFPBannerListener(customEventBannerListener, this, sdkManager!!.uiThread)
    customEventBannerAdapter = CustomEventBannerAdapter(
        appMonetAdSize, sdkManager, bundle?.toMap(), code,
        sdkManager?.auctionManager?.mediationManager,
        sdkManager?.auctionManager?.bidManager, sdkManager?.auctionManager,
        mListener as DFPBannerListener,
        sdkManager?.isPublisherAdView ?: false, mediationAdRequest
    ) { bid: BidResponse, size: AdSize ->
      BidRenderer.renderBid(context, sdkManager!!, bid, size, mListener!!)
    }

    customEventBannerAdapter?.requestBanner()
  }

  /**
   * When DFP destroyed the containing adunit, clean up our AdView.
   * Note: this method needs to change later when we allow for video ads through DFP,
   * since we want to call [AdView.destroy] to give the adView the opportunity
   * to return to the "loading" state.
   */
  override fun onDestroy() {
    customEventBannerAdapter?.destroy()
//    if (mAdView != null) {
//      try {
//        mAdView!!.destroyAdView(true)
//      } catch (e: Exception) {
//        logger.warn("error destroying ceb - ", e.message)
//      }
//    }
  }

  override fun onPause() {}
  override fun onResume() {}
  override fun onAdRefreshed(view: View?) {
    customEventBannerAdapter?.adView = view as AppMonetViewLayout?
  }

  override val currentView: IAppMonetViewLayout?
    get() = customEventBannerAdapter?.adView

  companion object {
    private val logger = Logger("CustomEventBanner")
  }
}