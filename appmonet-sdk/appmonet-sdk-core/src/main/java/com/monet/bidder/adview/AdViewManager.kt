package com.monet.bidder.adview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Message
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewParent
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebView.WebViewTransport
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import com.monet.bidder.AdServerBannerListener
import com.monet.bidder.AdServerWrapper
import com.monet.bidder.AdSize
import com.monet.bidder.AppMonetContext
import com.monet.bidder.AppMonetViewLayout
import com.monet.bidder.Constants
import com.monet.bidder.Constants.Configurations.REDIRECT_URL
import com.monet.bidder.Constants.JSMethods.IMPRESSION_ENDED
import com.monet.bidder.HttpUtil
import com.monet.bidder.Icons
import com.monet.bidder.Logger
import com.monet.bidder.MonetPubSubMessage
import com.monet.bidder.PubSubService
import com.monet.bidder.RenderingUtils
import com.monet.bidder.WebViewUtils
import com.monet.bidder.WebViewUtils.buildResponse
import com.monet.bidder.WebViewUtils.looseUrlMatch
import com.monet.bidder.WebViewUtils.quote
import com.monet.bidder.auction.AuctionManagerCallback
import com.monet.bidder.bid.BidResponse
import com.monet.bidder.bid.Pixel
import com.monet.bidder.callbacks.ReadyCallbackManager
import com.monet.bidder.threading.BackgroundThread
import com.monet.bidder.threading.InternalRunnable
import com.monet.bidder.threading.UIThread
import org.json.JSONObject
import java.net.URLEncoder
import java.util.HashMap
import java.util.UUID

class AdViewManager : AdViewManagerCallback {
  private val pubSubService: PubSubService
  private val adViewHTML: String
  private val adSize: AdSize
  private val adServerWrapper: AdServerWrapper
  private val adViewClient: AdViewClient
  private val adViewContext: AdViewContext
  private val adViewPoolManagerCallback: AdViewPoolManagerCallback
  private val adViewReadyCallback: ReadyCallbackManager<AdView>
  private val appMonetContext: AppMonetContext
  private val auctionManagerCallback: AuctionManagerCallback
  private val backgroundThread: BackgroundThread
  private val context: Context
  private val injectionDelay: Int
  private var isScrollEnabled: Boolean = false
  override var isBrowserOpening: Boolean = false
  private val uiThread: UIThread

  private var adServerListener: AdServerBannerListener? = null
  private var mHasCalledFinishLoad = false
  private var hasAdVieTouchStarted = false
  private var hasSeenV2VastEvent = false

  private var finishLoadChecker: Runnable? = null
  private var interceptor: WebView? = null
  override var urlOpeningMethod: String = "browser"
  override var wasAdViewClicked = false

  @JvmField
  internal val adView: AdView

  @JvmField
  val adViewUrl: String
  val containerView: AppMonetViewLayout
  val createdAt = System.currentTimeMillis()
  val hash: String
  var isAdRefreshed = false
  override var isLoaded = false

  @JvmField
  var renderCount = 0
  var shouldAdRefresh = false
  var state: AdViewState
  override val uuid: String

  private var hasRenderedSufficiently = false

  override var networkRequestCount = 0

  override fun ajax(request: String): String {
    return HttpUtil.makeRequest(adView, request)
  }

  internal constructor(
    adServerWrapper: AdServerWrapper,
    adViewContext: AdViewContext,
    adViewPoolManagerCallback: AdViewPoolManagerCallback,
    appMonetContext: AppMonetContext,
    auctionManagerCallback: AuctionManagerCallback,
    backgroundThread: BackgroundThread,
    context: Context,
    pubSubService: PubSubService,
    uiThread: UIThread
  ) {
    this.adSize = AdSize.from(adViewContext.width, adViewContext.height, adServerWrapper)
    this.adServerWrapper = adServerWrapper
    this.adViewHTML =
      "${ADVIEW_BASE_HTML_HEAD}${appMonetContext.applicationId}\">${ADVIEW_BASE_HTML_FOOT}"
    this.adViewClient = AdViewClient(this)
    this.adViewContext = adViewContext
    this.adViewPoolManagerCallback = adViewPoolManagerCallback
    this.adViewReadyCallback = ReadyCallbackManager()
    this.appMonetContext = appMonetContext
    this.auctionManagerCallback = auctionManagerCallback
    this.backgroundThread = backgroundThread
    this.context = context
    this.injectionDelay = adViewPoolManagerCallback.getSdkConfigurations()
        .getInt(Constants.Configurations.INJECTION_DELAY)
    this.state = AdViewState.AD_LOADING
    this.uiThread = uiThread
    this.adViewUrl = adViewContext.url
    this.uuid = UUID.randomUUID().toString()
    this.hash = adViewContext.toHash()
    this.pubSubService = pubSubService
    this.adView = this.buildView(context, adViewContext)
    this.containerView = buildContainerView(adSize)
  }

