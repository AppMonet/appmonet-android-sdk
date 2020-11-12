package com.monet.bidder.adview

import android.annotation.SuppressLint
import com.monet.BidResponse
import android.content.Context
import android.webkit.ValueCallback
import androidx.annotation.VisibleForTesting
import com.monet.bidder.*
import com.monet.bidder.Constants.JSMethods.HELPER_RESPOND
import com.monet.bidder.auction.AuctionManagerCallback
import com.monet.threading.BackgroundThread
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import com.monet.AdServerWrapper
import com.monet.threading.UIThread

/**
 * The [AdViewPoolManager] class is responsible of managing the different [AdView]
 * instances that are loaded in memory.
 */
class AdViewPoolManager : Subscriber, AdViewPoolManagerCallback {
  private val auctionManagerCallback: AuctionManagerCallback
  private val adViewManagerCollection: MutableMap<String, AdViewManager>
  private val backgroundThread: BackgroundThread
  private val context: Context
  private val pubSubService: PubSubService
  private val adViewRefCount: MutableMap<String, Int>
  private val adViewsByContext: MutableMap<String, MutableList<AdViewManager>>

  private val adViewsReadyState: MutableMap<String, Boolean>
  private val sLogger = Logger("PoolManager")
  private val messageHandlers: MutableMap<String, List<ValueCallback<String>>>
  private val uiThread: UIThread
  private val adServerWrapper: AdServerWrapper
  private val appMonetContext: AppMonetContext

  internal constructor(
    adServerWrapper: AdServerWrapper,
    appMonetContext: AppMonetContext,
    auctionManagerCallback: AuctionManagerCallback,
    context: Context,
    pubSubService: PubSubService,
    backgroundThread: BackgroundThread,
    uiThread: UIThread
  ) {
    this.adServerWrapper = adServerWrapper
    this.adViewsByContext = ConcurrentHashMap()
    this.adViewsReadyState = ConcurrentHashMap()
    this.adViewManagerCollection = ConcurrentHashMap()
    this.adViewRefCount = ConcurrentHashMap()
    this.appMonetContext = appMonetContext
    this.auctionManagerCallback = auctionManagerCallback
    this.backgroundThread = backgroundThread
    this.context = context
    this.messageHandlers = ConcurrentHashMap()
    this.uiThread = uiThread
    pubSubService.addSubscriber(Constants.PubSub.Topics.BIDS_CLEANED_UP_TOPIC, this)
    pubSubService.addSubscriber(Constants.PubSub.Topics.BID_ADDED_TOPIC, this)
    pubSubService.addSubscriber(Constants.PubSub.Topics.BID_INVALIDATED_TOPIC, this)
    this.pubSubService = pubSubService
  }

  @VisibleForTesting
  internal constructor(
    adServerWrapper: AdServerWrapper,
    adViewsByContext: MutableMap<String, MutableList<AdViewManager>>,
    adViewsReadyState: MutableMap<String, Boolean>,
    adViewManagerCollection: MutableMap<String, AdViewManager>,
    adViewRefCount: MutableMap<String, Int>,
    appMonetContext: AppMonetContext,
    auctionManagerCallback: AuctionManagerCallback,
    context: Context,
    messageHandlers: MutableMap<String, List<ValueCallback<String>>>,
    pubSubService: PubSubService,
    backgroundThread: BackgroundThread,
    uiThread: UIThread
  ) {
    this.adServerWrapper = adServerWrapper
    this.adViewsByContext = adViewsByContext
    this.adViewsReadyState = adViewsReadyState
    this.adViewManagerCollection = adViewManagerCollection
    this.adViewRefCount = adViewRefCount
    this.appMonetContext = appMonetContext
    this.auctionManagerCallback = auctionManagerCallback
    this.backgroundThread = backgroundThread
    this.context = context
    this.messageHandlers = messageHandlers
    this.pubSubService = pubSubService
    this.uiThread = uiThread
  }

  /**
   * This method retrieves the reference bid count associated to the given webview UUID.
   *
   * @param wvUUID The webview UUID to find the reference count for.
   * @return The number of bid references associated to the given UUID.
   */
  override fun getReferenceCount(wvUUID: String?): Int {
    val refCount = adViewRefCount[wvUUID]
    return refCount ?: 0
  }

  override fun adViewCreated(
    uuid: String,
    adViewContext: AdViewContext
  ) {
    auctionManagerCallback.helperCreated(uuid, adViewContext.toJson())
  }

  override fun adViewLoaded(uuid: String) {
    auctionManagerCallback.helperLoaded(uuid)
  }

  override fun adViewResponse(vararg args: String) {
    auctionManagerCallback.executeJs(HELPER_RESPOND, *args)
  }

