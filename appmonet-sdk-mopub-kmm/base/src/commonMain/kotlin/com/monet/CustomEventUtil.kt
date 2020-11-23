package com.monet

import com.monet.BidResponse.Mapper
import com.monet.Constants.ADUNIT_KEYWORD_KEY
import com.monet.Constants.BIDS_KEY
import com.monet.adview.AdSize

/**
 * Utility class used for performing miscellaneous methods needed by CustomEvents.
 */
fun Map<String, Any?>.toStringString(): Map<String, String> {
  val map = mutableMapOf<String, String>()
  this.map {
    it.value?.let { value ->
      if (value is String) {
        map[it.key] = value
      }
    }
  }
  return map
}

object CustomEventUtil {
  private const val SERVER_EXTRA_TAG_ID_KEY = "tagId"
  private const val SERVER_EXTRA_LOWER_TAG_ID_KEY = "tagid"
  private const val SERVER_EXTRA_ADUNIT_KEY = "adunitId"
  private const val SERVER_EXTRA_CPM_KEY = "cpm"

  fun getAdUnitId(
    serverExtras: Map<String, String>?,
    localExtras: Map<String, Any>?,
    adSize: AdSize
  ): String? {
    return if (localExtras?.get(ADUNIT_KEYWORD_KEY) != null) {
      localExtras[ADUNIT_KEYWORD_KEY] as String
    } else getAdUnitIdFromExtras(serverExtras, adSize)
  }

  fun getAdUnitId(
    serverExtras: Map<String, String?>?,
    adSize: AdSize
  ): String? {
    return if (serverExtras?.get(ADUNIT_KEYWORD_KEY) != null) {
      serverExtras[ADUNIT_KEYWORD_KEY]
    } else getAdUnitIdFromExtras(serverExtras, adSize)
  }

  fun getServerExtraCpm(
    serverExtras: Map<String, String?>?,
    defaultValue: Double
  ): Double {
    if (serverExtras?.containsKey(SERVER_EXTRA_CPM_KEY) == false) {
      return defaultValue
    }
    try {
      return serverExtras?.get(SERVER_EXTRA_CPM_KEY)?.toDouble() ?: defaultValue
    } catch (e: NumberFormatException) {
      // do nothing
    }
    return defaultValue
  }

  fun getBid(extras: Map<String, String?>?): BidResponse? {
    if (extras?.containsKey(BIDS_KEY) == true && extras[BIDS_KEY] != null) {
      return extras[BIDS_KEY]?.let { Mapper.from(it) }
    }
    return null
  }

  private fun getAdUnitIdFromExtras(
    serverExtras: Map<String, String?>?,
    adSize: AdSize?
  ): String? {
    var adUnitId = serverExtras?.get(SERVER_EXTRA_ADUNIT_KEY)
    if (adUnitId == null) {
      if (serverExtras?.containsKey(SERVER_EXTRA_TAG_ID_KEY) == true) {
        adUnitId = serverExtras[SERVER_EXTRA_TAG_ID_KEY]
      } else if (serverExtras?.containsKey(SERVER_EXTRA_LOWER_TAG_ID_KEY) == true) {
        adUnitId = serverExtras[SERVER_EXTRA_LOWER_TAG_ID_KEY]
      } else if (adSize != null && adSize.height != 0 && adSize.width != 0) {
        adUnitId = "${adSize.width}x${adSize.height}"
      }
    }
    return adUnitId
  }
}