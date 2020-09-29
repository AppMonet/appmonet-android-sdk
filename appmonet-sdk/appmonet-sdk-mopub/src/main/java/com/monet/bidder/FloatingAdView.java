package com.monet.bidder;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.monet.bidder.adview.AdView;
import com.monet.bidder.adview.AdViewPositioning;
import com.monet.bidder.bid.BidResponse;

class FloatingAdView extends FrameLayout {
  private ImageView closeButton;
  private FrameLayout viewContainer;
  private Runnable task;
  private final Handler handler = new Handler(Looper.getMainLooper());

  public FloatingAdView(SdkManager manager, Params params, Context context) {
    super(context);
    this.buildFloatingAd(manager, params);
    addView(RenderingUtils.generateBlankLayout(context,
        RenderingUtils.dpToPixels((params.width == null) ? 0 : params.width),
        RenderingUtils.dpToPixels((params.height == null) ? 0 : params.height)));
  }

  @Override
  public void removeAllViews() {
    super.removeAllViews();
    viewContainer.setVisibility(GONE);
    if (task != null) {
      handler.removeCallbacks(task);
    }
    if (this.getParent() != null) {
      ((ViewGroup) this.getParent()).removeView(this);
    }
    if (viewContainer != null && closeButton != null) {
      viewContainer.removeView(closeButton);
      closeButton.setOnClickListener(null);
    }
  }

  private void buildFloatingAd(SdkManager manager, final Params params) {
    viewContainer = (FrameLayout) params.view;
    GradientDrawable gd = new GradientDrawable();
    gd.setColor(Color.WHITE);
    gd.setCornerRadius(RenderingUtils.dpToPixels(5));
    viewContainer.setBackground(gd);
    viewContainer.setClipToPadding(false);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      viewContainer.setElevation(RenderingUtils.dpToPixels(5));
      viewContainer.setClipToOutline(true);
    }
    final AdView adView = (AdView) ((FrameLayout) params.view).getChildAt(0);
    AppMonetFloatingAdConfiguration floatingAdPosition = manager.getFloatingAdPosition(params.adUnit);
    FrameLayout rootWindowLayout = (FrameLayout) manager.currentActivity.get().getWindow().getDecorView().getRootView();
    AdViewPositioning positioning = RenderingUtils.getScreenPositioning(rootWindowLayout,
        floatingAdPosition.positionSettings, params.height, params.width);

    LayoutParams layoutParams =
        new LayoutParams(positioning.getWidth(), positioning.getHeight());
    layoutParams.setMargins(positioning.getX(), positioning.getY(), 0, 0);

    rootWindowLayout.addView(viewContainer, layoutParams);
    closeButton = RenderingUtils.getBase64ImageView(manager.currentActivity.get(), Icons.CLOSE_FLOATING_ADS);
    closeButton.setOnClickListener(v -> {
      removeAllViews();
      adView.destroy(true);
    });
    viewContainer.addView(closeButton);
    setupTask(adView, params.durationInMillis);
  }

  private void setupTask(final AdView adView, int durationInMillis) {
    task = () -> {
      removeAllViews();
      adView.destroy(true);
    };
    handler.postDelayed(task, durationInMillis);
  }

  static class Params {
    private final View view;
    private final Integer width;
    private final Integer height;
    private final String adUnit;
    private final int durationInMillis;

    Params(SdkManager manager, View view, BidResponse bid, Integer width, Integer height, String adUnit) {
      this.view = view;
      this.width = width;
      this.height = height;
      this.adUnit = adUnit;
      AppMonetFloatingAdConfiguration floatingAdPosition = manager.getFloatingAdPosition(adUnit);
      if (floatingAdPosition != null) {
        if (bid.getDuration() > 0) {
          durationInMillis = Math.min(floatingAdPosition.maxAdDuration, bid.getDuration());
        } else {
          durationInMillis = floatingAdPosition.maxAdDuration;
        }
      } else {
        durationInMillis = AppMonetFloatingAdConfiguration.DEFAULT_DURATION;
      }
    }
  }
}
