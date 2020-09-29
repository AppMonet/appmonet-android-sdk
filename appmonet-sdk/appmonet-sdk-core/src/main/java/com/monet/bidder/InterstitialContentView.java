package com.monet.bidder;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.concurrent.atomic.AtomicInteger;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

/**
 * Created by jose on 4/6/18.
 */

class InterstitialContentView extends RelativeLayout {
  final MonetVideoView videoView;
  final RelativeLayout contentLayout;
  int contentTitleHeight;
  ImageView imageView;
  View dimView;
  TextView contentTitle;

  InterstitialContentView(Context context) {
    super(context);
    videoView = createVideoView(context);
    imageView = createThumbnail(context);
    dimView = createDimView(context);
    contentLayout = new RelativeLayout(context);
    contentTitle = new TextView(context);
    LayoutParams params = new LayoutParams(MATCH_PARENT, MATCH_PARENT);
    contentTitle.setId(View.generateViewId());

    params.addRule(BELOW, contentTitle.getId());

    contentTitleHeight = convertDpToPx(30);
    LayoutParams titleParams = new LayoutParams(MATCH_PARENT, contentTitleHeight);

    contentTitle.setLayoutParams(titleParams);
    contentTitle.setTextColor(Color.WHITE);

    contentLayout.addView(imageView, setViewParams());
    contentLayout.addView(videoView, setViewParams());
    titleParams.addRule(ALIGN_PARENT_TOP);

    addView(contentTitle, titleParams);
    addView(contentLayout, params);
    addView(dimView, setViewParams());
  }

  void setDarkView() {
    dimView.setVisibility(View.VISIBLE);
    dimView.bringToFront();
  }

  void removeDarkView() {
    dimView.setVisibility(View.GONE);
  }

  void setTitleContent(String title) {
    if (title.equals("null")) {
      contentTitleHeight = 0;
      removeView(contentTitle);
    } else {
      contentTitle.setText(title);
    }
  }

  private ImageView createThumbnail(Context context) {
    ImageView view = new ImageView(context);
    view.setAdjustViewBounds(true);
    view.setVisibility(View.GONE);
    view.setScaleType(ImageView.ScaleType.FIT_CENTER);
    return view;
  }

  private MonetVideoView createVideoView(Context context) {
    return new MonetVideoView(context);
  }

  private View createDimView(Context context) {
    View view = new View(context);
    view.setBackgroundColor(Color.parseColor("#D8000000"));
    view.setVisibility(View.VISIBLE);
    return view;
  }

  private LayoutParams setViewParams() {
    return new LayoutParams(MATCH_PARENT, MATCH_PARENT);
  }

  private int convertDpToPx(int dp) {
    return Math.round(
        dp * (getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT));
  }
}

