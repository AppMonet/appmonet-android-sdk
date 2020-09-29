package com.monet.bidder;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.webkit.ValueCallback;

import com.mopub.mobileads.MoPubInterstitial;
import com.mopub.mobileads.MoPubView;
import com.mopub.nativeads.MoPubNative;
import com.mopub.nativeads.RequestParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code AppMonet} class contains static methods that are entry points to the AppMonet library
 */
public final class AppMonet {
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
   * @param context The context ({@link Context}) of your application.
   * @param appMonetConfiguration The application configurations needed to initialize the sdk.
   */
  public static void init(final Context context,
      final AppMonetConfiguration appMonetConfiguration) {
    SdkManager.initializeThreaded(context, appMonetConfiguration);
  }

  /**
   * Check if the AppMonet SDK has already been initialized
   *
   * @return true of init() has already successfully been called
   */
  public static boolean isInitialized() {
    return SdkManager.get("isInitialized") != null;
  }

  /**
   * This method allows you to add bids to a particular {@code MoPubView} class asynchronously.
   * When a bid response comes back, it gets attached to the provided {@code MoPubView} class
   * before it's returned back using the {@code ValueCallback}.
   *
   * @param adView The {@link MoPubView} class to be used to append bid response
   * information.
   * @param timeout The time given to a request to come back with a bid response before it
   * times out.
   * @param valueCallback The callback used to return the {@link MoPubView} with bid information
   * attached to it
   */
  public static void addBids(final MoPubView adView, int timeout,
      final ValueCallback<MoPubView> valueCallback) {
    SdkManager sdkManager = SdkManager.get("addBids");
    if (addBidsIsInitialized(sdkManager, adView, valueCallback)) {
      sdkManager.addBids(adView, adView.getAdUnitId(), timeout, valueCallback);
    }
  }

  public static void addBids(final MoPubInterstitial moPubInterstitial, String adUnitId,
      int timeout,
      final ValueCallback<MoPubInterstitial> valueCallback) {
    SdkManager sdkManager = SdkManager.get("addBids");
    if (addBidsIsInitialized(sdkManager, moPubInterstitial, valueCallback)) {
      sdkManager.addBids(moPubInterstitial, adUnitId, timeout, valueCallback);
    }
  }

  /**
   * This method allows you to get back the same {@code MoPubView} class that is provided but with
   * bids attached to it. Bids will only get attached if they are associated with the
   * view's ad unit id, and are locally cached. If bids are not cached,the {@code MoPubView} view
   * will be returned with no attached bids.
   *
   * @param adView The {@link MoPubView} class to be used to append bid response
   * information.
   * @return The {@link MoPubView} class with attached bid information.
   */
  public static MoPubView addBids(MoPubView adView) {
    SdkManager sdkManager = SdkManager.get("addBids");
    if (sdkManager == null) {
      return adView;
    }

    return sdkManager.addBids(adView, adView.getAdUnitId());
  }

  /**
   * This method allows you to get back the same {@link MoPubNative} and {@link RequestParameters}
   * class that is provided but with bids attached to it. Bids will only get attached if they are
   * associated with the view's ad unit id, and are locally cached. If bids are not cached,the
   * {@code MoPubView} view will be returned with no attached bids.
   *
   * @param nativeAd The {@link MoPubNative} class to be used to append bid response
   * information.
   * @param requestParameters The {@link RequestParameters} class to be used to append keywords
   * @param adUnitId The ad unit for the native ad.
   * @param timeout The timeout in milliseconds to wait for a bid to come back.
   * @param valueCallback The callback to return back {@link NativeAddBidsResponse} which
   * contains the {@link MoPubNative} and {@link RequestParameters} classes.
   */
  public static void addNativeBids(MoPubNative nativeAd, RequestParameters requestParameters,
      String adUnitId, int timeout,
      final ValueCallback<NativeAddBidsResponse> valueCallback) {
    SdkManager sdkManager = SdkManager.get("addBids");
    if (addBidsIsInitialized(sdkManager,
        new NativeAddBidsResponse(nativeAd, requestParameters),
        valueCallback)) {
      sdkManager.addBids(nativeAd, (requestParameters == null) ?
              new RequestParameters.Builder().build() : requestParameters, adUnitId, timeout,
          valueCallback);
    }
  }

  /**
   * This method allows you to get back the same {@link MoPubNative} class that is provided but with
   * bids attached to it. Bids will only get attached if they are associated with the view's ad unit
   * id, and are locally cached. If bids are not cached,the {@link MoPubView} view will be returned
   * with no attached bids.
   *
   * @param nativeAd The {@link MoPubNative} class to be used to append bid response
   * information.
   * @param adUnitId The ad unit for the native ad.
   * @param timeout The timeout in milliseconds to wait for a bid to come back.
   * @param valueCallback The callback to return back {@link NativeAddBidsResponse} which
   * contains the {@link MoPubNative} and {@link RequestParameters} classes.
   */
  public static void addNativeBids(MoPubNative nativeAd,
      String adUnitId, int timeout,
      final ValueCallback<NativeAddBidsResponse> valueCallback) {
    addNativeBids(nativeAd, null, adUnitId, timeout, valueCallback);
  }