  /**
   * This method checks if the provided [AdView] has a webview UUID, if the pool manager
   * ad view collection still has a reference to the [AdView], and if there are any bid
   * references associated to the webview UUID.
   *
   * @param adView The Ad view we want to check if it can be released.
   * @return A boolean value which determines if an [AdView] can be released.
   */
  override fun canReleaseAdViewManager(adViewManager: AdViewManager): Boolean {
    val wvUUID = adViewManager.uuid
    if (!adViewManagerCollection.containsKey(wvUUID)) {
      // for some reason it's not in the pool
      // we can release it
      sLogger.info("$wvUUID is not in the collection?!")
      return true
    }
    val refs = adViewRefCount[wvUUID]
    sLogger.info(wvUUID + " ref @" + (refs ?: 0))
    return refs == null || refs <= 0
  }

  override fun getSdkConfigurations(): SdkConfigurations {
    return auctionManagerCallback.getSdkConfigurations()
  }

  override fun impressionEnded() {
    auctionManagerCallback.executeJs(
        Constants.JSMethods.IMPRESSION_ENDED, "'ended'"
    ) // indicate that imp is over
  }

  /**
   * Request an AdView based on a BidResponse. This generates the corresponding
   * AdViewContext from the BidResponse in order to match up the Bid to the correct MonetAdView
   *
   * @param bid a BidResponse to be rendered into a MonetAdView
   * @return the MonetAdView instance
   */
  override fun request(bid: BidResponse): AdViewManager? {
    return if (bid.nativeRender && adViewManagerCollection.containsKey(bid.wvUUID)) {
      adViewManagerCollection[bid.wvUUID]
    } else request(AdViewContext(bid))
  }

  /**
   * If the webview is in a rendered state,
   * we can expire all of the bids in the webView
   * and set it's ref count to 0, so when it's finished
   * it will be destroyed
   *
   * @param wvUUID the webView's UUID
   * @return if the destroy was requested correctly
   */
  override fun requestDestroy(wvUUID: String): Boolean {
    val adview = adViewManagerCollection[wvUUID]
    if (adview == null) {
      sLogger.debug("requested helper not present: $wvUUID")
      return false
    }
    if (adview.state == AdViewManager.AdViewState.AD_LOADING) {
      sLogger.debug("adView is in loading state. Can be removed now")
      return remove(adview.uuid, true)
    }

    // otherwise, expire all bids for this adView
    auctionManagerCallback.invalidateBidsForAdView(wvUUID)
    val refCount = getReferenceCount(wvUUID)
    if (refCount > 0) {
      sLogger.warn("request failed; still have: $refCount references to view")
      return false
    }
    return true
  }

  /**
   * Remove a MonetAdView based on it's UUID
   * see {remove(MonetAdView, boolean destroyWv, boolean force)}
   *
   * @param uuid    the UUID of the MonetAdView
   * @param destroy should the AdView also be destroyed
   * @return if the MonetADView was successfully removed
   */
  override fun remove(
    uuid: String,
    destroy: Boolean
  ): Boolean {
    return remove(adViewManagerCollection[uuid], destroy, true) // the master can always release
  }

  override fun onBroadcast(subscriberMessages: MonetPubSubMessage?) {
    when (subscriberMessages?.topic) {
      Constants.PubSub.Topics.BIDS_CLEANED_UP_TOPIC -> syncWithBidManager(
          subscriberMessages.payload as Map<String, List<String>>
      )
      Constants.PubSub.Topics.BID_ADDED_TOPIC -> addRef(subscriberMessages.payload as String)
      Constants.PubSub.Topics.BID_INVALIDATED_TOPIC -> {
        val messagePayload = subscriberMessages.payload as Map<String, Any>
        val bidResponse = messagePayload[Constants.PubSub.BID_RESPONSE_KEY] as BidResponse
        val removeCreative = messagePayload[Constants.PubSub.REMOVE_CREATIVE_KEY] as Boolean
        sLogger.debug(
            "removing reference to native bid: @(" + getRenderCount(bidResponse.wvUUID) + ")"
        )
        removeBid(bidResponse, removeCreative)
      }
    }
  }

  /**
   * This method updates the reference count of how many bids are associated to a particular
   * webview UUID.
   *
   * @param wvUUID   The UUID associated to a webview.
   * @param refCount The number of bid references.
   */
  fun updateRefCount(
    wvUUID: String,
    refCount: Int
  ) {
    sLogger.debug("updating ref count for $wvUUID")
    adViewRefCount[wvUUID] = refCount
  }

  fun getAdViewByUuid(wvUUID: String): AdViewManager? {
    return adViewManagerCollection[wvUUID]
  }

  fun executeInContext(
    wvUUID: String,
    message: String?
  ): Boolean {
    val adViewManager = adViewManagerCollection[wvUUID] ?: return false
    adViewManager.executeJs("__a", message!!)
    return true
  }

