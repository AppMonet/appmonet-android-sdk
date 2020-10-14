package com.monet.threading

expect class BackgroundThread() {

  fun execute(runnable: ThreadRunnable)

  fun scheduleAtFixedRate(
    runnable: ThreadRunnable,
    interval: Long,
  ): ScheduledFutureCall
}