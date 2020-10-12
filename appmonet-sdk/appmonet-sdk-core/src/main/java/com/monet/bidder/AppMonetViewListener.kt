package com.monet.bidder

import android.view.View

interface AppMonetViewListener {
  fun onAdRefreshed(view: View?)
  val currentView: AppMonetViewLayout?
}