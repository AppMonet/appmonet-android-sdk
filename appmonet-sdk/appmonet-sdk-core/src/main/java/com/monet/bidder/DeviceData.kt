package com.monet.bidder

import android.Manifest.permission
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityManager.MemoryInfo
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.Configuration
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Debug
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.DisplayMetrics
import android.webkit.ValueCallback
import androidx.core.content.ContextCompat
import com.monet.BuildConfig
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

/**
 * Class responsible of gathering device information for analytics purposes.
 */
class DeviceData constructor(
  val context: Context
) {
  // get the memory stats for the device
  val vMStats: JSONObject
    get() {
      val json = JSONObject()
      try {
        val runtime = Runtime.getRuntime()
        json.put("totalMemory", runtime.totalMemory())
        json.put("maxMemory", runtime.maxMemory())
        json.put("freeMemory", runtime.freeMemory())
        json.put("nativeHeapAlloc", Debug.getNativeHeapAllocatedSize())
        json.put("nativeHeapFree", Debug.getNativeHeapFreeSize())

        // get the memory stats for the device
        val memoryInfo = memoryInfo
        if (memoryInfo != null) {
          json.put("miFree", memoryInfo.availMem)
          json.put("miTotal", memoryInfo.totalMem)
          json.put("miLow", memoryInfo.lowMemory)
          json.put("miThreshold", memoryInfo.threshold)
        }
      } catch (e: Exception) {
        // do nothing
      }
      return json
    }

  val appInfo: ApplicationInfo?
    get() = try {
      val pm = context.packageManager
      pm.getApplicationInfo(context.applicationInfo.packageName, 0)
    } catch (e: Exception) {
      null
    }

  internal fun getAdClientInfo(callback: ValueCallback<AdInfo>) {
    val adClientInfoBackgroundExecutor = AdClientBackgroundExecutor(context, callback)
    adClientInfoBackgroundExecutor.execute()
  }

  val data: JSONObject
    get() = buildData()

  fun toJSON(): String {
    return data.toString()
  }

  private val applicationData: JSONObject
    get() {
      val json = JSONObject()
      val packageInfo = packageInfo
      try {
        if (packageInfo != null) {
          json.put("version", packageInfo.versionName)
          json.put(
              "build",
              if (VERSION.SDK_INT >= VERSION_CODES.P) packageInfo.longVersionCode
              else packageInfo.versionCode
          )
          json.put("bundle", packageInfo.packageName)
        }
        json.put("name", applicationName)
      } catch (e: Exception) {
        return json
      }
      return json
    }

  private fun hasLocationPermission(): Boolean {
    return try {
      (ContextCompat.checkSelfPermission(context, permission.ACCESS_COARSE_LOCATION)
          == PackageManager.PERMISSION_GRANTED ||
          ContextCompat.checkSelfPermission(context, permission.ACCESS_FINE_LOCATION)
          == PackageManager.PERMISSION_GRANTED)
    } catch (e: Exception) {
      sLogger.debug(e.message)
      false
    }
  }

  private val connectionType: String
    get() {
      val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
      @SuppressLint("MissingPermission") val activeInfo = cm?.activeNetworkInfo
      if (activeInfo == null || !activeInfo.isConnected) {
        return UNKNOWN
      }
      @SuppressLint("MissingPermission") val wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
          ?: return "unknown"
      return if (wifi.isConnected) "wifi" else "cell"
    }

  private fun networkTypeToString(networkType: Int): String {
    return when (networkType) {
      TelephonyManager.NETWORK_TYPE_CDMA -> "cdma"
      TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyManager.NETWORK_TYPE_HSPAP -> "2g"
      TelephonyManager.NETWORK_TYPE_LTE -> "4g"
      else -> "3g"
    }
  }

  // do nothing; this could fail
  private val networkInfo: JSONObject
    get() {
      val json = JSONObject()
      val tel = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
          ?: return json
      val networkOperator = tel.networkOperator
      if (!TextUtils.isEmpty(networkOperator)) {
        try {
          json.put("mcc", networkOperator.substring(0, 3))
          json.put("mnc", networkOperator.substring(3))
          json.put("carrier", tel.networkOperatorName)
        } catch (e: Exception) {
          sLogger.warn("unable to fetch carrier data")
        }
      }
      try {
        json.put("connection", connectionType)
      } catch (e: Exception) {
        // do nothing; this could fail
      }
      try {
        json.put("cell_country", tel.networkCountryIso)
        json.put("sim_country", tel.simCountryIso)
        json.put("cell_type", networkTypeToString(tel.networkType))
      } catch (e: Exception) {
        sLogger.warn("error writing tel/network data")
      }
      return json
    }
  private val oSData: JSONObject
    get() {
      val json = JSONObject()
      try {
        json.put("name", "Android")
        json.put("version", VERSION.RELEASE)
        json.put("build", Build.DISPLAY)
      } catch (e: JSONException) {
        e.printStackTrace()
      }
      return json
    }

  private val locationData: JSONObject
    get() {
      val json = JSONObject()
      var location: Location? = null
      if (!hasLocationPermission()) {
        return json
      }
      val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
      if (lm != null) {
        val providers = lm.getProviders(true)
        try {
          for (provider in providers) {
            @SuppressLint("MissingPermission")
            val l = lm.getLastKnownLocation(provider) ?: continue
            if (location == null || l.time > location.time) {
              location = l
            }
          }
        } catch (e: Exception) {
          sLogger.warn("couldn't get location")
          return json
        }
        if (location == null) {
          return json
        }
        try {
          json.put("lat", location.latitude)
          json.put("lon", location.longitude)
          json.put("accuracy", location.accuracy.toDouble())
          json.put("provider", location.provider)
        } catch (e: Exception) {
          sLogger.warn("failed to write location")
        }
      }
      return json
    }
  private val screenData: JSONObject
    get() {
      val json = JSONObject()
      val displayMetrics = displayMetrics
      try {
        if (displayMetrics != null) {
          val largestSide = Math.max(displayMetrics.widthPixels, displayMetrics.heightPixels)
          val smallestSide = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels)
          val resolution = Integer.toString(largestSide) + "x" + smallestSide.toString()
          json.put("resolution", resolution)
          json.put("density", displayMetrics.density.toDouble())
          json.put("dpi", displayMetrics.densityDpi)
          json.put("scaled_density", displayMetrics.scaledDensity.toDouble())
          json.put("height", displayMetrics.heightPixels)
          json.put("width", displayMetrics.widthPixels)
        }
      } catch (e: Exception) {
        sLogger.error("failed to write display")
      }
      return json
    }
  @get:SuppressLint("HardwareIds") private val deviceData: JSONObject
    get() {
      val json = JSONObject()
      try {
        json.put("manufacturer", Build.MANUFACTURER)
        json.put("brand", Build.BRAND)
        json.put("model", Build.MODEL)
        json.put("family", family)
        json.put("model_id", Build.ID)
        json.put("orientation", orientation)
        json.put("product", Build.PRODUCT)
        json.put("type", Build.TYPE)
        json.put("display", Build.DISPLAY)
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
          json.put("arch", Build.SUPPORTED_ABIS[0])
        } else {
          json.put("arch", Build.CPU_ABI)
        }
        json.put("online", isConnected)
        val memInfo = memoryInfo
        if (memInfo != null) {
          json.put("free_memory", memInfo.availMem)
          json.put("memory_size", memInfo.totalMem)
          json.put("low_memory", memInfo.lowMemory)
        }
        json.put("os_version", VERSION.SDK_INT)
      } catch (e: Exception) {
        sLogger.warn("failed to build device")
      }
      return json
    }
  private val memoryInfo: MemoryInfo?
    get() = try {
      val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
      val memoryInfo = MemoryInfo()
      manager?.getMemoryInfo(memoryInfo)
      memoryInfo
    } catch (e: Exception) {
      sLogger.error("Error getting MemoryInfo. $e")
      null
    }
  private val localeData: String
    get() = Locale.getDefault().language

  private fun buildData(): JSONObject {
    val json = JSONObject()
    try {
      json.put("device", deviceData)
      json.put("app", applicationData)
      json.put("location", locationData)
      json.put("network", networkInfo)
      json.put("screen", screenData)
      json.put("locale", localeData)
      json.put("os", oSData)
      json.put("sdk", sdkInfo)
    } catch (e: Exception) {
      sLogger.error("error building devicedata")
    }
    return json
  }

  private val family: String?
    get() = try {
      Build.MODEL.split(" ".toRegex()).toTypedArray()[0]
    } catch (e: Exception) {
      sLogger.error("Error getting device family. $e")
      null
    }

  /**
   * Get the device's current screen orientation.
   *
   * @return the device's current screen orientation, or null if unknown
   */
  private val orientation: String?
    get() = try {
      val orientation = when (context.resources.configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> "landscape"
        Configuration.ORIENTATION_PORTRAIT -> "portrait"
        else -> null
      }
      orientation
    } catch (e: Exception) {
      sLogger.debug("Error getting device orientation.$e")
      null
    }

  /**
   * Check whether the application has internet access at a point in time.
   *
   * @return true if the application has internet access
   */
  private val isConnected: Boolean
    get() {
      val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
      @SuppressLint("MissingPermission")
      val activeNetworkInfo = connectivityManager.activeNetworkInfo
      return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

  /**
   * Get the DisplayMetrics object for the current application.
   *
   * @return the DisplayMetrics object for the current application
   */
  private val displayMetrics: DisplayMetrics?
    get() = try {
      context.resources.displayMetrics
    } catch (e: Exception) {
      sLogger.debug("Error getting DisplayMetrics. $e")
      null
    }

  /**
   * Return the Application's PackageInfo if possible, or null.
   *
   * @return the Application's PackageInfo if possible, or null
   */
  val packageInfo: PackageInfo?
    get() = try {
      context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (e: NameNotFoundException) {
      sLogger.error("Error getting package info.$e")
      null
    }

  /**
   * Get the human-facing Application name.
   *
   * @return Application name
   */
  private val applicationName: String?
    get() {
      try {
        val applicationInfo = context.applicationInfo
        val stringId = applicationInfo.labelRes
        if (stringId == 0) {
          if (applicationInfo.nonLocalizedLabel != null) {
            return applicationInfo.nonLocalizedLabel.toString()
          }
        } else {
          return context.getString(stringId)
        }
      } catch (e: Exception) {
        sLogger.error("Error getting application name. $e")
      }
      return null
    }
  private val sdkInfo: JSONObject?
    get() = try {
      val jsonObject = JSONObject()
      jsonObject.put("core", BuildConfig.VERSION_NAME)
      jsonObject
    } catch (e: Exception) {
      null
    }

  companion object {
    private val sLogger = Logger("Device")
    private const val UNKNOWN = "unknown"
  }
}