package com.monet.bidder.auction;

import android.os.Handler;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;

import com.monet.bidder.BaseManager;
import com.monet.bidder.Constants;
import com.monet.bidder.HttpUtil;
import com.monet.bidder.Logger;
import com.monet.bidder.Preferences;
import com.monet.bidder.RemoteConfiguration;
import com.monet.bidder.RenderingUtils;
import com.monet.bidder.adview.AdViewPoolManager;
import com.monet.bidder.bid.BidManager;
import com.monet.bidder.threading.BackgroundThread;
import com.monet.bidder.threading.InternalRunnable;
import com.monet.bidder.threading.UIThread;
import com.monet.bidder.CookieManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.monet.bidder.bid.BidResponse;

import static com.monet.bidder.WebViewUtils.quote;

/**
 * This JSInterface class provides
 * Java-javascript interop into the "auction" webview (where the HB auction takes place).
 * These are not exposed within ads/adviews
 *
 * @see AuctionWebView
 */
public class MonetJsInterface {
  private static final Logger sLogger = new Logger("MonetBridge");
  private static final String JS_CALLBACK = "window['%s'](%s);";
  private final char[] hexArray;
  private final Map<String, ValueCallback<String>> mCallbacks;
  private final String mWebViewUrl;
  private final Preferences mPreferences;
  private final BidManager mBidManager;
  private final RemoteConfiguration remoteConfiguration;
  private final BackgroundThread backgroundThread;
  private final AdViewPoolManager adViewPoolManager;
  private final BaseManager sdkManager;
  private final AuctionManagerCallback auctionManagerCallback;
  private final UIThread uiThread;

  public MonetJsInterface(@NonNull BaseManager sdkManager,
      @NonNull UIThread uiThread,
      @NonNull BackgroundThread backgroundThread,
      @NonNull AuctionWebViewParams auctionWebViewParams,
      @NonNull AuctionManagerCallback auctionManagerCallback,
      @NonNull Preferences preferences, @NonNull RemoteConfiguration remoteConfiguration) {
    this.remoteConfiguration = remoteConfiguration;
    this.uiThread = uiThread;
    this.backgroundThread = backgroundThread;
    mCallbacks = new ConcurrentHashMap<>();
    hexArray = "0123456789ABCDEF".toCharArray();
    mBidManager = sdkManager.getAuctionManager().getBidManager();
    adViewPoolManager = sdkManager.getAuctionManager().getAdViewPoolManager();
    this.sdkManager = sdkManager;
    this.auctionManagerCallback = auctionManagerCallback;
    mWebViewUrl = auctionWebViewParams.getAuctionUrl();
    mPreferences = preferences;
  }

  /**
   * Indicate that the javascript loaded in the webView has finished
   * initialization, and that the webView can be marked as 'init' (onReady handlers called)
   *
   * @return a message indicating that the
   */
  @JavascriptInterface
  public String indicateInit() {
    sLogger.debug("javascript initialized..");
    auctionManagerCallback.onInit();
    return success("init accepted");
  }

  @JavascriptInterface
  public String getAuctionUrl() {
    return mWebViewUrl;
  }

  @JavascriptInterface
  public String setAdUnitNames(String adUnitNameJson) {
    sLogger.debug("Syncing adunit names");
    if (mBidManager.setAdUnitNames(adUnitNameJson)) {
      return success("names set");
    }
    return error("failed to set names");
  }

  @JavascriptInterface
  public String hash(String str, String algorithm) {
    if (algorithm == null) {
      algorithm = "SHA-1";
    }

    try {
      MessageDigest md = MessageDigest.getInstance(algorithm);
      byte[] textBytes = str.getBytes("UTF-8");
      md.update(textBytes, 0, textBytes.length);
      return bytesToHex(md.digest());
    } catch (Exception e) {
      return "";
    }
  }

  @JavascriptInterface
  public String getAvailableBidCount(String adUnitId) {
    int count = mBidManager.countBids(adUnitId);
    return Integer.toString(
        count); // the JS can just go off of the native code; no need to keep our own store
  }

  @JavascriptInterface
  public void getConfiguration(final boolean forceServer, final String cb) {
    backgroundThread.submit(new InternalRunnable() {
      @Override
      public void runInternal() {
        String config = remoteConfiguration.getRemoteConfiguration(forceServer);
        auctionManagerCallback.executeCode(
            String.format(JS_CALLBACK, cb, config));
      }

      @Override
      public void catchException(Exception e) {
        auctionManagerCallback.executeCode(
            String.format(JS_CALLBACK, cb, null));
      }
    });
  }