  /**
   * This method adds a reference count to a webview's UUID key.
   *
   * @param wvUUID The webview UUID to add a reference to.
   */
  fun addRef(wvUUID: String) {
    val count = adViewRefCount[wvUUID]
    adViewRefCount[wvUUID] = if (count == null) 1 else count + 1
  }

  /**
   * This method retrieves the state of a particular webview using the given UUID.
   *
   * @param wvUUID The webview UUID to retrieve state for.
   * @return The [AdView.AdViewState] state string of the particular webview UUID reference.
   */
  fun getState(wvUUID: String): String {
    adViewManagerCollection[wvUUID]?.let {
      return it.state.toString()
    }
    return AdViewManager.AdViewState.NOT_FOUND.toString()
  }

  /**
   * This method removes a bid from the reference counter and if remove cache is true we also
   * invalidate the bid id.
   *
   * @param bid          The [BidResponse] we want to remove.
   * @param removeCached Boolean indicating if we want to invalidate the bid completely
   * (at JS level)
   */
  fun removeBid(
    bid: BidResponse,
    removeCached: Boolean
  ) {
    val adViewManager = adViewManagerCollection[bid.wvUUID]
    removeRef(bid.wvUUID)
    if (adViewManager != null && removeCached) {
      adViewManager.markBidInvalid(bid.id)
    }
  }

  /**
   * This method decreases the bid reference counter associated to the give webview UUID.
   *
   * @param wvUUID The UUID to remove a bid reference from.
   */
  fun removeRef(wvUUID: String) {
    val count = adViewRefCount[wvUUID]
    val updated = if (count == null) 0 else count - 1
    adViewRefCount[wvUUID] = updated
    if (updated <= 0) {
      sLogger.debug("ref count <= 0; can be removed")
    }
  }

  @SuppressLint("DefaultLocale")
  fun logState() {
    sLogger.debug("[Pool State Dump]")
    for ((key, value) in adViewRefCount) {
      var state = getState(key)
      state = state ?: "UNKNOWN"
      sLogger.debug(
          String.format(
              "\t%s => %d, %s", key, value, state
          )
      )
    }
    sLogger.debug("[End Pool State Dump]")
  }

  /**
   * This method checks if the [AdViewPoolManager] still contains the given webview UUID
   * reference. If the adview associated to the UUID is null then a cleanup is triggered.
   *
   * @param wvUUID The UUID to check if it is still associated in the [AdViewPoolManager].
   * @return Boolean value that says if the webview UUID is still referenced.
   */
  fun containsView(wvUUID: String): Boolean {
    val adView = adViewManagerCollection[wvUUID] // is it still in there??
    if (adViewManagerCollection.containsKey(wvUUID) && adView == null) {
      sLogger.warn("collection contains webView but webView is null. Cleaning reference")
      cleanUpOrphanReference(wvUUID)
    }
    return adView != null
  }

  /**
   * This method returns the number of references associated with a give webview UUID.
   *
   * @param wvUUID The UUID to get the total count of bid references.
   * @return The number of bid references associated with a given UUID.
   */
  fun getRenderCount(wvUUID: String): Int {
    val adView = adViewManagerCollection[wvUUID]
    return adView?.renderCount ?: 0
  }

  fun getUrl(wvUUID: String): String {
    val adViewManager = adViewManagerCollection[wvUUID]
    return adViewManager?.adViewUrl ?: ""
  }

  fun getNetworkCount(wvUUID: String): Int {
    val adView = adViewManagerCollection[wvUUID]
    return adView?.networkRequestCount ?: 0
  }

  /**
   * This method retrieves the timestamp when an [AdView] associated with the provided
   * webview UUID was created.
   *
   * @param wvUUID The UUID of the [AdView] to get the timestamp when it was created.
   * @return The time when an [AdView] was created.
   */
  fun getCreatedAt(wvUUID: String): Long {
    val adView = adViewManagerCollection[wvUUID]
    return adView?.createdAt ?: 0
  }

  /**
   * This method will update the adview reference count to match the size maintained by the
   * [BidManager]
   *
   * @param bidIdsByAdview This Map contains the bid Ids by for each adview. This value is used to
   * sync up the adview reference count.
   */
  private fun syncWithBidManager(bidIdsByAdview: Map<String, List<String>>) {
    for ((key, bidIds) in bidIdsByAdview) {
      if (bidIds.isEmpty()) {
        continue
      }
      val refCount = getRenderCount(key)
      if (refCount != bidIds.size) {
        sLogger.warn(
            "refcount mismatch. Updating ref count in adView: refCount=" + refCount + " / bidCount=" + bidIds.size
        )
        updateRefCount(key, bidIds.size)
      }
    }
  }

