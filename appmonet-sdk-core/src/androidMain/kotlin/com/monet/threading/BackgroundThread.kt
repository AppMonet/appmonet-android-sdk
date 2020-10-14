package com.monet.threading

import androidx.annotation.VisibleForTesting
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit.MILLISECONDS

actual class BackgroundThread {
  private val service: ScheduledThreadPoolExecutor

  actual constructor() {
    this.service = ScheduledThreadPoolExecutor(5)
  }

  @VisibleForTesting
  constructor(service: ScheduledThreadPoolExecutor) {
    this.service = service
  }

  actual fun execute(runnable: ThreadRunnable) {
    service.submit(runnable)

  }

  actual fun scheduleAtFixedRate(
    runnable: ThreadRunnable,
    interval: Long,
  ): ScheduledFutureCall {
    return ScheduledFutureCall(service.scheduleAtFixedRate(runnable, 0, interval, MILLISECONDS))
  }
}