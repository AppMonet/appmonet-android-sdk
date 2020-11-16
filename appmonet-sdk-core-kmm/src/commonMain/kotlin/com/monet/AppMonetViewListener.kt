package com.monet

interface AppMonetViewListener<T> {
  fun onAdRefreshed(view: T?)
  val currentView: IAppMonetViewLayout?
}