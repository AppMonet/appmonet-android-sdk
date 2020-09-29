package com.monet.bidder;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.monet.bidder.adview.AdViewManager;
import com.monet.bidder.bid.BidRenderer;
import com.monet.bidder.bid.BidResponse;
import com.mopub.common.LifecycleListener;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.AdData;
import com.mopub.mobileads.BaseAd;
import com.mopub.mobileads.MoPubErrorCode;

import org.json.JSONObject;

import java.util.Map;

import static com.monet.bidder.Constants.APPMONET_BROADCAST;
import static com.monet.bidder.Constants.APPMONET_BROADCAST_MESSAGE;
import static com.monet.bidder.Constants.BIDS_KEY;
import static com.monet.bidder.Constants.Configurations.DEFAULT_MEDIATION_FLOOR;
import static com.monet.bidder.Constants.INTERSTITIAL_ACTIVITY_BROADCAST;
import static com.monet.bidder.Constants.INTERSTITIAL_ACTIVITY_CLOSE;
import static com.monet.bidder.Constants.INTERSTITIAL_HEIGHT;
import static com.monet.bidder.Constants.INTERSTITIAL_WIDTH;
import static com.monet.bidder.Constants.Interstitial.AD_CONTENT_INTERSTITIAL;
import static com.monet.bidder.Constants.Interstitial.AD_UUID_INTERSTITIAL;
import static com.monet.bidder.Constants.Interstitial.BID_ID_INTERSTITIAL;
import static com.monet.bidder.MonetActivity.INTERSTITIAL_SHOWN;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.SHOW_SUCCESS;
import static com.mopub.common.logging.MoPubLog.ConsentLogEvent.CUSTOM;

public class CustomEventInterstitial extends BaseAd {
  private static final MonetLogger logger = new MonetLogger("CustomEventInterstitial");
  private static final String SERVER_EXTRA_CPM_KEY = "cpm";
  private AppMonetViewLayout mAdView;
  private BidResponse bidResponse;
  private String interstitialContent;
  private Context mContext;
  private SdkManager sdkManager;
  static final String ADAPTER_NAME = CustomEventInterstitial.class.getSimpleName();

