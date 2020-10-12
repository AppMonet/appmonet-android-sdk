package com.monet.bidder

internal class FloatingPosition(
  private val position: String,
  private val positionValues: Map<String, Value>
) {
  internal class Value(
    val value: Int,
    val unit: String
  )

  companion object {
    const val BOTTOM = "bottom"
    const val TOP = "top"
    const val START = "start"
    const val END = "end"
    const val HEIGHT = "height"
    const val WIDTH = "width"
    const val DP = "dp"
    const val PERCENT = "percent"
  }
}