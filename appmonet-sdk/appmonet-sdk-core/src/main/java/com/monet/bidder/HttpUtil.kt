package com.monet.bidder

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.text.TextUtils
import android.util.Log
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.monet.BuildConfig
import com.monet.bidder.Constants.EventParams
import com.monet.bidder.CookieManager.Companion.instance
import com.monet.bidder.RenderingUtils.base64Encode
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.ProtocolException
import java.net.URL
import java.nio.charset.Charset
import java.util.HashMap
import java.util.Random
import java.util.regex.Pattern

internal object HttpUtil {
  private val sLogger = Logger("Tracking")
  private val sRandom = Random()

  /**
   * Checks if there is network connectivity.
   *
   * @param context from the app to check for connectivity
   * @return boolean value indicating if there is a network connection
   */
  @JvmStatic fun hasNetworkConnection(context: Context): Boolean {
    val connectivityManager = context
        .getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager
        ?: return false
    val nInfo = connectivityManager.activeNetworkInfo
    return nInfo != null && nInfo.isConnected
  }

  fun resolveLocationUrl(
    baseUrl: Uri?,
    locationHeader: String?
  ): String? {
    // it's a relative redirect; we need to resolve it
    if (!URLUtil.isValidUrl(locationHeader)) {
      if (baseUrl == null) {
        return null
      }
      val additional = Uri.parse(locationHeader)
      val builder = baseUrl.buildUpon().path(additional.path)
          .clearQuery()

      // this is because just doing 'builder.query(additional.getQuery())'
      // was leaving it encoded.. which doesn't work
      for (name in additional.queryParameterNames) {
        builder.appendQueryParameter(name, additional.getQueryParameter(name))
      }
      builder.fragment(additional.fragment)
      return builder.toString()
    }
    return locationHeader
  }

  private fun requestFromJson(jsonStr: String?): AjaxRequest? {
    return try {
      if (jsonStr == null) {
        sLogger.error("null json string")
        return null
      }
      val json = JSONObject(jsonStr)
      val ajax = AjaxRequest(
          json.getString("method"),
          json.getString("url"),
          json.getString("body"),
          HashMap(),
          json.getInt("timeout"),
          0,
          false
      )
      ajax.mCallback = json.getString("callback")
      val headers = json.getJSONObject("headers")
      val it = headers.keys()
      while (it.hasNext()) {
        val k = it.next() ?: continue
        ajax.mHeaders[k] = headers.getString(k)
      }
      ajax
    } catch (e: JSONException) {
      null
    }
  }

  private fun error(message: String): String {
    return String.format("{\"error\":\"%s\"}", message)
  }

  private fun readStream(stream: InputStream?): String {
    var output = ""
    if (stream == null) {
      return ""
    }
    val isr = InputStreamReader(stream)
    try {
      val reader = BufferedReader(isr)
      val builder = StringBuilder()
      var line: String?
      while (reader.readLine().also { line = it } != null) {
        builder.append(line).append('\n')
      }
      output = builder.toString()
    } catch (e: IOException) {
      sLogger.debug("error reading input stream")
    } finally {
      try {
        isr.close()
      } catch (e: Exception) {
        sLogger.debug("error closing stream")
      }
    }
    return output
  }

  fun formatError(error: Throwable): String {
    val stack = Log.getStackTraceString(error)
    return error.message + "|" +
        stack.replace(error.message!!, "")
            .replace('\n', '|')
            .replace("\\s+".toRegex(), " ") // reduce whitespace
            .substring(0, 200)
  }

  fun fireTrackingPixel(
    appMonetContext: AppMonetContext,
    action: String,
    headerData: String?
  ) {
    val url = Uri.parse(Constants.TRACKING_URL + action)
        .buildUpon()
        .appendQueryParameter(EventParams.APPLICATION_ID, appMonetContext.applicationId)
        .appendQueryParameter(EventParams.VERSION, BuildConfig.VERSION_NAME)
        .build().toString()
    firePixelAsync(url)
  }

  fun firePixelAsync(pixelURL: String?) {
    try {
      Request().execute(pixelURL, HttpRequest.METHOD_GET)
    } catch (e: Exception) {
      sLogger.warn("error firing pixel: ", pixelURL)
    }
  }

  @JvmStatic fun makeRequest(
    webView: AppMonetWebView?,
    request: String?
  ): String {
    val ajax = requestFromJson(request) ?: return error("invalid request")
    val http = ajax.toRequest() ?: return error("invalid http request")
    val body: String
    body = try {
      readStream(http.inputStream)
    } catch (e: IOException) {
      readStream(http.errorStream)
    }
    respondWith(webView, ajax, http, body)
    return "{\"success\":\"made request\"}"
  }

