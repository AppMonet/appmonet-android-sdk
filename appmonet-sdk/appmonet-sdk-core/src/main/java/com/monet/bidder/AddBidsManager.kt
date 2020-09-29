package com.monet.bidder;

import android.os.CountDownTimer;

import androidx.annotation.NonNull;

import android.webkit.ValueCallback;

import com.monet.bidder.callbacks.ReadyCallbackManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class AddBidsManager {
  private final Logger sLogger = new Logger("AddBidsManager");
  private final List<AddBids> callbacks;
  private final AtomicBoolean isReady = new AtomicBoolean(false);
  private final ReadyCallbackManager<AppMonetWebView> auctionManagerReadyCallbacks;

  public AddBidsManager(@NonNull ReadyCallbackManager<AppMonetWebView> auctionManagerReadyCallbacks) {
    callbacks = new ArrayList<>();
    this.auctionManagerReadyCallbacks = auctionManagerReadyCallbacks;
  }

  public synchronized void executeReady() {
    isReady.set(true);
    if (callbacks.isEmpty()) {
      return;
    }
    sLogger.debug("executing addBids queue.  Size: ", String.valueOf(callbacks.size()));
    for (AddBids addBids : callbacks) {
      try {
        addBids.cancelTimeout();
        addBids.callback.execute((int) addBids.getRemainingTime());
      } catch (Exception e) {
        auctionManagerReadyCallbacks.onReady(auctionWebView -> {
          auctionWebView.trackEvent("addBidsManager", "execute_ready_error",
              "null", 0F, 0L);
          return null;
        });
        sLogger.warn("error in callback queue: ", e.getMessage());
      }
      callbacks.clear();
    }
  }

  synchronized public void onReady(int timeout, TimedCallback callback) {
    if (isReady.get()) {
      try {
        callback.execute(timeout);
      } catch (Exception e) {
        auctionManagerReadyCallbacks.onReady(auctionWebView -> {
          auctionWebView.trackEvent("addBidsManager", "on_ready_error",
              "null", 0F, 0L);
          return null;
        });
        sLogger.warn("error in onready:", e.getMessage());
      }
      return;
    }
    AddBids addBids = new AddBids(this, timeout, callback);
    sLogger.debug("queueing up addBids call");
    callbacks.add(addBids);
  }

  private synchronized void removeAddBids(AddBids addBids) {
    callbacks.remove(addBids);
  }

  private static class AddBids {
    private final Logger sLogger = new Logger("AddBids");
    private final TimedCallback callback;
    private final CountDownTimer countDownTimer;
    private final AtomicBoolean isCanceled = new AtomicBoolean(false);
    private final long endingTime;

    AddBids(final AddBidsManager addBidsManager, int timeout, final TimedCallback callback) {
      this.callback = callback;
      this.endingTime = getCurrentTime() + timeout;
      this.countDownTimer = new CountDownTimer(timeout, timeout) {
        @Override
        public void onTick(long millisUntilFinished) {
          //not needed
        }

        @Override
        public void onFinish() {
          sLogger.debug("addBids timeout triggered");
          addBidsManager.removeAddBids(AddBids.this);
          if (isCanceled.get()) {
            return;
          }
          isCanceled.set(true);
          callback.timeout();
        }
      };
      this.countDownTimer.start();
    }

    synchronized void cancelTimeout() {
      sLogger.debug("canceling addBids timeout");
      isCanceled.set(true);
      countDownTimer.cancel();
    }

    synchronized long getRemainingTime() {
      long remainingTime = endingTime - getCurrentTime();
      sLogger.debug("remaining time: ", String.valueOf(remainingTime));
      return (remainingTime < 0) ? 0 : remainingTime;
    }

    private long getCurrentTime() {
      return System.currentTimeMillis();
    }
  }
}
