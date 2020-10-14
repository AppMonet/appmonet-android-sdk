package com.monet.bidder

import android.util.Log
import com.monet.BuildConfig

/**
 * Created by jose on 8/28/17.
 */
object Constants {
  const val BIDS_KEY = "bids"
  internal const val LOG_LEVEL = Log.DEBUG
  internal const val AUCTION_WV_HK_PARAM = "u"
  internal const val AUCTION_WEBVIEW_HOOK = "h--k"
  internal const val JS_BRIDGE_VARIABLE = "__monet__"
  internal const val AUCTION_MANAGER_CONFIG_URL = "https://config.a-mo.net"
  internal const val TRACKING_URL = "https://1x1.a-mo.net/hbx/"
  internal const val SDK_VERSION = BuildConfig.VERSION_NAME
  private val AUCTION_SERVER_HOST =
    if (BuildConfig.DEBUG) BuildConfig.STAGING_SERVER_HOST else "https://cdn.88-f.net"
  internal val AUCTION_JS_URL =
    "$AUCTION_SERVER_HOST/js/$SDK_VERSION-auction.sdk.v2.min.js?v=$SDK_VERSION&nocache=true"
  internal val ADVIEW_JS_URL =
    "$AUCTION_SERVER_HOST/js/$SDK_VERSION-sdk.v2.js?v=$SDK_VERSION&"
  internal const val KW_KEY_PREFIX = "mm_"
  internal const val CUSTOM_KW_PREFIX_KEY = "mm_ckey_prefix"
  internal const val MARKET_SCHEME = "market"
  internal const val SDK_CLIENT = "android-native"
  internal const val AUCTION_URL_KEY = "auction_url"
  internal const val AUCTION_HTML_KEY = "mhtml"
  internal const val AUCTION_JS_KEY = "auction_js"
  internal const val APPMONET_APPLICATION_ID = "appmonet.application.id"
  private const val AUCTION_URL_BASE =
    "https://app.advertising"
  internal const val DEFAULT_AUCTION_URL =
    "$AUCTION_URL_BASE/sdk?v=09m="
  const val MISSING_INIT_ERROR_MESSAGE =
    "Error!\nError!\tYou must call AppMonet.init() in your Application subclass before calling AppMonet. %s\nError!"
  internal const val TEST_MODE_WARNING =
    "\n\n#######################################################################\n" +
        "APPMONET TEST MODE IS ENABLED\n" +
        "To disable remove AppMonet.testMode()\n" +
        "Rendering test demand only" +
        "\n#######################################################################\n"
  const val APPMONET_BROADCAST = "appmonet-broadcast"
  const val APPMONET_BROADCAST_MESSAGE = "message"
  const val INTERSTITIAL_ACTIVITY_BROADCAST = "interstitial-activity-broadcast"
  const val INTERSTITIAL_ACTIVITY_CLOSE = "interstitital-activity-close"
  const val INTERSTITIAL_HEIGHT = 480
  const val INTERSTITIAL_WIDTH = 320
  const val AD_SIZE = "ad_size"
  const val MONET_BID = "monet_bid"

  internal object JSAppStates {
    const val BACKGROUND = "appBackground"
    const val FOREGROUND = "appForeground"
    const val LOW_MEMORY = "appLowMemory"
    const val CONFIGURATION_CHANGED = "appConfigurationChanged"

    // activity-specific
    const val ACTIVITY_DESTROYED = "activityDestroyed"
    const val ACTIVITY_PAUSED = "activityPaused"
    const val ACTIVITY_STOPPED = "activityStopped"
    const val ACTIVITY_STARTED = "activityStarted"
    const val ACTIVITY_RESUMED = "activityResumed"
    const val ACTIVITY_CREATED = "activityCreated"
  }

