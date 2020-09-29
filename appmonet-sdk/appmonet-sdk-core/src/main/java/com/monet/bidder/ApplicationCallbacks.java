package com.monet.bidder;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import com.monet.bidder.auction.AuctionManager;

/**
 * Listen to application lifecycle events
 */
class ApplicationCallbacks implements Application.ActivityLifecycleCallbacks {
  private final AuctionManager auctionManager;
  private int mActiveActivities = 0;

  ApplicationCallbacks(AuctionManager auctionManager) {
    this.auctionManager = auctionManager;
  }

  private void trackAppState(String appStateChange, String identifier) {
    auctionManager.trackAppState(appStateChange, identifier);
  }

  @Override
  public void onActivityCreated(Activity activity, Bundle bundle) {
    trackAppState(Constants.JSAppStates.ACTIVITY_CREATED, activity.getLocalClassName());
  }

  @Override
  public void onActivityStarted(Activity activity) {
    trackAppState(Constants.JSAppStates.ACTIVITY_STARTED, activity.getLocalClassName());
    if (mActiveActivities == 0) {
      trackAppState(Constants.JSAppStates.FOREGROUND, "app");
    }
    mActiveActivities += 1;
  }

  @Override
  public void onActivityResumed(Activity activity) {
    trackAppState(Constants.JSAppStates.ACTIVITY_RESUMED, activity.getLocalClassName());
  }

  @Override
  public void onActivityPaused(Activity activity) {
    trackAppState(Constants.JSAppStates.ACTIVITY_PAUSED, activity.getLocalClassName());
  }

  @Override
  public void onActivityStopped(Activity activity) {
    trackAppState(Constants.JSAppStates.ACTIVITY_STOPPED, activity.getLocalClassName());
    mActiveActivities -= 1;
    if (mActiveActivities == 0) {
      trackAppState(Constants.JSAppStates.BACKGROUND, "app");
    }
  }

  @Override
  public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {
    //not implemented
  }

  @Override
  public void onActivityDestroyed(Activity activity) {
    trackAppState(Constants.JSAppStates.ACTIVITY_DESTROYED, activity.getLocalClassName());
  }
}
