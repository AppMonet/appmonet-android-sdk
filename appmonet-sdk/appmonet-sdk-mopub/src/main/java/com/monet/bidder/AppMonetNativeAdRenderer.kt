package com.monet.bidder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.mopub.nativeads.BaseNativeAd
import com.mopub.nativeads.MoPubAdRenderer
import com.mopub.nativeads.NativeImageHelper
import com.mopub.nativeads.NativeRendererHelper
import java.util.WeakHashMap

/**
 * Class responsible of rendering the app monet native ad.
 */
open class AppMonetNativeAdRenderer(private val mViewBinder: AppMonetNativeViewBinder) :
    MoPubAdRenderer<AppMonetStaticNativeAd> {
  private val mViewHolderMap: WeakHashMap<View, AppMonetNativeViewHolder> = WeakHashMap()
  override fun createAdView(
    context: Context,
    parent: ViewGroup?
  ): View {
    return LayoutInflater.from(context).inflate(mViewBinder.layoutId, parent, false)
  }

  override fun renderAdView(
    view: View,
    appMonetNativeAd: AppMonetStaticNativeAd
  ) {
    var appMonetNativeViewHolder = mViewHolderMap[view]
    if (appMonetNativeViewHolder == null) {
      appMonetNativeViewHolder = AppMonetNativeViewHolder.fromViewBinder(view, mViewBinder)
      mViewHolderMap[view] = appMonetNativeViewHolder
    }
    update(appMonetNativeViewHolder, appMonetNativeAd)
    NativeRendererHelper.updateExtras(
        appMonetNativeViewHolder.mainView, mViewBinder.extras,
        appMonetNativeAd.extras
    )
    setViewVisibility(appMonetNativeViewHolder, 0)
  }

  override fun supports(nativeAd: BaseNativeAd): Boolean {
    return nativeAd is AppMonetStaticNativeAd
  }

  private fun setIconView(
    iconView: ImageView?,
    imageSrc: String?
  ) {
    if (imageSrc == null || iconView == null || imageSrc.isEmpty()) {
      return
    }
    NativeImageHelper.loadImageView(imageSrc, iconView)
  }

  private fun update(
    staticNativeViewHolder: AppMonetNativeViewHolder,
    staticNativeAd: AppMonetStaticNativeAd
  ) {
    NativeRendererHelper.addTextView(staticNativeViewHolder.titleView, staticNativeAd.title)
    NativeRendererHelper.addTextView(staticNativeViewHolder.textView, staticNativeAd.text)
    NativeRendererHelper.addTextView(
        staticNativeViewHolder.callToActionView, staticNativeAd.callToAction
    )
    setIconView(staticNativeViewHolder.iconView, staticNativeAd.icon)
    if (staticNativeViewHolder.mediaLayout == null) {
      logger.debug("Attempted to add adView to null media layout")
    } else {
      if (staticNativeAd.media == null) {
        logger.debug("Attempted to set media layout content to null")
      } else {
        // it's possible that media already has a child -- be careful with this
        val mediaView = staticNativeAd.media ?: return

        // prevents media from being recycled
        if (mediaView.parent is ViewGroup) {
          logger.debug("media view has a parent; detaching")
          // try to remove the parent
          try {
            val parent = mediaView.parent as ViewGroup
            parent.removeView(mediaView)
          } catch (e: Exception) {
            // do nothing
          }
        }
        staticNativeViewHolder.mediaLayout!!.addView(staticNativeAd.media)
      }
    }
  }

  private fun setViewVisibility(
    staticNativeViewHolder: AppMonetNativeViewHolder,
    visibility: Int
  ) {
    if (staticNativeViewHolder.mainView != null) {
      staticNativeViewHolder.mainView!!.visibility = visibility
    }
  }

  companion object {
    private val logger = MonetLogger("AppMonetNativeAdRenderer")
  }

}