package com.monet.bidder;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.webkit.ValueCallback;

import androidx.annotation.Nullable;

import com.monet.bidder.threading.InternalRunnable;
import com.mopub.mobileads.MoPubInterstitial;
import com.mopub.mobileads.MoPubView;
import com.mopub.nativeads.MoPubNative;
import com.mopub.nativeads.RequestParameters;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import static com.monet.bidder.Constants.Configurations.MEDIATION_ENABLED;
import static com.monet.bidder.Constants.MISSING_INIT_ERROR_MESSAGE;

/**
 * Created by jose on 8/28/17.
 */

class SdkManager extends BaseManager {
  private final static Logger sLogger = new Logger("SdkManager");
  private static final Object LOCK = new Object();
  private static final int RE_INIT_DELAY = 1000;
  private static SdkManager sInstance;
  private final Map<String, WeakReference<MoPubView>> mopubAdViews = new HashMap<>();
  private final Map<String, AppMonetFloatingAdConfiguration> positions = new HashMap<>();
  static AppMonetConfiguration appMonetConfiguration;
  WeakReference<Activity> currentActivity;

  protected SdkManager(Context context, String applicationId) {
    super(context, applicationId, new MopubAdServerWrapper());
  }

  MoPubView getMopubAdView(String adUnitId) {
    WeakReference<MoPubView> mopubAdView = mopubAdViews.get(adUnitId);
    return (mopubAdView != null) ? mopubAdView.get() : null;
  }

  AppMonetFloatingAdConfiguration getFloatingAdPosition(String adUnitId) {
    return positions.get(adUnitId);
  }

  static void initializeThreaded(final Context context,
      final AppMonetConfiguration appMonetConfiguration) {
    HandlerThread handlerThread =
        new HandlerThread("monet-handler", Process.THREAD_PRIORITY_BACKGROUND);
    handlerThread.start();
    Looper looper = handlerThread.getLooper();
    Handler handler = new Handler(looper);
    handler.post(new InternalRunnable() {
      @Override
      public void runInternal() {
        SdkManager.initialize(context,
            appMonetConfiguration == null ? new AppMonetConfiguration.Builder().build()
                : appMonetConfiguration);
      }

      @Override
      public void catchException(Exception e) {
        sLogger.error("failed to initialize AppMonet SDK: " + e.getLocalizedMessage());
      }
    });
  }

  @SuppressLint("DefaultLocale")
  static void initialize(final Context context, final AppMonetConfiguration appMonetConfiguration) {
    int currentVersion = Build.VERSION.SDK_INT;
    if (currentVersion < Build.VERSION_CODES.JELLY_BEAN) {
      sLogger.warn(
          String.format("Warning! Sdk v%d is not supported. AppMonet SDK Disabled",
              currentVersion));
      return;
    }

    try {
      synchronized (LOCK) {
        if (sInstance != null) {
          sLogger.debug("Sdk has already been initialized. No need to initialize it again.");
          return;
        }
        SdkManager.appMonetConfiguration = appMonetConfiguration;
        sInstance =
            new SdkManager(context.getApplicationContext(), appMonetConfiguration.applicationId);
      }
    } catch (Exception e) {
      if (initRetry < 3) {
        sLogger.error("error initializing ... retrying " + e);
        initRetry += 1;

        Handler handler = new Handler(context.getMainLooper());
        handler.postDelayed(new InternalRunnable() {
          @Override
          public void runInternal() {
            initialize(context, appMonetConfiguration);
          }

          @Override
          public void catchException(Exception e) {
            sLogger.error("Error re-init @ context", e.getMessage());
          }
        }, RE_INIT_DELAY);
      }
    }
  }

  static SdkManager get() {
    return get("");
  }

  @Nullable
  static SdkManager get(String reason) {
    synchronized (LOCK) {
      if (sInstance == null) {
        sLogger.debug(String.format(MISSING_INIT_ERROR_MESSAGE, reason));
      }

      return sInstance;
    }
  }

  MoPubView addBids(MoPubView adView, String adUnitId) {
    logState();

    if (adView == null) {
      sLogger.warn("attempt to add bids to nonexistent AdView");
      return null;
    }

    if (adView.getAdUnitId() == null) {
      sLogger.warn("Mopub adunit id is null. Unable to fetch bids for unit");
      return adView;
    }

    SdkConfigurations config = getSdkConfigurations();
    if (config.getBoolean(MEDIATION_ENABLED)) {
      sLogger.debug("Mediation mode is enabled. Ignoring explicit addBids()");
      return adView;
    }

    MopubAdView mpView = new MopubAdView(adView);
    if (!adUnitId.equals(adView.getAdUnitId())) {
      mpView.setAdUnitId(adUnitId);
    }

    AdServerAdRequest baseRequest;
    registerView(adView, adUnitId);
    AdServerAdRequest request;
    try {
      baseRequest = new MopubAdRequest(adView);
      request = getAuctionManager().addBids(mpView, baseRequest);
    } catch (NullPointerException exp) {
      return null;
    }

    if (request != null) {
      if (request.hasBid()) {
        sLogger.info("found bids for view. attaching");
      } else {
        sLogger.debug("no bids available for request.");
      }

      ((MopubAdRequest) request).applyToView(mpView);
    }
    mopubAdViews.put(mpView.getAdUnitId(), new WeakReference<>(adView));

    return mpView.getMopubView();
  }

