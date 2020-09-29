package com.monet.bidder

import android.os.CountDownTimer
import com.monet.bidder.callbacks.ReadyCallbackManager
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicBoolean

internal class AddBidsManager(private val auctionManagerReadyCallbacks: ReadyCallbackManager<AppMonetWebView>) {
  private val sLogger = Logger("AddBidsManager")
  private val callbacks = ArrayList<AddBids>()
  private val isReady = AtomicBoolean(false)
  @Synchronized fun executeReady() {
    isReady.set(true)
    if (callbacks.isEmpty()) {
      return
    }
    sLogger.debug("executing addBids queue.  Size: ${callbacks.size}")
    for (addBids in callbacks) {
      try {
        addBids.cancelTimeout()
        addBids.callback.execute(addBids.remainingTime.toInt())
      } catch (e: Exception) {
        auctionManagerReadyCallbacks.onReady { auctionWebView: AppMonetWebView ->
          auctionWebView.trackEvent(
              "addBidsManager", "execute_ready_error",
              "null", 0f, 0L
          )
        }
        sLogger.warn("error in callback queue: ", e.message)
      }
      callbacks.clear()
    }
  }

  @Synchronized fun onReady(
    timeout: Int,
    callback: TimedCallback
  ) {
    if (isReady.get()) {
      try {
        callback.execute(timeout)
      } catch (e: Exception) {
        auctionManagerReadyCallbacks.onReady { auctionWebView: AppMonetWebView ->
          auctionWebView.trackEvent(
              "addBidsManager", "on_ready_error",
              "null", 0f, 0L
          )
        }
        sLogger.warn("error in onready:", e.message)
      }
      return
    }
    val addBids = AddBids(this, timeout, callback)
    sLogger.debug("queueing up addBids call")
    callbacks.add(addBids)
  }

  @Synchronized private fun removeAddBids(addBids: AddBids) {
    callbacks.remove(addBids)
  }

  private class AddBids constructor(
    addBidsManager: AddBidsManager,
    timeout: Int,
    val callback: TimedCallback
  ) {
    private val sLogger = Logger("AddBids")
    private val countDownTimer: CountDownTimer
    private val isCanceled = AtomicBoolean(false)
    private val endingTime: Long
    @Synchronized fun cancelTimeout() {
      sLogger.debug("canceling addBids timeout")
      isCanceled.set(true)
      countDownTimer.cancel()
    }

    @get:Synchronized val remainingTime: Long
      get() {
        val remainingTime = endingTime - currentTime
        sLogger.debug("remaining time: $remainingTime")
        return if (remainingTime < 0) 0 else remainingTime
      }
    private val currentTime: Long
      get() = System.currentTimeMillis()

    init {
      endingTime = currentTime + timeout
      countDownTimer = object : CountDownTimer(
          timeout.toLong(), timeout.toLong()
      ) {
        override fun onTick(millisUntilFinished: Long) {
          //not needed
        }

        override fun onFinish() {
          sLogger.debug("addBids timeout triggered")
          addBidsManager.removeAddBids(this@AddBids)
          if (isCanceled.get()) {
            return
          }
          isCanceled.set(true)
          callback.timeout()
        }
      }
      countDownTimer.start()
    }
  }
}