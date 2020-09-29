package com.monet.bidder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.text.TextUtils;
import android.util.Log;
import android.webkit.URLUtil;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import com.monet.BuildConfig;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

public class HttpUtil {
  private static final Logger sLogger = new Logger("Tracking");
  private static final Random sRandom = new Random();

  private static class Request extends AsyncTask<String, Integer, Integer> {

    @SuppressLint("DefaultLocale")
    @Override
    protected Integer doInBackground(String... params) {
      if (params == null) {
        return 400;
      }

      try {
        HttpRequest request = new HttpRequest(params[0], params[1]);
        request.disableSslv3();
        request.connectTimeout(15000);
        request.header(Constants.EventParams.HEADER_VERSION, Constants.SDK_VERSION);
        request.header(Constants.EventParams.HEADER_CLIENT, Constants.SDK_CLIENT);
        request.header(HttpRequest.HEADER_USER_AGENT, "AppMonet/SDK " + Constants.SDK_VERSION);
        if (params.length > 2) {
          request.send(RenderingUtils.base64Encode(params[2]));
        }
        return request.code();
      } catch (Exception e) {
        sLogger.debug("Error firing network call. " + e);
        return 400;
      }
    }
  }

  /**
   * Checks if there is network connectivity.
   *
   * @param context from the app to check for connectivity
   * @return boolean value indicating if there is a network connection
   */
  public static boolean hasNetworkConnection(Context context) {
    ConnectivityManager connectivityManager = (ConnectivityManager) context
        .getSystemService(Context.CONNECTIVITY_SERVICE);

    if (connectivityManager == null) {
      return false;
    }

    NetworkInfo nInfo = connectivityManager.getActiveNetworkInfo();
    return nInfo != null && nInfo.isConnected();
  }

  static String resolveLocationUrl(Uri baseUrl, String locationHeader) {
    // it's a relative redirect; we need to resolve it
    if (!URLUtil.isValidUrl(locationHeader)) {
      if (baseUrl == null) {
        return null;
      }

      Uri additional = Uri.parse(locationHeader);
      Uri.Builder builder = baseUrl.buildUpon().path(additional.getPath())
          .clearQuery();

      // this is because just doing 'builder.query(additional.getQuery())'
      // was leaving it encoded.. which doesn't work
      for (String name : additional.getQueryParameterNames()) {
        builder.appendQueryParameter(name, additional.getQueryParameter(name));
      }

      builder.fragment(additional.getFragment());
      return builder.toString();
    }

    return locationHeader;
  }


  static class AjaxRequest {
    private static final String ACCESS_CONTROL_ORIGIN = "Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_METHOD = "Access-Control-Allow-Methods";
    private static final Pattern sInvalidRequestPattern = Pattern.compile("\\.(?:mp4|webm|jsv)");
    private static final int MAX_REDIRECTS = 15;

    private String mMethod;
    private String mUrl;
    private Uri mUri;
    private String mBody;
    private String mCallback;
    private Map<String, String> mHeaders;
    private int mTimeout;
    private int mRedirectCount;
    private boolean mIsSecure;


    private AjaxRequest(String method, String url, String body, Map<String, String> headers, int timeout, int redirects, boolean isSecure) {
      mMethod = method;
      mUrl = url;
      mUri = Uri.parse(url);
      mBody = body;
      mHeaders = headers;
      mTimeout = timeout;
      mRedirectCount = redirects;
      mIsSecure = isSecure;
    }

    private static boolean shouldBeRequested(WebResourceRequest request, boolean isSecureMode) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        String url = request.getUrl().toString();
        if (sInvalidRequestPattern.matcher(url).matches() && !isSecureMode) {
          return false;
        }

