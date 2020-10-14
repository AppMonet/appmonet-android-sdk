package com.monet.threading

expect abstract class ThreadRunnable {
  actual abstract fun runInternal()
  actual abstract fun catchException(e: Exception?)
}