  /**
   * Check to see if the WebView has either too many references, or has been rendered too many times
   * (e.g. is eligible for removal).
   *
   * @param references  the number of bids currently cached in the view
   * @param renderCount the number of times that ads have been rendered in this view
   * @param force       causes it to always return true
   * @return whether or not the view can be removed
   */
  private fun canPerformRemove(
    references: Int?,
    renderCount: Int,
    force: Boolean
  ): Boolean {
    return if (force || references == null) {
      true
    } else renderCount > MAX_RENDER_THRESHOLD || references < REF_MIN_THRESHOLD
  }

  /**
   * Remove a MonetAdView from the pool
   *
   * @param adViewManager       the adView instance to remove
   * @param destroyWV    indicates whether the underlying instance should also be destroyed
   * @param forceDestroy force through safety checks (e.g. if bids are cached/ready in the specific view)
   * @return whether or not the MonetAdView was removed
   */
  override fun remove(
    adViewManager: AdViewManager?,
    destroyWV: Boolean,
    forceDestroy: Boolean
  ): Boolean {
    if (adViewManager == null) {
      return false
    }
    if (adViewManager.state == AdViewManager.AdViewState.AD_RENDERED && !forceDestroy) {
      sLogger.warn("attempt to remove webView in rendered state")
      return false
    }
    val wvUUID = adViewManager.uuid
    if (!adViewManagerCollection.containsKey(wvUUID)) {
      return false // it wasn't in here
    }
    val references = adViewRefCount[wvUUID]
    if (!canPerformRemove(references, adViewManager.renderCount, forceDestroy)) {
      sLogger.warn("attempt to remove webView with references")
      return false
    }
    val hash = adViewManager.hash
    val adViews = adViewsByContext[hash]
    if (adViews != null && adViews.contains(adViewManager)) {
      adViews.remove(adViewManager)
    } else {
      sLogger.warn("could not find view in context list. Invalid state for removal!")
    }

    // indicate that we're about to destroy this webView
    // we can do this by sending a 'destroy' message to all of the handlers
//        emit(wvUUID, HELPER_DESTROY_MESSAGE)
    auctionManagerCallback.helperDestroy(wvUUID)
    cleanUpOrphanReference(wvUUID)
    pubSubService.addMessageToQueue(
        MonetPubSubMessage(Constants.PubSub.Topics.AD_VIEW_REMOVED_TOPIC, null)
    )
    pubSubService.broadcast()
    if (destroyWV) {
      try {
        adViewManager.destroyRaw()
      } catch (e: Exception) {
        // do nothing
      }
    }
    return true
  }

  /**
   * Remove any references to the given WebView. This will remove it from the pool.
   *
   * @param wvUUID The UUID to remove references.
   */
  private fun cleanUpOrphanReference(wvUUID: String) {
    try {
      // find it in the pool collection & remove it there
      messageHandlers.remove(wvUUID)
      adViewManagerCollection.remove(wvUUID)
      adViewsReadyState.remove(wvUUID)
      adViewRefCount.remove(wvUUID)
    } catch (e: Exception) {
      sLogger.error("failed to remove orphan", e.message)
    }
  }

  /**
   * Find or create a webView for the given adViewContext. Most of the logic
   * around creating new contexts/managing contexts is in JavaScript.
   *
   * @param adViewContext The AdViewContext which describes the new WebView's environment
   * @return The created [AdView] (WebView)
   */
  internal fun request(adViewContext: AdViewContext): AdViewManager {
    //todo - revisit this.
    sLogger.debug("requesting adView with adViewContext")
    var adViews = adViewsByContext[adViewContext.toHash()]
    if (adViews == null) {
      adViews = ArrayList()
    }

    // find the first one that isn't already rendering
    for (adView in adViews) {
      if (adView.state != AdViewManager.AdViewState.AD_RENDERED) {
        return adView
      }
    }

    // release is handled by the bid manager;
    // when a webView has no valid bids in it,
    // it is destroyed
    sLogger.debug(
        "building AdView helper with adViewContext (pre-caching initiated)\n\t",
        adViewContext.toHash()
    )
    val found = AdViewManager(
        adServerWrapper, adViewContext, this,
        appMonetContext, auctionManagerCallback, backgroundThread, context, pubSubService,
        uiThread
    )
    // we created a new one, so add it into that list
    adViews.add(found)
    adViewManagerCollection[found.uuid] = found
    adViewsByContext[adViewContext.toHash()] = adViews
    return found
  }

  companion object {
    private const val REF_MIN_THRESHOLD = 3 // don't delete more than 3 ads cached
    private const val MAX_RENDER_THRESHOLD = 10 // don't render more than 10 times
  }
}