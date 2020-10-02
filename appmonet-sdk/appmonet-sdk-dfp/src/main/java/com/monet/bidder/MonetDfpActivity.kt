package com.monet.bidder;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import org.json.JSONException;

import static com.monet.bidder.Constants.APPMONET_BROADCAST_MESSAGE;
import static com.monet.bidder.Constants.INTERSTITIAL_ACTIVITY_BROADCAST;
import static com.monet.bidder.Constants.INTERSTITIAL_ACTIVITY_CLOSE;

public class MonetDfpActivity extends TrustedInterstitialActivity {
  private static final MonetLogger logger = new MonetLogger("MonetDfpActivity");
  private static final String BID_ID = "bidId";
  private static final String APPMONET_BROADCAST = "appmonet-broadcast";
  private static final String INTERNAL_ERROR = "internal_error";
  private SdkManager sdkManager;

  private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String message = intent.getStringExtra(APPMONET_BROADCAST_MESSAGE);
      if (INTERSTITIAL_ACTIVITY_CLOSE.equals(message)) {
        finish();
      }
    }
  };

  public static void start(@NonNull Context context, @NonNull SdkManager sdkManager,
      @Nullable String uuid, @Nullable String url) {
    Intent intent = new Intent(context, MonetDfpActivity.class);
    intent.putExtra(UUID, uuid);
    intent.putExtra(URL, url);
    startActivity(context, sdkManager, intent);
  }

  private static void startActivity(@NonNull Context context, @NonNull SdkManager sdkManager,
      @NonNull Intent intent) {
    try {
      context.startActivity(intent);
    } catch (ActivityNotFoundException e) {
      sdkManager.getAuctionManager().trackEvent("integration_error",
          "missing_interstitial_activity", "onStart", 0f, 0L);
      logger.error("Unable to create activity. Not defined in AndroidManifest.xml.");
    }
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    //noinspection
    sdkManager = SdkManager.get();

    if (sdkManager == null) {
      LocalBroadcastManager.getInstance(this)
          .sendBroadcast(getBroadcastIntent(INTERNAL_ERROR));
      return;
    }

    try {
      LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver,
          new IntentFilter(INTERSTITIAL_ACTIVITY_BROADCAST));
      if (uuid != null) {
        View view;
        if (getIntent().getStringExtra(BID_ID) == null) {
          view = createSingleAdView();
        } else {
          view = createInteractiveView();
        }

        if (view == null) {
          LocalBroadcastManager.getInstance(this)
              .sendBroadcast(getBroadcastIntent(INTERNAL_ERROR));
          return;
        }

        setContentView(view, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT));
      }
    } catch (Exception e) {
      LocalBroadcastManager.getInstance(this)
          .sendBroadcast(getBroadcastIntent(INTERNAL_ERROR));
    }
  }

  @Override
  public void onBackPressed() {
    // do nothing
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
  }

  @Nullable
  private View createInteractiveView() {
    try {
      return new InterstitialView(this, sdkManager, getIntent().getStringExtra(UUID));
    } catch (JSONException e) {
      return null;
    }
  }

  @Nullable
  private View createSingleAdView() {
    try {
      return new InterstitialView(this, sdkManager, getIntent().getStringExtra(UUID));
    } catch (JSONException e) {
      return null;
    }
  }

  private static Intent getBroadcastIntent(@NonNull String message) {
    return new Intent(APPMONET_BROADCAST)
        .putExtra("message", message);
  }
}
