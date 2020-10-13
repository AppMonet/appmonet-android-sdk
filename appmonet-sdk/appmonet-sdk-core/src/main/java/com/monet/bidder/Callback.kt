package com.monet.bidder

/**
 * Created by jose on 1/30/18.
 */
interface Callback<T> {
  fun onSuccess(response: T)
  fun onError()
}