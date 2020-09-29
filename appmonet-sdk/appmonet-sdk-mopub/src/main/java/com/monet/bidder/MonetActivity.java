package com.monet.bidder;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import static com.monet.bidder.Constants.APPMONET_BROADCAST_MESSAGE;
import static com.monet.bidder.Constants.INTERSTITIAL_ACTIVITY_BROADCAST;
import static com.monet.bidder.Constants.INTERSTITIAL_ACTIVITY_CLOSE;


public class MonetActivity extends TrustedInterstitialActivity {
  private static final MonetLogger logger = new MonetLogger("MonetActivity");
  private static final String BID_ID = "bidId";
  private static final String APPMONET_BROADCAST = "appmonet-broadcast";
  private static final String INTERNAL_ERROR = "internal_error";
  static final String INTERSTITIAL_SHOWN = "interstitial_shown";

  private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String message = intent.getStringExtra(APPMONET_BROADCAST_MESSAGE);
      if (INTERSTITIAL_ACTIVITY_CLOSE.equals(message)) {
        finish();
      }
    }
  };

  static void start(@NonNull Context context, @NonNull SdkManager sdkManager,
                    @Nullable String uuid, @Nullable String url)
      throws ActivityNotFoundException {
    Intent intent = new Intent(context, MonetActivity.class);
    intent.putExtra(UUID, uuid);
    intent.putExtra(URL, url);
    try {
      context.startActivity(intent);
    } catch (ActivityNotFoundException e) {
      sdkManager.getAuctionManager().auctionWebView.trackEvent("integration_error",
          "missing_interstitial_activity", "onStart", 0f, 0L);
      logger.error("Unable to create activity. Not defined in AndroidManifest.xml. " +
          "Please refer to https://docs.appmonet.com/ for integration infomration.\n" + e.getMessage());
      throw e;
    }
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    SdkManager sdkManager = SdkManager.get();
    if (sdkManager == null) {
      LocalBroadcastManager.getInstance(this)
          .sendBroadcast(getBroadcastIntent(INTERNAL_ERROR));
      return;
    }
    try {
      LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver,
          new IntentFilter(INTERSTITIAL_ACTIVITY_BROADCAST));
      if (uuid != null) {
        View view = new InterstitialView(this, sdkManager, uuid);
        setContentView(view, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
      }
      LocalBroadcastManager.getInstance(this)
          .sendBroadcast(getBroadcastIntent(INTERSTITIAL_SHOWN));
    } catch (Exception e) {
      LocalBroadcastManager.getInstance(this)
          .sendBroadcast(getBroadcastIntent(INTERNAL_ERROR));
    }
  }

  @Override
  public void onBackPressed() {
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
  }

  private Intent getBroadcastIntent(String message) {
    return new Intent(APPMONET_BROADCAST)
        .putExtra("message", message);
  }
}
