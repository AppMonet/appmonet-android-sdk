package com.monet.threading

actual abstract class ThreadRunnable {
  actual abstract fun runInternal()
  actual abstract fun catchException(e: Exception?)
}