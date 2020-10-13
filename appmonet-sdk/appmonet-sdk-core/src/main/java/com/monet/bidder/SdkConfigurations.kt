package com.monet.bidder;

import androidx.annotation.NonNull;
import com.monet.bidder.auction.MonetJsInterface;
import org.json.JSONException;
import org.json.JSONObject;

import static com.monet.bidder.Constants.Configurations.AD_UNIT_TIMEOUTS;

/**
 * SdkConfigurations holds a {@link JSONObject} which has the dynamic configuration options
 * retrieved by {@link MonetJsInterface}. This class encapsulates the required {@link JSONException}
 * handling and returns default values.
 */
public class SdkConfigurations {
  private final static Logger sLogger = new Logger("SdkConfigurations");
  private final JSONObject mConfigurations;

  public SdkConfigurations(JSONObject configurations) {
    mConfigurations = configurations;
  }

  public boolean hasKey(String key) {
    return mConfigurations != null && mConfigurations.has(key);
  }

  /**
   * Returns an integer value associated with the supplied key.
   *
   * @param key to be used to retrieve desired value.
   * @return integer value associated with the provided key.
   */
  public int getInt(String key) {
    int value = 0;
    try {
      value = mConfigurations.getInt(key);
    } catch (JSONException e) {
      sLogger.error("Error retrieving integer from JSONObject.");
    }
    return value;
  }

  /**
   * Returns a string value associated with the supplied key.
   *
   * @param key to be used to retrieve desired value.
   * @return string value associated with the provided key.
   */
  @NonNull
  public String getString(String key) {
    return mConfigurations.optString(key, "");
  }

  /**
   * Returns a double value associated with the supplied key
   *
   * @param key to be used to retrieve desired value.
   * @return double value associated with the provided key.
   */
  double getDouble(String key) {
    double value = 0.0;
    try {
      value = mConfigurations.getDouble(key);
    } catch (JSONException e) {
      sLogger.error("Error retrieving double from JSONObject. @ " + key);
    }
    return value;
  }

  JSONObject getJSONObject(String key) {
    JSONObject value = null;
    try {
      value = mConfigurations.getJSONObject(key);
    } catch (JSONException e) {
      sLogger.debug("key not found: " + key);
    }
    return value;
  }

  /**
   * Returns a boolean value associated with the supplied key.
   *
   * @param key to be used to retrieve desired value.
   * @return boolean value associated with the provided key.
   */
  public boolean getBoolean(String key) {
    boolean value = false;
    try {
      value = mConfigurations.getBoolean(key);
    } catch (JSONException e) {
      sLogger.error("Error retrieving boolean from JSONObject; trying as 1/0 int");
      return getInt(key) == 1;
    }
    return value;
  }

  int getAdUnitTimeout(String adUnitId) {
    JSONObject obj = getJSONObject(AD_UNIT_TIMEOUTS);
    try {
      return obj.getInt(adUnitId);
    } catch (Exception e) {
      return 0;
    }
  }
}
