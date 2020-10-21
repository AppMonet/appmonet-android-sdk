package com.monet.bidder;

import android.app.Application;
import android.content.Context;
import com.monet.ValueCallback;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.doubleclick.PublisherAdView;
import com.google.android.gms.ads.doubleclick.PublisherInterstitialAd;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code AppMonet} class contains static methods that are entry points to the AppMonet library.
 * All interactions will happen through this class.
 */
public class AppMonet {

  private AppMonet() {
  }

  /**
   * This method initializes the AppMonet library and all its internal components.
   * <p/>
   * You need to define {@code appmonet.application.id} {@code meta-data} in your
   * {@code AndroidManifest.xml}:
   * <pre>
   * &lt;manifest ...&gt;
   *
   * ...
   *
   *   &lt;application ...&gt;
   *     &lt;meta-data
   *       android:name="appmonet.application.id"
   *       android:value="@string/app_monet_app_id" /&gt;
   *
   *       ...
   *
   *   &lt;/application&gt;
   * &lt;/manifest&gt;
   * </pre>
   * <p/>
   * This must be called before your application can use the AppMonet library. The recommended
   * way is to call {@code AppMonet.init} at the {@code Application}'s {@code onCreate} method:
   * <p/>
   * <pre>
   * public class YourApplication extends Application {
   *     public void onCreate() {
   *          AppMonet.init(this);
   *     }
   * }
   * </pre>
   *
   * @param context The context ({@link Context}) of your application.
   */
  public static void init(Context context) {
    init(context, new AppMonetConfiguration.Builder().build());
  }

  /**
   * Register application state callbacks (e.g. foreground/background)
   *
   * @param application your Application instance
   */
  public static void registerCallbacks(Application application) {
    SdkManager sdkManager = SdkManager.get();
    if (sdkManager == null) {
      return;
    }

    sdkManager.registerCallbacks(application);
  }

  /**
   * This method initializes the AppMonet library and all its internal components.
   * <p/>
   * This method is required if you do not wish to define {@code appmonet.application.id}
   * {@code meta-data} in your {@code AndroidManifest.xml}
   * <p/>
   * This must be called before your application can use the AppMonet library. The recommended
   * way is to call {@code AppMonet.init} at the {@code Application}'s {@code onCreate} method:
   * <p/>
   * <pre>
   * public class YourApplication extends Application {
   *     public void onCreate() {
   *          AppMonet.init(this, &quot;AppMonet application id&quot;);
   *     }
   * }
   * </pre>
   *
   * @param context               The context ({@link Context}) of your application.
   * @param appMonetConfiguration The application configurations needed to initialize the sdk.
   */
  public static void init(Context context, AppMonetConfiguration appMonetConfiguration) {
    AppMonetConfiguration internalConfiguration = appMonetConfiguration;
    if (appMonetConfiguration == null) {
      internalConfiguration = new AppMonetConfiguration.Builder().build();
    }
    SdkManager.initialize(context, internalConfiguration);
  }

  public static void addBids(AdView adView, AdRequest adRequest, int timeout, ValueCallback<AdRequest> valueCallback) {
    SdkManager appMonetManager = SdkManager.get();
    if (appMonetManager == null) {
      valueCallback.onReceiveValue(adRequest);
      return;
    }

    appMonetManager.addBids(adView, adRequest, adView.getAdUnitId(), timeout, valueCallback);
  }

  /**
   * This method allows you to add bids to a particular {@code PublisherAdView} class asynchronously.
   * When a bid response comes back, it gets attached to the provided {@code PublisherAdView}
   * class before it's returned back using the {@code ValueCallback}.
   *
   * @param adView        The {@link PublisherAdView} instance that will load the
   *                      {@link PublisherAdRequest}.
   * @param adRequest     The {@link PublisherAdRequest} request instance for the give adView.
   * @param timeout       The wait time in milliseconds for a bid response.
   * @param valueCallback The callback to receive the adRequest with bids attached to it.
   */
  public static void addBids(PublisherAdView adView, final PublisherAdRequest adRequest,
                             int timeout, final ValueCallback<PublisherAdRequest> valueCallback) {
    addBids(adView, adRequest, adView.getAdUnitId(), timeout, valueCallback);
  }

