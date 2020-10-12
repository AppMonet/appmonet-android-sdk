package com.monet.bidder

import android.R.id
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.PowerManager
import android.text.TextUtils
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import android.widget.ImageView
import android.widget.RelativeLayout
import com.monet.bidder.bid.BidResponse
import java.lang.reflect.Field
import java.net.URI
import java.net.URISyntaxException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

object RenderingUtils {
  private val sVastTrackingPattern = Pattern.compile("monet://vast/(?:v2/)?([^/]+)/?([^/]+)?")
  private const val BASE64_FLAGS = Base64.NO_WRAP or Base64.NO_PADDING or Base64.NO_CLOSE
  fun generateBlankLayout(
    context: Context?,
    width: Int,
    height: Int
  ): FrameLayout {
    val blankView = View(context)
    val blankLayoutContainer = FrameLayout(
        context!!
    )
    blankLayoutContainer.addView(
        blankView,
        LayoutParams(width, height, Gravity.CENTER)
    )
    return blankLayoutContainer
  }

  @JvmStatic fun base64Encode(source: String?): String {
    return if (source == null || source.length == 0) {
      ""
    } else Base64.encodeToString(
        source.toByteArray(StandardCharsets.UTF_8), BASE64_FLAGS
    )
  }

  fun base64Decode(source: String?): String {
    return if (source == null || source.isEmpty()) {
      ""
    } else String(
        Base64.decode(source, BASE64_FLAGS),
        StandardCharsets.UTF_8
    )
  }

  fun encodeURIComponent(string: String?): String {
    return if (string == null || string.isEmpty()) {
      ""
    } else try {
      URI(null, null, string, null).rawPath
    } catch (e: URISyntaxException) {
      ""
    }
  }

  fun appendQueryParam(
    url: String,
    queryKey: String,
    queryValue: String?
  ): String {
    return if (url.isEmpty()) {
      url
    } else url + (if (url.indexOf('?') != -1) "&" else "?") +
        encodeURIComponent(queryKey) + "=" + encodeURIComponent(
        queryValue
    )
  }

  fun encodeStringByXor(source: String?): String {
    if (source == null || source.isEmpty()) {
      return ""
    }
    val utf8 = Charset.forName("UTF-8")
    val bytes = source.toByteArray(utf8)
    val output = ByteArray(bytes.size)
    for (i in bytes.indices) {
      output[i] = (bytes[i].toInt() xor 1).toByte()
    }
    return String(output, utf8)
  }

