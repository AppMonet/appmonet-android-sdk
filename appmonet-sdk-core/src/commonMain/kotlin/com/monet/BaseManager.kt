package com.monet

import com.monet.threading.BackgroundThread
import com.monet.threading.UIThread

abstract class BaseManager() {
  private val backgroundThread = BackgroundThread()
  private val uiThread = UIThread()
}