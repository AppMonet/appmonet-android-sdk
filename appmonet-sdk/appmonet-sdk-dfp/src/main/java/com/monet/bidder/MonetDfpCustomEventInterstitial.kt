package com.monet.bidder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.monet.bidder.bid.BidRenderer;
import com.monet.bidder.bid.BidResponse;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.customevent.CustomEventInterstitial;
import com.google.android.gms.ads.mediation.customevent.CustomEventInterstitialListener;

import static com.monet.bidder.Constants.APPMONET_BROADCAST;
import static com.monet.bidder.Constants.APPMONET_BROADCAST_MESSAGE;
import static com.monet.bidder.Constants.INTERSTITIAL_ACTIVITY_BROADCAST;
import static com.monet.bidder.Constants.INTERSTITIAL_ACTIVITY_CLOSE;
import static com.monet.bidder.Constants.INTERSTITIAL_HEIGHT;
import static com.monet.bidder.Constants.INTERSTITIAL_WIDTH;
import static com.monet.bidder.Constants.Interstitial.AD_CONTENT_INTERSTITIAL;
import static com.monet.bidder.Constants.Interstitial.AD_UUID_INTERSTITIAL;
import static com.monet.bidder.Constants.Interstitial.BID_ID_INTERSTITIAL;

public class MonetDfpCustomEventInterstitial implements CustomEventInterstitial {
  private static final MonetLogger logger = new MonetLogger("MonetDfpCustomEventInterstitial");

  private CustomEventInterstitialListener customEventInterstitialListener;
  private AppMonetViewLayout mAdView;
  private Context mContext;
  private BidResponse bidResponse;
  private SdkManager sdkManager;
  private MonetDfpInterstitialListener mAdServerListener = null;

  private void onActivityClosed(Context context) {
    Intent i = new Intent(INTERSTITIAL_ACTIVITY_BROADCAST);
    i.putExtra("message", INTERSTITIAL_ACTIVITY_CLOSE);
    LocalBroadcastManager.getInstance(context).sendBroadcast(i);
    onDestroy();
  }

  private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String message = intent.getStringExtra(APPMONET_BROADCAST_MESSAGE);
      switch (message) {
        case "interstitial_dismissed":
          customEventInterstitialListener.onAdClosed();
          onActivityClosed(context);
          break;
        default:
          customEventInterstitialListener.onAdFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
          onActivityClosed(context);
          break;
      }
      logger.debug("receiver", "Got message: " + message);
    }
  };

  public void requestInterstitialAd(final Context context, CustomEventInterstitialListener listener,
      String serverParameter, MediationAdRequest mediationAdRequest,
      Bundle customEventExtras) {
    this.customEventInterstitialListener = listener;
    AdSize adSize = new AdSize(INTERSTITIAL_WIDTH, INTERSTITIAL_HEIGHT);
    String adUnitId = DfpRequestHelper.getAdUnitID(customEventExtras, serverParameter, adSize);
    sdkManager = SdkManager.get();
    mContext = context;

    if (sdkManager == null) {
      logger.warn("AppMonet SDK Has not been initialized. Unable to serve ads.");
      customEventInterstitialListener.onAdFailedToLoad(AdRequest.ERROR_CODE_NO_FILL);
      return;
    }

    if (adUnitId == null || adUnitId.isEmpty()) {
      logger.debug("no adUnit/tagId: floor line item configured incorrectly");
      customEventInterstitialListener.onAdFailedToLoad(AdRequest.ERROR_CODE_NO_FILL);
      return;
    }
    sdkManager.getAuctionManager()
        .trackRequest(adUnitId, WebViewUtils.generateTrackingSource(AdType.INTERSTITIAL));

    BidResponse bid = null;
    if (serverParameter != null && !serverParameter.equals(adUnitId)) {
      bid = BidResponse.Mapper.from(customEventExtras);
    }

    if (bid != null && sdkManager.getAuctionManager().getBidManager().isValid(bid)) {
      logger.debug("bid from bundle is valid. Attaching!");
      setupBid(context, bid);
      return;
    }

    double floorCpm = DfpRequestHelper.getCpm(serverParameter);
    if (bid == null || bid.getId().isEmpty()) {
      bid = sdkManager.getAuctionManager()
          .getMediationManager()
          .getBidForMediation(adUnitId, floorCpm);
    }
    MediationManager mediationManager = sdkManager.getAuctionManager().getMediationManager();

    mediationManager.getBidReadyForMediationAsync(bid, adUnitId, adSize, AdType.INTERSTITIAL,
        floorCpm, new Callback<BidResponse>() {
          @Override
          public void onSuccess(BidResponse response) {
            setupBid(context, response);
          }

          @Override
          public void onError() {
            customEventInterstitialListener.onAdFailedToLoad(AdRequest.ERROR_CODE_NO_FILL);
          }
        });
  }

  private void setupBid(Context context, BidResponse bid) {
    try {
      LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver,
          new IntentFilter(APPMONET_BROADCAST));
      bidResponse = bid;
      mAdServerListener =
          new MonetDfpInterstitialListener(customEventInterstitialListener, context);
      if (bidResponse.getInterstitial() != null && bidResponse.getInterstitial().getTrusted()) {
        mAdServerListener.onAdLoaded(null);
        return;
      }
      mAdView = BidRenderer.renderBid(mContext, sdkManager, bidResponse, null, mAdServerListener);
      if (mAdView == null) {
        logger.error("unexpected: could not generate the adView");
        customEventInterstitialListener.onAdFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
      }
    } catch (Exception e) {
      logger.error("failed to render bid: " + e.getLocalizedMessage());
      customEventInterstitialListener.onAdFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
    }
  }

  /**
   * This methods is called when an interstitial is requested to be displayed.
   */
  public void showInterstitial() {
    sdkManager.getPreferences().setPreference(AD_CONTENT_INTERSTITIAL, bidResponse.getAdm());
    sdkManager.getPreferences().setPreference(BID_ID_INTERSTITIAL, bidResponse.getId());
    String uuid = (mAdView != null) ? mAdView.getAdViewUUID() : null;
    sdkManager.getPreferences().setPreference(AD_UUID_INTERSTITIAL, uuid);
    MonetDfpActivity.start(mContext, sdkManager, uuid, bidResponse.getAdm());
  }

  public void onDestroy() {
    if (mAdView != null) {
      mAdView.destroyAdView(true);
    }
    if (mContext == null) {
      return;
    }

    LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mMessageReceiver);
  }

  public void onPause() {

  }

  public void onResume() {

  }
}
