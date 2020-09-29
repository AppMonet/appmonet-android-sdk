package com.monet.bidder

import android.content.Context

/**
 * This class is basically abstract, except for the "from" helper.
 */
class AdSize(
  val width: Int = 0,
  val height: Int = 0
) {

  fun getWidthInPixels(context: Context?): Int {
    return Icons.asIntPixels(width.toFloat(), context)
  }

  fun getHeightInPixels(context: Context?): Int {
    return Icons.asIntPixels(height.toFloat(), context)
  }

  companion object {
    /**
     * Return an AdSize subclass based on the current AdServerWrapper
     * configured with the AppMonetBidder.
     *
     * @param width  the width of the adSize we want to create
     * @param height the height of the adSize to be created
     * @return an AdSize subclass
     */
    @JvmStatic fun from(
      width: Int,
      height: Int,
      adServerWrapper: AdServerWrapper
    ): AdSize {
      return adServerWrapper.newAdSize(width, height)
    }
  }
}