  // do nothing
  private val allActivities: Map<Any, Any>?
    private get() {
      try {
        val activityThreadClass = Class.forName("android.app.ActivityThread")
        val activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null)
        val activitiesField = activityThreadClass.getDeclaredField("mActivities")
        activitiesField.isAccessible = true
        return activitiesField[activityThread] as Map<Any, Any>
      } catch (e: Exception) {
        // do nothing
      }
      return null
    }

  fun getBase64ImageView(
    activity: Activity,
    icon: Icons
  ): ImageView {
    val drawable = icon.createDrawable(activity)
    val bitmap = (drawable as BitmapDrawable).bitmap
    val d: Drawable = BitmapDrawable(
        activity.resources,
        Bitmap.createScaledBitmap(
            bitmap, Icons.asIntPixels(16f, activity),
            Icons.asIntPixels(16f, activity), true
        )
    )
    val closeButton = ImageView(activity)
    closeButton.setImageDrawable(d)
    val closeButtonParams = LayoutParams(
        RelativeLayout.LayoutParams.WRAP_CONTENT,
        RelativeLayout.LayoutParams.WRAP_CONTENT,
        Gravity.TOP or Gravity.RIGHT
    )
    val closeButtonPadding = dpToPixels(16)
    closeButton.setPadding(closeButtonPadding, 0, 0, closeButtonPadding)
    closeButton.layoutParams = closeButtonParams
    closeButton.bringToFront()
    return closeButton
  }

  @JvmStatic fun isScreenLocked(context: Context): Boolean {
    val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    return km != null && km.inKeyguardRestrictedInputMode()
  }

  @JvmStatic fun isScreenOn(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        ?: return true
    return if (VERSION.SDK_INT >= VERSION_CODES.KITKAT_WATCH) {
      pm.isInteractive
    } else pm.isScreenOn
  }

  @JvmStatic val activitiesInfo: Array<String?>
    get() {
      val activities = allActivities ?: return arrayOf()
      val activityList = activities.values.toTypedArray()
      val output = arrayOfNulls<String>(activityList.size)
      for (i in activityList.indices) {
        output[i] = ActivityClientRecordHelper.toString(activityList[i])
      }
      return output
    }

  fun dpToPixels(dp: Int?): Int {
    dp?.let {
      return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }
    return 0
    //    final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
    //    return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics);
  }

  fun percentToPixels(
    containerDimension: Int,
    percent: Int?
  ): Int {
    percent?.let {
      return containerDimension * percent / 100
    }
    return containerDimension
  }

  @JvmStatic fun numVisibleActivities(): Int {
    try {
      val activityThreadClass = Class.forName("android.app.ActivityThread")
      val activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null)
      val visibleField = activityThreadClass.getDeclaredField("mNumVisibleActivities")
      visibleField.isAccessible = true
      return visibleField[activityThread] as Int ?: return -1
    } catch (e: Exception) {
      // do nothing
    }
    return -1
  }

  private val foregroundActivity: Activity?
    private get() {
      val record = foregroundActivityRecord
      return if (record != null) {
        ActivityClientRecordHelper.getActivity(record)
      } else null
    }

  fun getFlagByName(
    flagSource: Class<*>,
    flagName: String?
  ): Any? {
    return try {
      val field = flagSource.getDeclaredField(flagName!!)
      field[null]
    } catch (e: Exception) {
      null
    }
  }

  private val foregroundActivityRecord: Any?
    private get() {
      val activities = allActivities ?: return null
      for (record in activities.values) {
        if (!ActivityClientRecordHelper.isPaused(record)) {
          val found = ActivityClientRecordHelper.getActivity(record)
          if (found != null) {
            return record
          }
        }
      }
      return null
    }

  fun parseVastTracking(url: String?): VastTrackingMatch {
    if (url == null || url.isEmpty()) {
      return VastTrackingMatch(null, null)
    }
    val match = sVastTrackingPattern.matcher(url.toLowerCase())
    return if (match == null || !match.matches()) {
      VastTrackingMatch(null, null)
    } else VastTrackingMatch(
        match.group(1), match.group(2)
    )
  }

  fun isValidUrl(url: String?): Boolean {
    return url != null && !url.isEmpty() && URLUtil.isValidUrl(url)
  }

  fun setField(
    instance: Any?,
    fieldName: String?,
    expectedType: Class<*>?,
    value: Any?
  ): Any? {
    val field = getField(instance, fieldName, expectedType) ?: return null
    return try {
      field[instance] = value
      value
    } catch (e: IllegalAccessException) {
      null
    }
  }

  fun getField(
    instance: Any?,
    fieldName: String?,
    expectedType: Class<*>?
  ): Field? {
    if (instance == null || fieldName == null || fieldName.isEmpty()) {
      return null
    }
    val field: Field
    val klass =
      if (instance.javaClass == Class::class.java) instance as Class<*> else instance.javaClass
    field = try {
      klass.getDeclaredField(fieldName)
    } catch (e: NoSuchFieldException) {
      try {
        klass.getField(fieldName)
      } catch (e1: Exception) {
        return null
      }
    } catch (e2: Exception) {
      return null
    }
    if (field.type != expectedType) {
      return null
    }
    field.isAccessible = true
    return field
  }

  fun getDisplayDimensions(context: Context): IntArray {
    return try {
      val displayMetrics = context.resources.displayMetrics
      intArrayOf(
          Math.floor(displayMetrics.widthPixels / displayMetrics.density.toDouble())
              .toInt(),
          Math.floor(displayMetrics.heightPixels / displayMetrics.density.toDouble()).toInt()
      )
    } catch (e: Exception) {
      intArrayOf(0, 0)
    }
  }

  /**
   * Get the default URL to host the auction at. This can be overridden
   * by the configuration (set as a preference), but will default to the application's
   * bundle, backwards.
   *
   * @return a String representing the URL to host the auction at
   */
  fun getDefaultAuctionURL(deviceData: DeviceData): String {
    val app = deviceData.appInfo ?: return Constants.DEFAULT_AUCTION_URL
    val parts = TextUtils.split(app.packageName, "\\.")
    var len = parts.size
    val buffer = StringBuilder()
    while (--len >= 0) {
      buffer.append(parts[len])
      if (len > 0) {
        buffer.append(".")
      }
    }
    return "http://$buffer"
  }

  fun getCenterLayoutParams(
    context: Context?,
    adSize: AdSize
  ): LayoutParams {
    return LayoutParams(
        adSize.getWidthInPixels(context),
        adSize.getHeightInPixels(context),
        Gravity.CENTER
    )
  }

  private object ActivityClientRecordHelper {
    fun isPaused(record: Any): Boolean {
      try {
        val recordClass: Class<*> = record.javaClass
        val pausedField = recordClass.getDeclaredField("paused")
        pausedField.isAccessible = true
        return pausedField.getBoolean(record)
      } catch (e: Exception) {
        // do nothing
      }
      return false
    }

    fun getActivity(record: Any): Activity? {
      try {
        val recordClass: Class<*> = record.javaClass
        val activityField = recordClass.getDeclaredField("activity")
        activityField.isAccessible = true
        return activityField[record] as Activity
      } catch (e: Exception) {
        // do nothing
      }
      return null
    }

    fun toString(record: Any): String? {
      try {
        val recordClass: Class<*> = record.javaClass
        val getStateString = recordClass.getDeclaredMethod("getStateString")
        getStateString.isAccessible = true
        var representation = getStateString.invoke(record) as String
        val activity = getActivity(record)
        representation += if (activity != null) {
          val sb = StringBuilder()
          sb.append("||cn:")
          sb.append(activity.componentName.toString())
          sb.append("||r:")
          if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP_MR1) {
            sb.append(if (activity.referrer != null) activity.referrer.toString() else "noref")
          } else {
            sb.append("unknown")
          }
          sb.append("||t:")
          sb.append(activity.title)
          sb.toString()
        } else {
          "|no-name"
        }
        return representation
      } catch (e: Exception) {
        // do nothing
      }
      return null
    }
  }

  class VastTrackingMatch internal constructor(
    var event: String?,
    var bidId: String?
  ) {
    fun isForBid(bid: BidResponse?): Boolean {
      return if (bid == null || bidId == null) {
        true
      } else bidId.equals(bid.id, ignoreCase = true)
    }

    fun matches(): Boolean {
      return event != null
    }
  }
}