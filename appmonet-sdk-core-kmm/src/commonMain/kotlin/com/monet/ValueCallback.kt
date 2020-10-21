package com.monet


fun interface ValueCallback<T> {
  /**
   * Invoked when the value is available.
   * @param value The value.
   */
  fun onReceiveValue(value: T)
}

typealias Callback<T> = (T) -> Unit

