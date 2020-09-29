package com.monet.bidder.adview

import android.webkit.JavascriptInterface
import com.monet.bidder.AdServerBannerListener
import com.monet.bidder.RenderingUtils
import com.monet.bidder.WebViewUtils
import com.monet.bidder.auction.AuctionManagerCallback

internal class AdViewJsInterface(
  private val adServerListener: AdServerBannerListener?,
  private val adViewManagerCallback: AdViewManagerCallback,
  private val adViewPoolManagerCallback: AdViewPoolManagerCallback,
  private val auctionManagerCallback: AuctionManagerCallback
) {
  @JavascriptInterface fun respond(response: String?): String {
    try {
      adViewPoolManagerCallback.adViewResponse(
          WebViewUtils.quote(adViewManagerCallback.uuid),
          WebViewUtils.quote(response)
      )
    } catch (e: Exception) {
      return "{\"error\": true }"
    }
    return "{\"success\": true }"
  }

  @JavascriptInterface fun setThirdPartyCookies(enabledStr: String?) {
    val enabled = java.lang.Boolean.parseBoolean(enabledStr)
    adViewManagerCallback.enableThirdPartyCookies(enabled)
  }

  @JavascriptInterface fun markReady(): String {
    adViewManagerCallback.ready()
    return "success"
  }

  @JavascriptInterface fun nativePlacement(
    key: String,
    value: String
  ) {
    adViewManagerCallback.nativePlacement(key, value)
  }

  @JavascriptInterface fun ajax(request: String?): String {
    return adViewManagerCallback.ajax(request!!)
  }

  @get:JavascriptInterface val layoutState: String
    get() = adViewManagerCallback.isAdViewAttachedToLayout()

  @JavascriptInterface fun getBooleanValue(key: String?): String {
    return adViewManagerCallback.getBooleanValue(key!!)
  }

  @JavascriptInterface fun setBooleanValue(
    key: String?,
    value: String?
  ): String {
    return adViewManagerCallback.setBooleanValue(key!!, value!!)
  }

  @JavascriptInterface fun requestSelfDestroy() {
    adViewPoolManagerCallback.requestDestroy(adViewManagerCallback.uuid)
  }

  @JavascriptInterface fun setBackgroundColor(color: String) {
    adViewManagerCallback.setBackgroundColor(color)
  }

  @JavascriptInterface fun removeBid(bidId: String?): String {
    return if (auctionManagerCallback.removeBid(bidId!!) != null) "true" else "false"
  }

  @JavascriptInterface fun markBidRender(bidId: String?): String {
    return adViewManagerCallback.markBidRendered(bidId!!)
  }

  @JavascriptInterface fun wvUUID(): String {
    return adViewManagerCallback.uuid
  }

  @get:JavascriptInterface val refCount: String
    get() = adViewPoolManagerCallback.getReferenceCount(adViewManagerCallback.uuid).toString()

  /**
   * Get a String representing the current attachment state of this AdView.
   * When preloading video ads, this will be "LOADING_ENV". When rendering
   * it will return "RENDER_ENV".
   *
   * @return either "LOADING_ENV" or "RENDER_ENV"
   */
  @get:JavascriptInterface val environment: String
    get() = adViewManagerCallback.adViewEnvironment

  @get:JavascriptInterface val visibility: String
    get() = adViewManagerCallback.adViewVisibility

  /**
   * Get the total number of network requests made in this WebView. To be used for identifying heavy
   * creatives
   *
   * @return count of network requests
   */
  @get:JavascriptInterface val networkRequestCount: String
    get() = adViewManagerCallback.networkRequestCount.toString()

  /**
   * Let the creative (javascript) indicate that the ad has finished playing.
   *
   * @return "ok"
   */
  @JavascriptInterface fun finish(): String {
    adViewPoolManagerCallback.adViewResponse(
        WebViewUtils.quote(adViewManagerCallback.uuid),
        WebViewUtils.quote("adFinished")
    )
    return "ok"
  }

  /**
   * Trigger the interstitial to close. To be called by custom close-button UI
   */
  @JavascriptInterface fun closeInterstitial() {
    adServerListener?.onAdClosed()
  }
}