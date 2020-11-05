package com.monet.bidder

import android.app.Application
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.doubleclick.PublisherAdRequest
import com.google.android.gms.ads.doubleclick.PublisherAdView
import com.google.android.gms.ads.doubleclick.PublisherInterstitialAd
import com.monet.ValueCallback
import com.monet.bidder.AppMonetConfiguration.Builder
import com.monet.bidder.SdkManager.Companion.get
import com.monet.bidder.SdkManager.Companion.initialize
import java.util.ArrayList

/**
 * The `AppMonet` class contains static methods that are entry points to the AppMonet library.
 * All interactions will happen through this class.
 */
object AppMonet {
  /**
   * Register application state callbacks (e.g. foreground/background)
   *
   * @param application your Application instance
   */
  @JvmStatic
  fun registerCallbacks(application: Application?) {
    val sdkManager = get() ?: return
    sdkManager.registerCallbacks(application!!)
  }
  /**
   * This method initializes the AppMonet library and all its internal components.
   *
   *
   * This method is required if you do not wish to define `appmonet.application.id`
   * `meta-data` in your `AndroidManifest.xml`
   *
   *
   * This must be called before your application can use the AppMonet library. The recommended
   * way is to call `AppMonet.init` at the `Application`'s `onCreate` method:
   *
   *
   * <pre>
   * public class YourApplication extends Application {
   * public void onCreate() {
   * AppMonet.init(this, &quot;AppMonet application id&quot;);
   * }
   * }
  </pre> *
   *
   * @param context               The context ([Context]) of your application.
   * @param appMonetConfiguration The application configurations needed to initialize the sdk.
   */
  /**
   * This method initializes the AppMonet library and all its internal components.
   *
   *
   * You need to define `appmonet.application.id` `meta-data` in your
   * `AndroidManifest.xml`:
   * <pre>
   * &lt;manifest ...&gt;
   *
   * ...
   *
   * &lt;application ...&gt;
   * &lt;meta-data
   * android:name="appmonet.application.id"
   * android:value="@string/app_monet_app_id" /&gt;
   *
   * ...
   *
   * &lt;/application&gt;
   * &lt;/manifest&gt;
  </pre> *
   *
   *
   * This must be called before your application can use the AppMonet library. The recommended
   * way is to call `AppMonet.init` at the `Application`'s `onCreate` method:
   *
   *
   * <pre>
   * public class YourApplication extends Application {
   * public void onCreate() {
   * AppMonet.init(this);
   * }
   * }
  </pre> *
   *
   * @param context The context ([Context]) of your application.
   */
  @JvmStatic
  @JvmOverloads fun init(
    context: Context,
    appMonetConfiguration: AppMonetConfiguration? = Builder().build()
  ) {
    var internalConfiguration = appMonetConfiguration
    if (appMonetConfiguration == null) {
      internalConfiguration = Builder().build()
    }
    initialize(context, internalConfiguration!!)
  }

  @JvmStatic
  fun addBids(
    adView: AdView,
    adRequest: AdRequest,
    timeout: Int,
    valueCallback: ValueCallback<AdRequest>
  ) {
    val appMonetManager = get()
    if (appMonetManager == null) {
      valueCallback.onReceiveValue(adRequest)
      return
    }
    appMonetManager.addBids(adView, adRequest, adView.adUnitId, timeout, valueCallback)
  }

  /**
   * This method allows you to add bids to a particular `PublisherAdView` class asynchronously.
   * When a bid response comes back, it gets attached to the provided `PublisherAdView`
   * class before it's returned back using the `ValueCallback`.
   *
   * @param adView        The [PublisherAdView] instance that will load the
   * [PublisherAdRequest].
   * @param adRequest     The [PublisherAdRequest] request instance for the give adView.
   * @param timeout       The wait time in milliseconds for a bid response.
   * @param valueCallback The callback to receive the adRequest with bids attached to it.
   */
  @JvmStatic
  fun addBids(
    adView: PublisherAdView,
    adRequest: PublisherAdRequest,
    timeout: Int,
    valueCallback: ValueCallback<PublisherAdRequest>
  ) {
    addBids(adView, adRequest, adView.adUnitId, timeout, valueCallback)
  }