  private fun respondWith(
    webView: AppMonetWebView?,
    ajax: AjaxRequest,
    http: HttpURLConnection,
    body: String
  ) {
    val response = JSONObject()

    // read the error
    val headersMap = http.headerFields
    val headers = JSONObject()
    for ((key, value) in headersMap) {
      if (key == null || value == null) {
        continue
      }
      try {
        headers.put(key, TextUtils.join(",", value))
      } catch (e: JSONException) {
        // do nothign
      }
    }
    try {
      var statusCode = http.responseCode
      if (statusCode > 299 && statusCode < 400) {
        statusCode = 204
      }
      response.put("url", http.url)
      response.put("status", statusCode)
      response.put("headers", headers)
      response.put("body", body)
    } catch (e: JSONException) {
      sLogger.warn("invalid json in request", e.message)
      return
    } catch (e: Exception) {
      sLogger.warn("Unexpected error in response formation:", e.message)
      return
    }
    if (webView == null || webView.isDestroyed) {
      sLogger.warn("attempt to return response into destroyed webView")
      return
    }

    // write it to the webview
    webView.executeJsCode(
        String.format(
            "window['%s'](%s);",
            ajax.mCallback,
            response.toString()
        )
    )
  }

  private class Request : AsyncTask<String?, Int?, Int>() {
    @SuppressLint("DefaultLocale")
    override fun doInBackground(vararg params: String?): Int {
      return if (params == null || (params[0] == null && params[1] == null)) {
        400
      } else try {
        val request = HttpRequest(params[0]!!, params[1]!!)
        request.disableSslv3()
        request.connectTimeout(15000)
        request.header(EventParams.HEADER_VERSION, Constants.SDK_VERSION)
        request.header(EventParams.HEADER_CLIENT, Constants.SDK_CLIENT)
        request.header(HttpRequest.HEADER_USER_AGENT, "AppMonet/SDK " + Constants.SDK_VERSION)
        if (params.size > 2) {
          request.send(base64Encode(params[2]))
        }
        request.code()
      } catch (e: Exception) {
        sLogger.debug("Error firing network call. $e")
        400
      }
    }
  }

  internal class AjaxRequest internal constructor(
    private val mMethod: String,
    private val mUrl: String?,
    body: String,
    headers: MutableMap<String?, String?>,
    timeout: Int,
    redirects: Int,
    isSecure: Boolean
  ) {
    private val mUri: Uri
    private val mBody: String?
    var mCallback: String? = null
    val mHeaders: MutableMap<String?, String?>
    private val mTimeout: Int
    private val mRedirectCount: Int
    private val mIsSecure: Boolean
    private fun hasBody(): Boolean {
      return (mMethod == "POST" || mMethod == "PUT" || mMethod == "PATCH") && mBody != null && mBody.length > 0
    }

    fun toRequest(): HttpURLConnection? {
      val url: URL
      url = try {
        URL(mUrl)
      } catch (e: MalformedURLException) {
        return null
      }
      val urlConnection: HttpURLConnection
      urlConnection = try {
        url.openConnection() as HttpURLConnection
      } catch (e: IOException) {
        return null
      }
      try {
        urlConnection.requestMethod = mMethod
      } catch (e: ProtocolException) {
        return null
      }
      for ((key, value) in mHeaders) {
        urlConnection.setRequestProperty(
            key, value
        )
      }
      urlConnection.instanceFollowRedirects = false
      urlConnection.useCaches = false
      urlConnection.doInput = true
      if (hasBody()) {
        urlConnection.doOutput = true
        try {
          urlConnection.outputStream
              .write(mBody!!.toByteArray(Charset.forName("UTF-8")))
        } catch (e: IOException) {
          return null
        }
      }
      urlConnection.readTimeout = mTimeout
      return urlConnection
    }

    private fun getReasonString(found: String?): String {
      return if (found != null && found.length > 0) {
        found
      } else "OK"
    }

    private fun getCorsHeaders(headers: Map<String, String?>): Map<String, String?> {
      val cors: MutableMap<String, String?> = HashMap()
      if (!headers.containsKey(ACCESS_CONTROL_ORIGIN)) {
        cors[ACCESS_CONTROL_ORIGIN] = "*"
      }
      if (!headers.containsKey(ACCESS_CONTROL_METHOD)) {
        cors[ACCESS_CONTROL_METHOD] = "GET,HEAD,POST,PUT,DELETE"
      }
      return cors
    }

    private fun getErrorResponse(status: Int): WebResourceResponse? {
      return if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
        WebResourceResponse(
            "text/plain",
            "UTF-8",
            status,
            "ERROR",
            mHeaders,
            null
        )
      } else null
    }

    private fun getMimeType(contentType: String?): String {
      if (contentType == null || contentType.isEmpty()) {
        return "text/plain"
      }
      if (contentType.indexOf(';') == -1) {
        return contentType
      }

      // otherwise, split it and only send the first part
      val parts = contentType.split(";".toRegex()).toTypedArray()
      return parts[0]
    }

    private fun readStreamBinary(input: InputStream): InputStream {
      val outputStream = ByteArrayOutputStream()
      val bufferSize = 1024
      val buffer = ByteArray(bufferSize)
      var len = 0
      try {
        while (input.read(buffer).also { len = it } != -1) {
          outputStream.write(buffer, 0, len)
        }
      } catch (e: IOException) {
        return ByteArrayInputStream(outputStream.toByteArray())
      }
      return ByteArrayInputStream(outputStream.toByteArray())
    }

