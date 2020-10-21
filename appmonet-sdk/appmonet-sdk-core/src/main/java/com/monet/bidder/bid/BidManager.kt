package com.monet.bidder.bid

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import com.monet.BidResponse
import com.monet.bidder.AdServerAdRequest
import com.monet.bidder.AdServerAdView
import com.monet.bidder.Constants.JSMethods
import com.monet.bidder.Constants.PubSub
import com.monet.bidder.Constants.PubSub.Topics
import com.monet.bidder.Logger
import com.monet.bidder.MonetPubSubMessage
import com.monet.bidder.PubSubService
import com.monet.bidder.Subscriber
import com.monet.bidder.WebViewUtils.quote
import com.monet.bidder.adview.AdViewPoolManager
import com.monet.bidder.auction.AuctionManagerCallback
import com.monet.threading.BackgroundThread
import com.monet.threading.ScheduledFutureCall
import org.json.JSONObject
import java.util.ArrayList
import java.util.Comparator
import java.util.PriorityQueue
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by jose on 8/28/17.
 */
class BidManager : Subscriber {
  private val store: MutableMap<String?, PriorityQueue<BidResponse>?>
  private val adUnitNameMapping: MutableMap<String, String?>
  private val bidIdsByAdView: MutableMap<String, MutableList<String?>?>
  private val seenBids: MutableMap<String, String?>
  private val bidsById: MutableMap<String, BidResponse?>
  private var intervalFuture: ScheduledFutureCall? = null
  private val auctionManagerCallback: AuctionManagerCallback
  private val usedBids: MutableMap<String, String?>
  private val pubSubService: PubSubService;
  private val adViewPoolManager: AdViewPoolManager
  private val backgroundThread: BackgroundThread

  internal constructor(
    pubSubService: PubSubService,
    backgroundThread: BackgroundThread,
    adViewPoolManager: AdViewPoolManager,
    auctionManagerCallback: AuctionManagerCallback
  ) {
    this.pubSubService = pubSubService
    store = ConcurrentHashMap()
    adUnitNameMapping = ConcurrentHashMap()
    seenBids = ConcurrentHashMap()
    bidsById = ConcurrentHashMap()
    bidIdsByAdView = ConcurrentHashMap()
    usedBids = ConcurrentHashMap()
    this.adViewPoolManager = adViewPoolManager
    this.backgroundThread = backgroundThread
    this.auctionManagerCallback = auctionManagerCallback
    setupIntervalExecution()
    pubSubService.addSubscriber(Topics.AD_VIEW_REMOVED_TOPIC, this)
  }

  @VisibleForTesting
  internal constructor(
    pubSubService: PubSubService,
    backgroundThread: BackgroundThread,
    adViewPoolManager: AdViewPoolManager,
    auctionManagerCallback: AuctionManagerCallback,
    store: MutableMap<String?, PriorityQueue<BidResponse>?>,
    seenBids: MutableMap<String, String?>,
    adUnitNameMapping: MutableMap<String, String?>,
    bidsById: MutableMap<String, BidResponse?>,
    bidIdsByAdView: MutableMap<String, MutableList<String?>?>,
    usedBids: MutableMap<String, String?>
  ) {
    this.pubSubService = pubSubService
    this.store = store
    this.adUnitNameMapping = adUnitNameMapping
    this.seenBids = seenBids
    this.bidsById = bidsById
    this.bidIdsByAdView = bidIdsByAdView
    this.usedBids = usedBids
    this.backgroundThread = backgroundThread
    this.adViewPoolManager = adViewPoolManager
    this.auctionManagerCallback = auctionManagerCallback
    setupIntervalExecution()
  }

  override fun onBroadcast(subscriberMessages: MonetPubSubMessage?) {
    if (subscriberMessages?.topic == Topics.AD_VIEW_REMOVED_TOPIC) {
      try {
        sLogger.debug("forcing bid clean / destroyed adView")
        cleanBids()
      } catch (e: Exception) {
        sLogger.warn("failed to clean bids proactively.", e.message)
      }
    }
  }

