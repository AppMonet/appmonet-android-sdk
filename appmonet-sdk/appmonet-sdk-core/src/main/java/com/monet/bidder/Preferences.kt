package com.monet.bidder

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.annotation.VisibleForTesting
import java.util.HashMap
import com.monet.DeviceData

/**
 * This class serves as a wrapper for [SharedPreferences] for a much easier way of accessing
 * and saving values locally.
 */
class Preferences {
  private val mSharedPreferences: SharedPreferences
  val defaultSharedPreferences: SharedPreferences
  var keyValueFilter = mutableMapOf<String, String>()

  @VisibleForTesting
  internal constructor(
    sharedPreferences: SharedPreferences,
    defaultSharedPreferences: SharedPreferences
  ) {
    mSharedPreferences = sharedPreferences
    this.defaultSharedPreferences = defaultSharedPreferences
  }

  constructor(context: Context) {
    mSharedPreferences = getAtKey(context, SHARED_PREFERENCE_NAME)
    defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
  }

  /**
   * This method allows you pass a string key and a boolean value so it can be saved in
   * [SharedPreferences].
   *
   * @param key The key identifier for the value to be saved.
   * @param value The value to be saved.
   */
  fun setPreference(
    key: String?,
    value: Boolean?
  ) {
    try {
      val editor = mSharedPreferences.edit()
      editor.putBoolean(key, value!!)
      editor.apply()
    } catch (e: Exception) {
      sLogger.warn("Unable to update preference")
    }
  }

  /**
   * This method allows you pass a string key and a string value so it can be saved in
   * [SharedPreferences].
   *
   * @param key The key identifier for the value to be saved.
   * @param value The value to be saved.
   */
  fun setPreference(
    key: String?,
    value: String?
  ) {
    try {
      val editor = mSharedPreferences.edit()
      editor.putString(key, value)
      editor.apply()
    } catch (e: Exception) {
      sLogger.warn("Unable to set preference")
    }
  }

  /**
   * This method allows you to retrieve a string value associated to a provided key.
   * If the value is not there then the default value provided is returned instead.
   *
   * @param key The key identifier for the value to be retrieved.
   * @param defaultValue The default value to be returned if there is no value associated
   * with the given key.
   * @return The string value associated with the provided key or the default value if not
   * key/value pair is found.
   */
  fun getPref(
    key: String?,
    defaultValue: String
  ): String {
    try {
      return mSharedPreferences.getString(key, defaultValue) ?: defaultValue
    } catch (e: Exception) {
      sLogger.warn("Error getting pref")
    }
    return defaultValue
  }

  /**
   * This method allows you to retrieve a boolean value associated to a provided key.
   * If the value is not there then the default value provided is returned instead.
   *
   * @param key The key identifier for the value to be retrieved.
   * @param defaultValue The default value to be returned if there is no value associated
   * with the given key.
   * @return The boolean value associated with the provided key or the default value if no
   * key/value pair is found.
   */
  fun getPref(
    key: String?,
    defaultValue: Boolean
  ): Boolean {
    try {
      return mSharedPreferences.getBoolean(key, defaultValue)
    } catch (e: Exception) {
      sLogger.warn("Error getting pref")
    }
    return defaultValue
  }

  fun remove(key: String?) {
    try {
      val editor = mSharedPreferences.edit()
      editor.remove(key)
      editor.apply()
    } catch (e: Exception) {
      sLogger.warn("Error removing pref")
    }
  }

  companion object {
    private const val SHARED_PREFERENCE_NAME = "AppMonetBidder"
    private val sLogger = Logger("Bdr")
    fun getAtKey(
      context: Context,
      key: String?
    ): SharedPreferences {
      return context.getSharedPreferences(key, Context.MODE_PRIVATE)
    }

    fun getStringAtKey(
      deviceData: DeviceData,
      key: String?,
      subKey: String?,
      defaultValue: String
    ): String {
      return try {
        getAtKey(deviceData.context, key).getString(subKey, defaultValue) ?: defaultValue
      } catch (e: Exception) {
        defaultValue
      }
    }

    fun getBoolAtKey(
      deviceData: DeviceData,
      key: String?,
      subKey: String?,
      defaultValue: Boolean
    ): Boolean {
      return try {
        getAtKey(deviceData.context, key).getBoolean(subKey, defaultValue)
      } catch (e: Exception) {
        defaultValue
      }
    }
  }
}