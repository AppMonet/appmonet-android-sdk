package com.monet.bidder

import android.view.View

/**
 * Created by jose on 1/31/18.
 */
interface AppMonetNativeEventCallback {
  fun destroy(view: View?)
  fun onClick(view: View?)
}