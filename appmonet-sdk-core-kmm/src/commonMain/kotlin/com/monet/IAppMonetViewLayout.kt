package com.monet

import com.monet.adview.AdViewState

interface IAppMonetViewLayout {
  val uuid:String
  val state: AdViewState
  fun destroyAdView(invalidate: Boolean) {}
}

