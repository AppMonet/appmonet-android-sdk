package com.monet.bidder

import com.monet.bidder.AppMonetFloatingAdConfiguration.ValueType.PERCENT
import com.monet.bidder.FloatingPosition.Value
import org.json.JSONException
import org.json.JSONObject
import java.util.HashMap

/**
 * The `AppMonetFloatingAdConfiguration` class contains the builder methods to configure
 * the floating ad behavior.
 */
class AppMonetFloatingAdConfiguration private constructor(builder: Builder) {
  val maxAdDuration: Int
  val adUnitId: String

  /**
   * The enum values that can be set to configuration the floating ad.
   */
  enum class ValueType {
    PERCENT,
    DP
  }

  internal var positionSettings: MutableMap<String, Value> = mutableMapOf()
  @Throws(JSONException::class) fun toJson(): JSONObject {
    val magicPosition = JSONObject()
    for ((key, value) in positionSettings) {
      val magicPositionValue = JSONObject()
      magicPositionValue.put("unit", value.unit)
      magicPositionValue.put("value", value.value)
      magicPosition.put(key, magicPositionValue)
    }
    return magicPosition
  }

  /**
   * Builder class responsible for setting the parameters to customize the floating ad.
   */
  class Builder
  /**
   * Sets the ad unit id you are trying to load ads for. This is a required parameter.
   *
   * @param adUnitId The ad unit id you want to render ads from.
   */(val adUnitId: String) {
    internal val positionSettings: MutableMap<String, Value> = HashMap()
    var duration: Int? = null

    /**
     * Sets the left distance constraint relevant to the floating ad parent. This can be in either DP
     * or Percent.
     *
     * @param value The distance constraint you want for the floating ad
     * @param type  The type of value you want to pass. This can be DP or Percent.
     */
    fun left(
      value: Int,
      type: ValueType
    ): Builder {
      positionSettings[FloatingPosition.START] = Value(
          value,
          if (type == PERCENT) FloatingPosition.PERCENT else FloatingPosition.DP
      )
      return this
    }

    /**
     * Sets the right distance constraint relevant to the floating ad parent. This can be in either DP
     * or Percent.
     *
     * @param value The distance constraint you want for the floating ad
     * @param type  The type of value you want to pass. This can be DP or Percent.
     */
    fun right(
      value: Int,
      type: ValueType
    ): Builder {
      positionSettings[FloatingPosition.END] = Value(
          value,
          if (type == PERCENT) FloatingPosition.PERCENT else FloatingPosition.DP
      )
      return this
    }

    /**
     * Sets the top distance constraint relevant to the floating ad parent. This can be in either DP
     * or Percent.
     *
     * @param value The distance constraint you want for the floating ad
     * @param type  The type of value you want to pass. This can be DP or Percent.
     */
    fun top(
      value: Int,
      type: ValueType
    ): Builder {
      positionSettings[FloatingPosition.TOP] = Value(
          value,
          if (type == PERCENT) FloatingPosition.PERCENT else FloatingPosition.DP
      )
      return this
    }

    /**
     * Sets the bottom distance constraint relevant to the floating ad parent. This can be in either DP
     * or Percent.
     *
     * @param value The distance constraint you want for the floating ad
     * @param type  The type of value you want to pass. This can be DP or Percent.
     */
    fun bottom(
      value: Int,
      type: ValueType
    ): Builder {
      positionSettings[FloatingPosition.BOTTOM] = Value(
          value,
          if (type == PERCENT) FloatingPosition.PERCENT else FloatingPosition.DP
      )
      return this
    }

    /**
     * Sets the width for the floating ad. This can be in either DP
     * or Percent relevant to the parent.
     *
     * @param value The dimension value for your floating ad.
     * @param type  The type of value you want to pass. This can be DP or Percent.
     */
    fun width(
      value: Int,
      type: ValueType
    ): Builder {
      positionSettings[FloatingPosition.WIDTH] = Value(
          value,
          if (type == PERCENT) FloatingPosition.PERCENT else FloatingPosition.DP
      )
      return this
    }

    /**
     * Sets the height for the floating ad. This can be in either DP
     * or Percent relevant to the parent.
     *
     * @param value The dimension value for your floating ad.
     * @param type  The type of value you want to pass. This can be DP or Percent.
     */
    fun height(
      value: Int,
      type: ValueType
    ): Builder {
      positionSettings[FloatingPosition.HEIGHT] = Value(
          value,
          if (type == PERCENT) FloatingPosition.PERCENT else FloatingPosition.DP
      )
      return this
    }

    /**
     * Sets the duration you want the ad to be displayed before it disappears from the screen.
     * If not set the value will be set to the duration of the ad to be displayed.
     *
     * @param duration The time before the ad is closed. This value has to be passed as milliseconds.
     */
    fun maxAdDurationInMillis(duration: Int): Builder {
      this.duration = duration
      return this
    }

    /**
     * This method will build the `AppMonetFloatingAdConfiguration` object.
     *
     * @throws AppMonetFloatingAdConfigurationException This error is thrown if ad unit id is null
     */
    @Throws(AppMonetFloatingAdConfigurationException::class)
    fun build(): AppMonetFloatingAdConfiguration {
      return AppMonetFloatingAdConfiguration(this)
    }
  }

  class AppMonetFloatingAdConfigurationException internal constructor(message: String?) :
      RuntimeException(message)

  companion object {
    const val DEFAULT_DURATION = 90000
  }

  init {
    adUnitId = builder.adUnitId
    if (adUnitId == null) {
      throw AppMonetFloatingAdConfigurationException("AdUnitId is a required field")
    }
    positionSettings[FloatingPosition.WIDTH] = Value(150, FloatingPosition.DP)
    positionSettings[FloatingPosition.HEIGHT] = Value(180, FloatingPosition.DP)
    positionSettings.putAll(builder.positionSettings)
    maxAdDuration = if (builder.duration == null) DEFAULT_DURATION else builder.duration!!
  }
}