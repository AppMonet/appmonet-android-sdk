package com.monet.bidder

import com.monet.bidder.FloatingPosition.*
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner


@RunWith(RobolectricTestRunner::class)
class RenderUtilsTest {

    @Test
    fun `top percent is calculated correctly`() {
        //setting bid to have extras defined as 50% bottom;
        val position = mapOf(
                Pair(TOP, Value(50, PERCENT)),
                Pair(WIDTH, Value(320, DP)),
                Pair(HEIGHT, Value(50, DP))
        )
//        val magicPosition = FloatingPosition("", position)
        val adViewPositioning = RenderingUtils.calculateAdViewPositioning(300,
                250, 0, 0, position)
        Assert.assertEquals(150, adViewPositioning.y)

    }

    @Test
    fun `bottom percent is calculated correctly`() {
        val position = mapOf(
                Pair(BOTTOM, Value(50, PERCENT)),
                Pair(WIDTH, Value(320, DP)),
                Pair(HEIGHT, Value(50, DP))
        )
        val adViewPositioning = RenderingUtils.calculateAdViewPositioning(300,
                250, 0, 0, position)
        Assert.assertEquals(-150, adViewPositioning.y)

    }

    @Test
    fun `start percent is calculated correctly`() {
        val position = mapOf(
                Pair(START, Value(50, PERCENT)),
                Pair(WIDTH, Value(320, DP)),
                Pair(HEIGHT, Value(50, DP))
        )
        val magicPosition = FloatingPosition("", position)
        val adViewPositioning = RenderingUtils.calculateAdViewPositioning(300,
                250, 0, 0, position)
        Assert.assertEquals(125, adViewPositioning.x)

    }

    @Test
    fun `end percent is calculated correctly`() {
        val position = mapOf(
                Pair(END, Value(25, PERCENT)),
                Pair(WIDTH, Value(320, DP)),
                Pair(HEIGHT, Value(50, DP))
        )
        val adViewPositioning = RenderingUtils.calculateAdViewPositioning(300,
                250, 0, 0, position)
        Assert.assertEquals(-132, adViewPositioning.x)
    }

    @Test
    fun `top dp is calculated correctly`() {
        val position = mapOf(
                Pair(TOP, Value(50, DP)),
                Pair(WIDTH, Value(320, DP)),
                Pair(HEIGHT, Value(50, DP))
        )
        val adViewPositioning = RenderingUtils.calculateAdViewPositioning(300,
                250, 0, 0, position)
        Assert.assertEquals(50, adViewPositioning.y)
    }

    @Test
    fun `bottom dp is calculated correctly`() {
        val position = mapOf(
                Pair(BOTTOM, Value(50, DP)),
                Pair(WIDTH, Value(320, DP)),
                Pair(HEIGHT, Value(50, DP))
        )
        val adViewPositioning = RenderingUtils.calculateAdViewPositioning(300,
                250, 0, 0, position)
        Assert.assertEquals(-50, adViewPositioning.y)
    }

    @Test
    fun `start dp is calculated correctly`() {
        val position = mapOf(
                Pair(START, Value(50, DP)),
                Pair(WIDTH, Value(320, DP)),
                Pair(HEIGHT, Value(50, DP))
        )
        val adViewPositioning = RenderingUtils.calculateAdViewPositioning(300,
                250, 0, 0, position)
        Assert.assertEquals(50, adViewPositioning.x)
    }

    @Test
    fun `end dp is calculated correctly`() {
        val position = mapOf(
                Pair(END, Value(50, DP)),
                Pair(WIDTH, Value(320, DP)),
                Pair(HEIGHT, Value(50, DP))
        )
        val adViewPositioning = RenderingUtils.calculateAdViewPositioning(300,
                250, 0, 0, position)
        Assert.assertEquals(-120, adViewPositioning.x)
    }

    @Test
    fun `end dp is calculated correctly when ad width is in percent`() {
        val position = mapOf(
                Pair(END, Value(50, DP)),
                Pair(WIDTH, Value(100, PERCENT)),
                Pair(HEIGHT, Value(50, DP))
        )
        val adViewPositioning = RenderingUtils.calculateAdViewPositioning(300,
                250, 0, 0, position)
        Assert.assertEquals(-50, adViewPositioning.x)
    }

    @Test
    fun `width is in dp`() {
        val position = mapOf(
                Pair(WIDTH, Value(320, DP)),
                Pair(HEIGHT, Value(50, DP))
        )
        val adViewPositioning = RenderingUtils.calculateAdViewPositioning(300,
                250, 0, 0, position)
        Assert.assertEquals(320, adViewPositioning.width)
    }

    @Test
    fun `height is in dp`() {
        val position = mapOf(
                Pair(WIDTH, Value(320, DP)),
                Pair(HEIGHT, Value(50, DP))
        )
        val adViewPositioning = RenderingUtils.calculateAdViewPositioning(300,
                250, 0, 0, position)
        Assert.assertEquals(50, adViewPositioning.height)
    }

    @Test
    fun `width is in percent`() {
        val position = mapOf(
                Pair(WIDTH, Value(80, PERCENT)),
                Pair(HEIGHT, Value(50, DP))
        )
        val adViewPositioning = RenderingUtils.calculateAdViewPositioning(300,
                250, 0, 0, position)
        Assert.assertEquals(200, adViewPositioning.width)
    }

    @Test
    fun `height is in percent`() {
        val position = mapOf(
                Pair(WIDTH, Value(0, PERCENT)),
                Pair(HEIGHT, Value(60, PERCENT))
        )
        val adViewPositioning = RenderingUtils.calculateAdViewPositioning(300,
                250, 0, 0, position)
        Assert.assertEquals(180, adViewPositioning.height)
    }
}