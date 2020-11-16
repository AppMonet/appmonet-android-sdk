package com.monet

import platform.Foundation.NSUUID

actual class Util {
  actual companion object {
    actual fun getUUID(): String {
      return NSUUID.UUID().toString()
    }

    actual fun join(
      delimeter: String,
      list: List<*>
    ): String {
      return list.joinToString(delimeter)
    }

    actual fun isAdRequestExtra(
      map: MutableMap<String, Any>,
      key: String,
      extra: Any
    ): Boolean {
      return false
    }

    actual fun generateTrackingSource(adType: AdType): String {
      return "custom_event_$adType"
    }
  }
}