  private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
    private void onActivityClosed(Context context) {
      Intent i = new Intent(INTERSTITIAL_ACTIVITY_BROADCAST);
      i.putExtra("message", INTERSTITIAL_ACTIVITY_CLOSE);
      LocalBroadcastManager.getInstance(context).sendBroadcast(i);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
      String message = intent.getStringExtra(APPMONET_BROADCAST_MESSAGE);
      switch (message) {
        case INTERSTITIAL_SHOWN:
          if (mInteractionListener != null) {
            mInteractionListener.onAdShown();
            mInteractionListener.onAdImpression();
          }
          MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME,
              "AppMonet interstitial ad has been shown");
          break;
        case "interstitial_dismissed":
          MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
              "AppMonet interstitial ad has been dismissed");
          if (mInteractionListener != null) {
            mInteractionListener.onAdDismissed();
          }
          onActivityClosed(context);
          break;
        default:
          onMoPubError(MoPubErrorCode.INTERNAL_ERROR);
          onActivityClosed(context);
          break;
      }
      logger.debug("receiver", "Got message: " + message);
    }
  };
  private String adUnitId = "ZONE_ID";

  @Override
  protected void show() {
    sdkManager.getPreferences().setPreference(AD_CONTENT_INTERSTITIAL, bidResponse.getAdm());
    sdkManager.getPreferences().setPreference(BID_ID_INTERSTITIAL, bidResponse.getId());
    String uuid = (mAdView != null) ? mAdView.getAdViewUUID() : null;
    sdkManager.getPreferences().setPreference(AD_UUID_INTERSTITIAL, uuid);
    MonetActivity.start(mContext, sdkManager, uuid, bidResponse.getAdm());
  }

  @Override
  protected void onInvalidate() {
    if (mAdView != null) {
      if (mAdView.getAdViewState() != AdViewManager.AdViewState.AD_RENDERED) {
        logger.warn("attempt to remove loading adview..");
      }
      mAdView.destroyAdView(true);
      MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Interstitial destroyed");
    }
    LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mMessageReceiver);
  }

  @Nullable
  @Override
  protected LifecycleListener getLifecycleListener() {
    return null;
  }

  @NonNull
  @Override
  protected String getAdNetworkId() {
    return adUnitId;
  }

  @Override
  protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity,
      @NonNull AdData adData) throws Exception {
    return false;
  }

  @Override
  protected void load(@NonNull final Context context, @NonNull AdData adData) throws Exception {
    LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver,
        new IntentFilter(APPMONET_BROADCAST));

    sdkManager = SdkManager.get();
    if (sdkManager == null) {
      logger.warn("AppMonet SDK Has not been initialized. Unable to serve ads.");
      onMoPubError(MoPubErrorCode.NETWORK_NO_FILL);
      return;
    }
    Map<String, String> extras = adData.getExtras();
    final AdSize adSize = new AdSize(INTERSTITIAL_WIDTH, INTERSTITIAL_HEIGHT);
    adUnitId = CustomEventUtil.getAdUnitId(extras, adSize);

    if (adUnitId == null || adUnitId.isEmpty()) {
      logger.debug("no adUnit/tagId: floor line item configured incorrectly");
      onMoPubError(MoPubErrorCode.NETWORK_NO_FILL);
      return;
    }

    sdkManager.getAuctionManager().trackRequest(adUnitId,
        WebViewUtils.generateTrackingSource(AdType.INTERSTITIAL));

    SdkConfigurations configurations = sdkManager.getSdkConfigurations();
    BidResponse headerBiddingBid = null;
    if (extras.containsKey(BIDS_KEY) && extras.get(BIDS_KEY) != null) {
      headerBiddingBid = BidResponse.Mapper.from(new JSONObject(extras.get(BIDS_KEY)));
    }

    double floorCpm = getServerExtraCpm(extras, configurations.getDouble(DEFAULT_MEDIATION_FLOOR));
    if (headerBiddingBid == null) {
      headerBiddingBid =
          sdkManager.getAuctionManager().getMediationManager().getBidForMediation(adUnitId, floorCpm);
    }
    MediationManager mediationManager =
        sdkManager.getAuctionManager().getMediationManager();
    mediationManager.getBidReadyForMediationAsync(headerBiddingBid, adUnitId, adSize,
        AdType.INTERSTITIAL, floorCpm, new Callback<BidResponse>() {
          @Override
          public void onSuccess(BidResponse response) {
            mContext = context;
            bidResponse = response;

            AdServerBannerListener listener =
                new MonetInterstitialListener(mLoadListener, mInteractionListener, adUnitId,
                    context, sdkManager);
            if (bidResponse.getInterstitial() != null && bidResponse.getInterstitial()
                .getTrusted()) {
              listener.onAdLoaded(null);
              return;
            }
            mAdView = BidRenderer.renderBid(context, sdkManager, bidResponse, null, listener);
            if (mAdView == null) {
              logger.error("unexpected: could not generate the adView");
              onMoPubError(MoPubErrorCode.INTERNAL_ERROR);
            }
          }

          @Override
          public void onError() {
            onMoPubError(MoPubErrorCode.NETWORK_NO_FILL);
          }
        });
  }

  private void onMoPubError(MoPubErrorCode error) {
    MoPubLog.log(MoPubLog.AdapterLogEvent.LOAD_FAILED, ADAPTER_NAME, error.getIntCode(), error);
    if (mLoadListener != null) {
      mLoadListener.onAdLoadFailed(error);
    }
  }

  private double getServerExtraCpm(Map<String, String> serverExtras, double defaultValue) {
    if (serverExtras == null || !serverExtras.containsKey(SERVER_EXTRA_CPM_KEY)) {
      return defaultValue;
    }

    try {
      return Double.parseDouble(serverExtras.get(SERVER_EXTRA_CPM_KEY));
    } catch (NumberFormatException e) {
      // do nothing
    }

    return defaultValue;
  }
}
