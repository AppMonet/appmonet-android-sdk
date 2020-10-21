package com.monet.threading

import java.util.concurrent.ScheduledFuture

actual class ScheduledFutureCall(private val scheduledFuture: ScheduledFuture<*>) {
  actual fun cancel() {
    scheduledFuture.cancel(true)
  }
}