  /**
   * This method allows you to add bids to a particular `PublisherAdView` class asynchronously.
   * When a bid response comes back, it gets attached to the provided `PublisherAdView`
   * class before is returned back using the `ValueCallback`.
   *
   *
   * The appMonetAdUnitId acts as an alias to a particular adUnitId which has to be configured on
   * the AppMonet Dashboard.
   *
   * @param adView           The [PublisherAdView] instance that will load the
   * [PublisherAdRequest].
   * @param adRequest        The [PublisherAdRequest] request instance for the give adView.
   * @param appMonetAdUnitId The alias unit id to be set to the [PublisherAdView] instance.
   * This ad unit id is configured on the AppMonet Dashboard.
   * @param timeout          The wait time in milliseconds for a bid response.
   * @param valueCallback    The callback to receive the adRequest with bids attached to it.
   */
  @JvmStatic
  fun addBids(
    adView: PublisherAdView,
    adRequest: PublisherAdRequest,
    appMonetAdUnitId: String,
    timeout: Int,
    valueCallback: ValueCallback<PublisherAdRequest>
  ) {
    val appMonetManager = get()
    if (appMonetManager == null) {
      valueCallback.onReceiveValue(adRequest)
      return
    }
    appMonetManager.addBids(adView, adRequest, appMonetAdUnitId, timeout, valueCallback)
  }
  @JvmStatic
  fun addBids(
    adRequest: PublisherAdRequest,
    appMonetAdUnitId: String,
    timeout: Int,
    valueCallback: ValueCallback<PublisherAdRequest>
  ) {
    val appMonetManager = get()
    if (appMonetManager == null) {
      valueCallback.onReceiveValue(adRequest)
      return
    }
    appMonetManager.addBids(adRequest, appMonetAdUnitId, timeout, valueCallback)
  }
  @JvmStatic
  fun addBids(
    adRequest: PublisherAdRequest,
    appMonetAdUnitId: String?
  ): PublisherAdRequest {
    val appMonetManager = get() ?: return adRequest
    return appMonetManager.addBids(adRequest, appMonetAdUnitId!!)
  }
  @JvmStatic
  fun addBids(
    interstitialAd: PublisherInterstitialAd,
    adRequest: PublisherAdRequest,
    timeout: Int,
    valueCallback: ValueCallback<PublisherAdRequest>
  ) {
    addBids(interstitialAd, adRequest, interstitialAd.adUnitId, timeout, valueCallback)
  }
  @JvmStatic
  fun addBids(
    interstitialAd: PublisherInterstitialAd,
    adRequest: PublisherAdRequest,
    appMonetAdUnitId: String,
    timeout: Int,
    valueCallback: ValueCallback<PublisherAdRequest>
  ) {
    val manager = get()
    if (manager == null) {
      valueCallback.onReceiveValue(adRequest)
      return
    }
    manager.addBids(interstitialAd, adRequest, appMonetAdUnitId, timeout, valueCallback)
  }
  @JvmStatic
  fun addBids(
    interstitialAd: InterstitialAd,
    adRequest: AdRequest,
    timeout: Int,
    valueCallback: ValueCallback<AdRequest>
  ) {
    val manager = get()
    if (manager == null) {
      valueCallback.onReceiveValue(adRequest)
      return
    }
    manager.addBids(interstitialAd, adRequest, interstitialAd.adUnitId, timeout, valueCallback)
  }
  /**
   * This method allows you to get back the same `PublisherAdView` class that it's provided
   * but with bids attached to it. Bids will only get attached if they are associated with the
   * view's ad unit id, and are locally cached. If bids are not cached,the `PublisherAdView`
   * view will be returned with no attached bids.
   *
   *
   * The appMonetAdUnitId acts as an alias to a particular adUnitId which has to be configured on
   * the AppMonet Dashboard.
   *
   * @param adView           The [PublisherAdView] instance that will load the
   * [PublisherAdRequest].
   * @param adRequest        The [PublisherAdRequest] request instance for the give adView.
   * @param appMonetAdUnitId The alias unit id to be set to the [PublisherAdView] instance.
   * This ad unit id is configured on the AppMonet Dashboard.
   * @return The [PublisherAdView] class with or without bid information.
   */
  /**
   * This method allows you to get back the same `PublisherAdView` class that its provided
   * but with bids attached to it. Bids will only get attached if they are associated with the
   * view's ad unit id, and are locally cached. If bids are not cached,the `PublisherAdView`
   * view will be returned with no attached bids.
   *
   * @param adView    The [PublisherAdView] class to be used to append bid response
   * information.
   * @param adRequest The [PublisherAdRequest] request instance for the given adView
   * @return The [PublisherAdView] class with or without bid information.
   */
  @JvmStatic
  fun addBids(
    adView: PublisherAdView,
    adRequest: PublisherAdRequest,
    appMonetAdUnitId: String
  ): PublisherAdRequest {
    val appMonetManager = get() ?: return adRequest
    return appMonetManager.addBids(adView, adRequest, appMonetAdUnitId!!)
  }

  /**
   * This method allows you to pre-fetch bids manually. This can be used when pre-fetching is
   * disabled during initialization in the AppMonet Dashboard, and you want to start retrieving
   * bids at a later time.
   */

  @JvmStatic
  fun preFetchBids() {
    val appMonetManager = get() ?: return
    appMonetManager.preFetchBids(ArrayList())
  }

  /**
   * This method allows you to pre-fetch bids manually. This can be used when pre-fetching is
   * disabled during initialization in the AppMonet Dashboard, and you want to start retrieving
   * bids at a later time.
   *
   * @param adUnitIds This is the list of specific ad unit ids you want to fetch bids for.
   */

  @JvmStatic
  fun preFetchBids(adUnitIds: List<String>) {
    val appMonetManager = get() ?: return
    appMonetManager.preFetchBids(adUnitIds)
  }

  /**
   * This method allows you to enable or disable verbose logging coming from the AppMonet library.
   *
   * @param verboseLogging This boolean indicates if verbose logging should be activated.
   */

  @JvmStatic
  fun enableVerboseLogging(verboseLogging: Boolean) {
    val appMonetManager = get() ?: return
    appMonetManager.enableVerboseLogging(verboseLogging)
  }

  /**
   * Check if the AppMonet SDK has already been initialized
   *
   * @return true of init() has already successfully been called
   */

  @JvmStatic
  val isInitialized: Boolean
    get() = get() != null

  /**
   * Set the level of the logger
   *
   * @param level a log level (integer)
   */

  @JvmStatic
  fun setLogLevel(level: Int) {
    val appMonetManager = get() ?: return
    appMonetManager.setLogLevel(level)
  }

  /**
   * This method allows the SDK to get test demand that always fills. Use it only during development.
   */

  @JvmStatic
  fun testMode() {
    val sdkManager = get()
    if (sdkManager == null) {
      BaseManager.isTestMode = true
      return
    }
    sdkManager.testMode()
  }
}