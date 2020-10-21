package com.monet.threading

import platform.darwin.dispatch_source_cancel
import platform.darwin.dispatch_source_t

actual class ScheduledFutureCall(private var timer: dispatch_source_t) {
  actual fun cancel() {
    dispatch_source_cancel(timer);
  }
}