package com.monet.bidder

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.monet.bidder.FloatingAdRenderUtil.getScreenPositioning
import com.monet.bidder.Icons.CLOSE_FLOATING_ADS
import com.monet.bidder.adview.AdView
import com.monet.bidder.adview.AdViewPositioning
import com.monet.bidder.bid.BidResponse
import kotlin.math.min

@SuppressLint("ViewConstructor")
internal class FloatingAdView(
  private val activity: Activity,
  manager: SdkManager,
  params: Params,
  context: Context
) : FrameLayout(
    context
) {
  private var closeButton: ImageView? = null
  private val viewContainer: FrameLayout = params.view as FrameLayout

  private var task: Runnable? = null
  override fun removeAllViews() {
    super.removeAllViews()
    viewContainer.visibility = GONE
    task?.let {
      handler.removeCallbacks(it)
    }
    if (this.parent != null) {
      (this.parent as ViewGroup).removeView(this)
    }
    if (closeButton != null) {
      viewContainer.removeView(closeButton)
      closeButton!!.setOnClickListener(null)
    }
  }

  private fun buildFloatingAd(
    manager: SdkManager,
    params: Params
  ) {
    val gd = GradientDrawable()
    gd.setColor(Color.WHITE)
    gd.cornerRadius = RenderingUtils.dpToPixels(5).toFloat()
    viewContainer.background = gd
    viewContainer.clipToPadding = false
    if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
      viewContainer.elevation = RenderingUtils.dpToPixels(5).toFloat()
      viewContainer.clipToOutline = true
    }
    val adView = viewContainer.getChildAt(0) as AdView
    val floatingAdPosition = manager.getFloatingAdPosition(params.adUnit)
    val rootWindowLayout =
      manager.currentActivity?.get()?.window?.decorView?.rootView as FrameLayout?
    floatingAdPosition?.let {

//    val (x, y, width, height) = getScreenPositioning(
      rootWindowLayout?.let {
        val adViewPositioning = getScreenPositioning(
            rootWindowLayout,
            floatingAdPosition.positionSettings, params.height, params.width
        )
        val layoutParams = LayoutParams(width, height)
        layoutParams.setMargins(adViewPositioning.x, adViewPositioning.y, 0, 0)
        rootWindowLayout.addView(viewContainer, layoutParams)
        closeButton =
          RenderingUtils.getBase64ImageView(activity, CLOSE_FLOATING_ADS)
        closeButton?.setOnClickListener {
          removeAllViews()
          adView.destroy(true)
        }
        viewContainer.addView(closeButton)
        setupTask(adView, params.durationInMillis)
      }
    }
  }

  private fun setupTask(
    adView: AdView,
    durationInMillis: Int
  ) {
    task = Runnable {
      removeAllViews()
      adView.destroy(true)
    }
    task?.let {
      handler.postDelayed(it, durationInMillis.toLong())
    }
  }

  internal class Params(
    manager: SdkManager,
    val view: View,
    bid: BidResponse,
    val width: Int?,
    val height: Int?,
    val adUnit: String
  ) {
    var durationInMillis = 0

    init {
      val floatingAdPosition = manager.getFloatingAdPosition(
          adUnit
      )
      durationInMillis = if (floatingAdPosition != null) {
        if (bid.duration > 0) {
          min(floatingAdPosition.maxAdDuration, bid.duration)
        } else {
          floatingAdPosition.maxAdDuration
        }
      } else {
        AppMonetFloatingAdConfiguration.DEFAULT_DURATION
      }
    }
  }

  init {
    buildFloatingAd(manager, params)
    addView(
        RenderingUtils.generateBlankLayout(
            context,
            RenderingUtils.dpToPixels(params.width ?: 0),
            RenderingUtils.dpToPixels(params.height ?: 0)
        )
    )
  }
}