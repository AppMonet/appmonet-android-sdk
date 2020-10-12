package com.monet.bidder

import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import com.monet.bidder.FloatingPosition.Value
import com.monet.bidder.adview.AdViewPositioning

internal object FloatingAdRenderUtil {
  fun getScreenPositioning(
    view: View,
    positionSettings: Map<String, Value>,
    h: Int?,
    w: Int?
  ): AdViewPositioning {
    //retrieve location of view
    val location = IntArray(2)
    view.getLocationOnScreen(location)
    val viewStartTopX = location[0]
    val viewStartTopY = location[1]

    // if the view layout matches parent then get the view's width since its already rendered
    // if not get the width provided
    val viewWidth = if (view.parent != null && view.parent is FrameLayout && (
            (view.parent as FrameLayout).layoutParams.width
                == LayoutParams.WRAP_CONTENT)
        || view.width == 0
    ) RenderingUtils.dpToPixels(w!!) else view.width
    val viewHeight = if (view.parent != null && view.parent is FrameLayout && (
            (view.parent as FrameLayout).layoutParams.height
                == LayoutParams.WRAP_CONTENT)
        || view.height == 0
    ) RenderingUtils.dpToPixels(h!!) else view.height
    return calculateAdViewPositioning(
        viewHeight, viewWidth, viewStartTopX, viewStartTopY,
        positionSettings
    )
  }

  @VisibleForTesting fun calculateAdViewPositioning(
    viewHeight: Int,
    viewWidth: Int,
    viewStartX: Int,
    viewStartY: Int,
    positionSettings: Map<String, Value>
  ): AdViewPositioning {
    var unit: String
    var y = 0
    var x = 0
    var height = 0
    var width = 0
    for ((key, value) in positionSettings) {
      unit = value.unit
      var valueInPixels = 0
      //If unit is DP then convert it to pixels
      if (FloatingPosition.DP == unit) {
        valueInPixels = RenderingUtils.dpToPixels(value.value)
      }
      when (key) {
        FloatingPosition.BOTTOM -> y =
          if (FloatingPosition.DP == unit) viewStartY - valueInPixels else viewStartY - RenderingUtils.percentToPixels(
              viewHeight, value.value
          )
        FloatingPosition.TOP -> y =
          if (FloatingPosition.DP == unit) viewStartY + valueInPixels else viewStartY + RenderingUtils.percentToPixels(
              viewHeight, value.value
          )
        FloatingPosition.START -> x =
          if (FloatingPosition.DP == unit) viewStartX + valueInPixels else viewStartX + RenderingUtils.percentToPixels(
              viewWidth, value.value
          )
        FloatingPosition.END -> {
          val adWidth =
            if (FloatingPosition.DP == positionSettings[FloatingPosition.WIDTH]?.unit) RenderingUtils.dpToPixels(
                positionSettings[FloatingPosition.WIDTH]?.value
            ) else RenderingUtils.percentToPixels(
                viewWidth, positionSettings[FloatingPosition.WIDTH]?.value
            )
          val widthDifference = viewWidth - adWidth
          x =
            if (FloatingPosition.DP == value.unit) widthDifference - valueInPixels else widthDifference - RenderingUtils.percentToPixels(
                viewWidth, value.value
            )
        }
        FloatingPosition.HEIGHT -> {
          height =
            if (FloatingPosition.DP == unit || positionSettings[FloatingPosition.HEIGHT] == null) valueInPixels
            else RenderingUtils.percentToPixels(
                viewHeight, positionSettings[FloatingPosition.HEIGHT]?.value
            )
        }
        FloatingPosition.WIDTH -> width =
          if (FloatingPosition.DP == unit) valueInPixels else RenderingUtils.percentToPixels(
              viewWidth, positionSettings[FloatingPosition.WIDTH]?.value
          )
        else -> {
        }
      }
    }
    return AdViewPositioning(x, y, width, height)
  }
}