  @JavascriptInterface
  public String setBidsForAdUnit(String payload) {
    try {
      JSONObject json = new JSONObject(payload);
      JSONArray bidsJson = json.getJSONArray("bids");

      // turn the bids into an array
      List<BidResponse> bids = new ArrayList<>(bidsJson.length());
      for (int i = 0, l = bidsJson.length(); i < l; i++) {
        BidResponse bid = BidResponse.Mapper.from(bidsJson.getJSONObject(i));
        if (bid == null) {
          continue;
        }

        if (bid.getUrl() == null || bid.getUrl().isEmpty()) {
          bid.setUrl(mWebViewUrl);
        }

        bids.add(bid);
      }

      mBidManager.addBids(bids);
    } catch (Exception e) {
      sLogger.error("bad json passed for setBids: " + payload);
      return error("invalid json");
    }
    return success("bids received");
  }

  @JavascriptInterface
  public String ajax(String request) {
    if (request == null || request.isEmpty()) {
      return "{}";
    }

    return HttpUtil.makeRequest(auctionManagerCallback.getMonetWebView(), request);
  }

  @JavascriptInterface
  public String getValue(String request) {
    try {
      JSONObject json = new JSONObject(request);
      String key = json.getString("key");
      String type = json.getString("type");

      if (type.equals("boolean")) {
        return mPreferences.getPref(key, false) ? "true" : "false";
      }

      return mPreferences.getPref(key, "");
    } catch (Exception e) {
      return error("invalid request");
    }
  }

  @JavascriptInterface
  public String setValue(String request) {
    try {
      JSONObject json = new JSONObject(request);
      String key = json.getString("key");
      String type = json.getString("type");
      if (type.equals("boolean")) {
        mPreferences.setPreference(key, json.getBoolean("value"));
      } else {
        mPreferences.setPreference(key, json.getString("value"));
      }
      return success("set value");
    } catch (JSONException e) {
      sLogger.warn("error syncing native preferences: " + e.getMessage());
      return error("invalid request");
    }
  }

  // js interface (bridge methods)
  @JavascriptInterface
  public void getAdvertisingInfo() {
    auctionManagerCallback.getAdvertisingInfo();
  }

  // helpful when/if we need to avoid running
  // when nothing is visible or something..

  @JavascriptInterface
  public String getActivitiesInfo() {
    return TextUtils.join(";", RenderingUtils.getActivitiesInfo());
  }

  @JavascriptInterface
  public String getVisibleActivityCount() {
    return Integer.toString(RenderingUtils.numVisibleActivities());
  }

  @JavascriptInterface
  public String isScreenLocked() {
    return Boolean.toString(
        RenderingUtils.isScreenLocked(auctionManagerCallback.getDeviceData().getContext()));
  }

  @JavascriptInterface
  public String isScreenOn() {
    return Boolean.toString(
        RenderingUtils.isScreenOn(auctionManagerCallback.getDeviceData().getContext()));
  }

  // adView aka "helpers" interface

  @JavascriptInterface
  public String exec(String uuid, final String test) {
    return adViewPoolManager.executeInContext(uuid, test) ?
        success("called") : error("invalid");
  }

  @JavascriptInterface
  public String remove(String uuid) {
    if (uuid == null) {
      return error("empty uuid");
    }
    if (auctionManagerCallback.removeHelper(uuid)) {
      return success("removed");
    }
    return error("failed to remove");
  }

  @JavascriptInterface
  public String requestHelperDestroy(String uuid) {
    if (uuid == null) {
      return error("null uuid");
    }
    if (auctionManagerCallback.requestHelperDestroy(uuid)) {
      return success("requested");
    }
    return error("request failed");
  }

  @JavascriptInterface
  public String getRefCount(String wvUUID) {
    // this way the javascript can determine if
    // it wants to remove one of the helpers
    return Integer.toString(adViewPoolManager.getReferenceCount(wvUUID));
  }

  @JavascriptInterface
  public void getAdViewUrl(final String wvUUID, final String cb) {
    // must be async since getUrl accesses the webView
    uiThread.run(new InternalRunnable() {
      @Override
      public void runInternal() {
        String url = adViewPoolManager.getUrl(wvUUID);
        auctionManagerCallback.executeCode(String.format(JS_CALLBACK, cb, quote(url)));
      }

      @Override
      public void catchException(Exception e) {
        sLogger.warn("Unable to get url", e.getMessage());
      }
    });
  }

  @JavascriptInterface
  public String getNetworkCount(String wvUUID) {
    return Integer.toString(adViewPoolManager.getNetworkCount(wvUUID));
  }

  @JavascriptInterface
  public String getHelperCreatedAt(String wvUUID) {
    // this is in milliseconds, so can be used with javascript timestamp
    return Long.toString(adViewPoolManager.getCreatedAt(wvUUID));
  }

  @JavascriptInterface
  public String getHelperRenderCount(String wvUUID) {
    return Integer.toString(adViewPoolManager.getRenderCount(wvUUID));
  }

  @JavascriptInterface
  public String getHelperState(String wvUUID) {
    return adViewPoolManager.getState(wvUUID);
  }

  @JavascriptInterface
  public boolean reloadConfigurations() {
    return sdkManager.reloadConfigurations();
  }

