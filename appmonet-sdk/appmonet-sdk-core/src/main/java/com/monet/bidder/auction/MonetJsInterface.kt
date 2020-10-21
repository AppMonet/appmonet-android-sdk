package com.monet.bidder.auction

import android.os.Handler
import android.os.SystemClock
import android.text.TextUtils
import android.webkit.JavascriptInterface
import com.monet.Callback
import com.monet.BidResponse
import com.monet.BidResponses.Mapper
import com.monet.bidder.BaseManager
import com.monet.bidder.CookieManager.Companion.instance
import com.monet.bidder.HttpUtil.makeRequest
import com.monet.bidder.Logger
import com.monet.bidder.Preferences
import com.monet.bidder.RemoteConfiguration
import com.monet.bidder.RenderingUtils
import com.monet.bidder.RenderingUtils.isScreenLocked
import com.monet.bidder.RenderingUtils.isScreenOn
import com.monet.bidder.RenderingUtils.numVisibleActivities
import com.monet.bidder.WebViewUtils
import com.monet.bidder.adview.AdViewManager
import com.monet.bidder.adview.AdViewPoolManager
import com.monet.bidder.bid.BidManager
import com.monet.threading.BackgroundThread
import com.monet.bidder.threading.InternalRunnable
import com.monet.bidder.threading.UIThread
import org.json.JSONException
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * This JSInterface class provides
 * Java-javascript interop into the "auction" webview (where the HB auction takes place).
 * These are not exposed within ads/adviews
 *
 * @see AuctionWebView
 */
