package com.monet.bidder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.monet.bidder.adview.AdView;
import com.monet.bidder.bid.BidRenderer;
import com.monet.bidder.bid.BidResponse;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationAdapter;
import com.google.android.gms.ads.mediation.customevent.CustomEventBannerListener;

public class CustomEventBanner
    implements com.google.android.gms.ads.mediation.customevent.CustomEventBanner,
    MediationAdapter, AppMonetViewListener {
  private static final MonetLogger logger = new MonetLogger("CustomEventBanner");
  private AppMonetViewLayout mAdView;
  private AdServerBannerListener mListener;
  private SdkManager sdkManager;

  /**
   * Safely trigger an error on the listener. This will cause DFP
   * to cancel the rendering of this add and pass onto the next available ad.
   *
   * @param listener the DFP banner listener
   * @param errorCode the DFP AdRequest error indicating the type of error
   */
  private void loadError(CustomEventBannerListener listener, int errorCode) {
    if (listener == null) {
      return;
    }

    listener.onAdFailedToLoad(errorCode);
  }

  /**
   * When an impression is rendered, we want to queue up another bid to be sent to the adserver
   * on the following refresh.
   *
   * @param bid the current bid being rendered
   * @param adUnitId the string adUnitId of the current rendering adUnit
   * @param adRequest the request used to get to this current render
   */
  private void tryToAttachDemand(BidResponse bid, String adUnitId, MediationAdRequest adRequest) {
    if (!bid.getQueueNext()) {
      logger.debug("automatic refresh is disabled. Skipping queue next (clearing bids)");
      sdkManager.getAuctionManager().cancelRequest(adUnitId, AdRequestFactory
          .fromMediationRequest(sdkManager.isPublisherAdView, adRequest), null);
      return;
    }

    BidResponse nextBid = sdkManager.getAuctionManager().getBidManager().peekNextBid(adUnitId);
    if (nextBid == null || !sdkManager.getAuctionManager().getBidManager().isValid(nextBid)) {
      sdkManager.getAuctionManager().cancelRequest(adUnitId,
          AdRequestFactory.fromMediationRequest(sdkManager.isPublisherAdView, adRequest), null);
    } else {
      sdkManager.getAuctionManager().cancelRequest(adUnitId,
          AdRequestFactory.fromMediationRequest(sdkManager.isPublisherAdView, adRequest), nextBid);
    }
  }

  public void requestBannerAd(Context context, CustomEventBannerListener customEventBannerListener,
      String code, AdSize adSize, MediationAdRequest mediationAdRequest,
      Bundle bundle) {
    sdkManager = SdkManager.get();
    if (sdkManager == null) {
      loadError(customEventBannerListener, AdRequest.ERROR_CODE_INTERNAL_ERROR);
      return;
    }

    try {
      requestBannerAdInner(context, customEventBannerListener, code, adSize,
          mediationAdRequest, bundle);
    } catch (Exception e) {
      loadError(customEventBannerListener, AdRequest.ERROR_CODE_INTERNAL_ERROR);
    }
  }

  @SuppressLint("DefaultLocale")
  private void requestBannerAdInner(Context context, CustomEventBannerListener listener,
      String serverParameter, AdSize adSize,
      MediationAdRequest mediationAdRequest,
      Bundle customEventExtras) {

    com.monet.bidder.AdSize amAdSize =
        new com.monet.bidder.AdSize(adSize.getWidth(), adSize.getHeight());
    String adUnitId = DfpRequestHelper.getAdUnitID(customEventExtras, serverParameter, amAdSize);

    if (adUnitId == null) {
      logger.warn("load failed: invalid bid data");
      loadError(listener, AdRequest.ERROR_CODE_INTERNAL_ERROR);
      return;
    }
    sdkManager.getAuctionManager().trackRequest(adUnitId,
        WebViewUtils.generateTrackingSource(AdType.BANNER));

    BidResponse bid = BidResponse.Mapper.from(customEventExtras);

    double floorCpm = DfpRequestHelper.getCpm(serverParameter);
    if (bid == null || bid.getId().isEmpty()) {
      bid = sdkManager.getAuctionManager().getMediationManager().getBidForMediation(adUnitId, floorCpm);
    }
    MediationManager mediationManager =
        new MediationManager(sdkManager, sdkManager.getAuctionManager().getBidManager());
    try {
      bid =
          mediationManager.getBidReadyForMediation(bid, adUnitId, amAdSize, AdType.BANNER, floorCpm,
              true);

      // this will set adview
      try {
        tryToAttachDemand(bid, adUnitId, mediationAdRequest);
      } catch (Exception e) {
        logger.warn("unable to attach upcoming demand", e.getMessage());
      }
      mListener = new DFPBannerListener(listener, this, sdkManager.getUiThread());

      mAdView = BidRenderer.renderBid(context, sdkManager, bid, amAdSize, mListener);

      if (mAdView == null) {
        loadError(listener, AdRequest.ERROR_CODE_INTERNAL_ERROR);
      }
    } catch (MediationManager.NoBidsFoundException e) {
      loadError(listener, AdRequest.ERROR_CODE_NO_FILL);
    } catch (MediationManager.NullBidException e) {
      loadError(listener, AdRequest.ERROR_CODE_INTERNAL_ERROR);
    }
  }

  /**
   * When DFP destroyed the containing adunit, clean up our AdView.
   * Note: this method needs to change later when we allow for video ads through DFP,
   * since we want to call {@link AdView#destroy(boolean)} to give the adView the opportunity
   * to return to the "loading" state.
   */

  public void onDestroy() {
    if (mAdView != null) {
      try {
        mAdView.destroyAdView(true);
      } catch (Exception e) {
        logger.warn("error destroying ceb - ", e.getMessage());
      }
    }
  }

  public void onPause() {

  }

  public void onResume() {

  }

  @Override
  public void onAdRefreshed(View view) {
    mAdView = (AppMonetViewLayout) view;
  }

  @Override
  public AppMonetViewLayout getCurrentView() {
    return mAdView;
  }
}
