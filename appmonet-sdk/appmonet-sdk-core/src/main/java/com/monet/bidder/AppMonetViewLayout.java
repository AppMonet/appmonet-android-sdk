package com.monet.bidder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.monet.bidder.adview.AdViewManager;
import com.monet.bidder.adview.AdViewPoolManagerCallback;
import com.monet.bidder.auction.AuctionManagerCallback;
import com.monet.bidder.bid.BidResponse;

import java.lang.ref.WeakReference;

import static com.monet.bidder.bid.BidResponse.Constant.FLOATING_AD_TYPE;

@SuppressLint("ViewConstructor")
public class AppMonetViewLayout extends FrameLayout {
  private final AdViewPoolManagerCallback adViewPoolManagerCallback;
  private final AuctionManagerCallback auctionManager;
  private WeakReference<AdViewManager> adViewManager;
  private Handler handler;
  private Runnable runnable;
  private ViewGroup parent;

  public AppMonetViewLayout(@NonNull Context context,
                            @NonNull AdViewPoolManagerCallback adViewPoolManagerCallback,
                            @NonNull AuctionManagerCallback auctionManagerCallback,
                            AdViewManager adViewManager, AdSize adSize) {
    super(context);
    this.adViewManager = new WeakReference<>(adViewManager);
    this.adViewPoolManagerCallback = adViewPoolManagerCallback;
    this.auctionManager = auctionManagerCallback;
    addView(adViewManager.adView, getLayoutParams(adSize));
  }

  AdViewManager.AdViewState getAdViewState() {
    return adViewManager.get().getState();
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    parent = (ViewGroup) this.getParent();
  }

  String getAdViewUUID() {
    return adViewManager.get().getUuid();
  }

  void destroyAdView(boolean invalidate) {
    adViewManager.get().destroy(invalidate);
    cleanup();
  }

  @Override
  public void removeAllViews() {
    super.removeAllViews();
    cleanup();
  }

  public void activateRefresh(final BidResponse bid, final AdServerBannerListener listener) {
    handler = new Handler(Looper.getMainLooper());
    if (FLOATING_AD_TYPE.equals(bid.getAdType()) || bid.getRefresh() <= 1000) {
      return;
    }

    final String adUnit = bid.getAdUnitId();
    runnable = new Runnable() {
      @Override
      public void run() {
        BidResponse mediationBid = auctionManager.getBidWithFloorCpm(adUnit, 0);
        MediationManager mediationManager = auctionManager.getMediationManager();
        try {
          AdSize adSize = (mediationBid != null) ? new AdSize(mediationBid.getWidth(), mediationBid.getHeight()) : null;
          BidResponse nextBid = mediationManager.getBidReadyForMediation(mediationBid, adUnit, adSize,
              AdType.BANNER, 0, true);
          AdViewManager nextAdView = adViewPoolManagerCallback.request(nextBid);

          if (nextAdView == null) {
            handler.postDelayed(runnable, bid.getRefresh());
            return;
          }

          if (!nextAdView.isLoaded()) {
            nextAdView.load();
          }
          auctionManager.markBidAsUsed(bid);
          nextAdView.setAdRefreshed(true);
          nextAdView.setBid(nextBid);
          nextAdView.setBidForTracking(nextBid);
          nextAdView.setState(AdViewManager.AdViewState.AD_RENDERED, listener, getContext());
          // this is always done after the state change
          nextAdView.inject(nextBid);
        } catch (MediationManager.NoBidsFoundException | MediationManager.NullBidException e) {
          handler.postDelayed(runnable, bid.getRefresh());
        }
      }
    };
    handler.postDelayed(runnable, bid.getRefresh());
  }

  private LayoutParams getLayoutParams(AdSize adSize) {
    return new LayoutParams(
        adSize.getWidthInPixels(getContext()),
        adSize.getHeightInPixels(getContext()),
        Gravity.CENTER);
  }

  private void cleanup() {
    if (handler != null && runnable != null) {
      handler.removeCallbacks(runnable);
    }
    if (handler != null) {
      handler.removeCallbacksAndMessages(null);
    }
    runnable = null;
    handler = null;
    parent = null;
  }

  void swapViews(AppMonetViewLayout view, AdServerBannerListener listener) {
    if (view != this) {
      this.handler.removeCallbacksAndMessages(null);
      this.handler.removeCallbacks(runnable);
      view.parent = parent;
      parent.removeAllViews();
      parent.addView(view);
      parent.removeView(this);

      destroyAdView(true);
      parent = null;

    }
    listener.onAdRefreshed(view);
  }

  boolean isAdRefreshed() {
    return adViewManager.get().isAdRefreshed();
  }
}