class MonetJsInterface(
  sdkManager: BaseManager,
  private val uiThread: UIThread,
  private val backgroundThread: BackgroundThread,
  auctionWebViewParams: AuctionWebViewParams,
  auctionManagerCallback: AuctionManagerCallback,
  preferences: Preferences,
  private val remoteConfiguration: RemoteConfiguration
) {
  private val hexArray: CharArray
  private val mCallbacks: MutableMap<String?, Callback<String?>?>
  @get:JavascriptInterface val auctionUrl: String
  private val mPreferences: Preferences
  private val mBidManager: BidManager
  private val adViewPoolManager: AdViewPoolManager
  private val sdkManager: BaseManager
  private val auctionManagerCallback: AuctionManagerCallback

  /**
   * Indicate that the javascript loaded in the webView has finished
   * initialization, and that the webView can be marked as 'init' (onReady handlers called)
   *
   * @return a message indicating that the
   */
  @JavascriptInterface fun indicateInit(): String {
    sLogger.debug("javascript initialized..")
    auctionManagerCallback.onInit()
    return success("init accepted")
  }

  @JavascriptInterface fun setAdUnitNames(adUnitNameJson: String?): String {
    sLogger.debug("Syncing adunit names")
    return if (mBidManager.setAdUnitNames(adUnitNameJson)) {
      success("names set")
    } else error("failed to set names")
  }

  @JavascriptInterface fun hash(
    str: String,
    algorithm: String?
  ): String {
    var algorithm = algorithm
    if (algorithm == null) {
      algorithm = "SHA-1"
    }
    return try {
      val md = MessageDigest.getInstance(algorithm)
      val textBytes = str.toByteArray(charset("UTF-8"))
      md.update(textBytes, 0, textBytes.size)
      bytesToHex(md.digest())
    } catch (e: Exception) {
      ""
    }
  }

  @JavascriptInterface fun getAvailableBidCount(adUnitId: String?): String {
    val count = mBidManager.countBids(adUnitId)
    return Integer.toString(
        count
    ) // the JS can just go off of the native code; no need to keep our own store
  }

  @JavascriptInterface fun getConfiguration(
    forceServer: Boolean,
    cb: String?
  ) {
    backgroundThread.execute {
      val config = remoteConfiguration.getRemoteConfiguration(forceServer)
      auctionManagerCallback.executeCode(String.format(JS_CALLBACK, cb, config))
    }
  }

  @JavascriptInterface fun setBidsForAdUnit(payload: String): String {
    try {
//      val test = "{\"adUnitId\":\"*\",\"bids\":[{\"id\":\"v_39af60a945ae7616\",\"renderPixel\":\"https://1x1.a-mo.net/hbx/g_iimp?v=5.0.0-c1f8b25&aud=ao2d14bf63d1&mid=NN&aid=pjdfkud&C=pjdfkud&U=42&i=159&cc=US&av=1.0-mopub&lo=en&b=com.monet.app.mopub&sw=412&sh=869&m=480&M=311&ts=1603072034083&eid=gkgfvp5wj2x3f82y&a=*&ai=3891&B=16606&p=10&pp=100000&np=10&r=46&w=412&h=869&c1=LO&cn=16606&rj=none&A=appmonet&bid=v_39af60a945ae7616&c2=__event__\",\"duration\":30000,\"clickPixel\":\"https://1x1.a-mo.net/hbx/hclk?v=5.0.0-c1f8b25&aud=ao2d14bf63d1&mid=NN&aid=pjdfkud&C=pjdfkud&U=42&i=159&cc=US&av=1.0-mopub&lo=en&b=com.monet.app.mopub&sw=412&sh=869&m=480&M=311&ts=1603072034084&eid=hkgfvp5wk3voxf3g&a=*&ai=3891&B=16606&p=10&pp=100000&np=10&r=46&w=412&h=869&c1=LO&cn=16606&rj=none&A=appmonet&bid=v_39af60a945ae7616\",\"mega\":false,\"width\":412,\"height\":869,\"queueNext\":0,\"adm\":\"{\\\"data\\\":{\\\"width\\\":412,\\\"deviceID\\\":\\\"\\\",\\\"creativeURL\\\":null,\\\"lid\\\":16606,\\\"adunitId\\\":\\\"*\\\",\\\"cpm\\\":10,\\\"height\\\":869,\\\"interstitial\\\":\\\"portrait\\\",\\\"id\\\":\\\"v_39af60a945ae7616\\\",\\\"system\\\":\\\"AppMonet\\\",\\\"title\\\":\\\"AppMonet\\\",\\\"adid\\\":\\\"128a6.44d74.46b3\\\",\\\"ver\\\":\\\"2.0\\\",\\\"duration\\\":15,\\\"url\\\":\\\"https://assets.a-mo.net/vastv.xml?bundle=com.monet.app.mopub\\\",\\\"media\\\":[{\\\"url\\\":\\\"https://media.bidr.io/inmobi/2/402/43441_180919_Starbucks_video_VarietyCreative_MP4_HIGH.mp4\\\",\\\"type\\\":\\\"video/mp4\\\",\\\"bitrate\\\":800}],\\\"verification\\\":[],\\\"ctu\\\":\\\"http://appmonet.com\\\",\\\"tracking\\\":{\\\"Error\\\":[\\\"http://88.88-f.net/hbx/verr?e=\\\"],\\\"Viewable\\\":[],\\\"NotViewable\\\":[],\\\"ClickTracking\\\":[\\\"http://88.88-f.net/hbx/vclk?lid=test&aid=testapp\\\"],\\\"Impression\\\":[\\\"http://88.88-f.net/hbx/vimp?lid=test&aid=testapp\\\"],\\\"creativeView\\\":[],\\\"start\\\":[],\\\"skip\\\":[],\\\"firstQuartile\\\":[\\\"http://88.88-f.net/hbx/vfq?lid=test&aid=testapp\\\",\\\"http://88.88-f.net/hbx/vfq?lid=test&aid=testapp\\\"],\\\"midpoint\\\":[\\\"http://88.88-f.net/hbx/vmp?lid=test&aid=testapp\\\"],\\\"thirdQuartile\\\":[\\\"http://88.88-f.net/hbx/vtq?lid=test&aid=testapp\\\"],\\\"complete\\\":[\\\"http://88.88-f.net/hbx/vcmp?lid=test&aid=testapp\\\"],\\\"pause\\\":[],\\\"resume\\\":[],\\\"mute\\\":[],\\\"unmute\\\":[],\\\"progress\\\":[]}},\\\"type\\\":\\\"LOCAL_VIDEO\\\"}\",\"keywords\":\"mm_10:10.00,mm_25:10.00,mm_50:10.00,mm_1d:10.00,mm_bidder:appmonet,mm_code:default,mm_cpm:10.00,mm_cpm_md:10.00,mm_cpm_sm:10.00,mm_gte_5d:true,mm_gte_10d:true,mm_id:v_39af60a945ae7616,mm_size:412x869,mm_ckey_prefix:mm_\",\"adUnitId\":\"*\",\"flexSize\":false,\"interstitial\":{\"format\":\"portrait\",\"close\":true,\"trusted\":false},\"bidder\":\"appmonet\",\"code\":\"default\",\"ts\":1603072034083,\"cpm\":10,\"rtt\":46,\"brid\":16606,\"refresh\":0,\"extras\":{},\"u\":\"Mozilla/5.0 (Linux; Android 10; SM-G973U Build/QP1A.190711.020; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/86.0.4240.99 Mobile Safari/537.36\",\"url\":\"http://ads.mopub.com/\",\"adType\":\"LOCAL_VIDEO\",\"host\":\"\",\"cdown\":8,\"naRender\":false,\"wvUUID\":\"\",\"expiration\":900000, \"jose\": {\"test1\": \"value1\", \"test2\": 1}}]}"
      val mappedBids = Mapper.from(payload)
      val bids = mappedBids.bids
      mBidManager.addBids(bids)
    } catch (e: Exception) {
      sLogger.error("bad json passed for setBids: $payload")
      return error("invalid json")
    }
    return success("bids received")
  }

  @JavascriptInterface fun ajax(request: String?): String {
    return if (request == null || request.isEmpty()) {
      "{}"
    } else makeRequest(auctionManagerCallback.getMonetWebView(), request)
  }

  @JavascriptInterface fun getValue(request: String?): String {
    return try {
      val json = JSONObject(request)
      val key = json.getString("key")
      val type = json.getString("type")
      if (type == "boolean") {
        if (mPreferences.getPref(key, false)) "true" else "false"
      } else mPreferences.getPref(key, "")
    } catch (e: Exception) {
      error("invalid request")
    }
  }

  @JavascriptInterface fun setValue(request: String?): String {
    return try {
      val json = JSONObject(request)
      val key = json.getString("key")
      val type = json.getString("type")
      if (type == "boolean") {
        mPreferences.setPreference(key, json.getBoolean("value"))
      } else {
        mPreferences.setPreference(key, json.getString("value"))
      }
      success("set value")
    } catch (e: JSONException) {
      sLogger.warn("error syncing native preferences: " + e.message)
      error("invalid request")
    }
  }

  // js interface (bridge methods)
  @get:JavascriptInterface val advertisingInfo: Unit
    get() {
      auctionManagerCallback.getAdvertisingInfo()
    }

  // helpful when/if we need to avoid running
  // when nothing is visible or something..
  @get:JavascriptInterface val activitiesInfo: String
    get() = TextUtils.join(";", RenderingUtils.activitiesInfo)
  @get:JavascriptInterface val visibleActivityCount: String
    get() = Integer.toString(numVisibleActivities())
  @get:JavascriptInterface val isScreenLocked: String
    get() = java.lang.Boolean.toString(
        isScreenLocked(auctionManagerCallback.getDeviceData().context)
    )
  @get:JavascriptInterface val isScreenOn: String
    get() = java.lang.Boolean.toString(
        isScreenOn(auctionManagerCallback.getDeviceData().context)
    )

  // adView aka "helpers" interface
  @JavascriptInterface fun exec(
    uuid: String?,
    test: String?
  ): String {
    return if (adViewPoolManager.executeInContext(uuid!!, test)) success("called") else error(
        "invalid"
    )
  }

  @JavascriptInterface fun remove(uuid: String?): String {
    if (uuid == null) {
      return error("empty uuid")
    }
    return if (auctionManagerCallback.removeHelper(uuid)) {
      success("removed")
    } else error("failed to remove")
  }

  @JavascriptInterface fun requestHelperDestroy(uuid: String?): String {
    if (uuid == null) {
      return error("null uuid")
    }
    return if (auctionManagerCallback.requestHelperDestroy(uuid)) {
      success("requested")
    } else error("request failed")
  }

  @JavascriptInterface fun getRefCount(wvUUID: String?): String {
    // this way the javascript can determine if
    // it wants to remove one of the helpers
    return Integer.toString(adViewPoolManager.getReferenceCount(wvUUID))
  }

  @JavascriptInterface fun getAdViewUrl(
    wvUUID: String?,
    cb: String?
  ) {
    // must be async since getUrl accesses the webView
    uiThread.run(object : InternalRunnable() {
      override fun runInternal() {
        val url = adViewPoolManager.getUrl(wvUUID!!)
        auctionManagerCallback.executeCode(String.format(JS_CALLBACK, cb, WebViewUtils.quote(url)))
      }

      override fun catchException(e: Exception?) {
        sLogger.warn("Unable to get url", e!!.message)
      }
    })
  }

  @JavascriptInterface fun getNetworkCount(wvUUID: String?): String {
    return Integer.toString(adViewPoolManager.getNetworkCount(wvUUID!!))
  }

  @JavascriptInterface fun getHelperCreatedAt(wvUUID: String?): String {
    // this is in milliseconds, so can be used with javascript timestamp
    return java.lang.Long.toString(adViewPoolManager.getCreatedAt(wvUUID!!))
  }

  @JavascriptInterface fun getHelperRenderCount(wvUUID: String?): String {
    return Integer.toString(adViewPoolManager.getRenderCount(wvUUID!!))
  }

  @JavascriptInterface fun getHelperState(wvUUID: String?): String {
    return adViewPoolManager.getState(wvUUID!!)
  }

  @JavascriptInterface fun reloadConfigurations(): Boolean {
    return sdkManager.reloadConfigurations()
  }

  @JavascriptInterface fun launch(
    requestID: String,
    url: String?,
    ua: String?,
    html: String?,
    widthStr: String,
    heightStr: String,
    adUnitId: String?
  ): String {
    // try to parse the integers
    val height: Int
    val width: Int
    try {
      width = widthStr.toInt()
      height = heightStr.toInt()
    } catch (e: NumberFormatException) {
      return error("invalid integer")
    }
    if (url == null || ua == null || adUnitId == null) {
      return error("null values")
    }
    auctionManagerCallback.loadHelper(
        url, ua, html!!, width, height, adUnitId
    ) { value: AdViewManager ->
      auctionManagerCallback.executeJs(
          "helperReady", "'$requestID'",
          "'" + value.uuid + "'"
      )
    }
    return success("created")
  }

  @JavascriptInterface fun resetCookieManager() {
    instance!!.clear()
  }

  @JavascriptInterface fun loadCookieManager() {
    instance!!.load(auctionManagerCallback.getDeviceData().context)
  }

  @JavascriptInterface fun saveCookieManager() {
    instance!!.save(auctionManagerCallback.getDeviceData().context)
  }

  @get:JavascriptInterface val vMState: String
    get() {
      val json = auctionManagerCallback.getDeviceData().vMStats
      return json.toString()
    }

  @JavascriptInterface fun trigger(
    eventName: String?,
    response: String?
  ): String {
    synchronized(mCallbacks) {
      if (eventName == null || response == null) {
        return error("null")
      }
      val callback = mCallbacks[eventName]
          ?: return error("no callback")
      try {
        callback(response)
        mCallbacks.remove(eventName)
      } catch (e: Exception) {
        sLogger.warn("trigger error:", e.message)
        return error(e.message)
      }
      return success("received")
    }
  }

  @get:JavascriptInterface val deviceData: String
    get() = auctionManagerCallback.getDeviceData().toJsonString()

  @JavascriptInterface fun getSharedPreference(
    key: String?,
    subKey: String?,
    keyType: String,
    defaultBool: Boolean
  ): String {
    if (keyType == "string") {
      return Preferences.getStringAtKey(
          auctionManagerCallback.getDeviceData(), key,
          subKey, "null"
      )
    }
    return if (keyType == "boolean") {
      java.lang.Boolean.toString(
          Preferences.getBoolAtKey(
              auctionManagerCallback.getDeviceData(), key, subKey,
              defaultBool
          )
      )
    } else "null"
  }

  @JavascriptInterface fun subscribeKV(
    key: String?,
    valueType: String?
  ) {
    try {
      if (valueType != null && key != null) {
        mPreferences.keyValueFilter[key] = valueType
      }
    } catch (e: Exception) {
      sLogger.error("Error subscribing kV")
    }
  }

  /**
   * Subscribe to an event only once. When the event is emitted, the callback
   * will receive the payload supplied with the event (triggered from JavaScript)
   *
   * @param eventName the name of the event (usually a message UUID)
   * @param callback the handler to be called when the eventName is received
   */
  @Synchronized fun listenOnce(
    eventName: String?,
    timeout: Int,
    handler: Handler,
    callback: Callback<String?>
  ) {
    val handlerToken = Any()
    val onEvent: Callback<String?> = object : Callback<String?> {
      override fun invoke(value: String?) {
        // remove the listener
        removeListener(eventName, this)
        try {
          handler.removeCallbacksAndMessages(handlerToken)
          callback(value)
        } catch (e: Exception) {
        }
      }
    }
    val timeoutRunnable = Runnable {
      removeListener(eventName, onEvent)
      callback(null)
    }
    handler.postAtTime(timeoutRunnable, handlerToken, SystemClock.uptimeMillis() + timeout)
    mCallbacks[eventName] = onEvent
  }

  private fun bytesToHex(bytes: ByteArray): String {
    val hexChars = CharArray(bytes.size * 2)
    for (j in bytes.indices) {
      val v: Int = bytes[j].toInt() and 0xFF
      hexChars[j * 2] = hexArray[v ushr 4]
      hexChars[j * 2 + 1] = hexArray[v and 0x0F]
    }
    return String(hexChars)
  }

  /**
   * Remove a listener for the specified event. Since the eventemitter
   * behavior of this class is really only fire-once, we can clean up
   * the listeners as soon as an event is fired.
   *
   * @param eventName the name of the event the listener is subscribed to
   * @param callback the instance of ValueCallback listening
   */
  @Synchronized fun removeListener(
    eventName: String?,
    callback: Callback<String?>
  ) {
    if (mCallbacks.containsKey(eventName) && mCallbacks[eventName] === callback) {
      mCallbacks.remove(eventName)
    }
  }

  /**
   * Return a JSON error message with the given string
   *
   * @param message an error message string (must not contain double quotes)
   * @return a JSON-formatted error message to be sent to javascript
   */
  private fun error(message: String?): String {
    return "{\"error\": \"$message\"}"
  }

  /**
   * Format a "success" response as json to be delivered into the webView
   *
   * @param message the message to be returned as JSON. Must not contain double quotes
   * @return a JSON-encoded form of the message
   */
  private fun success(message: String): String {
    return "{\"success\": \"$message\"}"
  }

  companion object {
    private val sLogger = Logger("MonetBridge")
    private const val JS_CALLBACK = "window['%s'](%s);"
  }

  init {
    mCallbacks = ConcurrentHashMap()
    hexArray = "0123456789ABCDEF".toCharArray()
    mBidManager = sdkManager.auctionManager.bidManager
    adViewPoolManager = sdkManager.auctionManager.adViewPoolManager
    this.sdkManager = sdkManager
    this.auctionManagerCallback = auctionManagerCallback
    auctionUrl = auctionWebViewParams.auctionUrl
    mPreferences = preferences
  }
}