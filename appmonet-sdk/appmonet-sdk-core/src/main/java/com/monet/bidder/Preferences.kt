package com.monet.bidder;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.annotation.VisibleForTesting;

import java.util.HashMap;
import java.util.Map;

/**
 * This class serves as a wrapper for {@link SharedPreferences} for a much easier way of accessing
 * and saving values locally.
 */
public class Preferences {
  private static final String SHARED_PREFERENCE_NAME = "AppMonetBidder";
  private static final Logger sLogger = new Logger("Bdr");
  private final SharedPreferences mSharedPreferences;
  private final SharedPreferences defaultSharedPreferences;
  public Map<String, String> keyValueFilter = new HashMap<>();

  static SharedPreferences getAtKey(Context context, String key) {
    return context.getSharedPreferences(key, Context.MODE_PRIVATE);
  }

  public static String getStringAtKey(Context context, String key, String subKey,
      String defaultValue) {
    try {
      return getAtKey(context, key).getString(subKey, defaultValue);
    } catch (Exception e) {
      return defaultValue;
    }
  }

  public static Boolean getBoolAtKey(Context context, String key, String subKey,
      boolean defaultValue) {
    try {
      return getAtKey(context, key).getBoolean(subKey, defaultValue);
    } catch (Exception e) {
      return defaultValue;
    }
  }

  @VisibleForTesting Preferences(SharedPreferences sharedPreferences,
      SharedPreferences defaultSharedPreferences) {
    mSharedPreferences = sharedPreferences;
    this.defaultSharedPreferences = defaultSharedPreferences;
  }

  public Preferences(Context context) {
    mSharedPreferences = getAtKey(context, SHARED_PREFERENCE_NAME);
    defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
  }

  public SharedPreferences getDefaultSharedPreferences() {
    return defaultSharedPreferences;
  }

  /**
   * This method allows you pass a string key and a boolean value so it can be saved in
   * {@link SharedPreferences}.
   *
   * @param key The key identifier for the value to be saved.
   * @param value The value to be saved.
   */
  public void setPreference(String key, Boolean value) {
    try {
      SharedPreferences.Editor editor = mSharedPreferences.edit();
      editor.putBoolean(key, value);
      editor.apply();
    } catch (Exception e) {
      sLogger.warn("Unable to update preference");
    }
  }

  /**
   * This method allows you pass a string key and a string value so it can be saved in
   * {@link SharedPreferences}.
   *
   * @param key The key identifier for the value to be saved.
   * @param value The value to be saved.
   */
  public void setPreference(String key, String value) {
    try {
      SharedPreferences.Editor editor = mSharedPreferences.edit();
      editor.putString(key, value);
      editor.apply();
    } catch (Exception e) {
      sLogger.warn("Unable to set preference");
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
  public String getPref(String key, String defaultValue) {
    try {
      return mSharedPreferences.getString(key, defaultValue);
    } catch (Exception e) {
      sLogger.warn("Error getting pref");
    }
    return defaultValue;
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
  public Boolean getPref(String key, Boolean defaultValue) {
    try {
      return mSharedPreferences.getBoolean(key, defaultValue);
    } catch (Exception e) {
      sLogger.warn("Error getting pref");
    }
    return defaultValue;
  }

  void remove(String key) {
    try {
      SharedPreferences.Editor editor = mSharedPreferences.edit();
      editor.remove(key);
      editor.apply();
    } catch (Exception e) {
      sLogger.warn("Error removing pref");
    }
  }
}
