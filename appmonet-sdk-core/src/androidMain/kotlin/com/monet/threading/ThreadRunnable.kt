package com.monet.threading

actual abstract class ThreadRunnable : Runnable {
  override fun run() {
    try {
      runInternal()
    } catch (e: Exception) {
      catchException(e)
    }
  }

  actual abstract fun runInternal()
  actual abstract fun catchException(e: Exception?)
}