  /**
   * This method allows you to add bids to a particular {@code PublisherAdView} class asynchronously.
   * When a bid response comes back, it gets attached to the provided {@code PublisherAdView}
   * class before is returned back using the {@code ValueCallback}.
   * <p/>
   * The appMonetAdUnitId acts as an alias to a particular adUnitId which has to be configured on
   * the AppMonet Dashboard.
   *
   * @param adView           The {@link PublisherAdView} instance that will load the
   *                         {@link PublisherAdRequest}.
   * @param adRequest        The {@link PublisherAdRequest} request instance for the give adView.
   * @param appMonetAdUnitId The alias unit id to be set to the {@link PublisherAdView} instance.
   *                         This ad unit id is configured on the AppMonet Dashboard.
   * @param timeout          The wait time in milliseconds for a bid response.
   * @param valueCallback    The callback to receive the adRequest with bids attached to it.
   */
  public static void addBids(PublisherAdView adView, final PublisherAdRequest adRequest,
                             String appMonetAdUnitId, int timeout,
                             final ValueCallback<PublisherAdRequest> valueCallback) {
    SdkManager appMonetManager = SdkManager.get();
    if (appMonetManager == null) {
      valueCallback.onReceiveValue(adRequest);
      return;
    }
    appMonetManager.addBids(adView, adRequest, appMonetAdUnitId, timeout, valueCallback);
  }

  public static void addBids(final PublisherAdRequest adRequest, String appMonetAdUnitId,
                             int timeout, final ValueCallback<PublisherAdRequest> valueCallback) {
    SdkManager appMonetManager = SdkManager.get();
    if (appMonetManager == null) {
      valueCallback.onReceiveValue(adRequest);
      return;
    }
    appMonetManager.addBids(adRequest, appMonetAdUnitId, timeout, valueCallback);
  }

  public static PublisherAdRequest addBids(PublisherAdRequest adRequest, String appMonetAdUnitId) {
    SdkManager appMonetManager = SdkManager.get();
    if (appMonetManager == null) {
      return adRequest;
    }
    return appMonetManager.addBids(adRequest, appMonetAdUnitId);
  }

  public static void addBids(PublisherInterstitialAd interstitialAd, final PublisherAdRequest adRequest,
                             int timeout, final ValueCallback<PublisherAdRequest> valueCallback) {
    addBids(interstitialAd, adRequest, interstitialAd.getAdUnitId(), timeout, valueCallback);
  }

  public static void addBids(PublisherInterstitialAd interstitialAd, final PublisherAdRequest adRequest,
                             String appMonetAdUnitId, int timeout,
                             final ValueCallback<PublisherAdRequest> valueCallback) {
    SdkManager manager = SdkManager.get();
    if (manager == null) {
      valueCallback.onReceiveValue(adRequest);
      return;
    }
    manager.addBids(interstitialAd, adRequest, appMonetAdUnitId, timeout, valueCallback);
  }

  public static void addBids(InterstitialAd interstitialAd, final AdRequest adRequest,
                             int timeout, final ValueCallback<AdRequest> valueCallback) {
    SdkManager manager = SdkManager.get();
    if (manager == null) {
      valueCallback.onReceiveValue(adRequest);
      return;
    }
    manager.addBids(interstitialAd, adRequest, interstitialAd.getAdUnitId(), timeout, valueCallback);
  }

