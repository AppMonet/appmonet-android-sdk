package com.monet.bidder.bid

import com.monet.bidder.HttpUtil
import com.monet.bidder.Logger

internal object Pixel {
    private const val PIXEL_EVENT_REPLACE = "__event__"
    private val sLogger = Logger("Pixel")

    fun fire(pixel: String?, event: Events) {
        if (pixel == null || pixel == "") {
            return
        }

        // helps us avoid firing multiple himp event on old code
        if (!pixel.contains(PIXEL_EVENT_REPLACE)) {
            sLogger.warn("invalid pixel: no replace")
            return
        }
        fire(pixel.replace(PIXEL_EVENT_REPLACE, event.toString()))
    }

    fun fire(pixel: String?) {
        if (pixel == null || pixel == "") {
            return
        }
        HttpUtil.firePixelAsync(pixel)
    }

    enum class Events(private val event: String) {
        IMPRESSION("himp"),
        REQUEST("hreq"),
        VAST_IMPRESSION("vimp"),
        VAST_FIRST_QUARTILE("vfq"),
        VAST_MIDPOINT("vmp"),
        VAST_THIRD_QUARTILE("vtq"),
        VAST_COMPLETE("vcmp"),
        VAST_ERROR("verr"),
        ERROR("herr");

        override fun toString(): String {
            return event
        }

    }
}