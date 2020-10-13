package com.monet.bidder

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.monet.bidder.Constants.APPMONET_BROADCAST
import com.monet.bidder.Constants.APPMONET_BROADCAST_MESSAGE
import com.monet.bidder.Constants.Interstitial.AD_CONTENT_INTERSTITIAL
import com.monet.bidder.Constants.Interstitial.AD_UUID_INTERSTITIAL
import com.monet.bidder.Constants.Interstitial.BID_ID_INTERSTITIAL
import com.monet.bidder.Icons.CLOSE
import com.monet.bidder.Icons.Companion.asIntPixels
import com.monet.bidder.adview.AdViewManager
import org.json.JSONException
import java.lang.ref.WeakReference

@SuppressLint("ViewConstructor")
class InterstitialView(
  context: Context,
  private val sdkManager: BaseManager,
  uuid: String?
) : RelativeLayout(context) {
  private val preferences: Preferences
  private val analyticsTracker: InterstitialAnalyticsTracker? = null
  private var activityReference: WeakReference<Context>? = null
  private val adapter: InterstitialAdapter? = null
  @Throws(JSONException::class) fun setupSingleAd(
    context: Context,
    uuid: String
  ) {
    activityReference = WeakReference(context)
    val adViewManager = sdkManager.auctionManager.adViewPoolManager.getAdViewByUuid(uuid)
    createSingleAdView(uuid, activityReference!!.get())
    setupView(
        adViewManager == null || adViewManager.bid == null || adViewManager.bid!!.interstitial == null
            || adViewManager.bid!!.interstitial!!.close
    )
  }

  private fun createSingleAdView(
    uuid: String,
    activity: Any?
  ) {
    val adViewManager = sdkManager.auctionManager.adViewPoolManager.getAdViewByUuid(uuid)
    val adViewParams = getAdViewParameters(adViewManager)
    adViewManager!!.adView.layoutParams = FrameLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT
    )
    addView(adViewManager.adView.parent as View, adViewParams)
  }

  private fun setupView(showClose: Boolean) {
    if (activityReference!!.get() is Activity) {
      val activity = activityReference!!.get() as Activity?
      if (activity != null) {
        activity.requestWindowFeature(Window.FEATURE_NO_TITLE)
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        val actionBar = activity.actionBar
        actionBar?.hide()
        setBackgroundColor(Color.parseColor("#000000"))
        if (showClose) {
          val closeButton = createCloseButton(activity)
          addView(closeButton)
        }
      }
    } else {
      LocalBroadcastManager.getInstance(activityReference!!.get()!!)
          .sendBroadcast(
              Intent(APPMONET_BROADCAST)
                  .putExtra("message", "internal_error")
          )
    }
  }

  private fun getAdViewParameters(adViewManager: AdViewManager?): LayoutParams {
    if (adViewManager != null && adViewManager.adView.parent != null && adViewManager.adView.parent.parent != null) {
      (adViewManager.adView.parent.parent as ViewGroup).removeView(adViewManager.adView)
    }
    return LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT
    )
  }

  private fun createCloseButton(activity: Activity): ImageView {
    val drawable = CLOSE.createDrawable(activity)
    val bitmap = (drawable as BitmapDrawable?)!!.bitmap
    // Scale it to 50 x 50
    val d: Drawable = BitmapDrawable(
        resources,
        Bitmap.createScaledBitmap(
            bitmap, asIntPixels(30f, activity),
            asIntPixels(30f, activity), true
        )
    )
    val closeButton = ImageView(activity)
    closeButton.setImageDrawable(d)
    closeButton.setOnClickListener { v: View? ->
      LocalBroadcastManager.getInstance(
          context
      ).sendBroadcast(
          Intent(APPMONET_BROADCAST)
              .putExtra(
                  APPMONET_BROADCAST_MESSAGE,
                  "interstitial_dismissed"
              )
      )
    }
    val closeButtonParams = LayoutParams(
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
    )
    closeButtonParams.addRule(ALIGN_PARENT_RIGHT)
    closeButton.setPadding(20, 20, 20, 20)
    closeButton.layoutParams = closeButtonParams
    closeButton.bringToFront()
    return closeButton
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    analyticsTracker?.interstitialViewAttached()
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    preferences.remove(AD_CONTENT_INTERSTITIAL)
    preferences.remove(BID_ID_INTERSTITIAL)
    preferences.remove(AD_UUID_INTERSTITIAL)
    if (adapter != null && analyticsTracker != null) {
      adapter.cleanup()
      analyticsTracker.interstitialViewDetached()
      analyticsTracker.send()
    }
  }

  init {
    preferences = sdkManager.preferences
    setupSingleAd(context, preferences.getPref(AD_UUID_INTERSTITIAL, ""))
  }
}