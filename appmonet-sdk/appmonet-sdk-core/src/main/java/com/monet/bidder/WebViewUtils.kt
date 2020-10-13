package com.monet.bidder;

import android.webkit.WebResourceResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for repetitive operations done on the webviews.
 */
public class WebViewUtils {
  private WebViewUtils() {
  }

  public static WebResourceResponse buildResponse(String html) {
    return buildResponse(html, "text/html");
  }

  public static WebResourceResponse buildResponse(String html, String mimeType) {
    InputStream stream = new ByteArrayInputStream(
        html.getBytes(StandardCharsets.UTF_8));

    return new WebResourceResponse(mimeType, "UTF-8", stream);
  }

  /**
   * This method returns the javascript execution method string which will be called by the
   * webview.
   *
   * @param identifier This string is the first parameter which tells the javascript which
   *                   action to take.
   * @param body       This string is the information to be passed to javascript.
   * @return The string to be executed by the webview.
   */
  public static String javascriptExecute(String identifier, String body) {
    return ("javascript:window.monet.execute('" + identifier + "'," + body + ")");
  }

  /**
   * Surround a string with quotes. Useful for passing arguments to Javascript.
   * DO NOT pass a string with both types of quotes!!!
   * <p>
   *
   * @param str the string to be enclosed in quotes
   * @return the string wrapped in single quotes
   */
  public static String quote(String str) {
    if (str == null) {
      // since we are passing to javascript as a list of arguments
      // we want to represent the null value as a literal
      return "null";
    }

    String quoteChar = str.indexOf('\'') != -1 ? "\"" : "'";
    return quoteChar + str + quoteChar;
  }

  public static String generateTrackingSource(AdType adType) {
    return "custom_event_" + adType.toString();
  }

  public static boolean looseUrlMatch(String found, String expected) {
    if (found == null || expected == null) {
      return false;
    }

    // there is less than 4 char difference between what we found
    String baseUrl = urlWithoutQuery(found);
    return (baseUrl.startsWith(expected) ||
        expected.startsWith(baseUrl)) && (Math.abs(baseUrl.length() - expected.length()) < 4);
  }

  /**
   * Return a URL, anything after the query string removed
   * @param url the source URL
   * @return url without query string
   */
  static String urlWithoutQuery(String url) {
    int qsIndex = url.indexOf('?');
    String strippedURL = url.substring(0, qsIndex < 0 ? url.length() - 1 : qsIndex);

    // also remove the # param
    int hashIndex = strippedURL.indexOf('#');
    return url.substring(0, hashIndex < 0 ? strippedURL.length() : hashIndex);
  }
}
