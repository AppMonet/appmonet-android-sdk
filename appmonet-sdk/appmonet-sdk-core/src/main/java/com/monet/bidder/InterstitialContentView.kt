package com.monet.bidder

import android.content.Context
import android.graphics.Color
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.ImageView
import android.widget.ImageView.ScaleType.FIT_CENTER
import android.widget.RelativeLayout
import android.widget.TextView

/**
 * Created by jose on 4/6/18.
 */
internal class InterstitialContentView(context: Context) : RelativeLayout(context) {
  val videoView: MonetVideoView
  val contentLayout: RelativeLayout
  var contentTitleHeight: Int
  var imageView: ImageView
  var dimView: View
  var contentTitle: TextView
  fun setDarkView() {
    dimView.visibility = VISIBLE
    dimView.bringToFront()
  }

  fun removeDarkView() {
    dimView.visibility = GONE
  }

  fun setTitleContent(title: String) {
    if (title == "null") {
      contentTitleHeight = 0
      removeView(contentTitle)
    } else {
      contentTitle.text = title
    }
  }

  private fun createThumbnail(context: Context): ImageView {
    val view = ImageView(context)
    view.adjustViewBounds = true
    view.visibility = GONE
    view.scaleType = FIT_CENTER
    return view
  }

  private fun createVideoView(context: Context): MonetVideoView {
    return MonetVideoView(context)
  }

  private fun createDimView(context: Context): View {
    val view = View(context)
    view.setBackgroundColor(Color.parseColor("#D8000000"))
    view.visibility = VISIBLE
    return view
  }

  private fun setViewParams(): LayoutParams {
    return LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT
    )
  }

  private fun convertDpToPx(dp: Int): Int {
    return Math.round(
        dp * (resources.displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)
    )
  }

  init {
    videoView = createVideoView(context)
    imageView = createThumbnail(context)
    dimView = createDimView(context)
    contentLayout = RelativeLayout(context)
    contentTitle = TextView(context)
    val params = LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT
    )
    contentTitle.id = generateViewId()
    params.addRule(BELOW, contentTitle.id)
    contentTitleHeight = convertDpToPx(30)
    val titleParams =
      LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, contentTitleHeight)
    contentTitle.layoutParams = titleParams
    contentTitle.setTextColor(Color.WHITE)
    contentLayout.addView(imageView, setViewParams())
    contentLayout.addView(videoView, setViewParams())
    titleParams.addRule(ALIGN_PARENT_TOP)
    addView(contentTitle, titleParams)
    addView(contentLayout, params)
    addView(dimView, setViewParams())
  }
}