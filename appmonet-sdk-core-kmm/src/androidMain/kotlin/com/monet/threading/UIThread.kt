package com.monet.threading

import android.os.Handler
import android.os.Looper
import androidx.annotation.VisibleForTesting

actual class UIThread {
  private var handler: Handler

  @VisibleForTesting
  constructor(handler: Handler) {
    this.handler = handler
  }

  actual constructor() {
    this.handler = Handler(Looper.getMainLooper())
  }

  actual fun run(runnable: ThreadRunnable) {
    this.handler.post(runnable)
  }

  actual fun runDelayed(
    runnable: ThreadRunnable,
    delay: Long
  ) {
    this.handler.postDelayed(runnable, delay)
  }

  actual fun removeCallbacks(runnable: ThreadRunnable) {
    this.handler.removeCallbacks(runnable)
  }
}