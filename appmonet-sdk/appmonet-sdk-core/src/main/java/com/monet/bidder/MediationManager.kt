package com.monet.bidder

import com.monet.AdType
import com.monet.bidder.bid.BidManager
import com.monet.BidResponse
import com.monet.bidder.callbacks.Callback
import com.monet.bidder.threading.InternalRunnable
import java.util.Locale

/**
 * Created by jose on 3/19/18.
 */
class MediationManager(
  private val sdkManager: BaseManager,
  private val bidManager: BidManager
) {
  fun getBidReadyForMediationAsync(
    bid: BidResponse?,
    adUnitId: String,
    adSize: AdSize?,
    adType: AdType,
    floorCpm: Double,
    callback: Callback<BidResponse>
  ) {
    this.getBidReadyForMediationAsync(bid, adUnitId, adSize, adType, floorCpm, callback, null)
  }

  fun getBidReadyForMediationAsync(
    bid: BidResponse?,
    adUnitId: String,
    adSize: AdSize?,
    adType: AdType,
    floorCpm: Double,
    callback: Callback<BidResponse>,
    defaultTimeout: Int?
  ) {
    try {
      callback.onSuccess(getBidReadyForMediation(bid, adUnitId, adSize, adType, floorCpm, false))
    } catch (e: NoBidsFoundException) {
      //todo refactor this.
      val timeout = sdkManager.auctionManager.getSdkConfigurations().getAdUnitTimeout(adUnitId)
      val finalTimeout = if (timeout <= 0 && defaultTimeout != null) defaultTimeout else timeout
      if (finalTimeout <= 0) {
        sdkManager.indicateRequest(adUnitId, adSize, adType, floorCpm)
        callback.onError()
        return
      }
      sdkManager.indicateRequestAsync(
          adUnitId, finalTimeout, adSize, adType, floorCpm
      ) {
        sdkManager.uiThread.run(object : InternalRunnable() {
          override fun runInternal() {
            try {
              callback.onSuccess(
                  getBidReadyForMediation(
                      getBidForMediation(adUnitId, floorCpm),
                      adUnitId, adSize, adType, floorCpm, false
                  )
              )
            } catch (ex: NoBidsFoundException) {
              callback.onError()
            } catch (ex: NullBidException) {
              callback.onError()
            }
          }

          override fun catchException(e1: Exception?) {
            callback.onError()
          }
        })
      }
    } catch (e: NullBidException) {
      val timeout = sdkManager.auctionManager.getSdkConfigurations().getAdUnitTimeout(adUnitId)
      val finalTimeout = if (timeout <= 0 && defaultTimeout != null) defaultTimeout else timeout
      if (finalTimeout <= 0) {
        sdkManager.indicateRequest(adUnitId, adSize, adType, floorCpm)
        callback.onError()
        return
      }
      sdkManager.indicateRequestAsync(
          adUnitId, finalTimeout, adSize, adType, floorCpm
      ) {
        sdkManager.uiThread.run(object : InternalRunnable() {
          override fun runInternal() {
            try {
              callback.onSuccess(
                  getBidReadyForMediation(
                      getBidForMediation(adUnitId, floorCpm),
                      adUnitId, adSize, adType, floorCpm, false
                  )
              )
            } catch (ex: NoBidsFoundException) {
              callback.onError()
            } catch (ex: NullBidException) {
              callback.onError()
            }
          }

          override fun catchException(e1: Exception?) {
            callback.onError()
          }
        })
      }
    }
  }

  @Throws(
      NoBidsFoundException::class, NullBidException::class
  ) fun getBidReadyForMediation(
    bid: BidResponse?,
    adUnitId: String,
    adSize: AdSize?,
    adType: AdType,
    floorCpm: Double,
    indicateRequest: Boolean
  ): BidResponse {
    if (indicateRequest) {
      sdkManager.indicateRequest(adUnitId, adSize, adType, floorCpm)
    }
    if (bid == null || bid.id.isEmpty()) {
      logger.debug("first bid is null/invalid - no fill")
      throw NullBidException()
    }
    return if (!bidManager.isValid(bid)) {
      val nextBid = bidManager.peekNextBid(adUnitId)
      if (nextBid != null && bidManager.isValid(nextBid) && nextBid.cpm >= bid.cpm) {
        logger.debug(
            "bid is not valid, using next bid .", bidManager.invalidReason(bid)
        )
        nextBid
      } else {
        logger.debug("unable to attach next bid...")
        logger.debug("bid is invalid -", bidManager.invalidReason(bid))
        throw NoBidsFoundException()
      }
    } else {
      bid
    }
  }

  fun getBidForMediation(
    adUnitId: String?,
    floorCpm: Double
  ): BidResponse? {
    logger.debug("getting bids for mediation")
    val mediationBid = bidManager.peekNextBid(adUnitId)
    if (mediationBid == null || !bidManager.isValid(mediationBid)
        || mediationBid.cpm < floorCpm
    ) {
      if (mediationBid != null) {
        logger.debug(
            String.format(
                Locale.getDefault(), "next bid does not meet flor: %.2f < %.2f",
                mediationBid.cpm,
                floorCpm
            )
        )
      }
      logger.debug("no bid found for", adUnitId!!)
      return null
    }
    return mediationBid
  }

  class NoBidsFoundException : Exception()
  class NullBidException : Exception()
  companion object {
    private val logger = Logger("MediationManager")
  }
}