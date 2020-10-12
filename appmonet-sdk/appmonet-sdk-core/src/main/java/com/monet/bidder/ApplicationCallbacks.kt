package com.monet.bidder

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import com.monet.bidder.Constants.JSAppStates
import com.monet.bidder.auction.AuctionManager

/**
 * Listen to application lifecycle events
 */
internal class ApplicationCallbacks(private val auctionManager: AuctionManager) :
    ActivityLifecycleCallbacks {
  private var mActiveActivities = 0
  private fun trackAppState(
    appStateChange: String,
    identifier: String
  ) {
    auctionManager.trackAppState(appStateChange, identifier)
  }

  override fun onActivityCreated(
    activity: Activity,
    bundle: Bundle
  ) {
    trackAppState(JSAppStates.ACTIVITY_CREATED, activity.localClassName)
  }

  override fun onActivityStarted(activity: Activity) {
    trackAppState(JSAppStates.ACTIVITY_STARTED, activity.localClassName)
    if (mActiveActivities == 0) {
      trackAppState(JSAppStates.FOREGROUND, "app")
    }
    mActiveActivities += 1
  }

  override fun onActivityResumed(activity: Activity) {
    trackAppState(JSAppStates.ACTIVITY_RESUMED, activity.localClassName)
  }

  override fun onActivityPaused(activity: Activity) {
    trackAppState(JSAppStates.ACTIVITY_PAUSED, activity.localClassName)
  }

  override fun onActivityStopped(activity: Activity) {
    trackAppState(JSAppStates.ACTIVITY_STOPPED, activity.localClassName)
    mActiveActivities -= 1
    if (mActiveActivities == 0) {
      trackAppState(JSAppStates.BACKGROUND, "app")
    }
  }

  override fun onActivitySaveInstanceState(
    activity: Activity,
    bundle: Bundle
  ) {
    //not implemented
  }

  override fun onActivityDestroyed(activity: Activity) {
    trackAppState(JSAppStates.ACTIVITY_DESTROYED, activity.localClassName)
  }
}