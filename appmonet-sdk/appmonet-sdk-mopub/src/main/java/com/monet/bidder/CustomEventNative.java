package com.monet.bidder;

import android.content.Context;

import androidx.annotation.NonNull;

import com.monet.bidder.bid.BidRenderer;
import com.monet.bidder.bid.BidResponse;
import com.mopub.nativeads.NativeErrorCode;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import static com.monet.bidder.Constants.BIDS_KEY;

/**
 * Class called by Mopub which triggers our logic for serving ads.
 */
public class CustomEventNative extends com.mopub.nativeads.CustomEventNative {

  private static Logger logger = new Logger("CustomEventNative");

  @Override
  protected void loadNativeAd(@NonNull Context context,
      @NonNull CustomEventNativeListener customEventNativeListener,
      @NonNull Map<String, Object> localExtras,
      @NonNull Map<String, String> serverExtras) {
    logger.debug("Loading Native Ad");
    AdSize adSize = new AdSize(320, 250);
    String adUnitId = CustomEventUtil.getAdUnitId(serverExtras, localExtras, adSize);
    SdkManager sdkManager = SdkManager.get();
    if (sdkManager == null) {
      customEventNativeListener.onNativeAdFailed(NativeErrorCode.UNSPECIFIED);
      return;
    }

    if (adUnitId == null) {
      customEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_NO_FILL);
      return;
    }
    sdkManager.getAuctionManager().trackRequest(adUnitId,
        WebViewUtils.generateTrackingSource(AdType.NATIVE));
    BidResponse headerBiddingBid = null;

    try {
      if (localExtras.containsKey(BIDS_KEY) && localExtras.get(BIDS_KEY) != null) {
        headerBiddingBid =
            BidResponse.Mapper.from(new JSONObject((String) localExtras.get(BIDS_KEY)));
      }
    } catch (JSONException e) {
      // Exception
    }

    double serverExtraCpm = CustomEventUtil.getServerExtraCpm(serverExtras, 0);
    if (headerBiddingBid == null) {
      headerBiddingBid = sdkManager.getAuctionManager().getMediationManager()
          .getBidForMediation(adUnitId, serverExtraCpm);
    }
    MediationManager mediationManager =
        new MediationManager(sdkManager, sdkManager.getAuctionManager().getBidManager());
    try {
      BidResponse bid = mediationManager.getBidReadyForMediation(headerBiddingBid, adUnitId, adSize,
          AdType.NATIVE, serverExtraCpm, true);
      if (!bid.getExtras().isEmpty()) {
        for (Map.Entry<String, Object> kvp : bid.getExtras().entrySet()) {
          Object value = kvp.getValue();
          if (value == null) continue;

          // add all the data into server extras
          serverExtras.put(kvp.getKey(), value.toString());
        }
      }

      AdServerBannerListener mListener =
          new MopubNativeListener(context, customEventNativeListener, serverExtras);
      AppMonetViewLayout mAdView = BidRenderer.renderBid(context, sdkManager, bid, null, mListener);
      if (mAdView == null) {
        customEventNativeListener.onNativeAdFailed(NativeErrorCode.UNSPECIFIED);
      }
    } catch (MediationManager.NoBidsFoundException e) {
      customEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_NO_FILL);
    } catch (MediationManager.NullBidException e) {
      customEventNativeListener.onNativeAdFailed(NativeErrorCode.UNSPECIFIED);
    }
  }
}
