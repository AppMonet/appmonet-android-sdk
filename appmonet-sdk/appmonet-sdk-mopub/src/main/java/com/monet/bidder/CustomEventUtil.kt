package com.monet.bidder

import com.monet.adview.AdSize
import com.monet.bidder.Constants.Dfp.ADUNIT_KEYWORD_KEY

/**
 * Utility class used for performing miscellaneous methods needed by CustomEvents.
 */
internal object CustomEventUtil {
  private const val SERVER_EXTRA_TAG_ID_KEY = "tagId"
  private const val SERVER_EXTRA_LOWER_TAG_ID_KEY = "tagid"
  private const val SERVER_EXTRA_ADUNIT_KEY = "adunitId"
  private const val SERVER_EXTRA_CPM_KEY = "cpm"

  fun getAdUnitId(
    serverExtras: Map<String, String>,
    localExtras: Map<String, Any>,
    adSize: AdSize
  ): String? {
    return if (localExtras[ADUNIT_KEYWORD_KEY] != null) {
      localExtras[ADUNIT_KEYWORD_KEY] as String
    } else getAdUnitIdFromExtras(serverExtras, adSize)
  }

  fun getAdUnitId(
    serverExtras: Map<String, String?>,
    adSize: AdSize
  ): String? {
    return if (serverExtras[ADUNIT_KEYWORD_KEY] != null) {
      serverExtras[ADUNIT_KEYWORD_KEY]
    } else getAdUnitIdFromExtras(serverExtras, adSize)
  }

  fun getServerExtraCpm(
    serverExtras: Map<String, String?>,
    defaultValue: Double
  ): Double {
    if (!serverExtras.containsKey(SERVER_EXTRA_CPM_KEY)) {
      return defaultValue
    }
    try {
      return serverExtras[SERVER_EXTRA_CPM_KEY]?.toDouble() ?: defaultValue
    } catch (e: NumberFormatException) {
      // do nothing
    }
    return defaultValue
  }

  private fun getAdUnitIdFromExtras(
    serverExtras: Map<String, String?>,
    adSize: AdSize?
  ): String? {
    var adUnitId = serverExtras[SERVER_EXTRA_ADUNIT_KEY]
    if (adUnitId == null) {
      if (serverExtras.containsKey(SERVER_EXTRA_TAG_ID_KEY)) {
        adUnitId = serverExtras[SERVER_EXTRA_TAG_ID_KEY]
      } else if (serverExtras.containsKey(SERVER_EXTRA_LOWER_TAG_ID_KEY)) {
        adUnitId = serverExtras[SERVER_EXTRA_LOWER_TAG_ID_KEY]
      } else if (adSize != null && adSize.height != 0 && adSize.width != 0) {
        adUnitId = adSize.width.toString() + "x" + adSize.height
      }
    }
    return adUnitId
  }
}