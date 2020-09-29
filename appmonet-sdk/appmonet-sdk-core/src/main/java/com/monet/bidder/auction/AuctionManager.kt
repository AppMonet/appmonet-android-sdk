package com.monet.bidder.auction

import android.content.*
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Process
import android.text.TextUtils
import android.webkit.ValueCallback
import androidx.annotation.VisibleForTesting
import com.monet.bidder.*
import com.monet.bidder.Constants.JSMethods.ADVERTISING_ID_KEY
import com.monet.bidder.Constants.JSMethods.FETCH_BIDS_BLOCKING
import com.monet.bidder.Constants.JSMethods.HELPER_CREATED
import com.monet.bidder.Constants.JSMethods.HELPER_DESTROY
import com.monet.bidder.Constants.JSMethods.HELPER_LOADED
import com.monet.bidder.Constants.JSMethods.INVALIDATE_BID_REASON
import com.monet.bidder.Constants.JSMethods.IS_TRACKING_ENABLED_KEY
import com.monet.bidder.Constants.JSMethods.PREFETCH_UNITS
import com.monet.bidder.Constants.JSMethods.REGISTER_FLOATING_AD
import com.monet.bidder.Constants.JSMethods.SET_LOG_LEVEL
import com.monet.bidder.Constants.JSMethods.START
import com.monet.bidder.Constants.JSMethods.TRACK_APP_STATE
import com.monet.bidder.Constants.JSMethods.TRACK_REQUEST
import com.monet.bidder.WebViewUtils.quote
import com.monet.bidder.adview.AdViewContext
import com.monet.bidder.adview.AdViewManager
import com.monet.bidder.adview.AdViewPoolManager
import com.monet.bidder.bid.BidManager
import com.monet.bidder.callbacks.ReadyCallbackManager
import com.monet.bidder.threading.BackgroundThread
import com.monet.bidder.threading.InternalRunnable
import com.monet.bidder.threading.UIThread
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import com.monet.bidder.bid.BidResponse

class AuctionManager : Subscriber, AuctionManagerCallback {
  override val mediationManager: MediationManager
  override var advertisingId = ""

  private val addBidsManager: AddBidsManager
  private val appMonetBidder: AppMonetBidder
  private val appMonetContext: AppMonetContext
  private val auctionManagerReadyCallbacks: ReadyCallbackManager<AppMonetWebView>
  private val auctionWebViewCreatedCallbacks: ReadyCallbackManager<AppMonetWebView>
  private val backgroundThread: BackgroundThread
  private val baseManager: BaseManager
  private val deviceData: DeviceData
  private val sLogger = Logger("AuctionManager")
  private val uiThread: UIThread

  private var defaultSharePrefsListener: OnSharedPreferenceChangeListener? = null

  lateinit var auctionWebView: AppMonetWebView
  val adViewPoolManager: AdViewPoolManager
  val bidManager: BidManager

  internal constructor(
    context: Context,
    baseManager: BaseManager,
    uiThread: UIThread,
    backgroundThread: BackgroundThread,
    deviceData: DeviceData,
    pubSubService: PubSubService,
    appMonetContext: AppMonetContext,
    preferences: Preferences,
    sdkConfiguration: SdkConfigurations,
    remoteConfiguration: RemoteConfiguration,
    adServerWrapper: AdServerWrapper
  ) {
    this.auctionManagerReadyCallbacks = ReadyCallbackManager()
    this.auctionWebViewCreatedCallbacks = ReadyCallbackManager()
    this.baseManager = baseManager
    this.backgroundThread = backgroundThread
    this.uiThread = uiThread
    this.deviceData = deviceData
    this.adViewPoolManager = AdViewPoolManager(
        adServerWrapper, appMonetContext, this,
        context, pubSubService, backgroundThread, uiThread
    )
    this.bidManager = BidManager(
        pubSubService, backgroundThread, adViewPoolManager,
        this
    )

    mediationManager = MediationManager(baseManager, bidManager)
    this.addBidsManager = AddBidsManager(auctionManagerReadyCallbacks)
    this.appMonetContext = appMonetContext
    appMonetBidder = AppMonetBidder(
        context, baseManager, bidManager,
        adServerWrapper, this, backgroundThread
    )
    setPreferencesListener(preferences)
    val runnable: Runnable = object : InternalRunnable() {
      override fun runInternal() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY)
        val webViewParams = AuctionWebViewParams(
            RenderingUtils.getDefaultAuctionURL(deviceData),
            preferences,
            appMonetContext
        )
        val auctionJsInterface = MonetJsInterface(
            baseManager, uiThread, backgroundThread,
            webViewParams, this@AuctionManager, preferences,
            remoteConfiguration
        )
        setup(
            AuctionWebView(
                context, auctionJsInterface, webViewParams,
                sdkConfiguration
            )
        )
        start(context)
      }

