package com.monet.bidder

import android.app.Application
import android.content.ComponentCallbacks
import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.Configuration
import android.util.Log
import com.monet.AdType
import com.monet.Callback
import com.monet.DeviceData
import com.monet.bidder.Constants.Configurations
import com.monet.bidder.Constants.JSAppStates
import com.monet.bidder.auction.AuctionManager
import com.monet.bidder.exceptions.AppMonetInitException
import com.monet.bidder.threading.UIThread
import com.monet.threading.BackgroundThread
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * The [BaseManager] abstract class provides a template for initializing all the objects
 * required by the sdk. This class should be extended by the different SSP flavors which can decide
 * within their own implementation how to hold the instance of all the objects created in this
 * class.
 */
open class BaseManager(
  context: Context,
  applicationId: String?,
  val adServerWrapper: AdServerWrapper
) {
  private val remoteConfiguration = RemoteConfiguration(context, applicationId)
  private val backgroundThread = BackgroundThread()
  val auctionManager: AuctionManager
  val appMonetContext: AppMonetContext = AppMonetContext()
  private val deviceData: DeviceData = DeviceData(context)
  val preferences: Preferences = Preferences(context)
  val pubSubService: PubSubService = PubSubService()

  val context: WeakReference<Context> = WeakReference(context)
  val uiThread: UIThread = UIThread()
  private var mSdkConfigurations: SdkConfigurations? = null
  private var areCallbacksRegistered = false

  fun registerCallbacks(application: Application) {
    if (areCallbacksRegistered) {
      return
    }
    application.registerActivityLifecycleCallbacks(ApplicationCallbacks(auctionManager))
    application.registerComponentCallbacks(object : ComponentCallbacks {
      override fun onConfigurationChanged(configuration: Configuration) {
        auctionManager.trackAppState(JSAppStates.CONFIGURATION_CHANGED, "app")
      }

      override fun onLowMemory() {
        auctionManager.trackAppState(JSAppStates.LOW_MEMORY, "app")
      }
    })
    areCallbacksRegistered = true
    disableBidManager()
  }

  /**
   * This method allows you to pre-fetch bids manually.
   *
   * @param adUnitIds This is the list of specific ad unit ids you want to fetch bids for.
   */
  fun preFetchBids(adUnitIds: List<String>) {
    sLogger.debug("PreFetch invoked.")
    auctionManager.prefetchAdUnits(adUnitIds)
  }

  /**
   * This method will retrieve a single instance of [SdkConfigurations] for easy access to
   * our saved configuration options.
   *
   * @return [SdkConfigurations]
   */
  val sdkConfigurations: SdkConfigurations
    get() {
      try {
        if (mSdkConfigurations == null) {
          val configurations = preferences
              .getPref(Configurations.AM_SDK_CONFIGURATIONS, "")
          mSdkConfigurations = if (configurations == null || configurations.isEmpty()) {
            sLogger.warn("no configuration data found. Using defaults")
            SdkConfigurations(JSONObject())
          } else {
            SdkConfigurations(JSONObject(configurations))
          }
        }
      } catch (e: Exception) {
      }
      return mSdkConfigurations!!
    }

  fun reloadConfigurations(): Boolean {
    return try {
      val configurations = preferences
          .getPref(Configurations.AM_SDK_CONFIGURATIONS, "")
      if (configurations.isEmpty()) {
        return false
      }
      mSdkConfigurations = SdkConfigurations(JSONObject(configurations))
      sLogger.debug("configurations reloaded.")
      true
    } catch (e: Exception) {
      sLogger.error("Unable to reload config: ", e.message)
      false
    }
  }

  /**
   * This method allows us to enable/disable verbose logging.
   *
   * @param state The state boolean indicates if verbose logging should be activated.
   */
  fun enableVerboseLogging(state: Boolean) {
    val level = if (state) Log.DEBUG else Log.WARN
    setLogLevel(level)
  }

  /**
   * Set the logger's level
   *
   * @param level a log level
   */
  fun setLogLevel(level: Int) {
    try {
      sLogger.debug("changing log level")
      Logger.setGlobalLevel(level)
      auctionManager.syncLogger()
    } catch (e: Exception) {
    }
  }

  private fun loadRemoteConfiguration() {
    backgroundThread.execute {
      remoteConfiguration.getRemoteConfiguration(true)
    }
  }

  fun disableBidManager() {
    auctionManager.disableBidCleaner()
  }

  fun enableBidManager() {
    auctionManager.enableBidCleaner()
  }

  /**
   * Checks if a particular activity is set on the AndroidManifest and its accessible.
   *
   * @param context The context needed to access package manager.
   * @param interstitialActivityName The name of the activity that we want to know if it's declared.
   * @return Boolean that tells us if activity is declared or not.
   */
  fun isInterstitialActivityRegistered(
    context: Context,
    interstitialActivityName: String
  ): Boolean {
    return try {
      context.packageManager
          .getActivityInfo(
              ComponentName(context, interstitialActivityName),
              PackageManager.GET_META_DATA
          )
      true
    } catch (e: NameNotFoundException) {
      false
    }
  }

  fun indicateRequestAsync(
    adUnitId: String?,
    timeout: Int,
    adSize: AdSize?,
    adType: AdType?,
    floorCpm: Double,
    callback: Callback<String?>
  ) {
    auctionManager.indicateRequestAsync(adUnitId!!, timeout, adSize, adType!!, floorCpm, callback)
  }

  fun indicateRequest(
    adUnitId: String?,
    adSize: AdSize?,
    adType: AdType?,
    floorCpm: Double
  ) {
    logState()
    auctionManager.indicateRequest(adUnitId!!, adSize, adType!!, floorCpm)
  }

  fun logState() {
    auctionManager.logState()
  }

  fun testMode() {
    if (context.get() != null && 0 != (context.get()!!.applicationInfo.flags
            and ApplicationInfo.FLAG_DEBUGGABLE)
    ) {
      isTestMode = true
      auctionManager.testMode()
    } else {
      sLogger.warn("Test mode is not supported in release mode.")
      isTestMode = false
    }
  }

  /**
   * This method checks that all the parameters needed by the sdk to properly initialize.
   *
   * @param context This is the context from the android application implementing the sdk.
   * @param applicationId This is the unique identifier passed by the application in order
   * to work with our system.
   */
  private fun checkRequiredInitParameters(
    context: Context?,
    applicationId: String?
  ) {
    if (context == null) {
      throw AppMonetInitException("Context cannot be null")
    }
    if (applicationId == null || applicationId.isEmpty()) {
      checkManifestMetaData(context)
    } else {
      appMonetContext.applicationId = applicationId
    }
  }

  /**
   * This method checks if the applicationId was provided as metadata on the application manifest
   * file
   *
   * @param context This is the context from the android application implementing our sdk.
   */
  private fun checkManifestMetaData(context: Context) {
    val runTimeExceptionMessage =
      """Application ID not defined. You must provide Application ID in AndroidManifest.xml.

<meta-data
    android:name="${Constants.APPMONET_APPLICATION_ID}"
    android:value="<Your Application ID>" />
"""
    try {
      val applicationInfo = context.packageManager
          .getApplicationInfo(
              context.packageName,
              PackageManager.GET_META_DATA
          )
      val applicationId = applicationInfo.metaData.getString(Constants.APPMONET_APPLICATION_ID)
      if (applicationId == null || applicationId.isEmpty()) {
        throw AppMonetInitException(runTimeExceptionMessage)
      }
      appMonetContext.applicationId = applicationId
    } catch (e: Exception) {
      throw AppMonetInitException(runTimeExceptionMessage)
    }
  }

  fun trackTimeoutEvent(
    adUnitId: String?,
    timeout: Float
  ) {
    val currentTime = System.currentTimeMillis()
    auctionManager.trackEvent("addbids_nofill", "timeout", adUnitId!!, timeout, currentTime)
  }

  companion object {
    private val sLogger = Logger("SdkManager")
    var isTestMode: Boolean = false
    var initRetry = 0
  }

  init {
    loadRemoteConfiguration()
    checkRequiredInitParameters(context, applicationId)
    auctionManager = AuctionManager(
        context, this, uiThread, backgroundThread, deviceData,
        pubSubService, appMonetContext, preferences, sdkConfigurations, remoteConfiguration,
        adServerWrapper
    )
    if (isTestMode) {
      testMode()
    }
  }
}