package com.monet.bidder.auction;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;

import com.monet.bidder.Constants;
import com.monet.bidder.HttpUtil;
import com.monet.bidder.Logger;
import com.monet.bidder.MonetWebView;
import com.monet.bidder.SdkConfigurations;
import com.monet.bidder.threading.InternalRunnable;

/**
 * Created by jose on 8/28/17.
 */

@SuppressLint("ViewConstructor")
public class AuctionWebView extends MonetWebView {
  private static final Logger sLogger = new Logger("AuctionManager");
  private static final int MAX_LOAD_ATTEMPTS = 5;
  private static final int POST_LOAD_CHECK_DELAY = 6500;
  final SdkConfigurations configurations;
  private final AuctionWebViewParams auctionWebViewParams;

  public AuctionWebView(Context context, MonetJsInterface monetJsInterface, AuctionWebViewParams auctionWebViewParams,
                        SdkConfigurations configurations) {
    super(context);
    this.auctionWebViewParams = auctionWebViewParams;
    this.configurations = configurations;

    // some more settings
    setWebViewClient(auctionWebViewParams.getWebViewClient());
    setJsInterface(monetJsInterface);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && configurations != null) {
      setWebContentsDebuggingEnabled(configurations.getBoolean(Constants.Configurations.WEB_VIEW_DEBUGGING_ENABLED));
    }

    // this manager uses a basic chrome client,
    // the main work here is to forward console messages to our logger
    setWebChromeClient(new WebChromeClient() {
      @Override
      public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
        try {
          callback.invoke(origin, true, true);
        } catch (Exception e) {
          // do nothing
        }
      }

      @Override
      public void onPermissionRequest(PermissionRequest request) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          try {
            request.grant(request.getResources());
          } catch (Exception e) {
            // do nothing
          }
        }
      }

      @Override
      public boolean onConsoleMessage(ConsoleMessage cm) {
        sLogger.forward(cm, cm.message());
        return true;
      }
    });
  }

  /**
   * Load the javascript responsible for most of the auction logic.
   * This has logic to retry in the case of network/load failure.
   *
   * @param stagePageUrl the URL we want to load the auction under
   * @param tries        the number of times we've already tried to load
   */
  private void safelyLoadAuctionPage(final String stagePageUrl, final int tries) {
    try {
      sLogger.info("loading auction manager root: ", auctionWebViewParams.getAuctionHtml());

      // if the interceptor isn't working right
      // just inject the html directly into the webView.
      // we would prefer to access as a URL since this works better
      // within webkit, but can tolerate this.
      if (tries > 1) {
        loadHtml(auctionWebViewParams.getAuctionHtml(), auctionWebViewParams.getAuctionUrl());
      } else {
        sLogger.debug("loading url");
        loadUrl(stagePageUrl);
      }
    } catch (Exception e) {
    }

    // set up a timer to make sure that
    // we've successfully loaded
    setStartDetection(tries);
  }

  /**
   * Begin trying to load the auction javascript engine.
   * This will kick of the load process (on the correct thread)
   *
   * @param tries the number of times we've already tried this
   */
  private void loadAuctionPage(final int tries) {
    // depending, we want to use the different one
    String delimiter = auctionWebViewParams.getAuctionUrl().contains("?") ? "&" : "?";
    final String stagePageUrl =
        auctionWebViewParams.getAuctionUrl() + delimiter + "aid=" +
            auctionWebViewParams.getAppMonetContext().applicationId + "&v=" + Constants.SDK_VERSION;

    if (isDestroyed) {
      sLogger.error("attempt to load into destroyed auction manager.");
      return;
    }

    if (Looper.getMainLooper() == Looper.myLooper()) {
      safelyLoadAuctionPage(stagePageUrl, tries);
      return;
    }

    runOnUiThread(new InternalRunnable() {
      @Override
      public void runInternal() {
        safelyLoadAuctionPage(stagePageUrl, tries);
      }

      @Override
      public void catchException(Exception e) {
        sLogger.error("Exception caught : " + e);
      }
    });
  }

  /**
   * Start an auction
   */
  @Override
  public void start() {
    // load the auction page/stage
    loadAuctionPage(1);
  }

  /**
   * After a delay, check to see if the auction js actually loaded. If it hasn't, try to load
   * again, based on the number of tries we've already executed.
   *
   * @param tries number of times we've tried to load it
   */
  private void setStartDetection(final int tries) {
    final AuctionWebView self = this;
    handler.postDelayed(new InternalRunnable() {
      @Override
      public void runInternal() {
        sLogger.warn("Thread running on: " + Thread.currentThread().getName());
        if (!self.isLoaded.get()) {
          sLogger.warn("javascript not initialized yet. Reloading page");

          // check that the network is actually available.
          // if it's not, we just need to call this again
          // with the same number of tries
          if (!HttpUtil.hasNetworkConnection(getContext())) {
            sLogger.warn("no network connection detecting. Delaying load check");
            setStartDetection(tries);
            return;
          }

          if ((tries + 1) < MAX_LOAD_ATTEMPTS) {
            loadAuctionPage(tries + 1);
          } else {
            sLogger.debug("max load attempts hit");
          }
        } else {
          sLogger.debug("load already detected");
        }
      }

      @Override
      public void catchException(Exception e) {
        sLogger.error("Exception caught: " + e);
      }
    }, POST_LOAD_CHECK_DELAY * tries);
  }

  @Override
  public void executeJs(String method, String... args) {
    if (!isLoaded.get()) {
      sLogger.warn("js not initialized.");
      return;
    }
    super.executeJs(method, args);
  }

  @Override
  public void executeJs(int timeout, String method, String... args) {
    if (!isLoaded.get()) {
      sLogger.warn("js not initialized");
    }
    super.executeJs(timeout, method, args);
  }
}