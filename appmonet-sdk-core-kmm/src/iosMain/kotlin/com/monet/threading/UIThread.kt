package com.monet.threading

import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual class UIThread {
  actual fun run(runnable: ThreadRunnable) {
    dispatch_async(dispatch_get_main_queue()) {
      runnable()
    }
  }

  actual fun runDelayed(
    runnable: ThreadRunnable,
    delay: Long
  ) {
    dispatch_async(dispatch_get_main_queue()) {
      runnable()
    }
  }
}