    fun execute(): WebResourceResponse? {
      if (mRedirectCount > MAX_REDIRECTS) {
        return getErrorResponse(500)
      }
      if (mUrl == null || mUrl.isEmpty()) {
        return getErrorResponse(400)
      }
      val http = toRequest() ?: return null
      val headersMap = http.headerFields
      val responseCode: Int
      responseCode = try {
        http.responseCode
      } catch (e: IOException) {
        return null
      }
      when (responseCode) {
        307, 308, 303, HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP -> {
          val redirect = AjaxRequest(
              "GET",
              resolveLocationUrl(mUri, http.getHeaderField("Location")),
              "",
              mHeaders,
              mTimeout,
              mRedirectCount + 1,
              mIsSecure
          )
          return redirect.execute()
        }
        404 -> return getErrorResponse(404)
      }
      val definitiveHeaders: MutableMap<String, String?> = HashMap()
      for ((key, value) in headersMap) {
        definitiveHeaders[key] = value[0]
      }
      if (definitiveHeaders.containsKey(CookieManager.SET_COOKIE_HEADER)) {
        instance!!.add(
            definitiveHeaders[CookieManager.SET_COOKIE_HEADER]
        )
      }

      // don't allow the WV to get the headers
      definitiveHeaders.remove(CookieManager.SET_COOKIE_HEADER)

      // add CORS headers
      definitiveHeaders.putAll(getCorsHeaders(definitiveHeaders))
      definitiveHeaders.remove("Content-Type")
      val contentBody = ""
      var contentType = http.contentType
      if (contentType == null || contentType.length == 0) {
        contentType = "text/plain"
      }
      val mimeType = getMimeType(contentType)
      var responseInputStream: InputStream? = null
      try {
        val bodyStream = http.content as InputStream
        responseInputStream = readStreamBinary(bodyStream)
      } catch (e: Exception) {
        // do nothing
      }
      definitiveHeaders.remove("Content-Length")

      // attach the headers to the response
      if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
        val contentEncoding = http.contentEncoding

        // skip for video
        if ((contentType.contains("video") || contentType.contains("media")) && !mIsSecure) {
          sLogger.warn("Attempt to optimize video incomplete. Skipping")
          return null
        }
        return try {
          val input = responseInputStream ?: http.inputStream
          WebResourceResponse(
              mimeType,
              contentEncoding,
              responseCode,
              getReasonString(http.responseMessage),
              definitiveHeaders,
              input
          )
        } catch (e: IOException) {
          WebResourceResponse(
              mimeType,
              contentEncoding,
              500,
              "OK",
              definitiveHeaders,
              null
          )
        }
      }
      return null
    }

    companion object {
      private const val ACCESS_CONTROL_ORIGIN = "Access-Control-Allow-Origin"
      private const val ACCESS_CONTROL_METHOD = "Access-Control-Allow-Methods"
      private val sInvalidRequestPattern = Pattern.compile("\\.(?:mp4|webm|jsv)")
      private const val MAX_REDIRECTS = 15
      private fun shouldBeRequested(
        request: WebResourceRequest,
        isSecureMode: Boolean
      ): Boolean {
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
          val url = request.url.toString()
          if (sInvalidRequestPattern.matcher(url).matches() && !isSecureMode) {
            return false
          }
          val method = request.method
          if (method == "GET" || method == "OPTIONS" || method == "HEAD") {
            return true
          }
        }
        return false
      }

      fun from(
        request: WebResourceRequest,
        defaultHeaders: Map<String?, String?>?,
        isSecureMode: Boolean
      ): AjaxRequest? {
        if (!shouldBeRequested(request, isSecureMode)) {
          return null
        }
        if (VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
          return null
        }

        // headers to send
        val mergedHeaders: MutableMap<String?, String?> = HashMap()
        if (request.requestHeaders != null) {
          mergedHeaders.putAll(request.requestHeaders)
        }
        if (defaultHeaders != null) {
          for ((key, value) in defaultHeaders) {
            if (key == null) {
              continue
            }
            if (value == null || value.isEmpty()) {
              mergedHeaders.remove(key)
              continue
            }
            mergedHeaders[key] = value
          }
        }
        val urlString = request.url.toString()

        // apply the cookie middleware for this
        // to add in cookie captured by this proxy
        try {
          instance!!.apply(mergedHeaders, request.url.host)
        } catch (e: Exception) {
          // do nothing
        }
        return AjaxRequest(
            request.method,
            urlString,
            "", mergedHeaders, 5000, 0, isSecureMode
        )
      }
    }

    init {
      mUri = Uri.parse(mUrl)
      mBody = body
      mHeaders = headers
      mTimeout = timeout
      mRedirectCount = redirects
      mIsSecure = isSecure
    }
  }
}