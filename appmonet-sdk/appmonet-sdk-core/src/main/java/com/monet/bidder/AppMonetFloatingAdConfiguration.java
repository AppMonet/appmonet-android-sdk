package com.monet.bidder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * The {@code AppMonetFloatingAdConfiguration} class contains the builder methods to configure
 * the floating ad behavior.
 */
//todo - fix class access
public class AppMonetFloatingAdConfiguration {
  final int maxAdDuration;
  public final String adUnitId;
  static final int DEFAULT_DURATION = 90000;

  /**
   * The enum values that can be set to configuration the floating ad.
   */
  public enum ValueType {
    PERCENT,
    DP
  }

  Map<String, FloatingPosition.Value> positionSettings = new HashMap<>();

  private AppMonetFloatingAdConfiguration(Builder builder)
      throws AppMonetFloatingAdConfigurationException {
    this.adUnitId = builder.adUnitId;
    if (this.adUnitId == null) {
      throw new AppMonetFloatingAdConfigurationException("AdUnitId is a required field");
    }
    positionSettings.put(FloatingPosition.WIDTH, new FloatingPosition.Value(150, FloatingPosition.DP));
    positionSettings.put(FloatingPosition.HEIGHT, new FloatingPosition.Value(180, FloatingPosition.DP));
    positionSettings.putAll(builder.positionSettings);
    this.maxAdDuration = (builder.duration == null) ? DEFAULT_DURATION : builder.duration;
  }

  public JSONObject toJson() throws JSONException {
    JSONObject magicPosition = new JSONObject();

    for (Map.Entry entry : positionSettings.entrySet()) {
      FloatingPosition.Value positionValue = (FloatingPosition.Value) entry.getValue();
      JSONObject magicPositionValue = new JSONObject();
      magicPositionValue.put("unit", positionValue.getUnit());
      magicPositionValue.put("value", positionValue.getValue());
      magicPosition.put(entry.getKey().toString(), magicPositionValue);
    }
    return magicPosition;
  }

  /**
   * Builder class responsible for setting the parameters to customize the floating ad.
   */

  public static class Builder {
    private Map<String, FloatingPosition.Value> positionSettings = new HashMap<>();
    private Integer duration;
    private String adUnitId;

    /**
     * Sets the ad unit id you are trying to load ads for. This is a required parameter.
     *
     * @param adUnitId The ad unit id you want to render ads from.
     */
    public Builder(String adUnitId) {
      this.adUnitId = adUnitId;
    }

    /**
     * Sets the left distance constraint relevant to the floating ad parent. This can be in either DP
     * or Percent.
     *
     * @param value The distance constraint you want for the floating ad
     * @param type  The type of value you want to pass. This can be DP or Percent.
     */
    public Builder left(int value, ValueType type) {
      positionSettings.put(FloatingPosition.START,
          new FloatingPosition.Value(value, (type == ValueType.PERCENT)
              ? FloatingPosition.PERCENT : FloatingPosition.DP));
      return this;
    }

    /**
     * Sets the right distance constraint relevant to the floating ad parent. This can be in either DP
     * or Percent.
     *
     * @param value The distance constraint you want for the floating ad
     * @param type  The type of value you want to pass. This can be DP or Percent.
     */
    public Builder right(int value, ValueType type) {
      positionSettings.put(FloatingPosition.END,
          new FloatingPosition.Value(value, (type == ValueType.PERCENT)
              ? FloatingPosition.PERCENT : FloatingPosition.DP));
      return this;
    }

    /**
     * Sets the top distance constraint relevant to the floating ad parent. This can be in either DP
     * or Percent.
     *
     * @param value The distance constraint you want for the floating ad
     * @param type  The type of value you want to pass. This can be DP or Percent.
     */
    public Builder top(int value, ValueType type) {
      positionSettings.put(FloatingPosition.TOP,
          new FloatingPosition.Value(value, (type == ValueType.PERCENT)
              ? FloatingPosition.PERCENT : FloatingPosition.DP));
      return this;
    }

    /**
     * Sets the bottom distance constraint relevant to the floating ad parent. This can be in either DP
     * or Percent.
     *
     * @param value The distance constraint you want for the floating ad
     * @param type  The type of value you want to pass. This can be DP or Percent.
     */
    public Builder bottom(int value, ValueType type) {
      positionSettings.put(FloatingPosition.BOTTOM,
          new FloatingPosition.Value(value, (type == ValueType.PERCENT)
              ? FloatingPosition.PERCENT : FloatingPosition.DP));
      return this;
    }

    /**
     * Sets the width for the floating ad. This can be in either DP
     * or Percent relevant to the parent.
     *
     * @param value The dimension value for your floating ad.
     * @param type  The type of value you want to pass. This can be DP or Percent.
     */
    public Builder width(int value, ValueType type) {
      positionSettings.put(FloatingPosition.WIDTH,
          new FloatingPosition.Value(value, (type == ValueType.PERCENT)
              ? FloatingPosition.PERCENT : FloatingPosition.DP));
      return this;
    }

    /**
     * Sets the height for the floating ad. This can be in either DP
     * or Percent relevant to the parent.
     *
     * @param value The dimension value for your floating ad.
     * @param type  The type of value you want to pass. This can be DP or Percent.
     */
    public Builder height(int value, ValueType type) {
      positionSettings.put(FloatingPosition.HEIGHT,
          new FloatingPosition.Value(value, (type == ValueType.PERCENT)
              ? FloatingPosition.PERCENT : FloatingPosition.DP));
      return this;
    }

    /**
     * Sets the duration you want the ad to be displayed before it disappears from the screen.
     * If not set the value will be set to the duration of the ad to be displayed.
     *
     * @param duration The time before the ad is closed. This value has to be passed as milliseconds.
     */
    public Builder maxAdDurationInMillis(int duration) {
      this.duration = duration;
      return this;
    }

    /**
     * This method will build the {@code AppMonetFloatingAdConfiguration} object.
     *
     * @throws AppMonetFloatingAdConfigurationException This error is thrown if ad unit id is null
     */
    public AppMonetFloatingAdConfiguration build() throws AppMonetFloatingAdConfigurationException {
      return new AppMonetFloatingAdConfiguration(this);
    }
  }

  public static class AppMonetFloatingAdConfigurationException extends RuntimeException {
    AppMonetFloatingAdConfigurationException(String message) {
      super(message);
    }
  }

}
