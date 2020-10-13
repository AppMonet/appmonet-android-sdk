package com.monet.bidder.adview

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import com.monet.bidder.*

class AdViewClient internal constructor(private val adViewManagerCallback: AdViewManagerCallback) :
    BaseWebViewClient() {
  private var mListener: AdServerBannerListener? = null

  fun setListener(listener: AdServerBannerListener?) {
    mListener = listener
  }

  override fun onPageStartedInner(
      view: WebView,
      url: String,
      favicon: Bitmap?
  ) {
    super.onPageStartedInner(view, url, favicon)
    adViewManagerCallback.handleAdInteraction(view, url)
  }

  override fun onPageFinished(
      view: WebView,
      url: String
  ) {
    adViewManagerCallback.isLoaded = true
    super.onPageFinished(view, url)
  }

  override fun shouldInterceptRequestInner(
      view: WebView,
      url: String
  ): WebResourceResponse? {
    return adViewManagerCallback.shouldInterceptRequest(view, url)
        ?: super.shouldInterceptRequestInner(view, url)
  }

  override fun shouldInterceptRequestInner(
      view: WebView,
      request: WebResourceRequest
  ): WebResourceResponse? {
    return adViewManagerCallback.shouldInterceptRequest(view, request)
        ?: super.shouldInterceptRequestInner(view, request)
  }

  override fun shouldOverrideUrlLoadingInner(
      view: WebView,
      url: String
  ): Boolean {
    return adViewManagerCallback.checkOverride(view, Uri.parse(url))
  }

  override fun shouldOverrideUrlLoadingInner(
      view: WebView,
      request: WebResourceRequest
  ): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      // note that in older webviews, we can only stop loading
      // we can't control the initial redirect which sucks :/
      adViewManagerCallback.shouldOverrideUrlLoading(view, request)
    } else {
      super.shouldOverrideUrlLoadingInner(view, request)
    }
  }
}
