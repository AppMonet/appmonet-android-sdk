package com.monet

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.cValuesOf
import kotlinx.cinterop.useContents
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.AdSupport.ASIdentifierManager
import platform.CoreFoundation.CFAllocatorGetDefault
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.kCFBundleVersionKey
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreTelephony.CTRadioAccessTechnologyEdge
import platform.CoreTelephony.CTRadioAccessTechnologyLTE
import platform.CoreTelephony.CTRadioAccessTechnologyWCDMA
import platform.CoreTelephony.CTTelephonyNetworkInfo
import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.NSProcessInfo
import platform.Foundation.preferredLanguages
import platform.SystemConfiguration.SCNetworkReachabilityCreateWithName
import platform.SystemConfiguration.SCNetworkReachabilityGetFlags
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsConnectionRequired
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsIsWWAN
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsReachable
import platform.UIKit.UIDevice
import platform.UIKit.UIScreen
import platform.darwin.nil
import platform.darwin.sysctlbyname
import platform.posix.NULL
import platform.posix.__IPHONE_OS_VERSION_MIN_REQUIRED
import platform.posix.calloc
import platform.posix.free
import platform.posix.size_t

class DeviceData : CommonDeviceData {
  var advertisingId: String? = null
  override val appInfo: AppInfo
    get() {
      val mainBundle = NSBundle.mainBundle
      return AppInfo(
          mainBundle.bundleIdentifier ?: "",
          mainBundle.infoDictionary?.get("CFBundleShortVersionString")?.let { it as String } ?: "",
          mainBundle.infoDictionary?.get(kCFBundleVersionKey)?.let { it as String } ?: "",
          mainBundle.objectForInfoDictionaryKey("CFBundleName")?.let { it as String } ?: "",
          Extras(
              __IPHONE_OS_VERSION_MIN_REQUIRED.toString(), getSkAdNetwork(),
              UIDevice.currentDevice.identifierForVendor?.UUIDString
          )
      )
    }

  override val connectionType: String
    get() {
      val ref = SCNetworkReachabilityCreateWithName(CFAllocatorGetDefault(), "8.8.8.8")
      val flags: UInt = 0u

      val success = SCNetworkReachabilityGetFlags(ref, cValuesOf(flags))
      CFRelease(ref)

      if (!success) {
        return "unknown";
      }

      val isConnected = ((flags and kSCNetworkReachabilityFlagsReachable).toInt() != 0)
      val needsConn = ((flags and kSCNetworkReachabilityFlagsConnectionRequired).toInt() != 0)
      val isNetReachable = (isConnected && !needsConn)

      if (!isNetReachable) {
        return "none"
      }

      if ((flags and kSCNetworkReachabilityFlagsIsWWAN).toInt() != 0) {
        return "cell"
      }

      return "wifi"
    }

  override val deviceData: HardwareData
    get() {
      val device = UIDevice.currentDevice
      return HardwareData("Apple Inc.", os_version = device.systemVersion)
    }
  override val isConnected: Boolean
    get() {
      val ref = SCNetworkReachabilityCreateWithName(CFAllocatorGetDefault(), "8.8.8.8");
      val flags: UInt = 0u
      CFRelease(ref);
      return ((flags and kSCNetworkReachabilityFlagsReachable).toInt() != 0);
    }
  override val family: String = ""

  override val localeData: String = NSLocale.preferredLanguages[0]?.let { it as String } ?: ""

  override val locationData: LocationData
    get() {
      if ((CLLocationManager.authorizationStatus() != kCLAuthorizationStatusAuthorizedAlways
              || CLLocationManager.authorizationStatus() != kCLAuthorizationStatusAuthorizedWhenInUse)
          && !CLLocationManager.locationServicesEnabled()
      ) {
        return LocationData()
      }

      val locationManager = CLLocationManager()
      locationManager?.let { ccLocation ->
        ccLocation.location?.let { lastKnownLocation ->
          return LocationData(
              lastKnownLocation.coordinate.useContents { latitude },
              lastKnownLocation.coordinate.useContents { longitude },
              lastKnownLocation.verticalAccuracy + lastKnownLocation.horizontalAccuracy / 2
          )
        }
      }
      return LocationData()
    }

