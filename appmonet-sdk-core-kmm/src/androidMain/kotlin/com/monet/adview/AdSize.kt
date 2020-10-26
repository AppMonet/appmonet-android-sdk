package com.monet.adview

import android.content.Context
import android.util.TypedValue
import com.monet.AdServerWrapper

/**
 * This class is basically abstract, except for the "from" helper.
 */
actual class AdSize {
  private lateinit var context: Context
  actual val width: Int
  actual val height: Int

  actual constructor(
    width: Int,
    height: Int
  ) {
    this.width = width
    this.height = height
  }

  constructor(context: Context, width: Int, height: Int){
    this.context = context
    this.width = width
    this.height = height
  }

  actual fun getWidthInPixels(): Int {
    val displayMetrics = context.resources.displayMetrics
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, width.toFloat(), displayMetrics).toInt()
  }

  actual fun getHeightInPixels(): Int {
    val displayMetrics = context.resources.displayMetrics
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, width.toFloat(), displayMetrics).toInt()
  }

  actual companion object {
    /**
     * Return an AdSize subclass based on the current AdServerWrapper
     * configured with the AppMonetBidder.
     *
     * @param width  the width of the adSize we want to create
     * @param height the height of the adSize to be created
     * @return an AdSize subclass
     */
    actual fun from(
      width: Int,
      height: Int,
      adServerWrapper: AdServerWrapper
    ): AdSize {
      return adServerWrapper.newAdSize(width, height)
    }
  }

}