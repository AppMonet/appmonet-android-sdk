package com.monet.bidder;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.monet.bidder.adview.AdViewManager;
import java.lang.ref.WeakReference;
import org.json.JSONException;

import static com.monet.bidder.Constants.APPMONET_BROADCAST;
import static com.monet.bidder.Constants.APPMONET_BROADCAST_MESSAGE;
import static com.monet.bidder.Constants.Interstitial.AD_CONTENT_INTERSTITIAL;
import static com.monet.bidder.Constants.Interstitial.AD_UUID_INTERSTITIAL;
import static com.monet.bidder.Constants.Interstitial.BID_ID_INTERSTITIAL;

@SuppressLint("ViewConstructor")
public class InterstitialView extends RelativeLayout {
  private final BaseManager sdkManager;
  private Preferences preferences;
  private InterstitialAnalyticsTracker analyticsTracker;
  private WeakReference<Context> activityReference;
  private InterstitialAdapter adapter;

  void setupSingleAd(Context context, String uuid) throws JSONException {
    this.activityReference = new WeakReference<>(context);
    AdViewManager adViewManager = sdkManager.getAuctionManager().getAdViewPoolManager().getAdViewByUuid(uuid);
    createSingleAdView(uuid, activityReference.get());
    setupView((adViewManager == null
        || adViewManager.getBid() == null
        || adViewManager.getBid().getInterstitial() == null)
        || adViewManager.getBid().getInterstitial().getClose());
  }

  public InterstitialView(Context context, BaseManager sdkManager, String uuid)
      throws JSONException {
    super(context);
    this.sdkManager = sdkManager;
    preferences = sdkManager.getPreferences();
    setupSingleAd(context, preferences.getPref(AD_UUID_INTERSTITIAL, ""));
  }

  private void createSingleAdView(String uuid, Object activity) {
    AdViewManager adViewManager = sdkManager.getAuctionManager().getAdViewPoolManager().getAdViewByUuid(uuid);
    LayoutParams adViewParams = getAdViewParameters(adViewManager);
    adViewManager.adView.setLayoutParams(
        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT));
    addView((View) adViewManager.adView.getParent(), adViewParams);
  }

  private void setupView(boolean showClose) {
    if (activityReference.get() instanceof Activity) {
      final Activity activity = (Activity) activityReference.get();
      if (activity != null) {
        activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ActionBar actionBar = activity.getActionBar();
        if (actionBar != null) {
          actionBar.hide();
        }
        setBackgroundColor(Color.parseColor("#000000"));
        if (showClose) {
          ImageView closeButton = createCloseButton(activity);
          addView(closeButton);
        }
      }
    } else {
      LocalBroadcastManager.getInstance(activityReference.get())
          .sendBroadcast(new Intent(APPMONET_BROADCAST)
              .putExtra("message", "internal_error"));
    }
  }

  private LayoutParams getAdViewParameters(@Nullable AdViewManager adViewManager) {
    if (adViewManager != null
        && adViewManager.adView.getParent() != null
        && adViewManager.adView.getParent().getParent() != null) {
      ((ViewGroup) adViewManager.adView.getParent().getParent()).removeView(adViewManager.adView);
    }

    return new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT);
  }

  private ImageView createCloseButton(final Activity activity) {
    Drawable drawable = Icons.CLOSE.createDrawable(activity);

    Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
    // Scale it to 50 x 50
    Drawable d = new BitmapDrawable(getResources(),
        Bitmap.createScaledBitmap(bitmap, Icons.asIntPixels(30F, activity),
            Icons.asIntPixels(30F, activity), true));
    ImageView closeButton = new ImageView(activity);
    closeButton.setImageDrawable(d);
    closeButton.setOnClickListener(
        v -> LocalBroadcastManager.getInstance(getContext()).sendBroadcast(
            new Intent(APPMONET_BROADCAST)
                .putExtra(APPMONET_BROADCAST_MESSAGE,
                    "interstitial_dismissed")));
    LayoutParams closeButtonParams =
        new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
    closeButtonParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
    closeButton.setPadding(20, 20, 20, 20);

    closeButton.setLayoutParams(closeButtonParams);
    closeButton.bringToFront();
    return closeButton;
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (analyticsTracker != null) {
      analyticsTracker.interstitialViewAttached();
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    preferences.remove(AD_CONTENT_INTERSTITIAL);
    preferences.remove(BID_ID_INTERSTITIAL);
    preferences.remove(AD_UUID_INTERSTITIAL);

    if (adapter != null && analyticsTracker != null) {
      adapter.cleanup();
      analyticsTracker.interstitialViewDetached();
      analyticsTracker.send();
    }
  }
}
