package com.monet.bidder

import android.graphics.Bitmap
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.monet.bidder.WebViewUtils.buildResponse
import java.util.HashMap

open class BaseWebViewClient : WebViewClient() {
  private var mDefaultHeaders: Map<String, String> = HashMap()
  val blankPixelResponse = buildResponse("", "text/plain")
  fun setHeaders(headers: Map<String, String>?) {
    if (headers != null) {
      mDefaultHeaders = headers
    }
  }

  open fun shouldInterceptRequestInner(
    view: WebView,
    request: WebResourceRequest
  ): WebResourceResponse? {
    if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
      return super.shouldInterceptRequest(view, request)
    }
    return null
  }

  // exceptions in these methods propagate to
  // the chromium webView code (they're not handled well by other try/catch)
  // we create new signatures that wrap in try/catch so we don't accidentally
  // crash the app if we mess this up.
  open fun shouldInterceptRequestInner(
    view: WebView,
    url: String
  ): WebResourceResponse? {
    return super.shouldInterceptRequest(view, url)
  }

  override fun shouldInterceptRequest(
    view: WebView,
    request: WebResourceRequest
  ): WebResourceResponse? {
    return try {
      shouldInterceptRequestInner(view, request)
    } catch (e: Exception) {
      super.shouldInterceptRequest(view, request)
    }
  }

  override fun shouldInterceptRequest(
    view: WebView,
    url: String
  ): WebResourceResponse? {
    return try {
      shouldInterceptRequestInner(view, url)
    } catch (e: Exception) {
      super.shouldInterceptRequest(view, url)
    }
  }

  open fun shouldOverrideUrlLoadingInner(
    view: WebView,
    url: String
  ): Boolean {
    return super.shouldOverrideUrlLoading(view, url)
  }

  open fun shouldOverrideUrlLoadingInner(
    view: WebView,
    request: WebResourceRequest
  ): Boolean {
    return if (VERSION.SDK_INT >= VERSION_CODES.N) {
      super.shouldOverrideUrlLoading(view, request)
    } else false
  }

  override fun shouldOverrideUrlLoading(
    view: WebView,
    request: WebResourceRequest
  ): Boolean {
    return try {
      shouldOverrideUrlLoadingInner(view, request)
    } catch (e: Exception) {
      super.shouldOverrideUrlLoading(view, request)
    }
  }

  override fun shouldOverrideUrlLoading(
    view: WebView,
    url: String
  ): Boolean {
    return try {
      shouldOverrideUrlLoadingInner(view, url)
    } catch (e: java.lang.Exception) {
      super.shouldOverrideUrlLoading(view, url)
    }
  }

  open fun onPageStartedInner(
    view: WebView,
    url: String,
    favicon: Bitmap?
  ) {
    super.onPageStarted(view, url, favicon)
  }

  override fun onPageStarted(
    view: WebView,
    url: String,
    favicon: Bitmap?
  ) {
    try {
      onPageStartedInner(view, url, favicon)
    } catch (e: Exception) {
      super.onPageStarted(view, url, favicon)
    }
  }
}