  object JSMethods {
    const val INTERFACE_NAME = "monet"
    const val FETCH_BIDS = "fetchBids"
    const val FETCH_BIDS_BLOCKING = "fetchBidsBlocking"
    const val ADVERTISING_ID_KEY = "advertisingId"
    const val IS_TRACKING_ENABLED_KEY = "isTrackingEnabled"
    const val BID_USED = "bidUsed"
    const val STATE_CHANGE = "stateChange"
    const val INJECT = "inject"
    const val IMPRESSION_ENDED = "impressionEnded"
    const val MARK_INVALID = "markInvalid"
    const val RENDER = "render"
    const val TRACK_REQUEST = "trackRequest"
    const val START = "start"
    const val NAVIGATION_START = "navigationStart"
    const val CLICK = "click"
    const val TRACK_APP_STATE = "trackAppState"
    const val SET_LOG_LEVEL = "setLogLevel"
    const val JS_ASYNC_CALLBACK = "function (r) { window['%s'].trigger('%s', r); }"
    const val JS_CALL_TEMPLATE = "window['%s']['%s']([%s], %s)"
    const val PREFETCH_UNITS = "prefetch"
    const val REGISTER_FLOATING_AD = "prefetch"
    const val INVALIDATE_BID_REASON = "bidInvalidReason"
    const val HELPER_LOADED = "helperLoaded"
    const val HELPER_RESPOND = "helperRespond"
    const val HELPER_DESTROY = "helperDestroy"
    const val HELPER_CREATED = "helperCreated"
    const val ON_KV_CHANGE = "onKVChange"
  }

  object VASTEvents {
    const val START = "start"
    const val IMPRESSION = "impression"
    const val FIRST_QUARTILE = "firstquartile"
    const val MID_POINT = "midpoint"
    const val THIRD_QUARTILE = "thirdquartile"
    const val COMPLETE = "complete"
    const val ERROR = "error"
    const val FAIL_LOAD = "failload"
  }

  object Dfp {
    const val ADUNIT_KEYWORD_KEY = "__auid__"
    const val DEFAULT_BIDDER_KEY = "default"
  }

  internal object EventParams {
    const val APPLICATION_ID = "aid"
    const val VERSION = "v"
    const val HEADER_VERSION = "X-Monet-Version"
    const val HEADER_CLIENT = "X-Monet-Client"
    const val WRAPPER_VERSION = "wv"
  }

  object Configurations {
    internal const val WEB_VIEW_DEBUGGING_ENABLED = "f_webViewDebuggingEnabled"
    internal const val AM_SDK_CONFIGURATIONS = "amSdkConfiguration"
    const val MEDIATION_ENABLED = "f_mediationEnabled"
    const val DEFAULT_MEDIATION_FLOOR = "c_defaultMediationFloor"
    internal const val INJECTION_DELAY = "c_injectionDelay"
    internal const val SKIP_FETCH = "f_skipFetchIfLocal"
    internal const val FETCH_TIMEOUT_OVERRIDE = "c_fetchTimeoutOverride"
    internal const val AD_UNIT_TIMEOUTS = "d_adUnitTimeouts"
    internal const val REDIRECT_URL = "s_redirectUrl"
  }

  object Interstitial {
    const val AD_CONTENT_INTERSTITIAL = "adContent"
    const val BID_ID_INTERSTITIAL = "bidId"
    const val AD_UUID_INTERSTITIAL = "adUuid"
  }

  object PubSub {
    internal const val BID_RESPONSE_KEY = "bidKey"
    internal const val REMOVE_CREATIVE_KEY = "removeCreativeKey"

    object Topics {
      internal const val BIDS_CLEANED_UP_TOPIC = "cleanUpBids"
      internal const val AD_VIEW_REMOVED_TOPIC = "removeAdView"
      internal const val BID_ADDED_TOPIC = "bidAdded"
      internal const val BID_INVALIDATED_TOPIC = "bidInvalidated"
      internal const val BIDS_INVALIDATED_REASON_TOPIC = "bidsInvalidatedReason"
      const val NATIVE_PLACEMENT_TOPIC = "nativePlacement"
    }
  }
}