  /**
   * Add a bid to the bidmanager. Bids must be unique on their id (bid.id), or they'll be dropped
   * Bids also need to be valid when they're added to the bidManager.
   *
   * @param bid the BidResponse to add
   */
  fun addBid(bid: BidResponse?) {
    if (bid == null) {
      sLogger.warn("null bid tried add")
      return
    }
    if (!isValid(bid)) {
      sLogger.warn("attempt to add invalid bid", invalidReason(bid))
      return
    }
    var queue = getStoreForAdUnit(bid.adUnitId)
    if (queue == null) {
      queue = newBidResponseQueue()
      putStoreForAdUnit(bid.adUnitId, queue)
    }
    if (seenBids.containsKey(bid.id)) {
      return
    }
    seenBids[bid.id] = bid.id
    bidsById[bid.id] = bid
    sLogger.info("added bid: ", bid.toString())

    // if we're adding it, we need to update the ref count
    // if it's native
    if (bid.nativeRender && !bid.nativeInvalidated) {
      sLogger.debug("adding reference for bid")
      var bidIds = bidIdsByAdView[bid.wvUUID]
      if (bidIds == null) {
        bidIds = ArrayList()
      }
      bidIds.add(bid.id)
      bidIdsByAdView[bid.wvUUID] = bidIds // add it here
      pubSubService.addMessageToQueue(MonetPubSubMessage(Topics.BID_ADDED_TOPIC, bid.wvUUID))
      pubSubService.broadcast()
    }
    queue.add(bid)
  }

  /**
   * Adds a list of bids into our local store.
   */
  fun addBids(bidResponses: List<BidResponse?>) {
    for (bid in bidResponses) {
      try {
        addBid(bid)
      } catch (e: Exception) {
        sLogger.error(String.format("unexpected error syncing bid: %s", e.message))
      }
    }
  }

  /**
   * Clean any invalid bids in the store
   */
  fun cleanBids() {
    for (key in store.keys) {
      try {
        cleanBidsForAdUnit(key)
      } catch (e: Exception) {
        sLogger.error("failed to clean bids for key", e.message)
      }
    }
    sLogger.debug("syncing bidmanager with pool")
    pubSubService.addMessageToQueue(
        MonetPubSubMessage(Topics.BIDS_CLEANED_UP_TOPIC, bidIdsByAdView)
    )
    pubSubService.broadcast()
  }

  /**
   * Check how many bids we have available for the given ad unit. Used by javascript to know when we
   * want to fetch [MonetJsInterface.getAvailableBidCount]
   *
   * @param adUnitId the ad unit to check bid count
   * @return the number of bids available
   */
  fun countBids(adUnitId: String?): Int {
    var count = 0
    adUnitId?.let {
      val adUnitStore = store[resolveAdUnitId(it)]
      if (adUnitStore != null) {
        count = adUnitStore.size
      }
    }
    return count
  }

  /**
   * Disables interval bid cleaner.
   */
  fun disableIntervalCleaner() {
    if (intervalFuture != null) {
      intervalFuture?.cancel()
      intervalFuture = null
    } else {
      sLogger.warn("execution already disabled")
    }
  }

  /**
   * Enables interval cleaner.
   */
  fun enableIntervalCleaner() {
    if (intervalFuture == null) {
      setupIntervalExecution()
    } else {
      sLogger.warn("execution already enabled")
    }
  }

  /**
   * Retrieve bid by id.
   */
  fun getBidById(bidId: String?): BidResponse? {
    return bidsById[bidId]
  }

  /**
   * Retrieve [BidResponse] for a particular ad unit id.
   */
  fun getBidForAdUnit(adUnitId: String?): BidResponse? {
    val store = getStoreForAdUnit(adUnitId) ?: return null
    var bid: BidResponse? = null
    if (!store.isEmpty()) {
      bid = filterBidFromQueue(store)
    }
    return bid
  }