  @VisibleForTesting
  internal constructor(
    adSize: AdSize,
    adServerWrapper: AdServerWrapper,
    adView: AdView,
    adViewClient: AdViewClient,
    adViewContext: AdViewContext,
    adViewHtml: String,
    adViewPoolManagerCallback: AdViewPoolManagerCallback,
    adViewReadyCallback: ReadyCallbackManager<AdView>,
    adViewState: AdViewState,
    appMonetContext: AppMonetContext,
    auctionManagerCallback: AuctionManagerCallback,
    backgroundThread: BackgroundThread,
    containerView: AppMonetViewLayout,
    context: Context,
    injectionDelay: Int,
    pubSubService: PubSubService,
    uiThread: UIThread,
    uuid: String
  ) {
    this.adSize = adSize
    this.adServerWrapper = adServerWrapper
    this.adView = adView
    this.adViewClient = adViewClient
    this.adViewContext = adViewContext
    this.adViewHTML = adViewHtml
    this.adViewPoolManagerCallback = adViewPoolManagerCallback
    this.adViewReadyCallback = adViewReadyCallback
    this.auctionManagerCallback = auctionManagerCallback
    this.backgroundThread = backgroundThread
    this.containerView = containerView
    this.context = context
    this.appMonetContext = appMonetContext
    this.injectionDelay = injectionDelay
    this.state = adViewState
    this.uiThread = uiThread
    this.adViewUrl = adViewContext.url
    this.hash = adViewContext.toHash()
    this.uuid = uuid
    this.pubSubService = pubSubService
  }

  override val adViewEnvironment: String
    get() {
      val parent = containerView.parent
      return if (parent != null) {
        "LOADING_ENV"
      } else "RENDER_ENV"
    }

  override val adViewVisibility: String
    get() {
      when (adView.visibility) {
        VISIBLE -> return "visible"
        INVISIBLE -> return "invisible"
        GONE -> return "gone"
      }
      return "unknown"
    }

  override var bid: BidResponse? = null
  var bidForTracking: BidResponse? = null

  override fun executeJs(
    method: String,
    vararg args: String
  ) {
    adViewReadyCallback.onReady { webView ->
      webView.executeJs(method, *args)
    }
  }

  override fun getBooleanValue(key: String): String {
    val field = RenderingUtils.getField(adView, key, Boolean::class.javaPrimitiveType)
        ?: return "null"

    return try {
      (field.getBoolean(adView)).toString()
    } catch (e: IllegalAccessException) {
      "null"
    }
  }

  override fun ready() {
    sLogger.debug("adView sdk: mark ready")
    isLoaded = true
    adViewReadyCallback.executeReady(adView)
  }

  override fun setBackgroundColor(color: String) {
    uiThread.run(object : InternalRunnable() {
      override fun runInternal() {
        adView.setBackgroundColor(Color.parseColor(color))
      }

      override fun catchException(e: java.lang.Exception?) {
        sLogger.warn("SBC: ", e!!.message)
      }
    })
  }

  override fun setBooleanValue(
    key: String,
    value: String
  ): String {
    val bool = value.toBoolean()
    return try {
      val retVal = RenderingUtils.setField(
          adView, key, Boolean::class.javaPrimitiveType, bool
      )
      if (retVal != null) value else "null"
    } catch (e: java.lang.Exception) {
      "null"
    }
  }

  override fun destroy() {
    adView.destroy()
    backgroundThread.execute(object : InternalRunnable() {
      override fun runInternal() {
        adViewPoolManagerCallback.remove(this@AdViewManager.uuid, false)
      }

      override fun catchException(e: java.lang.Exception?) {}
    })
  }