        String method = request.getMethod();
        if (method.equals("GET") || method.equals("OPTIONS") || method.equals("HEAD")) {
          return true;
        }
      }
      return false;
    }

    static AjaxRequest from(WebResourceRequest request, Map<String, String> defaultHeaders, boolean isSecureMode) {
      if (!shouldBeRequested(request, isSecureMode)) {
        return null;
      }

      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        return null;
      }

      // headers to send
      Map<String, String> mergedHeaders = new HashMap<>();
      if (request.getRequestHeaders() != null) {
        mergedHeaders.putAll(request.getRequestHeaders());
      }

      if (defaultHeaders != null) {
        for (Map.Entry<String, String> kvp : defaultHeaders.entrySet()) {
          String key = kvp.getKey();
          String value = kvp.getValue();

          if (key == null) {
            continue;
          }

          if (value == null || value.isEmpty()) {
            mergedHeaders.remove(key);
            continue;
          }

          mergedHeaders.put(key, value);
        }
      }

      String urlString = request.getUrl().toString();

      // apply the cookie middleware for this
      // to add in cookie captured by this proxy
      try {
        CookieManager.getInstance().apply(mergedHeaders, request.getUrl().getHost());
      } catch (Exception e) {
        // do nothing
      }

      return new AjaxRequest(
          request.getMethod(),
          urlString,
          "", mergedHeaders, 5000, 0, isSecureMode);
    }

    private boolean hasBody() {
      return (mMethod.equals("POST") ||
          mMethod.equals("PUT") ||
          mMethod.equals("PATCH")) &&
          mBody != null && mBody.length() > 0;
    }

    private HttpURLConnection toRequest() {
      URL url;
      try {
        url = new URL(mUrl);
      } catch (MalformedURLException e) {
        return null;
      }

      HttpURLConnection urlConnection;
      try {
        urlConnection = (HttpURLConnection) url.openConnection();
      } catch (IOException e) {
        return null;
      }

      try {
        urlConnection.setRequestMethod(mMethod);
      } catch (ProtocolException e) {
        return null;
      }

      for (Map.Entry<String, String> kvp : mHeaders.entrySet()) {
        urlConnection.setRequestProperty(
            kvp.getKey(), kvp.getValue());
      }

      urlConnection.setInstanceFollowRedirects(false);
      urlConnection.setUseCaches(false);
      urlConnection.setDoInput(true);

      if (hasBody()) {
        urlConnection.setDoOutput(true);
        try {
          urlConnection.getOutputStream()
              .write(mBody.getBytes(Charset.forName("UTF-8")));
        } catch (IOException e) {
          return null;
        }
      }

      urlConnection.setReadTimeout(mTimeout);
      return urlConnection;
    }

    private String getReasonString(String found) {
      if (found != null && found.length() > 0) {
        return found;
      }

      return "OK";
    }

    private Map<String, String> getCorsHeaders(Map<String, String> headers) {
      Map<String, String> cors = new HashMap<>();
      if (!headers.containsKey(ACCESS_CONTROL_ORIGIN)) {
        cors.put(ACCESS_CONTROL_ORIGIN, "*");
      }

      if (!headers.containsKey(ACCESS_CONTROL_METHOD)) {
        cors.put(ACCESS_CONTROL_METHOD, "GET,HEAD,POST,PUT,DELETE");
      }

      return cors;
    }

    private WebResourceResponse getErrorResponse(int status) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        return new WebResourceResponse(
            "text/plain",
            "UTF-8",
            status,
            "ERROR",
            mHeaders,
            null);
      }

      return null;
    }

    private String getMimeType(String contentType) {
      if (contentType == null || contentType.isEmpty()) {
        return "text/plain";
      }

      if (contentType.indexOf(';') == -1) {
        return contentType;
      }

      // otherwise, split it and only send the first part
      String[] parts = contentType.split(";");
      return parts[0];
    }

    private InputStream readStreamBinary(InputStream input) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      int bufferSize = 1024;
      byte[] buffer = new byte[bufferSize];

      int len = 0;
      try {
        while ((len = input.read(buffer)) != -1) {
          outputStream.write(buffer, 0, len);
        }
      } catch (IOException e) {
        return new ByteArrayInputStream(outputStream.toByteArray());
      }

      return new ByteArrayInputStream(outputStream.toByteArray());
    }

    WebResourceResponse execute() {
      if (mRedirectCount > MAX_REDIRECTS) {
        return getErrorResponse(500);
      }

      if (mUrl == null || mUrl.isEmpty()) {
        return getErrorResponse(400);
      }

      HttpURLConnection http = toRequest();
      if (http == null) {
        return null;
      }

      Map<String, List<String>> headersMap = http.getHeaderFields();
      int responseCode;

      try {
        responseCode = http.getResponseCode();
      } catch (IOException e) {
        return null;
      }

      switch (responseCode) {
        case 307:
        case 308:
        case 303:
        case HttpURLConnection.HTTP_MOVED_PERM:
        case HttpURLConnection.HTTP_MOVED_TEMP:
          AjaxRequest redirect = new AjaxRequest(
              "GET",
              resolveLocationUrl(mUri, http.getHeaderField("Location")),
              "",
              mHeaders,
              mTimeout,
              mRedirectCount + 1,
              mIsSecure
          );

          return redirect.execute();
        case 404:
          return getErrorResponse(404);
      }

      Map<String, String> definitiveHeaders = new HashMap<>();
      for (Map.Entry<String, List<String>> kvp : headersMap.entrySet()) {
        definitiveHeaders.put(kvp.getKey(), kvp.getValue().get(0));
      }

      if (definitiveHeaders.containsKey(CookieManager.SET_COOKIE_HEADER)) {
        CookieManager.getInstance().add(definitiveHeaders.get(CookieManager.SET_COOKIE_HEADER));
      }

      // don't allow the WV to get the headers
      definitiveHeaders.remove(CookieManager.SET_COOKIE_HEADER);

      // add CORS headers
      definitiveHeaders.putAll(getCorsHeaders(definitiveHeaders));
      definitiveHeaders.remove("Content-Type");

      String contentBody = "";
      String contentType = http.getContentType();
      if (contentType == null || contentType.length() == 0) {
        contentType = "text/plain";
      }

      String mimeType = getMimeType(contentType);
      InputStream responseInputStream = null;

      try {
        InputStream bodyStream = (InputStream) http.getContent();
        responseInputStream = readStreamBinary(bodyStream);
      } catch (Exception e) {
        // do nothing
      }


      definitiveHeaders.remove("Content-Length");

      // attach the headers to the response
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        String contentEncoding = http.getContentEncoding();

        // skip for video
        if ((contentType.contains("video") || contentType.contains("media")) && !mIsSecure) {
          sLogger.warn("Attempt to optimize video incomplete. Skipping");
          return null;
        }

        try {
          if (definitiveHeaders.containsKey(null)) {
            definitiveHeaders.remove(null);
          }

          InputStream input = responseInputStream == null ?
              http.getInputStream() : responseInputStream;

          return new WebResourceResponse(
              mimeType,
              contentEncoding,
              responseCode,
              getReasonString(http.getResponseMessage()),
              definitiveHeaders,
              input);

        } catch (IOException e) {
          return new WebResourceResponse(
              mimeType,
              contentEncoding,
              500,
              "OK",
              definitiveHeaders,
              null);
        }
      }

      return null;
    }
  }

  private HttpUtil() {
  }

  private static AjaxRequest requestFromJson(String jsonStr) {
    try {
      if (jsonStr == null) {
        sLogger.error("null json string");
        return null;
      }

      JSONObject json = new JSONObject(jsonStr);
      AjaxRequest ajax = new AjaxRequest(
          json.getString("method"),
          json.getString("url"),
          json.getString("body"),
          new HashMap<String, String>(),
          json.getInt("timeout"),
          0,
          false
      );

      ajax.mCallback = json.getString("callback");
      JSONObject headers = json.getJSONObject("headers");
      for (Iterator<String> it = headers.keys(); it.hasNext(); ) {
        String k = it.next();
        if (k == null) {
          continue;
        }

        ajax.mHeaders.put(k, headers.getString(k));
      }
      return ajax;
    } catch (JSONException e) {
      return null;
    }
  }

  private static String error(String message) {
    return String.format("{\"error\":\"%s\"}", message);
  }

  @NonNull
  private static String readStream(@Nullable InputStream stream) {
    String output = "";
    if (stream == null) {
      return "";
    }

    InputStreamReader isr = new InputStreamReader(stream);
    try {
      BufferedReader reader = new BufferedReader(isr);
      StringBuilder builder = new StringBuilder();
      String line;

      while ((line = reader.readLine()) != null) {
        builder.append(line).append('\n');
      }

      output = builder.toString();
    } catch (IOException e) {
      sLogger.debug("error reading input stream");
    } finally {
      try {
        isr.close();
      } catch (Exception e) {
        sLogger.debug("error closing stream");
      }
    }
    return output;
  }

  static String formatError(Throwable error) {
    String stack = Log.getStackTraceString(error);

    return error.getMessage() + "|" +
        stack.replace(error.getMessage(), "")
            .replace('\n', '|')
            .replaceAll("\\s+", " ") // reduce whitespace
            .substring(0, 200);
  }

  static void fireTrackingPixel(AppMonetContext appMonetContext, String action, String headerData) {
    String url = Uri.parse(Constants.TRACKING_URL + action)
        .buildUpon()
        .appendQueryParameter(Constants.EventParams.APPLICATION_ID, appMonetContext.applicationId)
        .appendQueryParameter(Constants.EventParams.VERSION, BuildConfig.VERSION_NAME)
        .build().toString();
    firePixelAsync(url);
  }

  public static void firePixelAsync(String pixelURL) {
    try {
      new Request().execute(pixelURL, HttpRequest.METHOD_GET);
    } catch (Exception e) {
      sLogger.warn("error firing pixel: ", pixelURL);
    }
  }

  public static String makeRequest(@Nullable AppMonetWebView webView, String request) {
    AjaxRequest ajax = requestFromJson(request);

    if (ajax == null) {
      return error("invalid request");
    }

    HttpURLConnection http = ajax.toRequest();

    if (http == null) {
      return error("invalid http request");
    }

    String body;

    try {
      body = readStream(http.getInputStream());
    } catch (IOException e) {
      body = readStream(http.getErrorStream());
    }

    respondWith(webView, ajax, http, body);
    return "{\"success\":\"made request\"}";
  }

  private static void respondWith(@Nullable AppMonetWebView webView, AjaxRequest ajax, HttpURLConnection http, String body) {
    JSONObject response = new JSONObject();

    // read the error
    Map<String, List<String>> headersMap = http.getHeaderFields();
    JSONObject headers = new JSONObject();

    for (Map.Entry<String, List<String>> kvp : headersMap.entrySet()) {
      String key = kvp.getKey();
      List<String> value = kvp.getValue();

      if (key == null || value == null) {
        continue;
      }

      try {
        headers.put(key, TextUtils.join(",", value));
      } catch (JSONException e) {
        // do nothign
      }
    }

    try {
      int statusCode = http.getResponseCode();
      if (statusCode > 299 && statusCode < 400) {
        statusCode = 204;
      }

      response.put("url", http.getURL());
      response.put("status", statusCode);
      response.put("headers", headers);
      response.put("body", body);
    } catch (JSONException e) {
      sLogger.warn("invalid json in request", e.getMessage());
      return;
    } catch (Exception e) {
      sLogger.warn("Unexpected error in response formation:", e.getMessage());
      return;
    }

    if (webView == null || webView.isDestroyed()) {
      sLogger.warn("attempt to return response into destroyed webView");
      return;
    }

    // write it to the webview
    webView.executeJsCode(
        String.format(
            "window['%s'](%s);",
            ajax.mCallback,
            response.toString()
        )
    );
  }

}