      override fun catchException(e: Exception?) {}
    }
    uiThread.run(runnable)
    pubSubService.addSubscriber(Constants.PubSub.Topics.BIDS_INVALIDATED_REASON_TOPIC, this)
  }

  @VisibleForTesting
  internal constructor(
    addBidsManager: AddBidsManager,
    adViewPoolManager: AdViewPoolManager,
    appMonetBidder: AppMonetBidder,
    appMonetContext: AppMonetContext,
    auctionManagerReadyCallbacks: ReadyCallbackManager<AppMonetWebView>,
    auctionWebView: AppMonetWebView,
    auctionWebViewCreatedCallbacks: ReadyCallbackManager<AppMonetWebView>,
    backgroundThread: BackgroundThread,
    baseManager: BaseManager,
    bidManager: BidManager,
    mediationManager: MediationManager,
    uiThread: UIThread,
    deviceData: DeviceData
  ) {
    this.addBidsManager = addBidsManager
    this.adViewPoolManager = adViewPoolManager
    this.appMonetBidder = appMonetBidder
    this.appMonetContext = appMonetContext
    this.auctionManagerReadyCallbacks = auctionManagerReadyCallbacks
    this.auctionWebViewCreatedCallbacks = auctionWebViewCreatedCallbacks

    this.backgroundThread = backgroundThread
    this.baseManager = baseManager
    this.bidManager = bidManager
    this.deviceData = deviceData
    this.mediationManager = mediationManager
    this.uiThread = uiThread
    setup(auctionWebView)
  }

  override fun cancelRequest(
    adUnitId: String?,
    adRequest: AdServerAdRequest?,
    bid: BidResponse?
  ) {
    appMonetBidder.cancelRequest(adUnitId, adRequest, bid)
  }

  override fun executeCode(code: String) {
    auctionManagerReadyCallbacks.onReady { webView ->
      webView.executeJsCode(code)
    }
  }

  override fun executeJs(
    method: String,
    vararg args: String
  ) {
    auctionManagerReadyCallbacks.onReady { webView ->
      webView.executeJs(method, *args)
    }
  }

  override fun executeJs(
    timeout: Int,
    method: String,
    callback: ValueCallback<String>?,
    vararg args: String
  ) {
    auctionManagerReadyCallbacks.onReady { webView ->
      webView.executeJs(timeout, method, callback, *args)
    }
  }

  override fun getAdvertisingInfo() {
    auctionManagerReadyCallbacks.onReady { webView ->
      deviceData.getAdClientInfo(ValueCallback { adInfo ->
        val json = JSONObject()
        try {
          advertisingId = adInfo.advertisingId
          json.put(ADVERTISING_ID_KEY, adInfo.advertisingId)
          json.put(IS_TRACKING_ENABLED_KEY, adInfo.isLimitAdTrackingEnabled)
        } catch (e: JSONException) {
          sLogger.error("error creating advertising ID JSON")
        }
        webView.loadView(WebViewUtils.javascriptExecute("advertisingId", json.toString()))
      })
    }
  }

  override fun getBidById(bidId: String): BidResponse? {
    return bidManager.getBidById(bidId)
  }

  override fun getBidWithFloorCpm(
    adUnitId: String,
    floorCpm: Double
  ): BidResponse? {
    return mediationManager.getBidForMediation(adUnitId, floorCpm)
  }

  override fun getDeviceData(): DeviceData {
    return deviceData
  }

  override fun getMonetWebView(): AppMonetWebView? {
    return auctionManagerReadyCallbacks.onReadySync()
  }

  override fun getSdkConfigurations(): SdkConfigurations {
    return this.baseManager.sdkConfigurations
  }

  override fun helperCreated(
    uuid: String,
    adViewContextJson: String
  ) {
    auctionManagerReadyCallbacks.onReady { webView ->
      val quotedUUID = quote(uuid)
      webView.executeJs(HELPER_CREATED, quotedUUID, adViewContextJson)
    }
  }

  override fun helperDestroy(wvUUID: String) {
    auctionManagerReadyCallbacks.onReady { webView ->
      val quotedUUID = quote(wvUUID)
      webView.executeJs(HELPER_DESTROY, quotedUUID)
    }
  }

  override fun helperLoaded(uuid: String) {
    auctionManagerReadyCallbacks.onReady { webView ->
      val quotedUUID = quote(uuid)
      webView.executeJs(HELPER_LOADED, quotedUUID)
    }
  }

  override fun invalidateBidsForAdView(uuid: String) {
    bidManager.invalidate(uuid)
  }

  override fun loadHelper(
    url: String,
    ua: String,
    html: String,
    width: Int,
    height: Int,
    adUnitId: String,
    onLoad: ValueCallback<AdViewManager>
  ) {
    val context = AdViewContext(url, ua, width, height, adUnitId)
    uiThread.run(object : InternalRunnable() {
      override fun runInternal() {
        val adViewManager: AdViewManager = adViewPoolManager.request(context)
        if (!adViewManager.isLoaded) {
          adViewManager.load()
        }
        onLoad.onReceiveValue(adViewManager)
      }

      override fun catchException(e: Exception?) {
        sLogger.error("LoadHelper exception: " + e!!.message)
      }
    })
  }

  override fun markBidAsUsed(bid: BidResponse) {
    bidManager.markUsed(bid)
  }

  override fun onInit() {
    auctionWebViewCreatedCallbacks.onReady { webView ->
      webView.isLoaded().set(true)
      webView.executeJs(SET_LOG_LEVEL, quote(Logger.levelString()))
      webView.executeJs(START, "''", quote(appMonetContext.applicationId))
      addBidsManager.executeReady()
      auctionManagerReadyCallbacks.executeReady(webView)
    }
  }

  override fun removeHelper(uuid: String): Boolean {
    return this.adViewPoolManager.remove(uuid, true)
  }

  override fun requestHelperDestroy(uuid: String): Boolean {
    return this.adViewPoolManager.requestDestroy(uuid)
  }

  override fun trackEvent(
    event: String,
    detail: String,
    key: String,
    value: Float,
    currentTime: Long
  ) {
    auctionManagerReadyCallbacks.onReady { webView ->
      webView.trackEvent(event, detail, key, value, currentTime)
    }
  }

  override fun trackRequest(
    adUnitId: String,
    source: String
  ) {
    auctionManagerReadyCallbacks.onReady { webView ->
      webView.executeJs(TRACK_REQUEST, quote(adUnitId), quote(source))
    }
  }

  override fun removeBid(bidId: String): BidResponse? {
    return bidManager.removeBid(bidId)
  }

  @Suppress("UNCHECKED_CAST")
  override fun onBroadcast(subscriberMessages: MonetPubSubMessage?) {
    auctionManagerReadyCallbacks.onReady { webView ->
      try {
        if (subscriberMessages!!.topic == Constants.PubSub.Topics.BIDS_INVALIDATED_REASON_TOPIC) {
          val messagePayload = subscriberMessages.payload as Map<String, String>
          val jsonObject = JSONObject()
          for ((key, value) in messagePayload) {
            jsonObject.put(key, value)
          }
          webView.executeJs(INVALIDATE_BID_REASON, jsonObject.toString())
        }
      } catch (e: JSONException) {
        sLogger.error("Json parsing exception : $e")
      }
    }
  }

  fun addBids(
    adServerAdView: AdServerAdView,
    baseRequest: AdServerAdRequest
  ): AdServerAdRequest? {
    return appMonetBidder.addBids(adServerAdView, baseRequest)
  }

  fun addBids(
    adServerAdView: AdServerAdView,
    baseRequest: AdServerAdRequest,
    remainingTime: Int,
    valueCallback: ValueCallback<AdServerAdRequest>
  ) {
    appMonetBidder.addBids(adServerAdView, baseRequest, remainingTime, valueCallback)
  }

  fun disableBidCleaner() {
    bidManager.disableIntervalCleaner()
  }

  fun enableBidCleaner() {
    bidManager.enableIntervalCleaner()
  }

  fun indicateRequest(
    adUnitId: String,
    adSize: AdSize?,
    adType: AdType,
    floorCpm: Double
  ) {
    auctionManagerReadyCallbacks.onReady { webView ->
      val params = arrayOf(quote(adUnitId), "15000", "{}", "'mediation'", quote(adType.toString()))
      val args = getIndicateRequestArgs(adSize, floorCpm, params)
      webView.executeJs(FETCH_BIDS_BLOCKING, *args.toTypedArray())
    }
  }

  fun indicateRequestAsync(
    adUnitId: String,
    timeout: Int,
    adSize: AdSize?,
    adType: AdType,
    floorCpm: Double,
    callback: ValueCallback<String>
  ) {
    auctionManagerReadyCallbacks.onReady { webView ->
      val params = arrayOf(
          quote(adUnitId), timeout.toString(), "{}",
          quote("iwin"), quote(adType.toString())
      )
      val args = getIndicateRequestArgs(adSize, floorCpm, params)
      webView.executeJs(timeout, FETCH_BIDS_BLOCKING, callback, *args.toTypedArray())
    }
  }

  fun logState() {
    sLogger.debug("\n<<<[DNE|SdkManager State Dump]>>>")
    bidManager.logState()
    adViewPoolManager.logState()
    sLogger.debug("\n<<<[DNE|SdkManager State Dump]>>>")
  }

  fun prefetchAdUnits(adUnitIds: List<String>) {
    auctionManagerReadyCallbacks.onReady { webView ->
      // format the adUnit ids to be passed into javascript
      val adUnitsQuoted: MutableList<String> = ArrayList()
      for (adUnitId in adUnitIds) {
        adUnitsQuoted.add(quote(adUnitId))
      }
      webView.executeJs(PREFETCH_UNITS, TextUtils.join(",", adUnitsQuoted))
    }
  }

  fun registerFloatingAd(adConfiguration: AppMonetFloatingAdConfiguration) {
    auctionManagerReadyCallbacks.onReady { webView ->
      try {
        webView.executeJs(
            REGISTER_FLOATING_AD, quote(adConfiguration.adUnitId),
            adConfiguration.toJson().toString()
        )
      } catch (e: JSONException) {
        sLogger.error("error registering floating ad with auction manager")
      }
    }
  }

  /**
   * Tell the javascript to log at the same level as native code.
   */
  fun syncLogger() {
    auctionManagerReadyCallbacks.onReady { webView ->
      webView.executeJs(SET_LOG_LEVEL, quote(Logger.levelString()))
    }
  }

  fun testMode() {
    auctionManagerReadyCallbacks.onReady { webView ->
      sLogger.warn(
          """

                        ########################################################################
                        APP MONET TEST MODE ENABLED. USE ONLY DURING DEVELOPMENT.
                        ########################################################################

                        """.trimIndent()
      )
      webView.executeJs("testMode")
    }
  }

  fun timedCallback(
    timeout: Int,
    timedCallback: TimedCallback
  ) {
    addBidsManager.onReady(timeout, timedCallback)
  }

  fun trackAppState(
    appState: String,
    identifier: String
  ) {
    auctionManagerReadyCallbacks.onReady { webView ->
      sLogger.info("state change: ", appState)
      webView.executeJs(TRACK_APP_STATE, quote(appState), quote(identifier))
    }
  }

  private fun getIndicateRequestArgs(
    adSize: AdSize?,
    floorCpm: Double,
    params: Array<String>
  ): MutableList<String> {
    val args: MutableList<String> = ArrayList(listOf(*params))
    if (adSize != null && adSize.height != 0 && adSize.width != 0) {
      args.add(adSize.width.toString())
      args.add(adSize.height.toString())
    } else {
      args.add("0")
      args.add("0")
    }
    args.add(floorCpm.toString())
    return args
  }

  private fun start(context: Context) {
    backgroundThread.execute(object : InternalRunnable() {
      override fun runInternal() {
        auctionWebViewCreatedCallbacks.onReady { webView ->
          webView.start()
          setupNetworkConnectivityListener(context)
        }
      }

      override fun catchException(e: Exception?) {}
    })
  }

  /**
   * This method sets up the listeners for [SharedPreferences]so we can know when different
   * key values change.
   *
   * @param preferences The [Preferences] object holding default [SharedPreferences].
   */
  private fun setPreferencesListener(preferences: Preferences) {
    auctionManagerReadyCallbacks.onReady { webView ->
      defaultSharePrefsListener = OnSharedPreferenceChangeListener { sharedPreferences, key ->
        try {
          val filteringKeys: Set<String> = preferences.keyValueFilter.keys
          if (filteringKeys.contains(key)) {
            val keyType = preferences.keyValueFilter[key]
            if (keyType != null) {
              when (keyType) {
                "string" -> webView.executeJsAsync(
                    null,
                    Constants.JSMethods.ON_KV_CHANGE, 0, quote(key),
                    quote(sharedPreferences.getString(key, ""))
                )
                "long" -> webView.executeJsAsync(
                    null,
                    Constants.JSMethods.ON_KV_CHANGE, 0, quote(key),
                    quote(sharedPreferences.getLong(key, -404L).toString())
                )
                "float" -> webView.executeJsAsync(
                    null,
                    Constants.JSMethods.ON_KV_CHANGE, 0, quote(key),
                    quote(sharedPreferences.getFloat(key, -404f).toString())
                )
                "integer" -> webView.executeJsAsync(
                    null,
                    Constants.JSMethods.ON_KV_CHANGE, 0, quote(key),
                    quote(sharedPreferences.getInt(key, -404).toString())
                )
                "boolean" -> webView.executeJsAsync(
                    null,
                    Constants.JSMethods.ON_KV_CHANGE, 0, quote(key),
                    quote(java.lang.Boolean.toString(sharedPreferences.getBoolean(key, false)))
                )
              }
            }
          }
        } catch (e: java.lang.Exception) {
          sLogger.error("Error finding kv change")
        }
      }
      preferences.defaultSharedPreferences
          .registerOnSharedPreferenceChangeListener(defaultSharePrefsListener)
    }
  }

  private fun setup(auctionWebView: AppMonetWebView) {
    this.auctionWebView = auctionWebView
    auctionWebViewCreatedCallbacks.executeReady(auctionWebView)
  }

  /**
   * Creates broadcast receiver to listen for network connectivity changes. This allows us to
   * initialize the sdk when network is available.
   *
   * @param context for registering broadcast receiver.
   */
  private fun setupNetworkConnectivityListener(context: Context) {
    val broadcastReceiver = object : BroadcastReceiver() {
      override fun onReceive(
        context: Context?,
        intent: Intent?
      ) {
        if (auctionWebView.isLoaded().get()) {
          context?.unregisterReceiver(this)
        }
        if (isInitialStickyBroadcast) {
          return
        }
        if (!auctionWebView.isLoaded().get() && HttpUtil.hasNetworkConnection(context)) {
          auctionWebView.start()
        }
      }
    }

    val filter = IntentFilter()
    filter.addAction("android.net.conn.CONNECTIVITY_CHANGE")
    context.registerReceiver(broadcastReceiver, filter)
  }
}