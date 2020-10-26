package com.monet.bidder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.monet.bidder.auction.MonetJsInterface;
import com.monet.bidder.threading.InternalRunnable;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by jose on 8/28/17.
 */

public class MonetWebView extends WebView implements AppMonetWebView {
  protected static final String WV_TYPE_UA = "ua";
  private static final String WV_TYPE_CK = "ck";
  private static final Logger sLogger = new Logger("MWV");
  protected MonetJsInterface mJsInterface;
  public boolean isDestroyed;
  protected Handler handler;
  public final AtomicBoolean isLoaded = new AtomicBoolean(false);

  protected MonetWebView(Context context) {
    super(context);
    isDestroyed = false;
    handler = new Handler();
    initialize();
  }

  @Override
  public void destroy() {
    try {
      destroyWebView();
    } catch (Exception e) {
      sLogger.warn("failed to properly destroy webView!");
    }

    isDestroyed = true;
    super.destroy();
  }

  @NotNull
  @Override
  public AtomicBoolean isLoaded() {
    return isLoaded;
  }

  @Override
  public void start() {
    //to be implemented by extension
  }

  @Override
  public void trackEvent(String eventName, String detail, String key, float value,
      long currentTime) {
    if (currentTime <= 0) {
      currentTime = System.currentTimeMillis();
    }
    executeJs("trackRequest", eventName, detail, String.valueOf(value),
        String.valueOf(currentTime));
  }

  @SuppressLint({ "JavascriptInterface", "AddJavascriptInterface" })
  protected void setJsInterface(MonetJsInterface jsInterface) {
    mJsInterface = jsInterface;
    if (mJsInterface != null) {
      addJavascriptInterface(mJsInterface, Constants.JS_BRIDGE_VARIABLE);
    }
  }

  protected void loadHtml(String html, String baseUrl) {
    if (baseUrl == null || html == null) {
      sLogger.warn("url or html is null");
      return;
    }

    if (isDestroyed) {
      sLogger.warn("attempt to load HTML in destroyed state");
      return;
    }

    try {
      loadDataWithBaseURL(
          baseUrl,
          html,
          "text/html",
          "UTF-8",
          null
      );
    } catch (Exception e) {
    }
  }

  @Override
  public void executeJs(String method, String... args) {
    executeJs(0, method, null, args);
  }

  @Override
  public void executeJs(int timeout, String method, String... args) {
    executeJs(timeout, method, null, args);
  }

  /**
   * Similar to executeJsAsync, except we block & wait on the result for the given timeout
   *
   * @param timeout number of milliseconds to wait for a result
   * @param method method to be called on window.monet
   * @param args string arguments to pass to javascript method
   * @return the result of javascript execution
   */

  @Override
  public void executeJs(int timeout, String method, ValueCallback<String> callback,
      String... args) {
    if (isDestroyed) {
      callback.onReceiveValue(null);
    }
    sLogger.debug("executing js with timeout - " + timeout);
    executeJsAsync(callback, method, timeout, args);
  }

  /**
   * Execute a method on our javascript API (e.g., window.monet) and return a result
   * asynchronously.
   * Note that this requires the method being called to implement the follow signature (javascript,
   * types in Flowtype):
   * <p>
   * monet[{FUNCTION_NAME}] = (args: Array<string>, (string) => null) => null
   * <p>
   * We pass that function a callback as the last argument, which itself calls a bridge method
   * 'trigger', sending a message
   * identified by UUID back through to the webView.
   *
   * @param callback The callback for the javascript response.
   * @param method The javascript method to invoke.
   * @param args The arguments to be passed to the javascript method.
   */
  @Override
  public void executeJsAsync(final ValueCallback<String> callback, String method,
      int timeout, String... args) {
    final String cbName = "cb__" + UUID.randomUUID().toString();
    String argStr = TextUtils.join(",", args);

    if (argStr.equals("")) {
      argStr = "null";
    }

    String jsCallback =
        String.format(Constants.JSMethods.JS_ASYNC_CALLBACK, Constants.JS_BRIDGE_VARIABLE, cbName);

    String jsCall =
        String.format(Constants.JSMethods.JS_CALL_TEMPLATE, Constants.JSMethods.INTERFACE_NAME,
            method, argStr, jsCallback);

    //if callback is null then we don't care about the response from javascript.
    if (mJsInterface != null && callback != null && timeout > 0) {
      mJsInterface.listenOnce(cbName, timeout, handler, callback);
    }
    // exec immediately if there is a problem
    if (!executeJsCode(jsCall)) {
      mJsInterface.trigger(cbName, "{\"error\": true }");
    }
  }

