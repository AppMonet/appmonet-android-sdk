package com.monet.bidder

import com.monet.bidder.Constants.Configurations.AD_UNIT_TIMEOUTS
import org.json.JSONException
import org.json.JSONObject

/**
 * SdkConfigurations holds a [JSONObject] which has the dynamic configuration options
 * retrieved by [MonetJsInterface]. This class encapsulates the required [JSONException]
 * handling and returns default values.
 */
class SdkConfigurations(private val mConfigurations: JSONObject?) {
  fun hasKey(key: String?): Boolean {
    return mConfigurations != null && mConfigurations.has(key)
  }

  /**
   * Returns an integer value associated with the supplied key.
   *
   * @param key to be used to retrieve desired value.
   * @return integer value associated with the provided key.
   */
  fun getInt(key: String?): Int {
    var value = 0
    try {
      value = mConfigurations!!.getInt(key)
    } catch (e: JSONException) {
      sLogger.error("Error retrieving integer from JSONObject.")
    }
    return value
  }

  /**
   * Returns a string value associated with the supplied key.
   *
   * @param key to be used to retrieve desired value.
   * @return string value associated with the provided key.
   */
  fun getString(key: String?): String {
    return mConfigurations!!.optString(key, "")
  }

  /**
   * Returns a double value associated with the supplied key
   *
   * @param key to be used to retrieve desired value.
   * @return double value associated with the provided key.
   */
  fun getDouble(key: String): Double {
    var value = 0.0
    try {
      value = mConfigurations!!.getDouble(key)
    } catch (e: JSONException) {
      sLogger.error(
          "Error retrieving double from JSONObject. @ $key"
      )
    }
    return value
  }

  fun getJSONObject(key: String): JSONObject? {
    var value: JSONObject? = null
    try {
      value = mConfigurations!!.getJSONObject(key)
    } catch (e: JSONException) {
      sLogger.debug("key not found: $key")
    }
    return value
  }

  /**
   * Returns a boolean value associated with the supplied key.
   *
   * @param key to be used to retrieve desired value.
   * @return boolean value associated with the provided key.
   */
  fun getBoolean(key: String?): Boolean {
    var value = false
    value = try {
      mConfigurations!!.getBoolean(key)
    } catch (e: JSONException) {
      sLogger.error("Error retrieving boolean from JSONObject; trying as 1/0 int")
      return getInt(key) == 1
    }
    return value
  }

  fun getAdUnitTimeout(adUnitId: String?): Int {
    val obj = getJSONObject(AD_UNIT_TIMEOUTS)
    return try {
      obj!!.getInt(adUnitId)
    } catch (e: Exception) {
      0
    }
  }

  companion object {
    private val sLogger = Logger("SdkConfigurations")
  }
}