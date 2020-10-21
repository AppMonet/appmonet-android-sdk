package com.monet.bidder

import android.app.Activity
import android.app.Application
import android.content.Context
import com.monet.ValueCallback
import com.monet.bidder.AppMonetConfiguration.Builder
import com.monet.bidder.SdkManager.Companion.initializeThreaded
import com.mopub.mobileads.MoPubInterstitial
import com.mopub.mobileads.MoPubView
import com.mopub.nativeads.MoPubNative
import com.mopub.nativeads.RequestParameters
import java.util.ArrayList

/**
 * The `AppMonet` class contains static methods that are entry points to the AppMonet library
 */
object AppMonet {
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
   * @param context The context ([Context]) of your application.
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
  fun init(
    context: Context?,
    appMonetConfiguration: AppMonetConfiguration? = Builder().build()
  ) {
    initializeThreaded(context!!, appMonetConfiguration)
  }

  /**
   * Check if the AppMonet SDK has already been initialized
   *
   * @return true of init() has already successfully been called
   */
  @JvmStatic
  val isInitialized: Boolean
    get() = SdkManager["isInitialized"] != null

  /**
   * This method allows you to add bids to a particular `MoPubView` class asynchronously.
   * When a bid response comes back, it gets attached to the provided `MoPubView` class
   * before it's returned back using the `ValueCallback`.
   *
   * @param adView The [MoPubView] class to be used to append bid response
   * information.
   * @param timeout The time given to a request to come back with a bid response before it
   * times out.
   * @param valueCallback The callback used to return the [MoPubView] with bid information
   * attached to it
   */
  @JvmStatic
  fun addBids(
    adView: MoPubView,
    timeout: Int,
    valueCallback: ValueCallback<MoPubView>
  ) {
    val sdkManager = SdkManager["addBids"]
    if (addBidsIsInitialized(sdkManager, adView, valueCallback)) {
      sdkManager!!.addBids(adView, adView.getAdUnitId(), timeout, valueCallback)
    }
  }

  @JvmStatic
  fun addBids(
    moPubInterstitial: MoPubInterstitial?,
    adUnitId: String?,
    timeout: Int,
    valueCallback: ValueCallback<MoPubInterstitial?>
  ) {
    val sdkManager = SdkManager["addBids"]
    if (addBidsIsInitialized(sdkManager, moPubInterstitial, valueCallback)) {
      sdkManager!!.addBids(moPubInterstitial!!, adUnitId, timeout, valueCallback)
    }
  }

  /**
   * This method allows you to get back the same `MoPubView` class that is provided but with
   * bids attached to it. Bids will only get attached if they are associated with the
   * view's ad unit id, and are locally cached. If bids are not cached,the `MoPubView` view
   * will be returned with no attached bids.
   *
   * @param adView The [MoPubView] class to be used to append bid response
   * information.
   * @return The [MoPubView] class with attached bid information.
   */

  @JvmStatic
  fun addBids(adView: MoPubView): MoPubView? {
    val sdkManager = SdkManager["addBids"] ?: return adView
    return sdkManager.addBids(adView, adView.getAdUnitId()!!)
  }

  /**
   * This method allows you to get back the same [MoPubNative] and [RequestParameters]
   * class that is provided but with bids attached to it. Bids will only get attached if they are
   * associated with the view's ad unit id, and are locally cached. If bids are not cached,the
   * `MoPubView` view will be returned with no attached bids.
   *
   * @param nativeAd The [MoPubNative] class to be used to append bid response
   * information.
   * @param requestParameters The [RequestParameters] class to be used to append keywords
   * @param adUnitId The ad unit for the native ad.
   * @param timeout The timeout in milliseconds to wait for a bid to come back.
   * @param valueCallback The callback to return back [NativeAddBidsResponse] which
   * contains the [MoPubNative] and [RequestParameters] classes.
   */

  @JvmStatic
  fun addNativeBids(
    nativeAd: MoPubNative?,
    requestParameters: RequestParameters?,
    adUnitId: String?,
    timeout: Int,
    valueCallback: ValueCallback<NativeAddBidsResponse?>
  ) {
    val sdkManager = SdkManager["addBids"]
    if (addBidsIsInitialized(
            sdkManager,
            NativeAddBidsResponse(nativeAd!!, requestParameters),
            valueCallback
        )
    ) {
      sdkManager!!.addBids(
          nativeAd, requestParameters ?: RequestParameters.Builder().build(), adUnitId, timeout,
          valueCallback
      )
    }
  }

  /**
   * This method allows you to get back the same [MoPubNative] class that is provided but with
   * bids attached to it. Bids will only get attached if they are associated with the view's ad unit
   * id, and are locally cached. If bids are not cached,the [MoPubView] view will be returned
   * with no attached bids.
   *
   * @param nativeAd The [MoPubNative] class to be used to append bid response
   * information.
   * @param adUnitId The ad unit for the native ad.
   * @param timeout The timeout in milliseconds to wait for a bid to come back.
   * @param valueCallback The callback to return back [NativeAddBidsResponse] which
   * contains the [MoPubNative] and [RequestParameters] classes.
   */

