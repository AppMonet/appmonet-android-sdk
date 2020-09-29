package com.monet.bidder;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

import static com.monet.bidder.Constants.BIDS_KEY;
import static com.monet.bidder.Constants.Configurations.DEFAULT_MEDIATION_FLOOR;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.ConsentLogEvent.CUSTOM;

/**
 * Created by jose on 2/1/18.
 */

public class CustomEventBanner extends BaseAd implements AppMonetViewListener {
  private static final Logger sLogger = new Logger("CustomEventBanner");
  private AppMonetViewLayout mAdView;
  private MopubBannerListener listener;
  private String adUnitID = "ZONE_ID";
  static final String ADAPTER_NAME = CustomEventBanner.class.getSimpleName();

  @Override
  protected void onInvalidate() {
    if (mAdView != null) {
      if (mAdView.getAdViewState() != AdViewManager.AdViewState.AD_RENDERED) {
        sLogger.warn("attempt to remove loading adview..");
      }
      mAdView.destroyAdView(true);
      MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Banner destroyed");
    }

    if (listener != null && listener.moPubAdViewContainer != null
        && listener.moPubAdViewContainer instanceof FloatingAdView) {
      ((FloatingAdView) listener.moPubAdViewContainer).removeAllViews();
    }
  }

  @Nullable
  @Override
  protected LifecycleListener getLifecycleListener() {
    return null;
  }

  @NonNull
  @Override
  protected String getAdNetworkId() {
    return adUnitID;
  }

  @Override
  protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity,
      @NonNull AdData adData) throws Exception {
    return false;
  }

  @Override
  protected void load(@NonNull Context context, @NonNull AdData adData) throws Exception {
    Map<String, String> extras = adData.getExtras();
    AdSize adSize = new AdSize((adData.getAdWidth() != null) ? adData.getAdWidth() : 0,
        (adData.getAdHeight() != null) ? adData.getAdHeight() : 0);

    adUnitID = CustomEventUtil.getAdUnitId(extras, adSize);
    SdkManager sdkManager = SdkManager.get();
    //    // check if it's null first
    if (sdkManager == null) {
      sLogger.warn("AppMonet SDK Has not been initialized. Unable to serve ads.");
      onMoPubError(MoPubErrorCode.INTERNAL_ERROR);
      return;
    }

    if (adUnitID == null || adUnitID.isEmpty()) {
      sLogger.debug("no adUnit/tagId: floor line item configured incorrectly");
      onMoPubError(MoPubErrorCode.NETWORK_NO_FILL);
      return;
    }
    sdkManager.getAuctionManager().trackRequest(adUnitID,
        WebViewUtils.generateTrackingSource(AdType.BANNER));
    SdkConfigurations configurations = sdkManager.getSdkConfigurations();

    //    // try to get the bid from the localExtras
    //    // thanks to localExtras we don't need to serialize/deserialize

    BidResponse headerBiddingBid = null;
    if (extras.containsKey(BIDS_KEY) && extras.get(BIDS_KEY) != null) {
      headerBiddingBid = BidResponse.Mapper.from(new JSONObject(extras.get(BIDS_KEY)));
    }
    double floorCpm = CustomEventUtil.getServerExtraCpm(
        extras, configurations.getDouble(DEFAULT_MEDIATION_FLOOR));
    if (headerBiddingBid == null) {
      sLogger.debug("checking store for precached bids");
      headerBiddingBid =
          sdkManager.getAuctionManager().getMediationManager().getBidForMediation(adUnitID, floorCpm);
    }
    MediationManager mediationManager = sdkManager.getAuctionManager().getMediationManager();
    try {
      BidResponse bid = mediationManager.getBidReadyForMediation(headerBiddingBid, adUnitID, adSize,
          AdType.BANNER, floorCpm, true);
      listener =
          new MopubBannerListener(sdkManager, mLoadListener, mInteractionListener, bid, adUnitID,
              this);
      // this will set adview
      mAdView = BidRenderer.renderBid(context, sdkManager, bid, adSize, listener);

      if (mAdView == null) {
        sLogger.error("unexpected: could not generate the adView");
        onMoPubError(MoPubErrorCode.INTERNAL_ERROR);
      }
    } catch (MediationManager.NoBidsFoundException e) {
      onMoPubError(MoPubErrorCode.NETWORK_NO_FILL);
    } catch (MediationManager.NullBidException e) {
      onMoPubError(MoPubErrorCode.INTERNAL_ERROR);
    }
  }

  @Nullable
  @Override
  protected View getAdView() {
    return mAdView;
  }

  @Override
  public void onAdRefreshed(View view) {
    mAdView = (AppMonetViewLayout) view;
  }

  @Override
  public AppMonetViewLayout getCurrentView() {
    return mAdView;
  }

  private void onMoPubError(MoPubErrorCode error) {
    MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, error.getIntCode(), error);
    if (mLoadListener != null) {
      mLoadListener.onAdLoadFailed(error);
    }
  }
}
