package com.monet.adview

enum class AdViewState(private val mReadableName: String) {
  AD_LOADING("LOADING"),
  AD_RENDERED("RENDERED"),
  AD_MIXED_USE("MIXED_USE"),
  AD_OPEN("OPEN"),
  NOT_FOUND("NOT_FOUND");

  override fun toString(): String {
    return mReadableName
  }
}