  @JvmStatic
  fun addNativeBids(
    nativeAd: MoPubNative?,
    adUnitId: String?,
    timeout: Int,
    valueCallback: ValueCallback<NativeAddBidsResponse?>
  ) {
    addNativeBids(nativeAd, null, adUnitId, timeout, valueCallback)
  }

  /**
   * This method allows you to pre-fetch bids manually. This can be used when pre-fetching is
   * disabled during initialization in the AppMonet Dashboard, and you want to start retrieving
   * bids at a later time.
   */

  @JvmStatic
  fun preFetchBids() {
    val sdkManager = SdkManager["preFetchBids"] ?: return
    sdkManager.preFetchBids(ArrayList())
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
    val sdkManager = SdkManager["preFetchBids"] ?: return
    sdkManager.preFetchBids(adUnitIds)
  }

  /**
   * This method allows you to enable or disable verbose logging coming from the AppMonet library.
   *
   * @param verboseLogging This boolean indicates if verbose logging should be activated.
   */

  @JvmStatic
  fun enableVerboseLogging(verboseLogging: Boolean) {
    val sdkManager = SdkManager["enableVerboseLogging"] ?: return
    sdkManager.enableVerboseLogging(verboseLogging)
  }

  /**
   * Set the level of the logger
   *
   * @param level a log level (integer)
   */

  @JvmStatic
  fun setLogLevel(level: Int) {
    val sdkManager = SdkManager["setLogLevel"] ?: return
    sdkManager.setLogLevel(level)
  }

  /**
   * Register application state callbacks (e.g. foreground/background)
   *
   * @param application your Application instance
   */

  @JvmStatic
  fun registerCallbacks(application: Application?) {
    val sdkManager = SdkManager["registerCallbacks"] ?: return
    sdkManager.registerCallbacks(application!!)
  }

  /**
   * Disables the sdk execution.
   */

  @JvmStatic
  @Deprecated("") fun disableExecution() {
    val sdkManager = SdkManager["disableExecution"] ?: return
    sdkManager.disableBidManager()
  }

  /**
   * Enables the sdk execution
   */

  @JvmStatic
  fun enableExecution() {
    val sdkManager = SdkManager["enableExecution"] ?: return
    sdkManager.enableBidManager()
  }
  /**
   * Registers and activity wanting to display floating ads by using a MoPubView as its container.
   * `AppMonetFloatingAdConfiguration` will determine how the ad is displayed.
   *
   * @param activity The `Activity` where the floating ad will be displayed.
   * @param adPosition The configuration object defining the floating ad behavior.
   * @param moPubView The `MoPubView` to be used as constrains for the floating ad.
   */
  /**
   * Registers and activity wanting to display floating ads by using the phone screen as its
   * container.
   * `AppMonetFloatingAdConfiguration` will determine how the ad is displayed.
   *
   * @param activity The `Activity` where the floating ad will be displayed.
   * @param adPosition The configuration object defining the floating ad behavior.
   */

  @JvmStatic
  fun registerFloatingAd(
    activity: Activity?,
    adPosition: AppMonetFloatingAdConfiguration?,
    moPubView: MoPubView? = null
  ) {
    val sdkManager = SdkManager["registerFloatingAd"] ?: return
    sdkManager.registerFloatingAd(activity!!, adPosition!!, moPubView)
  }
  /**
   * This method should be called when you want to stop displaying floating ads using the
   * `MoPubView` as its container.
   *
   * @param activity The `Activity` that was used to register the floating ad.
   * @param moPubView The `MoPubView` that was used as the container for the floating ad.
   */
  /**
   * This method should be called when you want to stop displaying floating ads using the screen as
   * its container.
   *
   * @param activity The `Activity` that was used to register the floating ad.
   */
  @JvmStatic fun unregisterFloatingAd(
    activity: Activity?,
    moPubView: MoPubView? = null
  ) {
    val sdkManager = SdkManager["unregisterFloatingAd"] ?: return
    sdkManager.unregisterFloatingAd(activity, moPubView)
  }

  /**
   * This method allows the SDK to get test demand that always fills. Use it only during
   * development.
   */
  @JvmStatic
  fun testMode() {
    val sdkManager = SdkManager["testMode"]
    if (sdkManager == null) {
      BaseManager.Companion.isTestMode = true
      return
    }
    sdkManager.testMode()
  }

  private fun <T> addBidsIsInitialized(
    sdkManager: SdkManager?,
    originalValue: T,
    valueCallback: ValueCallback<T>
  ): Boolean {
    if (sdkManager == null) {
      valueCallback.onReceiveValue(originalValue)
      return false
    }
    return true
  }
}