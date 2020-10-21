package com.monet

/**
 * Created by jose on 8/29/17.
 */
enum class AdType(private val type: String) {
  BANNER("banner"),
  INTERSTITIAL("interstitial"),
  NATIVE("native");

  override fun toString(): String {
    return type
  }
}