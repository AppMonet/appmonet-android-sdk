package com.monet

import android.content.Context
import com.monet.adview.AdSize

class MoPubAdServerWrapper(private val context: Context) : BaseMoPubAdServerWrapper {
  override fun newAdSize(
    width: Int,
    height: Int
  ): AdSize {
    return AdSize(context, width, height)
  }
}