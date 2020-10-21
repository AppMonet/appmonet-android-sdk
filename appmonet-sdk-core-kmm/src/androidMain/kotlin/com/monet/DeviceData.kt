package com.monet

import android.Manifest.permission
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityManager.MemoryInfo
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
import com.monet.CommonDeviceData.Companion.UNKNOWN
import kotlinx.serialization.Serializable
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Serializable
data class VMStats(
  val totalMemory: Long = 0L,
  val maxMemory: Long = 0L,
  val freeMemory: Long = 0L,
  val nativeHeapAlloc: Long = 0L,
  val nativeHeapFree: Long = 0L,
  val miFree: Long = 0L,
  val miTotal: Long = 0L,
  val miLow: Boolean = false,
  val miThreshold: Long = 0L,
)

class DeviceData(
  val context: Context
) : CommonDeviceData {
  companion object {
//    private val sLogger = Logger("Device")
  }

  val vMStats: VMStats
    get() {
      try {

        val runtime = Runtime.getRuntime()
        val memoryInfo = memoryInfo
        return VMStats(
            runtime.totalMemory(), runtime.maxMemory(), runtime.freeMemory(),
            Debug.getNativeHeapAllocatedSize(), Debug.getNativeHeapFreeSize(),
            memoryInfo.miFree, memoryInfo.memorySize, memoryInfo.lowMemory, memoryInfo.miThreshold
        )
      } catch (e: Exception) {
        // do nothing
      }
      return VMStats()
    }

  override val appInfo: AppInfo
    get() {
      var packageInfo: PackageInfo? = null
      try {
        packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
      } catch (e: java.lang.Exception) {
//        sLogger
      }
      return AppInfo(
          packageInfo?.packageName ?: "", packageInfo?.versionName ?: "",
          if (VERSION.SDK_INT >= VERSION_CODES.P) packageInfo?.longVersionCode?.toString()
              ?: "" else packageInfo?.versionCode?.toString() ?: "",
          applicationName
      )
    }
  override val connectionType: String
    get() {
      val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
      @SuppressLint("MissingPermission") val activeInfo = cm?.activeNetworkInfo
      if (activeInfo == null || !activeInfo.isConnected) {
        return UNKNOWN
      }
      @SuppressLint("MissingPermission") val wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
          ?: return UNKNOWN
      return if (wifi.isConnected) "wifi" else "cell"
    }

  override val deviceData: HardwareData
    get() {
      return HardwareData(
          Build.MANUFACTURER, Build.BRAND, Build.MODEL, family, Build.ID,
          orientation, Build.PRODUCT, Build.TYPE, Build.DISPLAY,
          if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) Build.SUPPORTED_ABIS[0] else Build.CPU_ABI,
          isConnected, memoryInfo.freeMemory, memoryInfo.memorySize, memoryInfo.lowMemory,
          VERSION.SDK_INT.toString()
      )
    }

  override val family: String = Build.MODEL?.split(" ".toRegex())?.toTypedArray()?.get(0) ?: ""

  override val localeData: String = Locale.getDefault().language

  override val locationData: LocationData
    get() {
      var location: Location? = null
      if (!hasLocationPermission()) {
        return LocationData()
      }
      (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?)?.let { lm ->
        val providers = lm.getProviders(true)
        try {
          for (provider in providers) {
            @SuppressLint("MissingPermission")
            val l = lm.getLastKnownLocation(provider) ?: continue
            if (location == null || l.time > location?.time ?: 0) {
              location = l
            }
          }
        } catch (e: Exception) {
//          sLogger.warn("couldn't get location")
          return LocationData()
        }
        location?.let {
          return LocationData(it.latitude, it.longitude, it.accuracy.toDouble(), it.provider)
        }
      }
      return LocationData()
    }

  override val isConnected: Boolean
    get() {
      val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

      @SuppressLint("MissingPermission")
      val activeNetworkInfo = connectivityManager.activeNetworkInfo
      return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

  override val memoryInfo: MemInfo
    get() {
      val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
      val memory = MemoryInfo()
      manager.getMemoryInfo(memory)
      return MemInfo(
          memory.availMem, memory.totalMem, memory.lowMemory, memory.availMem, memory.threshold
      )
    }
  override val networkInfo: NetworkInfo
    get() {
      (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?)?.let { tel ->
        val networkOperator = tel.networkOperator
        var mcc = ""
        var mnc = ""
        var carrier = ""
        var cellCountry = ""
        var simCountry = ""
        var cellType = ""
        var connection = ""
        if (!TextUtils.isEmpty(networkOperator)) {
          try {
            mcc = networkOperator.substring(0, 3)
            mnc = networkOperator.substring(3)
            carrier = tel.networkOperatorName
          } catch (e: Exception) {
//            sLogger.warn("unable to fetch carrier data")
          }
        }
        try {
          connection = connectionType
        } catch (e: Exception) {
          // do nothing; this could fail
        }
        try {
          cellCountry = tel.networkCountryIso
          simCountry = tel.simCountryIso
          cellType = networkTypeToString(tel.networkType)
        } catch (e: Exception) {
//          sLogger.warn("error writing tel/network data")
        }
        return NetworkInfo(mcc, mnc, carrier, connection, cellCountry, simCountry, cellType)
      } ?: return NetworkInfo()

    }

  override val orientation: String
    get() {
      return when (context.resources.configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> "landscape"
        Configuration.ORIENTATION_PORTRAIT -> "portrait"
        else -> ""
      }
    }

  override val osData: OSData = OSData("Android", VERSION.RELEASE, Build.DISPLAY)

  override val screenData: ScreenData
    get() {
      val displayMetrics = try {
        context.resources.displayMetrics
      } catch (e: Exception) {
//        sLogger.debug("Error getting DisplayMetrics. $e")
        null
      }
      try {
        displayMetrics?.let {
          val largestSide = max(displayMetrics.widthPixels, displayMetrics.heightPixels)
          val smallestSide = min(displayMetrics.widthPixels, displayMetrics.heightPixels)
          val resolution = largestSide.toString() + "x" + smallestSide.toString()
          return ScreenData(
              resolution, displayMetrics.density.toDouble(), displayMetrics.densityDpi,
              displayMetrics.scaledDensity.toDouble(), displayMetrics.heightPixels,
              displayMetrics.widthPixels
          )
        }
      } catch (e: Exception) {
//        sLogger.error("failed to write display")
      }
      return ScreenData()
    }

  override fun getAdClientInfo(callback: Callback<AdInfo>) {
    val adClientInfoBackgroundExecutor = AdClientBackgroundExecutor(context, callback)
    adClientInfoBackgroundExecutor.execute()
  }

  private val applicationName: String
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
//        sLogger.error("Error getting application name. $e")
      }
      return ""
    }

  private fun hasLocationPermission(): Boolean {
    return try {
      (ContextCompat.checkSelfPermission(context, permission.ACCESS_COARSE_LOCATION)
          == PackageManager.PERMISSION_GRANTED ||
          ContextCompat.checkSelfPermission(context, permission.ACCESS_FINE_LOCATION)
          == PackageManager.PERMISSION_GRANTED)
    } catch (e: Exception) {
//      sLogger.debug(e.message)
      false
    }
  }

  private fun networkTypeToString(networkType: Int): String {
    return when (networkType) {
      TelephonyManager.NETWORK_TYPE_CDMA -> "cdma"
      TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyManager.NETWORK_TYPE_HSPAP -> "2g"
      TelephonyManager.NETWORK_TYPE_LTE -> "4g"
      else -> "3g"
    }
  }
}