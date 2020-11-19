package com.monet.bidder

import android.view.View
import com.monet.AdServerBannerListener
import com.monet.bidder.AppMonetStaticNativeAd.Parameter.CALL_TO_ACTION
import com.monet.bidder.AppMonetStaticNativeAd.Parameter.ICON
import com.monet.bidder.AppMonetStaticNativeAd.Parameter.TEXT
import com.monet.bidder.AppMonetStaticNativeAd.Parameter.TITLE
import com.mopub.nativeads.BaseNativeAd
import com.mopub.nativeads.ClickInterface
import com.mopub.nativeads.CustomEventNative.CustomEventNativeListener
import com.mopub.nativeads.ImpressionInterface
import com.mopub.nativeads.ImpressionTracker
import com.mopub.nativeads.NativeClickHandler
import com.mopub.nativeads.NativeErrorCode.SERVER_ERROR_RESPONSE_CODE
import com.mopub.nativeads.NativeErrorCode.UNEXPECTED_RESPONSE_CODE
import java.util.HashMap
import java.util.HashSet

/**
 * Class which holds the logic and interface callbacks for the native ad to be displayed and
 * interacted with.
 */
class AppMonetStaticNativeAd(
  private val serverExtras: Map<String, String>,
  var media: View?,
  private val mImpressionTracker: ImpressionTracker,
  private val mNativeClickHandler: NativeClickHandler,
  private val mCustomEventNativeListener: CustomEventNativeListener,
  private val appMonetNativeEventCallback: AppMonetNativeEventCallback
) : BaseNativeAd(), ClickInterface, ImpressionInterface {
  private val mExtras: HashMap<String, Any?> = HashMap()

  internal enum class Parameter(
    val type: String,
    val required: Boolean
  ) {

    IMPRESSION_TRACKER("imptracker", false),
    TITLE("title", false),
    TEXT("text", false),
    ICON("icon", false),
    CALL_TO_ACTION("ctatext", false);

    companion object {
      fun from(name: String): Parameter? {
        for (parameter in values()) {
          if (parameter.type == name) {
            return parameter
          }
        }
        return null
      }

      val requiredKeys: MutableSet<String> = HashSet()

      init {

        for (parameter in values()) {
          if (parameter.required) {
            requiredKeys.add(parameter.type)
          }
        }
      }
    }
  }

  var mainView: View? = null
  var title: String? = null
  var icon: String? = null
  var text: String? = null
  var callToAction: String? = null
  private var mImpressionRecorded = false
  private val mImpressionMinTimeViewed = 1000
  override fun prepare(view: View) {
    mImpressionTracker.addView(view, this)
    mNativeClickHandler.setOnClickListener(view, this)
  }

  override fun clear(view: View) {
    mImpressionTracker.removeView(view)
    mNativeClickHandler.clearOnClickListener(view)
  }

  override fun destroy() {
    mImpressionTracker.destroy()
    if (media != null) {
      appMonetNativeEventCallback.destroy(media)
    }
  }

  override fun recordImpression(view: View) {
    notifyAdImpressed()
  }

  override fun getImpressionMinPercentageViewed(): Int {
    return 50
  }

  override fun getImpressionMinVisiblePx(): Int? {
    return null
  }

  override fun getImpressionMinTimeViewed(): Int {
    return mImpressionMinTimeViewed
  }

  override fun isImpressionRecorded(): Boolean {
    return mImpressionRecorded
  }

  override fun setImpressionRecorded() {
    mImpressionRecorded = true
  }

  //
  override fun handleClick(view: View) {
    if (media != null) {
      appMonetNativeEventCallback.onClick(media)
      notifyAdClicked()
    }
  }

  val extras: Map<String, Any?>
    get() = HashMap(mExtras)

  fun onAdClicked() {
    notifyAdClicked()
  }

  fun loadAd() {
    if (!containsRequiredKeys(serverExtras)) {
      mCustomEventNativeListener.onNativeAdFailed(SERVER_ERROR_RESPONSE_CODE)
    }
    val keys = serverExtras.keys
    for (key in keys) {
      val parameter = Parameter.from(key)
      if (parameter != null) {
        try {
          addInstanceVariable(parameter, serverExtras[key])
        } catch (e: ClassCastException) {
          mCustomEventNativeListener.onNativeAdFailed(UNEXPECTED_RESPONSE_CODE)
        }
      } else {
        addExtra(key, serverExtras[key])
      }
    }
    mCustomEventNativeListener.onNativeAdLoaded(this)
  }

  private fun containsRequiredKeys(serverExtras: Map<String, String>): Boolean {
    val keys = serverExtras.keys
    return keys.containsAll(Parameter.requiredKeys)
  }

  private fun addInstanceVariable(
    key: Parameter,
    value: Any?
  ) {
    try {
      when (key) {
        ICON -> icon = value as String?
        CALL_TO_ACTION -> callToAction = value as String?
        TITLE -> title = value as String?
        TEXT -> text = value as String?
        else -> logger.debug("Unable to add JSON key to internal mapping: " + key.type)
      }
    } catch (e: ClassCastException) {
      if (!key.required) {
        logger.debug("Ignoring class cast exception for optional key: " + key.type)
      } else {
        throw e
      }
    }
  }

  fun addExtra(
    key: String,
    value: Any?
  ) {
    mExtras[key] = value
  }

  fun swapViews(
    view: AppMonetViewLayout,
    listener: AdServerBannerListener<View?>
  ) {
    (media as AppMonetViewLayout?)!!.swapViews(view, listener)
  }

  companion object {
    private val logger = Logger("AppMonetStaticNativeAd")
  }

}