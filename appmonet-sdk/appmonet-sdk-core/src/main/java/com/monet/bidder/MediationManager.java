package com.monet.bidder;

import android.webkit.ValueCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.monet.bidder.bid.BidManager;
import com.monet.bidder.bid.BidResponse;
import com.monet.bidder.threading.InternalRunnable;
import java.util.Locale;

/**
 * Created by jose on 3/19/18.
 */

public final class MediationManager {
  private static final Logger logger = new Logger("MediationManager");
  private BaseManager sdkManager;
  private final BidManager bidManager;

  public MediationManager(@NonNull BaseManager sdkManager, @NonNull BidManager bidManager) {
    this.sdkManager = sdkManager;
    this.bidManager = bidManager;
  }

  void getBidReadyForMediationAsync(@Nullable final BidResponse bid, @NonNull final String adUnitId,
      @Nullable final AdSize adSize, @NonNull final AdType adType, final double floorCpm,
      @NonNull final Callback<BidResponse> callback) {
    this.getBidReadyForMediationAsync(bid, adUnitId, adSize, adType, floorCpm, callback, null);
  }

  void getBidReadyForMediationAsync(@Nullable final BidResponse bid, @NonNull final String adUnitId,
      @Nullable final AdSize adSize, @NonNull final AdType adType, final double floorCpm,
      @NonNull final Callback<BidResponse> callback, @Nullable Integer defaultTimeout) {
    try {
      callback.onSuccess(getBidReadyForMediation(bid, adUnitId, adSize, adType, floorCpm, false));
    } catch (NoBidsFoundException | NullBidException e) {
      //todo refactor this.
      int timeout = sdkManager.getAuctionManager().getSdkConfigurations().getAdUnitTimeout(adUnitId);
      int finalTimeout = (timeout <= 0 && defaultTimeout != null) ? defaultTimeout : timeout;
      if (finalTimeout <= 0) {
        sdkManager.indicateRequest(adUnitId, adSize, adType, floorCpm);
        callback.onError();
        return;
      }
      sdkManager.indicateRequestAsync(adUnitId, finalTimeout, adSize, adType, floorCpm,
          value -> sdkManager.getUiThread().run(new InternalRunnable() {
            @Override
            public void runInternal() {
              try {
                callback.onSuccess(
                    getBidReadyForMediation(getBidForMediation(adUnitId, floorCpm),
                        adUnitId, adSize, adType, floorCpm, false));
              } catch (NoBidsFoundException | NullBidException ex) {
                callback.onError();
              }
            }

            @Override
            public void catchException(Exception e1) {
              callback.onError();
            }
          }));
    }
  }

  @NonNull
  BidResponse getBidReadyForMediation(@Nullable BidResponse bid, @NonNull String adUnitId,
      @Nullable AdSize adSize,
      @NonNull AdType adType, double floorCpm, boolean indicateRequest)
      throws NoBidsFoundException, NullBidException {
    if (indicateRequest) {
      sdkManager.indicateRequest(adUnitId, adSize, adType, floorCpm);
    }

    if (bid == null || bid.getId().isEmpty()) {
      logger.debug("first bid is null/invalid - no fill");
      throw new NullBidException();
    }

    if (!bidManager.isValid(bid)) {
      BidResponse nextBid = bidManager.peekNextBid(adUnitId);

      if (nextBid != null && bidManager.isValid(nextBid) && nextBid.getCpm() >= bid.getCpm()) {
        logger.debug("bid is not valid, using next bid .", bidManager.invalidReason(bid));
        return nextBid;
      } else {
        logger.debug("unable to attach next bid...");
        logger.debug("bid is invalid -", bidManager.invalidReason(bid));
        throw new NoBidsFoundException();
      }
    } else {
      return bid;
    }
  }

  @Nullable
  public BidResponse getBidForMediation(String adUnitId, Double floorCpm) {
    logger.debug("getting bids for mediation");
    BidResponse mediationBid = bidManager.peekNextBid(adUnitId);
    if (mediationBid == null
        || !bidManager.isValid(mediationBid)
        || mediationBid.getCpm() < floorCpm) {
      if (mediationBid != null) {
        logger.debug(
            String.format(Locale.getDefault(), "next bid does not meet flor: %.2f < %.2f",
                mediationBid.getCpm(),
                floorCpm
            )
        );
      }
      logger.debug("no bid found for", adUnitId);
      return null;
    }
    return mediationBid;
  }

  static class NoBidsFoundException extends Exception {

  }

  static class NullBidException extends Exception {
  }
}
