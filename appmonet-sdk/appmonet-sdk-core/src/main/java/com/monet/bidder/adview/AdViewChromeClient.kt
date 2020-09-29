package com.monet.bidder.adview

import android.content.Context
import android.graphics.Bitmap
import android.os.Message
import android.webkit.ConsoleMessage
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.monet.bidder.Icons.TRANSPARENT
import com.monet.bidder.Logger

/**
 * This is the Chrome Client for the AdView. The most important
 * functionality here is [AdViewChromeClient.onCreateWindow],
 * which is responsible for handling `window.open()` calls from within the webView (some creatives open windows that way).
 *
 * @see {@link AdView}
 */
internal class AdViewChromeClient(
  private val adViewManagerCallback: AdViewManagerCallback,
  private val context: Context
) : WebChromeClient() {
  override fun onJsAlert(
    view: WebView,
    url: String,
    message: String,
    result: JsResult
  ): Boolean {
    result.confirm()
    return true
  }

  override fun onJsPrompt(
    view: WebView,
    url: String,
    message: String,
    defaultValue: String,
    result: JsPromptResult
  ): Boolean {
    result.confirm()
    return true
  }

  override fun getDefaultVideoPoster(): Bitmap? {
    return TRANSPARENT.getBitmap(context)
  }

  override fun onJsConfirm(
    view: WebView,
    url: String,
    message: String,
    result: JsResult
  ): Boolean {
    result.confirm()
    return true
  }

  override fun onJsBeforeUnload(
    view: WebView,
    url: String,
    message: String,
    result: JsResult
  ): Boolean {
    result.confirm()
    return true
  }

  override fun onCreateWindow(
    view: WebView,
    isDialog: Boolean,
    isUserGesture: Boolean,
    resultMsg: Message
  ): Boolean {
    adViewManagerCallback.interceptWindowOpen(view, resultMsg)
    return true
  }

  override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
    sLogger.forward(consoleMessage, consoleMessage.message())
    return true
  }

  companion object {
    private val sLogger = Logger("AdViewChromeClient")
  }
}