  override fun enableThirdPartyCookies(enabled: Boolean) {
    uiThread.run(object : InternalRunnable() {
      override fun runInternal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          CookieManager.getInstance().setAcceptThirdPartyCookies(adView, enabled)
        }
      }

      override fun catchException(e: Exception?) {
        sLogger.warn("sTPC err: ", e!!.message)
      }
    })
  }

  override fun checkOverride(
    view: WebView,
    uri: Uri
  ): Boolean {
    if (adServerListener == null) return false
    if (uri.scheme == MONET_SCHEME) {
      val url = uri.toString()
      if (url.contains("finishLoad")) {
        if (bidForTracking == null) return false
        bidForTracking?.let {
          if (!it.nativeRender) {
            callFinishLoad(it)
          }
        }
      } else if (url.contains("failLoad")) {
        if (bid == null) return false
        bid?.let {
          if (!it.nativeRender) {
            // detach ourselves since we don't have the ad anyway
            if (!mHasCalledFinishLoad) {
              // this will end up calling error
              adServerListener?.onAdError(AdServerBannerListener.ErrorCode.NO_FILL)
            } else if (!hasRenderedSufficiently) {
              sLogger.warn("attempt to call failLoad after finishLoad")
            }
          }
        }
      }
      return true
    }
    if (uri.scheme == Constants.MARKET_SCHEME) {
      return loadAppStoreUrl(view, uri)
    }
    if (wasAdViewClicked) {
      handleAdInteraction(view, uri.toString())
      return true
    }
    // default
    return false
  }

  override fun handleAdInteraction(
    view: WebView,
    url: String
  ) {
    // we are in loading mode
    if (adServerListener == null || bid == null) {
      return
    }

    if (url == this.adViewUrl) {
      return
    }

    // helps us track everything
    executeJs(
        Constants.JSMethods.NAVIGATION_START,
        WebViewUtils.quote(url)
    )
    // if the url is the ctu, then we open the browser
    if (isSaneUrl(url)) {
      if (wasAdViewClicked
          && !isBrowserOpening
      ) {
        view.stopLoading()
        isBrowserOpening = true // only call this once
        openUrlInBrowser(processClickUrl(url))
      } else {
        sLogger.debug(
            "attempt at redirect without user click. " +
                "ignoring. redirect to: " + url
        )

        // we should probably prevent the rediirect here...
        view.stopLoading()
      }
    } else if (url.indexOf(Constants.MARKET_SCHEME) == 0) {
      view.stopLoading()
      loadAppStoreUrl(view, Uri.parse(url))
    }
  }

  override fun interceptWindowOpen(
    webView: WebView,
    resultMsg: Message
  ) {

    // create an interception webView that has the correct client
    val intercept = getInterceptor() ?: return

    val transport = resultMsg.obj as WebViewTransport
    transport.webView = intercept
    resultMsg.sendToTarget()
  }

  override fun isAdViewAttachedToLayout(): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      return if (containerView.isAttachedToWindow) "attached_window" else "detached_window"
    }
    when (adView.windowVisibility) {
      GONE -> return "window_gone"
      INVISIBLE -> return "window_invisible"
      VISIBLE -> return "window_visible"
    }
    return "unknown"
  }

  override fun markBidRendered(bidId: String): String {
    sLogger.debug("marking bid ", bidId, " as rendered. Removing from BidManager")
    val bid = auctionManagerCallback.removeBid(bidId)

    if (bid != null) {
      sLogger.info("setting new bid in render (pod render)", bid.toString())
      bidForTracking = bid
    }

    return "true"
  }

  override fun nativePlacement(
    key: String,
    value: String
  ) {
    val payload: MutableMap<String, String> = HashMap()
    payload[key] = value
    pubSubService.addMessageToQueue(
        MonetPubSubMessage(
            Constants.PubSub.Topics.NATIVE_PLACEMENT_TOPIC, payload
        )
    )
    pubSubService.broadcast()
  }

  override fun onAdViewTouchEvent(event: MotionEvent): Boolean {
    when (event.action) {
      MotionEvent.ACTION_DOWN -> hasAdVieTouchStarted = true
      MotionEvent.ACTION_UP -> if (hasAdVieTouchStarted) {
        triggerClick()
        hasAdVieTouchStarted = false
      }
      MotionEvent.ACTION_MOVE -> {
        if (!isScrollEnabled) {
          return true // don't scroll
        }
      }
    }
    return false
  }

  /**
   * Load a market:// URL, ideally in the play store
   *
   * @param webView the webView requesting load of the URL
   * @param uri     the market:// url to load
   * @return if the URL was loaded in the play store (vs. in the browser)
   */
  override fun loadAppStoreUrl(
    webView: WebView,
    uri: Uri
  ): Boolean {
    if (!loadWithApplicationScheme(webView.context, uri)) {
      webView.stopLoading()
      webView.loadUrl(
          "http://play.google.com/store/apps/" + uri.host + "?" + uri.query
      )
      return false
    }
    return true
  }

  /**
   * Indicate that an impression should be counted (e.g. that the ad has actually rendered).
   * This fires an impression pixel in our system and calls the listener, which indicates the
   * impression to the MoPub SDK (and causes the adView to be added to the MoPubView's hierarchy)
   */
  override fun callFinishLoad(bidToBeTracked: BidResponse) {
    if (!mHasCalledFinishLoad) {
      firePixel(bidToBeTracked.renderPixel, Pixel.Events.IMPRESSION)
    }
    mHasCalledFinishLoad = true
    sLogger.info("finishLoad called. Impression loaded")
    if (adServerListener == null) {
      sLogger.warn("impression available while in unavailable state. Stopping")
      return
    }
    uiThread.run(object : InternalRunnable() {
      override fun runInternal() {
        containerView.activateRefresh(bidToBeTracked, adServerListener)
        adServerListener?.onAdLoaded(containerView)
      }

      override fun catchException(e: java.lang.Exception?) {
        sLogger.error("Failed to call onAdLoaded: " + e!!.localizedMessage)
        adServerListener?.onAdError(AdServerBannerListener.ErrorCode.INTERNAL_ERROR)
      }
    })
  }

  override fun openUrl(url: String) {
    // try to open it with the scheme if it's custom..
    val uri = Uri.parse(url)
    if (loadWithApplicationScheme(context, uri)) {
      isBrowserOpening = false
      return
    }
    startBrowser(url)
  }

  override fun openUrlInBrowser(url: String) {
    if (adServerListener == null) return

    // fire the click pixel
    bid?.let {
      sLogger.debug("firing click pixel ", it.clickPixel)
      firePixel(it.clickPixel)
      executeJs(Constants.JSMethods.CLICK, quote(url))
    }

    openUrl(url)
    sLogger.info("opening landing page in browser", url)
    adServerListener?.onAdClicked()
    destroyInterceptor()
  }

  override fun shouldInterceptRequest(
    webView: WebView,
    url: String
  ): WebResourceResponse? {
    if (trackVastEvent(url) || url.contains("favicon.ico") || url.contains("mraid.js")) {
      return adViewClient.blankPixelResponse
    }
    try {
      if (looseUrlMatch(url, this.adViewUrl)) {
        sLogger.debug("Loose match found on url: injecting sdk.js", url)
        return buildResponse(adViewHTML)
      }
    } catch (e: Exception) {
      sLogger.error("Failed to forward response:", e.message)
    }
    return null
  }

  override fun shouldInterceptRequest(
    view: WebView,
    request: WebResourceRequest
  ): WebResourceResponse? {
    try {
      networkRequestCount += 1 // track the number of requests
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val url = request.url.toString()
        if (trackVastEvent(url)) {
          return adViewClient.blankPixelResponse
        }
        if (looseUrlMatch(url, this.adViewUrl)) {
          sLogger.debug("Loose match found on url:  injecting sdk.js", url)
          return buildResponse(adViewHTML)
        }
      }
    } catch (e: Exception) {
      // do nothing
      sLogger.error("Error occurred. $e")
    }
    return null
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  override fun shouldOverrideUrlLoading(
    view: WebView,
    request: WebResourceRequest
  ): Boolean {
    return if (handleWebViewClick(view, request)) {
      true
    } else checkOverride(view, request.url)
  }

  private fun startBrowser(url: String): Boolean {
    isBrowserOpening = false
    val intent = Intent(Intent.ACTION_VIEW)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.data = Uri.parse(url)
    try {
      context.startActivity(intent)
    } catch (e: java.lang.Exception) {
      sLogger.warn("Unable to open url: ", e.message)
      return false
    }
    return true
  }

  override fun destroy(invalidate: Boolean) {
    removeFromParent()
    if (invalidate) {
      markImpressionEnded()
    }
    if (!adViewPoolManagerCallback.canReleaseAdViewManager(this)) {
      // this should probably be switched back to
      // hidden state
      if (bid != null) {
        sLogger.info("hiding: " + bid?.url)
      }
      setState(AdViewState.AD_LOADING, null, null)
      return
    }

    // we do this regardless
    sLogger.debug("adView marked for removal")
    clearFinishLoadTimeout()
    if (bid == null || !invalidate || bid?.nativeRender!!) {
      adViewPoolManagerCallback.remove(this, true, true)
      return
    }
    bid?.let {
      destroySafely(it)
    }
  }

  fun destroyRaw() {
    // we need to get onto the UI thread
    // in order to destroy the webview
    uiThread.run(object : InternalRunnable() {
      override fun runInternal() {
        destroyInterceptor()
        destroy()
      }

      override fun catchException(e: Exception?) {}
    })

  }

  fun destroySafely(bid: BidResponse) {
    if (bid.cool <= 0) {
      this.destroy()
      return
    }

    uiThread.runDelayed(object : InternalRunnable() {
      override fun runInternal() {
        this@AdViewManager.destroy()
      }

      override fun catchException(e: java.lang.Exception?) {
        //do nothing
      }
    }, bid.cool.toLong())
  }

  fun inject(html: String?) {
    adViewReadyCallback.onReady { webView ->
      // since we already loaded the base
      sLogger.debug("requesting inject of bid")
      webView.executeJs(
          injectionDelay, Constants.JSMethods.INJECT,
          quote(RenderingUtils.base64Encode(html))
      )
      sLogger.debug("bid injection complete")
    }
  }

  fun inject(bid: BidResponse): Boolean {
    mHasCalledFinishLoad = false
    hasRenderedSufficiently = false
    renderCount += 1
    networkRequestCount = 0 // reset the network request count

    // this will set ourselves back into 'loading'
    // if we fail to attach
    startFinishLoadTimeout()
    if (!bid.url.equals(adViewContext.url, ignoreCase = true)) {
      return false
    }
    val pixelBidWidth = Icons.asIntPixels(bid.width.toFloat(), context)
    val realWidth: Int = adView.width

    // if the AdView is not attached, we don't know it's width already.
    // however, it's will default to the mAdSize, which is why we can compare
    // against that (in dps). If it's been rendered, it's size is known :)
    if (realWidth == 0 && bid.width != adSize.width ||
        realWidth > 0 && pixelBidWidth != realWidth
    ) {
      sLogger.debug("bid should be rendered at a different size: resizing")
      resizeWithBid(bid)
    }
    if (bid.nativeRender) {
      inject(bid.adm)
      return true
    }

    // create a command
    try {
      sLogger.debug("queuing render for adView load")
      val b64Adm = RenderingUtils.base64Encode(bid.adm)
      adViewReadyCallback.onReady { webView ->
        sLogger.debug("adView loaded. Rendering bid")
        webView.executeJs(
            250,
            Constants.JSMethods.RENDER,
            quote(b64Adm),
            bid.width.toString(),
            bid.height.toString()
        )
      }
    } catch (e: Exception) {
      sLogger.error("error executing render command", e.message)
      return false
    }
    return true
  }

  fun load() {
    adView.load()
  }

  /**
   * When we're pre-caching bids in an AdView (nativeRender/video), we might
   * want to invalidate the bid before it's rendered. This lets us indicate to the javascript
   * in that AdView that the bid is invalid and that it's IFrame can be destroyed/cleaned.
   *
   * @param id the BidResponse#id
   */
  fun markBidInvalid(id: String?) {
    executeJs(Constants.JSMethods.MARK_INVALID, WebViewUtils.quote(id))
  }

  /**
   * Switch the state of the AdView. When used for pre-loading bids, it will be
   * in "AD_LOADING". When attached to the MoPubView hierarchy & visible to the user,
   * it will be "AD_RENDERED". Note that a view which is "AD_RENDERED" may still be maintaining
   * references to pre-cached bids, in which case it will return to "AD_LOADING" after the render
   * has finished.
   *
   * @param newState the state we want to switch to
   * @param listener (may be null) the listener to be used for capturing events during rendering
   * @param context  the context the AdView will be moved into
   * @see AdView.destroy
   */
  fun setState(
    newState: AdViewState,
    listener: AdServerBannerListener?,
    context: Context?
  ) {
    adViewReadyCallback.onReady { webView ->

      if (newState !== state && newState === AdViewState.AD_RENDERED && !mHasCalledFinishLoad) {
        sLogger.warn("attempt to set to rendered before finish load called")
      }
      sLogger.debug("changing state to: $newState")
      when (newState) {
        AdViewState.AD_RENDERED -> {
          detachHidden()
          initForCreative(listener)
          state = AdViewState.AD_RENDERED
          webView.executeJs(Constants.JSMethods.STATE_CHANGE, WebViewUtils.quote("RENDERING"))
        }
        AdViewState.AD_LOADING -> {
          clearFinishLoadTimeout()
          adServerListener = null
          state = AdViewState.AD_LOADING
          webView.executeJs(Constants.JSMethods.STATE_CHANGE, WebViewUtils.quote("LOADING"))
        }
        else -> {//do nothing
        }
      }
    }
  }

  private fun buildContainerView(adSize: AdSize): AppMonetViewLayout {
    return AppMonetViewLayout(
        context, adViewPoolManagerCallback, auctionManagerCallback,
        this, adSize
    )
  }

  /**
   * Create a new MonetAdView instance based on an AdViewContext
   *
   * @param adViewContext information about the new WebView's environment
   * @return the created MonetAdView
   */
  private fun buildView(
    context: Context,
    adViewContext: AdViewContext
  ): AdView {
    sLogger.debug("Loading adView with HTML: ", adViewHTML)
    val created = AdView(
        AdSize.from(adViewContext.width, adViewContext.height, adServerWrapper),
        adViewClient,
        AdViewJsInterface(
            adServerListener, this, adViewPoolManagerCallback, auctionManagerCallback
        ),
        context,
        this,
        adViewContext.url,
        adViewContext.userAgent, adViewHTML
    )

    // hook up the webView to the AuctionManager..
    if (!adViewContext.explicitRequest) {
      sLogger.debug("notifying auction manager of new context")
      adViewPoolManagerCallback.adViewCreated(uuid, adViewContext)
    }

    adViewReadyCallback.onReady {
      adViewPoolManagerCallback.adViewLoaded(uuid)
    }
    return created
  }

  private fun clearFinishLoadTimeout() {
    if (finishLoadChecker != null) {
      uiThread.removeCallbacks(finishLoadChecker!!)
    }
  }

  private fun destroyInterceptor() {
    if (interceptor == null) {
      return
    }
    interceptor?.destroy()
    interceptor = null
  }

  fun detachHidden() {
    uiThread.run(object : InternalRunnable() {
      override fun runInternal() {
        setVisibilityState(true)
        removeFromParent()
      }

      override fun catchException(e: java.lang.Exception?) {}
    })
  }

  @SuppressLint("SetJavaScriptEnabled")
  private fun getInterceptor(): WebView? {
    if (adServerListener == null) return null
    if (interceptor != null) {
      return interceptor
    }
    interceptor = WebView(context)
    interceptor?.settings?.javaScriptEnabled = true
    adView.addView(interceptor)
    val client = AdViewClient(this)
    client.setListener(adServerListener)
    interceptor?.webViewClient = client
    return interceptor
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private fun handleWebViewClick(
    view: WebView,
    request: WebResourceRequest
  ): Boolean {
    if (!request.isForMainFrame) {
      return false
    }
    val requestUri = request.url
    if ("monet" == requestUri.scheme || adViewUrl.contains(requestUri.toString())) {
      return false
    }
    sLogger.info("attempt to load a different url in webView frame")
    executeJs(
        Constants.JSMethods.NAVIGATION_START,
        quote(requestUri.toString())
    )
    if (wasAdViewClicked && !isBrowserOpening) {
      sLogger.info("there is a user click - open in Browser")
      openUrlInBrowser(processClickUrl(requestUri.toString()))
    }
    return true
  }

  /**
   * When being rendered, we need to attach the listener & set a click
   * handler for this AdView.
   *
   * @param listener the AdServerBannerListener responsible for propagating events about creative rendering
   */
  private fun initForCreative(listener: AdServerBannerListener?) {
    adServerListener = listener
    adViewClient.setListener(listener)
    adView.setOnLongClickListener {
      triggerClick()
      true
    }
    adView.isLongClickable = false
  }

  private fun isSaneUrl(url: String): Boolean {
    // must be the right protocol
    return url.indexOf("http") == 0
        || url.indexOf("tel") == 0
        || url.indexOf("sms") == 0
  }

  fun removeFromParent() {
    val parentGroup = containerView.parent as ViewGroup?
    parentGroup?.removeView(containerView)
  }

  fun resize(adSize: AdSize?) {
    val layout: FrameLayout.LayoutParams =
      RenderingUtils.getCenterLayoutParams(adView.context, adSize)
    adView.layoutParams = layout
    adView.requestLayout()
  }

  private fun resizeWithBid(bid: BidResponse) {
    resize(AdSize(bid.width, bid.height))
  }

  private fun setVisibilityState(isVisible: Boolean) {
    containerView.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
  }

  private fun startFinishLoadTimeout() {
    // we might not be attached to a view, so
    // run the handler on the main UI thread
    clearFinishLoadTimeout()
    finishLoadChecker = object : InternalRunnable() {
      override fun runInternal() {
        if (adView.isDestroyed) {
          return
        }
        if (state !== AdViewState.AD_RENDERED) {
          return
        }

        // check to see if we are rendered
        // or if we're still attached to the incorrect
        // context :/
        val parent: ViewParent = containerView.parent
        if (parent != null) {
          return
        }
        sLogger.warn("adView failed to attach to the ad container. Triggering failload")
        if (parent == null) {
          sLogger.warn("adView parent is null.")
        }

        // ad error will move us to loading anyway
        adServerListener?.onAdError(AdServerBannerListener.ErrorCode.INTERNAL_ERROR)
      }

      override fun catchException(e: java.lang.Exception?) {
        sLogger.error("failed to check finish load after timeout", e!!.message)
      }
    }
  }

  private fun triggerClick() {
    if (adServerListener == null) return
    sLogger.debug("Native click detected")
    wasAdViewClicked = true
    uiThread.runDelayed(object : InternalRunnable() {
      override fun runInternal() {
        wasAdViewClicked = false
      }

      override fun catchException(e: Exception?) {//do nothing
      }
    }, 3000) // 3s.. very long!
  }

  private fun loadWithApplicationScheme(
    context: Context,
    uri: Uri
  ): Boolean {
    val scheme = uri.scheme
    if (scheme == null || !(scheme == "market" || scheme == "tel" || scheme == "sms")) {
      return false
    }
    try {
      val intent = Intent(Intent.ACTION_VIEW)
      intent.data = uri
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
    } catch (e: Exception) {
      sLogger.warn("failed to load uri: ", e.localizedMessage)
      return false
    }
    return true
  }

  private fun markImpressionEnded() {
    adViewReadyCallback.onReady { webView ->
      webView.executeJs(IMPRESSION_ENDED, "'ended'")
    }
  }

  /**
   * VAST (video) ad events are tracked by pinging a nonstandard scheme+host,
   * "monet://vast". We switch on the specific path in order to fire a pixel
   * stored on the current BidResponse object (mBidForTracking).
   *
   * @param url the full URL to be intercepted/handled by the client
   * @return if the URL is a VAST event/should be handled by this method.
   * @see AdView.MONET_VAST_TRACK
   *
   * @see AdView.mBidForTracking
   *
   * @see BidResponse.firePixel
   */
  private fun trackVastEvent(url: String): Boolean {
    if (!url.startsWith(MONET_VAST_TRACK)) {
      return false
    }
    val match = RenderingUtils.parseVastTracking(url)
    if (!match.matches()) {
      return false
    }
    if (match.bidId != null) {
      hasSeenV2VastEvent = true
    } else if (hasSeenV2VastEvent) {
      return false
    }
    var bidToBeTracked = bid
    if (!match.isForBid(bidToBeTracked)) {
      bidToBeTracked = auctionManagerCallback.getBidById(match.bidId)
    }
    if (bidToBeTracked == null) {
      sLogger.warn("failed to find bid to be logged. Skipping event.")
      return false
    }
    val trackingPixel = bidToBeTracked.renderPixel
    when (match.event) {
      Constants.VASTEvents.START -> if (!hasRenderedSufficiently) {
        callFinishLoad(bidToBeTracked)
      } else {
        sLogger.debug("rendering second impression into slot")
      }
      Constants.VASTEvents.IMPRESSION -> firePixel(trackingPixel, Pixel.Events.VAST_IMPRESSION)
      Constants.VASTEvents.FIRST_QUARTILE -> {
        hasRenderedSufficiently = true // so we don't mind the finish load later
        if (!mHasCalledFinishLoad) {
          sLogger.warn("first quartile called without impression.")
        }
        firePixel(trackingPixel, Pixel.Events.VAST_FIRST_QUARTILE)
      }
      Constants.VASTEvents.MID_POINT -> firePixel(trackingPixel, Pixel.Events.VAST_MIDPOINT)
      Constants.VASTEvents.THIRD_QUARTILE -> firePixel(
          trackingPixel, Pixel.Events.VAST_THIRD_QUARTILE
      )
      Constants.VASTEvents.COMPLETE -> firePixel(trackingPixel, Pixel.Events.VAST_COMPLETE)
      Constants.VASTEvents.ERROR -> firePixel(trackingPixel, Pixel.Events.VAST_ERROR)
      Constants.VASTEvents.FAIL_LOAD -> {
        if (bidToBeTracked !== this.bid) {
          sLogger.debug("failLoad called on different bid from current rendering")
        } else if (!mHasCalledFinishLoad) {
          // detach ourselves since we don't have the ad anyway

          // this will end up calling error
          adServerListener?.onAdError(AdServerBannerListener.ErrorCode.NO_FILL)
        } else if (!hasRenderedSufficiently) {
          sLogger.warn("attempt to call failLoad after finishLoad")
        }
      }
      else -> sLogger.info("logging vast event:", match.event, "for bid:", match.bidId)
    }
    return true
  }

  private fun firePixel(
    trackingPicker: String,
    event: Pixel.Events? = null
  ) {
    if (event != null) {
      Pixel.fire(trackingPicker, event)
    } else {
      Pixel.fire(trackingPicker)
    }
  }

  private fun processClickUrl(url: String): String {
    val redirectUrl = auctionManagerCallback.getSdkConfigurations().getString(REDIRECT_URL)
    if (redirectUrl.isNotEmpty()) {
      val userData = JSONObject().apply {
        putOpt("D", auctionManagerCallback.advertisingId)
        putOpt("b", auctionManagerCallback.getDeviceData().packageInfo?.packageName ?: "")
        putOpt("aid", appMonetContext.applicationId)
        putOpt("ts", System.currentTimeMillis())
      }.toString()
      val encodedData = "&p=${URLEncoder.encode(RenderingUtils.base64Encode(userData), "UTF-8")}"
      return redirectUrl + URLEncoder.encode(url, "UTF-8") + encodedData
    }
    return url
  }

  enum class AdViewState(private val mReadableName: String) {
    AD_LOADING("LOADING"),
    AD_RENDERED("RENDERED"),
    AD_MIXED_USE("MIXED_USE"),
    AD_OPEN("OPEN"),
    NOT_FOUND("NOT_FOUND");

    override fun toString(): String {
      return mReadableName
    }
  }

  companion object {
    private const val MONET_SCHEME = "monet"
    private const val MONET_VAST_TRACK = "$MONET_SCHEME://vast"
    private val ADVIEW_BASE_HTML_HEAD =
      "<html><head><script src=\"${Constants.ADVIEW_JS_URL}aid="
    private const val ADVIEW_BASE_HTML_FOOT = "</script></head></html>"
    private val sLogger = Logger("AdViewManager")
  }
}