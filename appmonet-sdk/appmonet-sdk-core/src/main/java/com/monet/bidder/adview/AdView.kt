package com.monet.bidder.adview

import android.annotation.SuppressLint
import android.content.Context
import android.content.MutableContextWrapper
import android.view.MotionEvent
import android.view.View
import android.view.View.OnFocusChangeListener
import android.webkit.WebSettings.RenderPriority.HIGH
import com.monet.bidder.AdSize
import com.monet.bidder.Constants
import com.monet.bidder.MonetWebView
import com.monet.bidder.threading.InternalRunnable

class AdView internal constructor(
  private val adSize:AdSize,
  private val adViewClient: AdViewClient,
  adViewJsInterface: AdViewJsInterface,
  context: Context,
  private val adViewManagerCallback: AdViewManagerCallback,
  private val adViewUrl: String,
  private val userAgent: String,
  private val html: String
) : MonetWebView(MutableContextWrapper(context)) {
  private val mLoadWithData = true

  init {
    uiInitialize()
    setupView(adViewJsInterface)
  }

  private fun setupView(adViewJsInterface: AdViewJsInterface) {
    webViewClient = adViewClient
    webChromeClient = AdViewChromeClient(adViewManagerCallback, this.context)
    initSettings(adViewJsInterface)
    trackViewState()
  }

  private fun trackViewState() {
    addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
      private fun notifyAttachState(isAttached: Boolean) {
        runOnUiThread(object : InternalRunnable() {
          override fun runInternal() {
            executeJs("attachChange", java.lang.Boolean.toString(isAttached))
          }

          override fun catchException(e: Exception?) {
            // do nothing
          }
        })
      }

      override fun onViewAttachedToWindow(view: View) {
        notifyAttachState(true)
      }

      override fun onViewDetachedFromWindow(view: View) {
        notifyAttachState(false)
      }
    })
    onFocusChangeListener = OnFocusChangeListener { view, b ->
      runOnUiThread(object : InternalRunnable() {
        override fun runInternal() {
          executeJs("focusChange", java.lang.Boolean.toString(b))
        }

        override fun catchException(e: Exception?) {
          // do nothing
        }
      })
    }
  }

  fun load() {
    // an experiment that could be changed by the js;
    // not sure if/when we would want to change that
    if (mLoadWithData) {
      loadDataWithBaseURL(adViewUrl, html, "text/html", "UTF-8", null)
    } else {
      loadUrl(adViewUrl)
    }
  }

  @SuppressLint("AddJavascriptInterface", "SetJavaScriptEnabled")
  private fun initSettings(adViewJsInterface: AdViewJsInterface) {
    val settings = settings
    setCaching(false)
    settings.setRenderPriority(HIGH)
    settings.javaScriptCanOpenWindowsAutomatically = false
    settings.mediaPlaybackRequiresUserGesture = false

    // after this size, rely on the meta tags to set viewport
    if (adSize.width >= MAX_NON_WIDE_WIDTH) {
      settings.useWideViewPort = true
    }

    // do *not* use a cache!!!
    addJavascriptInterface(adViewJsInterface, Constants.JS_BRIDGE_VARIABLE)
    if (userAgent.isNotEmpty()) {
      settings.userAgentString = userAgent
    }
  }

  fun destroy(invalidate: Boolean) {
    adViewManagerCallback.destroy(invalidate)
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    return adViewManagerCallback.onAdViewTouchEvent(event) || super.onTouchEvent(event)
  }

  companion object {
    private const val MAX_NON_WIDE_WIDTH = 600
  }

}