  override val memoryInfo: MemInfo = MemInfo()

  override val networkInfo: NetworkInfo
    get() {
      val netInfo = CTTelephonyNetworkInfo()
      val carrierProvider = netInfo.subscriberCellularProvider
      val mcc = carrierProvider?.mobileCountryCode ?: "0"
      val mnc = carrierProvider?.mobileNetworkCode ?: "0"
      val carrier = carrierProvider?.carrierName ?: "unknown"
      val connection = connectionType
      val cellType = getNetworkType(netInfo)
      val simCountry = netInfo.subscriberCellularProvider?.isoCountryCode ?: ""

      return NetworkInfo(mcc, mnc, carrier, connection, "", simCountry, cellType)
    }
  override val orientation: String = ""
  override val osData: OSData
    get() {
      val systemVersion = NSProcessInfo.processInfo.operatingSystemVersion
      val version =
        "${systemVersion.useContents { majorVersion }}.${systemVersion.useContents { minorVersion }}.${systemVersion.useContents { patchVersion }}"

      val ctlKey = "kern.osversion"
      var buildValue: String? = null
      val size: size_t = 0u
      cValuesOf(size)
      if (sysctlbyname(ctlKey, NULL, cValuesOf(size), NULL, 0) != -1) {
        val machine: COpaquePointer? = calloc(1, size)
        sysctlbyname(ctlKey, machine, cValuesOf(size), NULL, 0)
        val ctlValue = machine.toString()
        free(machine);
        buildValue = ctlValue;
      }
      return OSData("iOS", version, buildValue ?: "")
    }
  override val screenData: ScreenData
    get() {
      val screenWidth = UIScreen.mainScreen.bounds.useContents { size.width }
      val screenHeight = UIScreen.mainScreen.bounds.useContents { size.height }
      val density = UIScreen.mainScreen.scale
      val resolution = "${screenHeight}x${screenWidth}"
      return ScreenData(
          resolution, density, 0, density, screenHeight.toInt(), screenWidth.toInt()
      )
    }

  override fun getAdClientInfo(callback: Callback<AdInfo>) {
    val adManager = ASIdentifierManager.sharedManager()
    advertisingId = adManager.advertisingIdentifier.UUIDString
    val adInfo = AdInfo(advertisingId!!, adManager.isAdvertisingTrackingEnabled())
    callback(adInfo)
  }

  private fun getNetworkType(telephonyInfo: CTTelephonyNetworkInfo): String {
    val technologyString = telephonyInfo.currentRadioAccessTechnology;
    return when {
      technologyString === CTRadioAccessTechnologyLTE -> {
        "4g";
        // LTE (4G)
      }
      technologyString === CTRadioAccessTechnologyWCDMA -> {
        "3g";
        // 3G
      }
      technologyString === CTRadioAccessTechnologyEdge -> {
        "2g";
        // EDGE (2G)
      }
      else -> "undefined"
    }
  }

  private fun getSkAdNetwork(): MutableList<String> {
    val skAdNetworkItems = mutableListOf<String>();
    val infoDict = NSBundle.mainBundle.infoDictionary;
    if (infoDict == nil) {
      return skAdNetworkItems;
    }
    val adNetworkItems = infoDict?.get("SKAdNetworkItems") as List<*>
    if (adNetworkItems == nil) {
      return skAdNetworkItems;
    }

    adNetworkItems.forEach { item ->
      if (item is Map<*, *>) {
        val skAdNetworkIdentifier = item["SKAdNetworkIdentifier"]
        skAdNetworkIdentifier?.let {
          if (skAdNetworkIdentifier is String) {
            skAdNetworkItems.add(skAdNetworkIdentifier)
          }
        }
      }
    }
    return skAdNetworkItems;
  }

}