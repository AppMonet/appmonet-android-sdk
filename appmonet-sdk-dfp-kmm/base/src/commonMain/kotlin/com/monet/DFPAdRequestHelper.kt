package com.monet

import com.monet.Constants.ADUNIT_KEYWORD_KEY
import com.monet.adview.AdSize

object DFPAdRequestHelper {
  fun getAdUnitID(
    customEventExtras: Map<String, Any>?,
    serverParameter: String?,
    adSize: AdSize?
  ): String? {
    if (customEventExtras != null && customEventExtras.containsKey(ADUNIT_KEYWORD_KEY)) {
      return customEventExtras[ADUNIT_KEYWORD_KEY] as String?
    }
    var adUnit: String? = null
    if (serverParameter != null && serverParameter != "default" && serverParameter != "AMAdSize") {
      adUnit = if (serverParameter.startsWith("$")) {
        getWidthHeightAdUnit(adSize)
      } else {
        parseServerParameter(serverParameter)[0]
      }
    }
    if (adUnit == null || adUnit.isEmpty()) {
      adUnit = getWidthHeightAdUnit(adSize)
    }
    return adUnit
  }

  fun getCpm(serverParameter: String?): Double {
    if (serverParameter == null || serverParameter.isEmpty()) {
      return 0.0
    }

    // another option: a server parameter
    // is *only* the floor, e.g. $5.00...
    if (serverParameter.startsWith("$")) {
      return try {
        serverParameter.substring(1).toDouble()
      } catch (e: NumberFormatException) {
        0.0
      }
    }
    try {
      return parseServerParameter(serverParameter)[1].toDouble()
    } catch (e: Exception) {
      // error
    }
    return 0.0
  }


  private fun getWidthHeightAdUnit(adSize: AdSize?): String? {
    var adUnit: String? = null
    if (adSize != null && adSize.height != 0 && adSize.width != 0) {
      adUnit = "${adSize.width}x${adSize.height}"
    }
    return adUnit
  }

  private fun parseServerParameter(serverParameter: String): Array<String> {
    return serverParameter.split("@\\$".toRegex()).toTypedArray()
  }
}