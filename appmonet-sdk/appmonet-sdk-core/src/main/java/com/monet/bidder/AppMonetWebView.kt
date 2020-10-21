package com.monet.bidder

import com.monet.Callback
import java.util.concurrent.atomic.AtomicBoolean

interface AppMonetWebView {
  fun executeJs(
      method: String,
      vararg args: String
  )

  fun executeJs(
      timeout: Int,
      method: String,
      vararg args: String
  )

  fun executeJs(
      timeout: Int,
      method: String,
      callback: Callback<String?>?,
      vararg args: String
  )

  fun executeJsAsync(
      callback: Callback<String?>?,
      method: String,
      timeout: Int,
      vararg args: String
  )

  fun executeJsCode(javascript: String?): Boolean
  var isDestroyed: Boolean
  val isLoaded: AtomicBoolean
  fun loadView(url: String)
  fun start()
  fun trackEvent(
      eventName: String,
      detail: String,
      key: String,
      value: Float,
      currentTime: Long
  )

}
