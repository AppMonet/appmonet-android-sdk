package com.monet.adview

import com.monet.AdServerWrapper

/**
 * This class is basically abstract, except for the "from" helper.
 */
expect class AdSize(width: Int, height: Int) {
  val width: Int
  val height: Int

  fun getWidthInPixels(): Int

  fun getHeightInPixels(): Int

  companion object {
    /**
     * Return an AdSize subclass based on the current AdServerWrapper
     * configured with the AppMonetBidder.
     *
     * @param width  the width of the adSize we want to create
     * @param height the height of the adSize to be created
     * @return an AdSize subclass
     */
    fun from(
      width: Int,
      height: Int,
      adServerWrapper: AdServerWrapper
    ): AdSize
  }
}