  /**
   * This method allows you to get back the same {@code PublisherAdView} class that its provided
   * but with bids attached to it. Bids will only get attached if they are associated with the
   * view's ad unit id, and are locally cached. If bids are not cached,the {@code PublisherAdView}
   * view will be returned with no attached bids.
   *
   * @param adView    The {@link PublisherAdView} class to be used to append bid response
   *                  information.
   * @param adRequest The {@link PublisherAdRequest} request instance for the given adView
   * @return The {@link PublisherAdView} class with or without bid information.
   */
  public static PublisherAdRequest addBids(PublisherAdView adView, PublisherAdRequest adRequest) {
    return addBids(adView, adRequest, adView.getAdUnitId());
  }

  /**
   * This method allows you to get back the same {@code PublisherAdView} class that it's provided
   * but with bids attached to it. Bids will only get attached if they are associated with the
   * view's ad unit id, and are locally cached. If bids are not cached,the {@code PublisherAdView}
   * view will be returned with no attached bids.
   * <p/>
   * The appMonetAdUnitId acts as an alias to a particular adUnitId which has to be configured on
   * the AppMonet Dashboard.
   *
   * @param adView           The {@link PublisherAdView} instance that will load the
   *                         {@link PublisherAdRequest}.
   * @param adRequest        The {@link PublisherAdRequest} request instance for the give adView.
   * @param appMonetAdUnitId The alias unit id to be set to the {@link PublisherAdView} instance.
   *                         This ad unit id is configured on the AppMonet Dashboard.
   * @return The {@link PublisherAdView} class with or without bid information.
   */
  public static PublisherAdRequest addBids(PublisherAdView adView, PublisherAdRequest adRequest,
                                           String appMonetAdUnitId) {
    SdkManager appMonetManager = SdkManager.get();
    if (appMonetManager == null) {
      return adRequest;
    }
    return appMonetManager.addBids(adView, adRequest, appMonetAdUnitId);
  }

  /**
   * This method allows you to pre-fetch bids manually. This can be used when pre-fetching is
   * disabled during initialization in the AppMonet Dashboard, and you want to start retrieving
   * bids at a later time.
   */
  public static void preFetchBids() {
    SdkManager appMonetManager = SdkManager.get();
    if (appMonetManager == null) {
      return;
    }
    appMonetManager.preFetchBids(new ArrayList<String>());
  }

  /**
   * This method allows you to pre-fetch bids manually. This can be used when pre-fetching is
   * disabled during initialization in the AppMonet Dashboard, and you want to start retrieving
   * bids at a later time.
   *
   * @param adUnitIds This is the list of specific ad unit ids you want to fetch bids for.
   */
  public static void preFetchBids(List<String> adUnitIds) {
    SdkManager appMonetManager = SdkManager.get();
    if (appMonetManager == null) {
      return;
    }
    appMonetManager.preFetchBids(adUnitIds);
  }

  /**
   * This method allows you to enable or disable verbose logging coming from the AppMonet library.
   *
   * @param verboseLogging This boolean indicates if verbose logging should be activated.
   */
  public static void enableVerboseLogging(boolean verboseLogging) {
    SdkManager appMonetManager = SdkManager.get();
    if (appMonetManager == null) {
      return;
    }
    appMonetManager.enableVerboseLogging(verboseLogging);
  }

  /**
   * Check if the AppMonet SDK has already been initialized
   *
   * @return true of init() has already successfully been called
   */
  public static boolean isInitialized() {
    return SdkManager.get() != null;
  }

  /**
   * Set the level of the logger
   *
   * @param level a log level (integer)
   */
  public static void setLogLevel(int level) {
    SdkManager appMonetManager = SdkManager.get();
    if (appMonetManager == null) {
      return;
    }
    appMonetManager.setLogLevel(level);
  }

  /**
   * This method allows the SDK to get test demand that always fills. Use it only during development.
   */
  public static void testMode() {
    SdkManager sdkManager = SdkManager.get();
    if (sdkManager == null) {
      SdkManager.isTestMode = true;
      return;
    }

    sdkManager.testMode();
  }
}
