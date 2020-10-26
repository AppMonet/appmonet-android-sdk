package com.monet.bidder

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebSettings.PluginState.ON
import android.webkit.WebSettings.RenderPriority.HIGH
import android.webkit.WebView
import com.monet.Callback
import com.monet.bidder.Constants.JSMethods
import com.monet.bidder.auction.MonetJsInterface
import com.monet.bidder.threading.InternalRunnable
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by jose on 8/28/17.
 */
open class MonetWebView protected constructor(context: Context?) : WebView(context),
    AppMonetWebView {
  private val innerHandler = Handler()
  var mJsInterface: MonetJsInterface? = null
  override fun getHandler(): Handler {
    return innerHandler
  }

  override fun destroy() {
    try {
      destroyWebView()
    } catch (e: Exception) {
      sLogger.warn("failed to properly destroy webView!")
    }
    isDestroyed = true
    super.destroy()
  }

  override fun start() {
    //to be implemented by extension
  }

  override fun trackEvent(
    eventName: String,
    detail: String,
    key: String,
    value: Float,
    currentTime: Long
  ) {
    var currentTime = currentTime
    if (currentTime <= 0) {
      currentTime = System.currentTimeMillis()
    }
    executeJs("trackRequest", eventName, detail, value.toString(), currentTime.toString())
  }

  @SuppressLint("JavascriptInterface", "AddJavascriptInterface")
  protected fun setJsInterface(jsInterface: MonetJsInterface?) {
    mJsInterface = jsInterface
    if (mJsInterface != null) {
      addJavascriptInterface(mJsInterface, Constants.JS_BRIDGE_VARIABLE)
    }
  }

  protected fun loadHtml(
    html: String?,
    baseUrl: String?
  ) {
    if (baseUrl == null || html == null) {
      sLogger.warn("url or html is null")
      return
    }
    if (isDestroyed) {
      sLogger.warn("attempt to load HTML in destroyed state")
      return
    }
    try {
      loadDataWithBaseURL(
          baseUrl,
          html,
          "text/html",
          "UTF-8",
          null
      )
    } catch (e: Exception) {
    }
  }

  override fun executeJs(
    method: String,
    vararg args: String
  ) {
    executeJs(0, method, null, *args)
  }

  override fun executeJs(
    timeout: Int,
    method: String,
    vararg args: String
  ) {
    executeJs(timeout, method, null, *args)
  }

  /**
   * Similar to executeJsAsync, except we block & wait on the result for the given timeout
   *
   * @param timeout number of milliseconds to wait for a result
   * @param method method to be called on window.monet
   * @param args string arguments to pass to javascript method
   * @return the result of javascript execution
   */
  override fun executeJs(
    timeout: Int,
    method: String,
    callback: Callback<String?>?,
    vararg args: String
  ) {
    if (isDestroyed) {
      callback?.let { it(null) }
    }
    sLogger.debug("executing js with timeout - $timeout")
    executeJsAsync(callback, method, timeout, *args)
  }

  /**
   * Execute a method on our javascript API (e.g., window.monet) and return a result
   * asynchronously.
   * Note that this requires the method being called to implement the follow signature (javascript,
   * types in Flowtype):
   *
   *
   * monet[{FUNCTION_NAME}] = (args: Array<string>, (string) => null) => null
  </string> *
   *
   * We pass that function a callback as the last argument, which itself calls a bridge method
   * 'trigger', sending a message
   * identified by UUID back through to the webView.
   *
   * @param callback The callback for the javascript response.
   * @param method The javascript method to invoke.
   * @param args The arguments to be passed to the javascript method.
   */
  override fun executeJsAsync(
    callback: Callback<String?>?,
    method: String,
    timeout: Int,
    vararg args: String
  ) {
    val cbName = "cb__" + UUID.randomUUID().toString()
    var argStr = TextUtils.join(",", args)
    if (argStr == "") {
      argStr = "null"
    }
    val jsCallback =
      String.format(JSMethods.JS_ASYNC_CALLBACK, Constants.JS_BRIDGE_VARIABLE, cbName)
    val jsCall = String.format(
        JSMethods.JS_CALL_TEMPLATE, JSMethods.INTERFACE_NAME,
        method, argStr, jsCallback
    )

    //if callback is null then we don't care about the response from javascript.
    if (mJsInterface != null && callback != null && timeout > 0) {
      mJsInterface!!.listenOnce(cbName, timeout, handler, callback)
    }
    // exec immediately if there is a problem
    if (!executeJsCode(jsCall)) {
      mJsInterface!!.trigger(cbName, "{\"error\": true }")
    }
  }

  /**
   * Lowest-level javascript execution helper. Run a string of Javascript code.
   * Supports pre `evaluateJavascript` webView API.
   *
   * @param javascript a string of javascript code to be evaluated in the webView
   */
  override fun executeJsCode(javascript: String?): Boolean {
    // make sure we're on *this* thread
    val jsExecute = Runnable {
      val basicJsExec = "javascript:(function() { $javascript}());"
      if (VERSION.SDK_INT > VERSION_CODES.KITKAT) {
        try {
          evaluateJavascript(javascript) { }
        } catch (e: Exception) {
          // fall back to trying to call the URL
          try {
            loadUrl(basicJsExec)
          } catch (err: Exception) {
          }
        }
      } else {
        try {
          loadUrl(basicJsExec)
        } catch (e: Exception) {
        }
      }
    }
    return runOnUiThread(jsExecute)
  }

  override var isDestroyed: Boolean = false

  override var isLoaded: AtomicBoolean = AtomicBoolean(false)

  /**
   * Safely run the runnable on the UI thread (the webView's thread)
   *
   * @param runnable code to execute on ui thread (webView thread)
   * @return if it was able to execute succesfully
   */
  fun runOnUiThread(
    runnable: Runnable?,
    allowDestroyed: Boolean
  ): Boolean {
    if (isDestroyed) {
      return false
    }
    try {
      handler.post(object : InternalRunnable() {
        override fun runInternal() {
          if (isDestroyed && !allowDestroyed) {
            sLogger.warn("attempt to execute webView runnable on destroyed wv")
            return
          }
          runnable?.run()
        }

        override fun catchException(e: Exception?) {}
      })
    } catch (e: Exception) {
      return false
    }
    return true
  }

  protected fun runOnUiThread(runnable: Runnable?): Boolean {
    return runOnUiThread(runnable, false)
  }

  /**
   * Check to see if we're currently running on the UI thread
   *
   * @return are we on the UI thread? (webView thread)
   */
  protected fun onUIThread(): Boolean {
    return Looper.getMainLooper().thread === Thread.currentThread()
  }

  /**
   * Clean up the webView as well as possible
   */
  private fun destroyWebView() {
    // make sure we do this
    if (parent != null && parent is ViewGroup) {
      (parent as ViewGroup).removeView(this)
    }
    try {
      tag = null
    } catch (e: Exception) {
      sLogger.error("failed to clean up webView")
    }
    removeAllViews()
  }

  protected fun uiInitialize() {
    val settings = settings
    isHorizontalScrollBarEnabled = false
    isVerticalScrollBarEnabled = false
    settings.setSupportZoom(false)
    settings.setSupportMultipleWindows(true)
    settings.loadWithOverviewMode = true
    setBackgroundColor(Color.TRANSPARENT)
    setDefaultLayerType(LAYER_TYPE_HARDWARE)
    try {
      settings.setRenderPriority(HIGH)
      settings.pluginState = ON
    } catch (e: Exception) {
      // don't do anything
    }
  }

  @SuppressLint("SetJavaScriptEnabled") private fun initialize() {
    val settings = settings
    settings.javaScriptEnabled = true
    settings.allowFileAccess = false

    //        if (Looper.getMainLooper().equals(Looper.myLooper())) {
    //            uiInitialize();
    //        }
    settings.setGeolocationEnabled(true)
    setCaching(true)
    allowCookies()
    if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
      settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
    }
    if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2) {
      settings.allowFileAccessFromFileURLs = false
      settings.allowUniversalAccessFromFileURLs = false
    }
  }

  protected fun setCaching(enabled: Boolean) {
    val settings = settings
    settings.domStorageEnabled = enabled
    settings.databaseEnabled = enabled
    settings.setAppCachePath(context.cacheDir.absolutePath)
    settings.setAppCacheEnabled(enabled)
    settings.cacheMode =
      if (enabled) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_NO_CACHE
    settings.savePassword = enabled
    settings.saveFormData = enabled
  }

  private fun setDefaultLayerType(layerType: Int) {
    if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
      setLayerType(layerType, null)
      return
    }
    setLayerType(LAYER_TYPE_SOFTWARE, null)
  }

  private fun allowCookies() {
    if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
      CookieManager.getInstance().setAcceptCookie(true)
    }
    if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
      CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
    }
  }

  override fun loadView(url: String) {
    this.loadUrl(url)
  }

  companion object {
    protected const val WV_TYPE_UA = "ua"
    private const val WV_TYPE_CK = "ck"
    private val sLogger = Logger("MWV")
  }

  init {
    initialize()
  }
}