  /**
   * Fetch available bids from the local (e.g. native code) bid manager.
   *
   * @param adView the adView bids are being requested for.
   * @return a list of BidResponses available for the current impression.
   */
  fun getLocalBid(adUnitId: String?): BidResponse? {
    val bid = getBidForAdUnit(adUnitId) ?: return null
    markBidUsed(adUnitId, bid)
    sLogger.debug("Found bid from local store. ${countBids(adUnitId)} bids remaining")
    return bid
  }

  /**
   * Returns a boolean
   */
  fun hasLocalBids(adUnitId: String?): Boolean {
    return countBids(adUnitId) > 0
  }

  /**
   * Indicate to the javascript that the following bids have been used and should not be counted
   * anymore.
   *
   *
   * Note that this method has become less important now that we keep bid count completely in native
   * code @see {BidManager}. We might want to take this out.
   *
   * @param adUnitId the ID of the adUnit (should be equal to bids.get(0).adUnitId)
   * @param bid the bids to be removed
   */
  private fun markBidUsed(
    adUnitId: String?,
    bid: BidResponse
  ) {
    auctionManagerCallback.executeJs(
        JSMethods.BID_USED,
        quote(adUnitId),
        quote(bid.id)
    )
  }

  fun needsNewBids(
    adView: AdServerAdView,
    adRequest: AdServerAdRequest
  ): Boolean {
    val bid = adRequest.bid
    if (!adRequest.hasBid() || bid == null) {
      return true
    }
    val newBid = peekBidForAdUnit(adView.adUnitId)
    if (newBid == null) {
      sLogger.debug("no new bids. Leaving older bids")
      return false
    }
    if (isValid(bid) && newBid.cpm > bid.cpm) {
      sLogger.debug("found newer bid @$${newBid.cpm}. Need new bids")
      return true
    }
    sLogger.debug("found bid, unneeded on request: $newBid")
    sLogger.debug("no newer bids found")
    return false
  }

  private fun newBidResponseQueue(): PriorityQueue<BidResponse> {
    return PriorityQueue(10, BidManagerComparator())
  }

  private fun resolveAdUnitId(adUnitId: String): String? {
    return if (adUnitNameMapping.containsKey(adUnitId)) adUnitNameMapping[adUnitId] else adUnitId
  }

  /**
   * Given an adunit ID, find the correct set of bids that match. In cases where the bids stored are
   * under regular expression keys, this method needs to find the key(s) that match the passed in
   * adunit
   *
   * @param adUnitId the adunit id (also path)
   * @return a map of bids by code (e.g. line item subtype)
   */
  private fun getStoreForAdUnit(adUnitId: String?): PriorityQueue<BidResponse>? {
    return store[adUnitId?.let { resolveAdUnitId(it) }]
  }

  private fun putStoreForAdUnit(
    adUnitId: String?,
    store: PriorityQueue<BidResponse>?
  ) {
    adUnitId?.let {
      this.store[resolveAdUnitId(it)] = store
    }
  }

  /**
   * In some cases, the adUnit name may differ from the adUnit name that we have stored in our
   * configured/ad server. E.g. there are 100s of possible adUnits but we only care about a certain
   * level of specificity. In the javascript engine, we can provide adUnit names with wildcards.
   * These will be matched in JS and the resulting mapping will be passed back here, where we load
   * it into the map.
   *
   *
   * e.g.
   *
   *
   * requested:
   *
   *
   * /my/special/mrect_adunit
   *
   *
   * stored in config:
   *
   *
   * /my/special/ *_adunit
   *
   * @param jsonStr json request passed in from javascript
   * @return whether or not we successfully loaded into bid manager
   */
  fun setAdUnitNames(jsonStr: String?): Boolean {
    try {
      val json = JSONObject(jsonStr)
      val requested = json.getJSONArray("requested")
      val found = json.getJSONArray("found")
      if (requested == null || found == null) {
        return false
      }

      // don't keep going
      if (requested.length() == 0) {
        return true
      }
      var i = 0
      val l = requested.length()
      while (i < l) {
        val req = requested.getString(i)
        val match = found.getString(i)
        if (req != null && match != null) {
          adUnitNameMapping[req] = match
        }
        i++
      }
    } catch (e: Exception) {
      sLogger.warn(
          "error in adUnit name sync: ", e.message, jsonStr
      )
      return false
    }
    return true
  }