  /**
   * This method allows you to pre-fetch bids manually. This can be used when pre-fetching is
   * disabled during initialization in the AppMonet Dashboard, and you want to start retrieving
   * bids at a later time.
   */
  public static void preFetchBids() {
    SdkManager sdkManager = SdkManager.get("preFetchBids");
    if (sdkManager == null) {
      return;
    }
    sdkManager.preFetchBids(new ArrayList<String>());
  }

  /**
   * This method allows you to pre-fetch bids manually. This can be used when pre-fetching is
   * disabled during initialization in the AppMonet Dashboard, and you want to start retrieving
   * bids at a later time.
   *
   * @param adUnitIds This is the list of specific ad unit ids you want to fetch bids for.
   */
  public static void preFetchBids(List<String> adUnitIds) {
    SdkManager sdkManager = SdkManager.get("preFetchBids");
    if (sdkManager == null) {
      return;
    }
    sdkManager.preFetchBids(adUnitIds);
  }

  /**
   * This method allows you to enable or disable verbose logging coming from the AppMonet library.
   *
   * @param verboseLogging This boolean indicates if verbose logging should be activated.
   */
  public static void enableVerboseLogging(boolean verboseLogging) {
    SdkManager sdkManager = SdkManager.get("enableVerboseLogging");
    if (sdkManager == null) {
      return;
    }
    sdkManager.enableVerboseLogging(verboseLogging);
  }

  /**
   * Set the level of the logger
   *
   * @param level a log level (integer)
   */
  public static void setLogLevel(int level) {
    SdkManager sdkManager = SdkManager.get("setLogLevel");
    if (sdkManager == null) {
      return;
    }
    sdkManager.setLogLevel(level);
  }

  /**
   * Register application state callbacks (e.g. foreground/background)
   *
   * @param application your Application instance
   */
  public static void registerCallbacks(Application application) {
    SdkManager sdkManager = SdkManager.get("registerCallbacks");
    if (sdkManager == null) {
      return;
    }

    sdkManager.registerCallbacks(application);
  }

  /**
   * Disables the sdk execution.
   */
  @Deprecated
  public static void disableExecution() {
    SdkManager sdkManager = SdkManager.get("disableExecution");
    if (sdkManager == null) {
      return;
    }

    sdkManager.disableBidManager();
  }

  /**
   * Enables the sdk execution
   */
  public static void enableExecution() {
    SdkManager sdkManager = SdkManager.get("enableExecution");
    if (sdkManager == null) {
      return;
    }

    sdkManager.enableBidManager();
  }

  /**
   * Registers and activity wanting to display floating ads by using a MoPubView as its container.
   * {@code AppMonetFloatingAdConfiguration} will determine how the ad is displayed.
   *
   * @param activity The {@code Activity} where the floating ad will be displayed.
   * @param adPosition The configuration object defining the floating ad behavior.
   * @param moPubView The {@code MoPubView} to be used as constrains for the floating ad.
   */

  public static void registerFloatingAd(Activity activity,
      AppMonetFloatingAdConfiguration adPosition,
      MoPubView moPubView) {
    SdkManager sdkManager = SdkManager.get("registerFloatingAd");
    if (sdkManager == null) {
      return;
    }

    sdkManager.registerFloatingAd(activity, adPosition, moPubView);
  }

  /**
   * Registers and activity wanting to display floating ads by using the phone screen as its
   * container.
   * {@code AppMonetFloatingAdConfiguration} will determine how the ad is displayed.
   *
   * @param activity The {@code Activity} where the floating ad will be displayed.
   * @param adPosition The configuration object defining the floating ad behavior.
   */
  public static void registerFloatingAd(Activity activity,
      AppMonetFloatingAdConfiguration adPosition) {
    registerFloatingAd(activity, adPosition, null);
  }

  /**
   * This method should be called when you want to stop displaying floating ads using the screen as
   * its container.
   *
   * @param activity The {@code Activity} that was used to register the floating ad.
   */
  public static void unregisterFloatingAd(Activity activity) {
    unregisterFloatingAd(activity, null);
  }

  /**
   * This method should be called when you want to stop displaying floating ads using the
   * {@code MoPubView} as its container.
   *
   * @param activity The {@code Activity} that was used to register the floating ad.
   * @param moPubView The {@code MoPubView} that was used as the container for the floating ad.
   */
  public static void unregisterFloatingAd(Activity activity, MoPubView moPubView) {
    SdkManager sdkManager = SdkManager.get("unregisterFloatingAd");
    if (sdkManager == null) {
      return;
    }

    sdkManager.unregisterFloatingAd(activity, moPubView);
  }

  /**
   * This method allows the SDK to get test demand that always fills. Use it only during
   * development.
   */
  public static void testMode() {
    SdkManager sdkManager = SdkManager.get("testMode");
    if (sdkManager == null) {
      SdkManager.isTestMode = true;
      return;
    }

    sdkManager.testMode();
  }

  private static <T> boolean addBidsIsInitialized(SdkManager sdkManager,
      T originalValue, ValueCallback<T> valueCallback) {
    if (sdkManager == null) {
      valueCallback.onReceiveValue(originalValue);
      return false;
    }
    return true;
  }
}