  @JavascriptInterface
  public String launch(final String requestID, String url, String ua, String html, String widthStr,
      String heightStr, String adUnitId) {
    // try to parse the integers
    Integer height;
    Integer width;

    try {
      width = Integer.parseInt(widthStr);
      height = Integer.parseInt(heightStr);
    } catch (NumberFormatException e) {
      return error("invalid integer");
    }
    if (url == null || ua == null || adUnitId == null) {
      return error("null values");
    }
    auctionManagerCallback.loadHelper(url, ua, html, width, height, adUnitId,
        value -> auctionManagerCallback.executeJs("helperReady", "'" + requestID + "'",
            "'" + value.getUuid() + "'"));

    return success("created");
  }

  @JavascriptInterface
  public void resetCookieManager() {
    CookieManager.getInstance().clear();
  }

  @JavascriptInterface
  public void loadCookieManager() {
    CookieManager.getInstance().load(auctionManagerCallback.getDeviceData().getContext());
  }

  @JavascriptInterface
  public void saveCookieManager() {
    CookieManager.getInstance().save(auctionManagerCallback.getDeviceData().getContext());
  }

  @JavascriptInterface
  public String getVMState() {
    JSONObject json = auctionManagerCallback.getDeviceData().getVMStats();
    return json.toString();
  }

  @JavascriptInterface
  public String trigger(String eventName, String response) {
    synchronized (mCallbacks) {
      if (eventName == null || response == null) {
        return error("null");
      }

      ValueCallback<String> callback = mCallbacks.get(eventName);
      if (callback == null) {
        return error("no callback");
      }

      try {
        callback.onReceiveValue(response);
        mCallbacks.remove(eventName);
      } catch (Exception e) {
        sLogger.warn("trigger error:", e.getMessage());
        return error(e.getMessage());
      }

      return success("received");
    }
  }

  @JavascriptInterface
  public String getDeviceData() {
    return auctionManagerCallback.getDeviceData().toJSON();
  }

  @JavascriptInterface
  public String getSharedPreference(String key, String subKey, String keyType,
      boolean defaultBool) {
    if (keyType.equals("string")) {
      return Preferences.getStringAtKey(auctionManagerCallback.getDeviceData().getContext(), key,
          subKey, "null");
    }

    if (keyType.equals("boolean")) {
      return Boolean.toString(
          Preferences.getBoolAtKey(auctionManagerCallback.getDeviceData().getContext(), key, subKey,
              defaultBool));
    }

    return "null";
  }

  @JavascriptInterface
  public void subscribeKV(String key, String valueType) {
    try {
      if (valueType != null && key != null) {
        mPreferences.keyValueFilter.put(key, valueType);
      }
    } catch (Exception e) {
      sLogger.error("Error subscribing kV");
    }
  }

  /**
   * Subscribe to an event only once. When the event is emitted, the callback
   * will receive the payload supplied with the event (triggered from JavaScript)
   *
   * @param eventName the name of the event (usually a message UUID)
   * @param callback the handler to be called when the eventName is received
   */
  public synchronized void listenOnce(final String eventName, int timeout, final Handler handler,
      final ValueCallback<String> callback) {
    final Object handlerToken = new Object();
    final ValueCallback<String> onEvent = new ValueCallback<String>() {
      @Override
      public void onReceiveValue(String value) {
        try {
          handler.removeCallbacksAndMessages(handlerToken);
          callback.onReceiveValue(value);
        } catch (Exception e) {
        }
        // remove the listener
        removeListener(eventName, this);
      }
    };
    Runnable timeoutRunnable = new Runnable() {
      @Override
      public void run() {
        removeListener(eventName, onEvent);
        callback.onReceiveValue(null);
      }
    };
    handler.postAtTime(timeoutRunnable, handlerToken, SystemClock.uptimeMillis() + timeout);
    mCallbacks.put(eventName, onEvent);
  }

  private String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }

  /**
   * Remove a listener for the specified event. Since the eventemitter
   * behavior of this class is really only fire-once, we can clean up
   * the listeners as soon as an event is fired.
   *
   * @param eventName the name of the event the listener is subscribed to
   * @param callback the instance of ValueCallback listening
   */
  synchronized void removeListener(String eventName, ValueCallback<String> callback) {
    if (mCallbacks.containsKey(eventName) && mCallbacks.get(eventName) == callback) {
      mCallbacks.remove(eventName);
    }
  }

  /**
   * Return a JSON error message with the given string
   *
   * @param message an error message string (must not contain double quotes)
   * @return a JSON-formatted error message to be sent to javascript
   */
  private String error(String message) {
    return "{\"error\": \"" + message + "\"}";
  }

  /**
   * Format a "success" response as json to be delivered into the webView
   *
   * @param message the message to be returned as JSON. Must not contain double quotes
   * @return a JSON-encoded form of the message
   */
  private String success(String message) {
    return "{\"success\": \"" + message + "\"}";
  }
}