  private fun peekBidForAdUnit(adUnitId: String?): BidResponse? {
    val store = getStoreForAdUnit(adUnitId) ?: return null
    var peekedBid: BidResponse? = null
    if (!store.isEmpty()) {
      for (bid in store) {
        if (isValid(bid)) {
          peekedBid = bid
          break
        }
      }
    }
    return peekedBid
  }

  fun invalidate(wvUUID: String) {
    val bidIds: MutableList<String?>? = bidIdsByAdView[wvUUID]
    if (bidIds != null) {
      sLogger.debug("invalidting all for: $wvUUID")
      for (id in bidIds) {
        removeBid(id)
      }
    } else {
      sLogger.debug("Bid Id's not found for $wvUUID")
    }
  }

  private fun invalidateBid(
    bid: BidResponse,
    removeCreative: Boolean
  ) {
    if (needsInvalidation(bid)) {
      val bidIds = bidIdsByAdView[bid.wvUUID]
      bidIds?.remove(bid.id)
      bidIdsByAdView[bid.wvUUID] = bidIds
      val messagePayload = mutableMapOf<String, Any>()
      messagePayload[PubSub.BID_RESPONSE_KEY] = bid
      messagePayload[PubSub.REMOVE_CREATIVE_KEY] = removeCreative
      pubSubService.addMessageToQueue(
          MonetPubSubMessage(Topics.BID_INVALIDATED_TOPIC, messagePayload)
      )
      pubSubService.broadcast()
      markInvalidated(bid)
    }
  }

  private fun filterBidFromQueue(pq: PriorityQueue<BidResponse>): BidResponse? {
    var bid: BidResponse? = pq.poll() ?: return null
    sLogger.debug("found bid @ top of queue: $bid")
    while (!isValid(bid)) {
      sLogger.debug("invalid bid in queue, removing")
      bid = pq.poll()
      if (bid == null) {
        break
      }

      // only invalidating because it's "invalid"...
      // not because we're removing it
      invalidateBid(bid, false)
    }
    // we broke out of that loop
    return bid
  }

  fun peekNextBid(adUnitId: String?): BidResponse? {
    return peekBidForAdUnit(adUnitId)
  }

  /**
   * Remove a bid from the bidManager by its BidId. Note that we do not remove it from seenBids,
   * since the javascript could accidentally send the same bid again
   *
   * @param bidId the UUID of the bid (bid.id)
   * @return the removed bid (if it was removed)
   */
  private fun removeBid(
    bidId: String?,
    destroyCreative: Boolean
  ): BidResponse? {
    sLogger.debug("removing bid ", bidId)
    if (!bidsById.containsKey(bidId)) {
      return null
    }
    val bid = bidsById[bidId] ?: return null
    markUsed(bid) // it needs to be mark used so if it's fetched again it's known as invalid
    val collection = getStoreForAdUnit(bid.adUnitId)
    if (collection != null) {
      sLogger.debug("bid not found in collection", bid.adUnitId)
      collection.remove(bid)
    }
    invalidateBid(bid, destroyCreative)
    return bid
  }

  fun markUsed(bid: BidResponse) {
    usedBids[bid.uuid] = bid.id
  }

  private fun markInvalidated(bid: BidResponse) {
    if (needsInvalidation(bid)) {
      bid.nativeInvalidated = true
    }
  }

  private fun needsInvalidation(bid: BidResponse): Boolean {
    return bid.nativeRender && !bid.nativeInvalidated
  }

  private fun isUsed(bid: BidResponse): Boolean {
    return usedBids.containsKey(bid.uuid)
  }

  fun removeBid(bidId: String?): BidResponse? {
    return removeBid(bidId, true)
  }

  @SuppressLint("DefaultLocale") fun logState() {
    sLogger.debug("[Bid State Dump]")
    for ((key) in store) {
      sLogger.debug(
          String.format("\t%s => %d bids", key, countBids(key))
      )
    }
    sLogger.debug("[End Bid State Dump]")
  }

