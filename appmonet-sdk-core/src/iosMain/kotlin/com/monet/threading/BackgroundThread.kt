package com.monet.threading

import platform.darwin.DISPATCH_SOURCE_TYPE_TIMER
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.NSEC_PER_MSEC
import platform.darwin.NSEC_PER_SEC
import platform.darwin.dispatch_queue_create
import platform.darwin.dispatch_queue_serial_t
import platform.darwin.dispatch_source_create
import platform.darwin.dispatch_source_set_event_handler
import platform.darwin.dispatch_source_set_timer
import platform.darwin.dispatch_source_t
import platform.darwin.dispatch_time
import platform.posix.uint64_t

actual class BackgroundThread {
  private val backgroundQueue =
    dispatch_queue_create("com.monet.background.queue", dispatch_queue_serial_t())

  actual fun execute(runnable: ThreadRunnable) {
    backgroundQueue.run { runnable.runInternal() }
  }

  actual fun scheduleAtFixedRate(
    runnable: ThreadRunnable,
    interval: Long,
  ): ScheduledFutureCall {
    val timer: dispatch_source_t =
      dispatch_source_create(DISPATCH_SOURCE_TYPE_TIMER, 0, 0, backgroundQueue)
    val timerDelay: uint64_t = 7000.toULong() * NSEC_PER_MSEC
    dispatch_source_set_timer(
        timer,
        dispatch_time(DISPATCH_TIME_NOW, timerDelay.toLong()),
        timerDelay, (1.toULong() * NSEC_PER_SEC) / 10.toULong()
    );
    dispatch_source_set_event_handler(timer) {
      runnable.runInternal()
    }
    return ScheduledFutureCall(timer)
  }
}