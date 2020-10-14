package com.monet.threading

expect class UIThread() {
  fun run(runnable: ThreadRunnable)

  fun runDelayed(
    runnable: ThreadRunnable,
    delay: Long
  )
}