  /**
   * Lowest-level javascript execution helper. Run a string of Javascript code.
   * Supports pre `evaluateJavascript` webView API.
   *
   * @param javascript a string of javascript code to be evaluated in the webView
   */
  @Override
  public boolean executeJsCode(final String javascript) {
    // make sure we're on *this* thread
    Runnable jsExecute = new Runnable() {
      @Override
      public void run() {
        String basicJsExec = "javascript:(function() { " + javascript + "}());";
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
          try {
            evaluateJavascript(javascript, new ValueCallback<String>() {
              @Override
              public void onReceiveValue(String value) {
              }
            });
          } catch (Exception e) {
            // fall back to trying to call the URL
            try {
              loadUrl(basicJsExec);
            } catch (Exception err) {
            }
          }
        } else {
          try {
            loadUrl(basicJsExec);
          } catch (Exception e) {
          }
        }
      }
    };

    return runOnUiThread(jsExecute);
  }

  /**
   * Safely run the runnable on the UI thread (the webView's thread)
   *
   * @param runnable code to execute on ui thread (webView thread)
   * @return if it was able to execute succesfully
   */
  boolean runOnUiThread(final Runnable runnable, final boolean allowDestroyed) {
    if (isDestroyed) {
      return false;
    }

    try {
      handler.post(new InternalRunnable() {
        @Override
        public void runInternal() {
          if (isDestroyed && !allowDestroyed) {
            sLogger.warn("attempt to execute webView runnable on destroyed wv");
            return;
          }

          if (runnable != null) {
            runnable.run();
          }
        }

        @Override
        public void catchException(Exception e) {
        }
      });
    } catch (Exception e) {
      return false;
    }

    return true;
  }

  protected boolean runOnUiThread(Runnable runnable) {
    return runOnUiThread(runnable, false);
  }

  /**
   * Check to see if we're currently running on the UI thread
   *
   * @return are we on the UI thread? (webView thread)
   */
  protected boolean onUIThread() {
    return Looper.getMainLooper().getThread() == Thread.currentThread();
  }

  /**
   * Clean up the webView as well as possible
   */
  private void destroyWebView() {
    // make sure we do this
    if (getParent() != null && getParent() instanceof ViewGroup) {
      ((ViewGroup) getParent()).removeView(this);
    }

    try {
      setTag(null);
    } catch (Exception e) {
      sLogger.error("failed to clean up webView");
    }

    removeAllViews();
  }

  protected void uiInitialize() {
    WebSettings settings = getSettings();
    setHorizontalScrollBarEnabled(false);
    setVerticalScrollBarEnabled(false);
    settings.setSupportZoom(false);
    settings.setSupportMultipleWindows(true);
    settings.setLoadWithOverviewMode(true);
    setBackgroundColor(Color.TRANSPARENT);
    setDefaultLayerType(LAYER_TYPE_HARDWARE);

    try {
      settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
      settings.setPluginState(WebSettings.PluginState.ON);
    } catch (Exception e) {
      // don't do anything
    }
  }

  @SuppressLint("SetJavaScriptEnabled")
  private void initialize() {
    WebSettings settings = getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setAllowFileAccess(false);

    //        if (Looper.getMainLooper().equals(Looper.myLooper())) {
    //            uiInitialize();
    //        }

    settings.setGeolocationEnabled(true);

    setCaching(true);
    allowCookies();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      settings.setAllowFileAccessFromFileURLs(false);
      settings.setAllowUniversalAccessFromFileURLs(false);
    }
  }

  protected void setCaching(boolean enabled) {
    WebSettings settings = getSettings();
    settings.setDomStorageEnabled(enabled);
    settings.setDatabaseEnabled(enabled);
    settings.setAppCachePath(getContext().getCacheDir().getAbsolutePath());
    settings.setAppCacheEnabled(enabled);
    settings.setCacheMode(enabled ? WebSettings.LOAD_DEFAULT : WebSettings.LOAD_NO_CACHE);
    settings.setSavePassword(enabled);
    settings.setSaveFormData(enabled);
  }

  private void setDefaultLayerType(int layerType) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      setLayerType(layerType, null);
      return;
    }

    setLayerType(LAYER_TYPE_SOFTWARE, null);
  }

  private void allowCookies() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      CookieManager.getInstance().setAcceptCookie(true);
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      CookieManager.getInstance().setAcceptThirdPartyCookies(this, true);
    }
  }

  @Override
  public boolean isDestroyed() {
    return isDestroyed;
  }

  @Override
  public void loadView(String url) {
    this.loadUrl(url);
  }
}
