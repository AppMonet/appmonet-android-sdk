package com.monet.bidder

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * Utility class for repetitive operations done on the webviews.
 */
object WebViewUtils {
  @JvmStatic @JvmOverloads fun buildResponse(
    html: String,
    mimeType: String? = "text/html"
  ): WebResourceResponse {
    val stream: InputStream = ByteArrayInputStream(
        html.toByteArray(StandardCharsets.UTF_8)
    )
    return WebResourceResponse(mimeType, "UTF-8", stream)
  }

  /**
   * This method returns the javascript execution method string which will be called by the
   * webview.
   *
   * @param identifier This string is the first parameter which tells the javascript which
   * action to take.
   * @param body       This string is the information to be passed to javascript.
   * @return The string to be executed by the webview.
   */
  fun javascriptExecute(
    identifier: String,
    body: String
  ): String {
    return "javascript:window.monet.execute('$identifier',$body)"
  }

  /**
   * Surround a string with quotes. Useful for passing arguments to Javascript.
   * DO NOT pass a string with both types of quotes!!!
   *
   *
   *
   * @param str the string to be enclosed in quotes
   * @return the string wrapped in single quotes
   */
  fun quote(str: String?): String {
    if (str == null) {
      // since we are passing to javascript as a list of arguments
      // we want to represent the null value as a literal
      return "null"
    }
    val quoteChar = if (str.indexOf('\'') != -1) "\"" else "'"
    return quoteChar + str + quoteChar
  }

  @JvmStatic fun generateTrackingSource(adType: AdType): String {
    return "custom_event_$adType"
  }

  @JvmStatic fun looseUrlMatch(
    found: String?,
    expected: String?
  ): Boolean {
    if (found == null || expected == null) {
      return false
    }

    // there is less than 4 char difference between what we found
    val baseUrl = urlWithoutQuery(found)
    return (baseUrl.startsWith(expected) ||
        expected.startsWith(baseUrl)) && Math.abs(baseUrl.length - expected.length) < 4
  }

  /**
   * Return a URL, anything after the query string removed
   * @param url the source URL
   * @return url without query string
   */
  fun urlWithoutQuery(url: String): String {
    val qsIndex = url.indexOf('?')
    val strippedURL = url.substring(0, if (qsIndex < 0) url.length - 1 else qsIndex)

    // also remove the # param
    val hashIndex = strippedURL.indexOf('#')
    return url.substring(0, if (hashIndex < 0) strippedURL.length else hashIndex)
  }
}