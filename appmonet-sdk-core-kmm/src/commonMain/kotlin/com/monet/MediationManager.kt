package com.monet

import com.monet.adview.AdSize
import com.monet.threading.UIThread

typealias MediationCallback = (bid: BidResponse?, error: Exception?) -> Unit

class MediationManager(
  private val bidManager: IBidManager,
  private val baseManager: IBaseManager,
  private val uiThread: UIThread
) {

  fun getBidReadyForMediationAsync(
    bid: BidResponse?,
    adUnitId: String,
    adSize: AdSize?,
    adType: AdType,
    floorCpm: Double,
    configurationsTimeout: Int,
    callback: MediationCallback,
  ) {
    this.getBidReadyForMediationAsync(
        bid, adUnitId, adSize, adType, floorCpm, callback, configurationsTimeout, null
    )
  }

  fun getBidReadyForMediationAsync(
    bid: BidResponse?,
    adUnitId: String,
    adSize: AdSize?,
    adType: AdType,
    floorCpm: Double,
    callback: MediationCallback,
    configurationsTimeout: Int?,
    defaultTimeout: Int?
  ) {
    try {
      callback(getBidReadyForMediation(bid, adUnitId, adSize, adType, floorCpm, false), null)
    } catch (e: NoBidsFoundException) {
      indicareRequestAsync(
          configurationsTimeout, defaultTimeout, adUnitId, adSize, adType, floorCpm, e, callback
      )
    } catch (e: NullBidException) {
      indicareRequestAsync(
          configurationsTimeout, defaultTimeout, adUnitId, adSize, adType, floorCpm, e, callback
      )
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
      baseManager.indicateRequest(adUnitId, adSize, adType, floorCpm)
    }
    if (bid == null || bid.id.isEmpty()) {
//      logger.debug("first bid is null/invalid - no fill")
      throw NullBidException()
    }
    return if (!bidManager.isValid(bid)) {
      val nextBid = bidManager.peekNextBid(adUnitId)
      if (nextBid != null && bidManager.isValid(nextBid) && nextBid.cpm >= bid.cpm) {
//        logger.debug(
//            "bid is not valid, using next bid .", bidManager.invalidReason(bid)
//        )
        nextBid
      } else {
//        logger.debug("unable to attach next bid...")
//        logger.debug("bid is invalid -", bidManager.invalidReason(bid))
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
//    logger.debug("getting bids for mediation")
    val mediationBid = bidManager.peekNextBid(adUnitId)
    if (mediationBid == null || !bidManager.isValid(mediationBid)
        || mediationBid.cpm < floorCpm
    ) {
//      if (mediationBid != null) {
//        logger.debug(
//            String.format(
//                Locale.getDefault(), "next bid does not meet flor: %.2f < %.2f",
//                mediationBid.cpm,
//                floorCpm
//            )
//        )
//      }
//      logger.debug("no bid found for", adUnitId!!)
      return null
    }
    return mediationBid
  }

  private fun indicareRequestAsync(
    configurationsTimeout: Int?,
    defaultTimeout: Int?,
    adUnitId: String,
    adSize: AdSize?,
    adType: AdType,
    floorCpm: Double,
    exception: Exception,
    callback: MediationCallback
  ) {
    val finalTimeout =
      if (configurationsTimeout == null || configurationsTimeout <= 0 && defaultTimeout != null)
        defaultTimeout else configurationsTimeout
    if (finalTimeout == null || finalTimeout <= 0) {
      baseManager.indicateRequest(adUnitId, adSize, adType, floorCpm)
      callback(null, exception)
      return
    }
    baseManager.indicateRequestAsync(
        adUnitId, finalTimeout, adSize, adType, floorCpm
    ) {
      uiThread.run {
        try {
          callback(
              getBidReadyForMediation(
                  getBidForMediation(adUnitId, floorCpm),
                  adUnitId, adSize, adType, floorCpm, false
              ), null
          )
        } catch (ex: NoBidsFoundException) {
          callback(null, ex)
        } catch (ex: NullBidException) {
          callback(null, ex)
        }
      }
    }
  }

  class NoBidsFoundException : Exception()
  class NullBidException : Exception()
}