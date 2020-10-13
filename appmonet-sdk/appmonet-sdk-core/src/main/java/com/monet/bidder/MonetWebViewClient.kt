package com.monet.bidder

import android.net.Uri
import android.os.Build.VERSION_CODES
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.annotation.RequiresApi
import com.monet.bidder.WebViewUtils.buildResponse
import com.monet.bidder.WebViewUtils.looseUrlMatch

/**
 * This WebViewClient subclass handles intercepting requests to the auction URL,
 * which sends a preconfigured static HTML.
 */
class MonetWebViewClient(
  private val mHtml: String,
  private val mAuctionURL: String
) : BaseWebViewClient() {
  override fun shouldInterceptRequestInner(
    view: WebView,
    url: String
  ): WebResourceResponse? {
    if (looseUrlMatch(url, mAuctionURL)) {
      return buildResponse(mHtml)
    }
    if (url.contains("favicon.ico")) {
      return blankPixelResponse
    }
    try {
      if (url.contains(Constants.AUCTION_WEBVIEW_HOOK)) {
        val resp = hookResponse(url)
        if (resp != null) {
          return resp
        }
      }
    } catch (e: Exception) {
      // do nothing
    }
    return super.shouldInterceptRequestInner(view, url)
  }

  @RequiresApi(api = VERSION_CODES.LOLLIPOP) override fun shouldInterceptRequestInner(
    view: WebView,
    request: WebResourceRequest
  ): WebResourceResponse? {
    val uri = request.url
    val url = uri.toString()
    if (looseUrlMatch(url, mAuctionURL)) {
      return buildResponse(mHtml)
    }
    return if (url.contains("favicon.ico")) {
      blankPixelResponse
    } else super.shouldInterceptRequestInner(view, request)
  }

  /**
   * This method returns a [WebResourceResponse].
   *
   * @param url This contains the script url as a query parameter which is to be added as a src
   * on the html web resource.
   * @return [WebResourceResponse]
   */
  private fun hookResponse(url: String): WebResourceResponse? {
    return try {
      val uri = Uri.parse(url)
      if (uri == null) null else buildResponse(
          String.format(
              "<html><body><script src=\"%s\"></script></body></html>",
              uri.getQueryParameter(Constants.AUCTION_WV_HK_PARAM)
          )
      )
    } catch (e: Exception) {
      null
    }
  }
}