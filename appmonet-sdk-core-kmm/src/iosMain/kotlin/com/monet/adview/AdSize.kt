package com.monet.adview

import com.monet.AdServerWrapper
import platform.UIKit.UIScreen

/**
 * This class is basically abstract, except for the "from" helper.
 */
actual class AdSize actual constructor(
  actual val width: Int,
  actual val height: Int
) {

  actual fun getWidthInPixels(): Int {
    val scale = UIScreen.mainScreen.scale
    return (width * scale).toInt()
  }

  actual fun getHeightInPixels(): Int {
    val scale = UIScreen.mainScreen.scale
    return (height * scale).toInt()
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