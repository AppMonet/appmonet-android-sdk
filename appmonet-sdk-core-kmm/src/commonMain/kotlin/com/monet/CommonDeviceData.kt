package com.monet

import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Extras(
  val minSdkVersion: String?,
  val skadnetwork: List<String>?,
  val idfv: String?
)

@Serializable
data class AdInfo(
  var advertisingId: String = "",
  var isLimitAdTrackingEnabled: Boolean = false
)

@Serializable
data class AppInfo(
  val bundle: String,
  val version: String,
  val build: String,
  val applicationName: String,
  val extras: Extras? = null
)

@Serializable
data class HardwareData(
  val manufacturer: String = "",
  val brand: String = "",
  val model: String = "",
  val family: String = "",
  val model_id: String = "",
  val orientation: String = "",
  val product: String = "",
  val type: String = "",
  val display: String = "",
  val arch: String = "",
  val online: Boolean = false,
  val free_memory: Long = 0L,
  val memory_size: Long = 0L,
  val low_memory: Boolean = false,
  val os_version: String = ""
)

@Serializable
data class NetworkInfo(
  val mcc: String = "",
  val mnc: String = "",
  val carrier: String = "",
  val connection: String = "",
  val cell_country: String = "",
  val sim_country: String = "",
  val cell_type: String = ""
)

@Serializable
data class DeviceDataInfo(
  val device: HardwareData,
  val app: AppInfo,
  val location: LocationData,
  val network: NetworkInfo,
  val screen: ScreenData,
  val locale: String,
  val os: OSData
)

@Serializable
data class OSData(
  val name: String,
  val version: String,
  val build: String
)

@Serializable
data class MemInfo(
  val freeMemory: Long = 0L,
  val memorySize: Long = 0L,
  val lowMemory: Boolean = false,
  val miFree: Long = 0L,
  val miThreshold: Long = 0L
)

@Serializable
data class LocationData(
  val lat: Double = 0.0,
  val lon: Double = 0.0,
  val accuracy: Double = 0.0,
  val provider: String = ""
)

@Serializable
data class ScreenData(
  val resolution: String = "",
  val density: Double = 0.0,
  val dpi: Int = 0,
  val scaled_density: Double = 0.0,
  val height: Int = 0,
  val width: Int = 0
)

interface CommonDeviceData {
  companion object {
    const val UNKNOWN = "unknown"
  }

  val appInfo: AppInfo
  val connectionType: String
  val deviceData: HardwareData
  val data: DeviceDataInfo
    get() = DeviceDataInfo(
        deviceData, appInfo, locationData, networkInfo, screenData, localeData, osData
    )

  val isConnected: Boolean
  val family: String
  val localeData: String
  val locationData: LocationData
  val memoryInfo: MemInfo
  val networkInfo: NetworkInfo
  val orientation: String
  val osData: OSData
  val screenData: ScreenData
  fun getAdClientInfo(callback: Callback<AdInfo>)
  fun toJsonString(): String {
    return Json.encodeToString(data)
  }
}