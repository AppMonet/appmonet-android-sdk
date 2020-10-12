package com.monet.bidder

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

/**
 * This class holds the views from the layout provided for native rendering.
 */
internal class AppMonetNativeViewHolder private constructor() {
  var iconView: ImageView? = null
  var mainView: View? = null
  var titleView: TextView? = null
  var textView: TextView? = null
  var callToActionView: TextView? = null
  var mediaLayout: ViewGroup? = null

  companion object {
    fun fromViewBinder(
      view: View,
      viewBinder: AppMonetNativeViewBinder
    ): AppMonetNativeViewHolder {
      val appMonetNativeViewHolder = AppMonetNativeViewHolder()
      appMonetNativeViewHolder.mainView = view
      appMonetNativeViewHolder.titleView = view.findViewById(viewBinder.titleId)
      appMonetNativeViewHolder.textView = view.findViewById(viewBinder.textId)
      appMonetNativeViewHolder.callToActionView = view.findViewById(viewBinder.callToActionId)
      appMonetNativeViewHolder.mediaLayout = view.findViewById(viewBinder.mediaLayoutId)
      appMonetNativeViewHolder.iconView = view.findViewById(viewBinder.iconId)
      return appMonetNativeViewHolder
    }
  }
}