  void addBids(final MoPubInterstitial moPubInterstitial, final String alternateAdUnitId,
      final int timeout, final ValueCallback<MoPubInterstitial> onDone) {
    getAuctionManager().timedCallback(timeout, new TimedCallback() {
      @Override
      public void execute(final int remainingTime) {
        if (!isInterstitialActivityRegistered(
            moPubInterstitial.getActivity().getApplicationContext(),
            MonetActivity.class.getName())) {
          String error = "Unable to create activity. Not defined in AndroidManifest.xml. " +
              "Please refer to https://docs.appmonet.com/ for integration infomration.\n";
          sLogger.error(error);
          //todo refactor this.
          getAuctionManager().auctionWebView.trackEvent("integration_error",
              "missing_interstitial_activity", alternateAdUnitId, 0f, 0L);

          throw new ActivityNotFoundException(error);
        }
        logState();
        // todo -> do we need this?
        //        if (appMonetBidder == null) {
        //          onDone.onReceiveValue(moPubInterstitial);
        //          return;
        //        }

        final MopubInterstitialAdView mpView =
            new MopubInterstitialAdView(moPubInterstitial, alternateAdUnitId);
        getAuctionManager().addBids(mpView, new MopubInterstitialAdRequest(moPubInterstitial),
            remainingTime,
            value -> {
              // apply the request to the ad view, and pass that back
              MopubInterstitialAdRequest request = (MopubInterstitialAdRequest) value;
              request.applyToView(mpView);
              onDone.onReceiveValue(moPubInterstitial);
            }
        );
      }

      @Override
      public void timeout() {
        trackTimeoutEvent(alternateAdUnitId, timeout);
        onDone.onReceiveValue(moPubInterstitial);
      }
    });
  }

  void addBids(final MoPubView adView, final String alternateAdUnitId, final int timeout,
      final ValueCallback<MoPubView> onDone) {
    getAuctionManager().timedCallback(timeout, new TimedCallback() {

      @Override
      public void execute(int remainingTime) {
        logState();
        final MopubAdView mpView = new MopubAdView(adView);
        if (adView.getAdUnitId() == null) {
          sLogger.warn("Mopub adunit id is null. Unable to fetch bids for unit");
          onDone.onReceiveValue(adView); // can't continue
          return;
        }

        String adUnit = (alternateAdUnitId == null) ? adView.getAdUnitId() : alternateAdUnitId;
        if (!adView.getAdUnitId().equals(adUnit)) {
          mpView.setAdUnitId(adUnit);
        }

        registerView(adView, adUnit);
        getAuctionManager().addBids(mpView, new MopubAdRequest(adView), remainingTime,
            value -> {
              // apply the request to the ad view, and pass that back
              MopubAdRequest request = (MopubAdRequest) value;
              request.applyToView(mpView);
              mopubAdViews.put(mpView.getAdUnitId(), new WeakReference<>(adView));
              onDone.onReceiveValue(adView);
            }
        );
      }

      @Override
      public void timeout() {
        String adUnit = (alternateAdUnitId == null) ? adView.getAdUnitId() : alternateAdUnitId;
        trackTimeoutEvent(adUnit, timeout);
        onDone.onReceiveValue(adView);
      }
    });
  }

  void addBids(final MoPubNative nativeAd, final RequestParameters requestParameters,
      final String adUnitId,
      final int timeout, final ValueCallback<NativeAddBidsResponse> onDone) {
    getAuctionManager().timedCallback(timeout, new TimedCallback() {
      @Override
      public void execute(final int remainingTime) {
        logState();
        final MopubNativeAdView mpView = new MopubNativeAdView(nativeAd, adUnitId);

        getAuctionManager().addBids(mpView,
            new MopubNativeAdRequest(nativeAd, adUnitId, requestParameters), remainingTime,
            value -> {
              // apply the request to the ad view, and pass that back
              MopubNativeAdRequest request = (MopubNativeAdRequest) value;
              request.applyToView(mpView);
              onDone.onReceiveValue(
                  new NativeAddBidsResponse(nativeAd, request.modifiedRequestParameters));
            }
        );
      }

      @Override
      public void timeout() {
        trackTimeoutEvent(adUnitId, timeout);
        onDone.onReceiveValue(new NativeAddBidsResponse(nativeAd, requestParameters));
      }
    });
  }

  void registerFloatingAd(Activity activity, final AppMonetFloatingAdConfiguration adConfiguration,
      MoPubView moPubView) {
    if (moPubView != null) {
      mopubAdViews.put(adConfiguration.adUnitId, new WeakReference<>(moPubView));
    }
    positions.put(adConfiguration.adUnitId, adConfiguration);
    this.currentActivity = new WeakReference<>(activity);
    getAuctionManager().registerFloatingAd(adConfiguration);
  }

  void unregisterFloatingAd(Activity activity, MoPubView moPubView) {
    if (moPubView != null) {
      mopubAdViews.remove(moPubView.getAdUnitId());
    }
    this.currentActivity = null;
  }

  private void registerView(MoPubView adView, String adUnitIdAlias) {
    if (adView == null) return;

    final String adUnitId = adUnitIdAlias == null ? adView.getAdUnitId() : adUnitIdAlias;
    if (adUnitId == null) {
      sLogger.warn("adView id is null! Cannot register view");
      return;
    }

    final MoPubView.BannerAdListener extant = adView.getBannerAdListener();
    synchronized (LOCK) {
      if (!(extant instanceof MopubBannerAdListener) &&
          !appMonetConfiguration.disableBannerListener && adView.getAdUnitId() != null) {
        sLogger.debug("registering view with internal listener: " + adUnitId);
        adView.setBannerAdListener(new MopubBannerAdListener(adUnitId, extant, this));
      }
    }
  }
}
