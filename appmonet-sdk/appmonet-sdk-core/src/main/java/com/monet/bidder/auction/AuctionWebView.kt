package com.monet.bidder.auction

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Looper
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions.Callback
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import com.monet.bidder.Constants
import com.monet.bidder.Constants.Configurations
import com.monet.bidder.HttpUtil.hasNetworkConnection
import com.monet.bidder.Logger
import com.monet.bidder.MonetWebView
import com.monet.bidder.SdkConfigurations
import com.monet.bidder.threading.InternalRunnable

/**
 * Created by jose on 8/28/17.
 */
@SuppressLint("ViewConstructor")
class AuctionWebView(
  context: Context?,
  monetJsInterface: MonetJsInterface?,
  private val auctionWebViewParams: AuctionWebViewParams,
  val configurations: SdkConfigurations?
) : MonetWebView(context) {
  /**
   * Load the javascript responsible for most of the auction logic.
   * This has logic to retry in the case of network/load failure.
   *
   * @param stagePageUrl the URL we want to load the auction under
   * @param tries        the number of times we've already tried to load
   */
  private fun safelyLoadAuctionPage(
    stagePageUrl: String,
    tries: Int
  ) {
    try {
      sLogger.info("loading auction manager root: ", auctionWebViewParams.auctionHtml)

      // if the interceptor isn't working right
      // just inject the html directly into the webView.
      // we would prefer to access as a URL since this works better
      // within webkit, but can tolerate this.
      if (tries > 1) {
        loadHtml(auctionWebViewParams.auctionHtml, auctionWebViewParams.auctionUrl)
      } else {
        sLogger.debug("loading url")
        loadUrl(stagePageUrl)
      }
    } catch (e: Exception) {
    }

    // set up a timer to make sure that
    // we've successfully loaded
    setStartDetection(tries)
  }

  /**
   * Begin trying to load the auction javascript engine.
   * This will kick of the load process (on the correct thread)
   *
   * @param tries the number of times we've already tried this
   */
  private fun loadAuctionPage(tries: Int) {
    // depending, we want to use the different one
    val delimiter = if (auctionWebViewParams.auctionUrl.contains("?")) "&" else "?"
    val stagePageUrl = auctionWebViewParams.auctionUrl + delimiter + "aid=" +
        auctionWebViewParams.appMonetContext.applicationId + "&v=" + Constants.SDK_VERSION
    if (isDestroyed) {
      sLogger.error("attempt to load into destroyed auction manager.")
      return
    }
    if (Looper.getMainLooper() == Looper.myLooper()) {
      safelyLoadAuctionPage(stagePageUrl, tries)
      return
    }
    runOnUiThread(object : InternalRunnable() {
      override fun runInternal() {
        safelyLoadAuctionPage(stagePageUrl, tries)
      }

      override fun catchException(e: Exception?) {
        sLogger.error("Exception caught : $e")
      }
    })
  }

  /**
   * Start an auction
   */
  override fun start() {
    // load the auction page/stage
    loadAuctionPage(1)
  }

  /**
   * After a delay, check to see if the auction js actually loaded. If it hasn't, try to load
   * again, based on the number of tries we've already executed.
   *
   * @param tries number of times we've tried to load it
   */
  private fun setStartDetection(tries: Int) {
    val self = this
    handler.postDelayed(object : InternalRunnable() {
      override fun runInternal() {
        sLogger.warn("Thread running on: " + Thread.currentThread().name)
        if (!self.isLoaded.get()) {
          sLogger.warn("javascript not initialized yet. Reloading page")

          // check that the network is actually available.
          // if it's not, we just need to call this again
          // with the same number of tries
          if (!hasNetworkConnection(context)) {
            sLogger.warn("no network connection detecting. Delaying load check")
            setStartDetection(tries)
            return
          }
          if (tries + 1 < MAX_LOAD_ATTEMPTS) {
            loadAuctionPage(tries + 1)
          } else {
            sLogger.debug("max load attempts hit")
          }
        } else {
          sLogger.debug("load already detected")
        }
      }

      override fun catchException(e: Exception?) {
        sLogger.error("Exception caught: $e")
      }
    }, POST_LOAD_CHECK_DELAY * tries.toLong())
  }

  override fun executeJs(
    method: String,
    vararg args: String
  ) {
    if (!isLoaded.get()) {
      sLogger.warn("js not initialized.")
      return
    }
    super.executeJs(method, *args)
  }

  override fun executeJs(
    timeout: Int,
    method: String,
    vararg args: String
  ) {
    if (!isLoaded.get()) {
      sLogger.warn("js not initialized")
    }
    super.executeJs(timeout, method, *args)
  }

  companion object {
    private val sLogger = Logger("AuctionManager")
    private const val MAX_LOAD_ATTEMPTS = 5
    private const val POST_LOAD_CHECK_DELAY = 6500
  }

  init {

    // some more settings
    webViewClient = auctionWebViewParams.webViewClient
    setJsInterface(monetJsInterface)
    if (VERSION.SDK_INT >= VERSION_CODES.KITKAT && configurations != null) {
      setWebContentsDebuggingEnabled(
          configurations.getBoolean(Configurations.WEB_VIEW_DEBUGGING_ENABLED)
      )
    }

    // this manager uses a basic chrome client,
    // the main work here is to forward console messages to our logger
    webChromeClient = object : WebChromeClient() {
      override fun onGeolocationPermissionsShowPrompt(
        origin: String,
        callback: Callback
      ) {
        try {
          callback.invoke(origin, true, true)
        } catch (e: Exception) {
          // do nothing
        }
      }

      override fun onPermissionRequest(request: PermissionRequest) {
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
          try {
            request.grant(request.resources)
          } catch (e: Exception) {
            // do nothing
          }
        }
      }

      override fun onConsoleMessage(cm: ConsoleMessage): Boolean {
        sLogger.forward(cm, cm.message())
        return true
      }
    }
  }
}