  /**
   * Remove any invalid bids for the given adunit. If it's being cleaned, invalid most likely means
   * that either the bid expired, or the webview specificied by the bid's wvUUID has been destroyed
   *
   * @param adUnitId the id of the adunit to clean bids for
   * @see BidManager.cleanBids
   */
  private fun cleanBidsForAdUnit(adUnitId: String?) {
    val removedBids = arrayListOf<BidResponse>()
    val queue = getStoreForAdUnit(adUnitId) ?: return
    val cleanedQueue = newBidResponseQueue()
    for (bid in queue) {
      if (!isValid(bid)) {
        removedBids.add(bid)
        sLogger.debug("Removing invalid bid : $bid")
      } else {
        cleanedQueue.add(bid)
      }
    }
    queue.clear()
    // set everything to the new (cleaned) queue
    putStoreForAdUnit(adUnitId, cleanedQueue)
    val messagePayload = mutableMapOf<String, Any>()
    for (removed in removedBids) {
      messagePayload[removed.id] = invalidFlag(removed)
      bidsById.remove(removed.id)
      invalidateBid(removed, true)
    }
    if (messagePayload.isNotEmpty()) {
      pubSubService.addMessageToQueue(
          MonetPubSubMessage(
              Topics.BIDS_INVALIDATED_REASON_TOPIC,
              messagePayload
          )
      )
      pubSubService.broadcast()
    }
  }

  /**
   * The comparator for ordering our PriorityQueue. We want to sort by CPM descending (highest
   * first).
   *
   * @see BidManager.addBid
   */
  private inner class BidManagerComparator : Comparator<BidResponse> {
    override fun compare(
      o1: BidResponse,
      o2: BidResponse
    ): Int {
      return o2.cpm.compareTo(o1.cpm)
    }
  }

  @SuppressLint("DefaultLocale")
  fun invalidReason(bid: BidResponse): String {
    if (isUsed(bid)) {
      return "bid used"
    }
    if (!hasNotExpired(bid)) {
      val ms = ttl(bid)
      return "bid expired - $ms ms old ${String.format("(%dl) -- %dl", ms, bid.expiration)}"
    }
    return if (!renderWebViewExists(bid)) {
      "missing render webView"
    } else "invalid adm - ${bid.adm}"
  }

  private fun invalidFlag(bid: BidResponse): String {
    if (isUsed(bid)) {
      return "USED_BID"
    }
    if (!hasNotExpired(bid)) {
      return "EXPIRED_BID"
    }
    return if (!renderWebViewExists(bid)) {
      "MISSING_WEBVIEW"
    } else "INVALID_ADM"
  }

  private fun ttl(bid: BidResponse): Long {
    val now = System.currentTimeMillis()
    return now - bid.createdAt
  }

  private fun getExpiration(bid: BidResponse): Long {
    //    if (bid.getExpiration() > 0) {
    return bid.expiration
    //    }

    // default to what's set for the bidder
    //    Long expiration = sBidderExpiration.get(bidder);
    //    if (expiration == null) {
    //      expiration = EXPIRES_DIFF;
    //    }
    //
    //    return expiration * 1000;
  }

  private fun hasNotExpired(bid: BidResponse): Boolean {
    return ttl(bid) < getExpiration(bid)
  }

  fun isValid(bid: BidResponse?): Boolean {
    return bid != null && !isUsed(bid) && hasNotExpired(bid)
        && bid.adm.isNotEmpty() && renderWebViewExists(bid)
  }

  private fun setupIntervalExecution() {
    intervalFuture?.cancel()
    intervalFuture = backgroundThread.scheduleAtFixedRate({
      cleanBids()
    }, 10000)
  }

  private fun renderWebViewExists(bid: BidResponse): Boolean {
    return (!bid.nativeRender
        || bid.wvUUID.isEmpty()
        || adViewPoolManager.containsView(bid.wvUUID))
  }

  companion object {
    private val sLogger = Logger("BidManager")
  }
}