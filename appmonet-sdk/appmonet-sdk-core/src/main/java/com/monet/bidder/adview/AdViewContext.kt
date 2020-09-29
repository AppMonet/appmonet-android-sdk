package com.monet.bidder.adview

import com.monet.bidder.bid.BidResponse
import org.json.JSONException
import org.json.JSONObject

/**
 * This class holds information about the {@link AdView} webview's environment.
 */
class AdViewContext {
    @JvmField
    var url: String

    @JvmField
    var userAgent: String

    @JvmField
    var width: Int

    @JvmField
    var height: Int

    @JvmField
    var adUnitId: String

    @JvmField
    var explicitRequest = false

    constructor(url: String, userAgent: String, width: Int, height: Int,
                adUnitId: String) {
        this.url = url
        this.userAgent = userAgent
        this.width = width
        this.height = height
        this.adUnitId = adUnitId
        this.explicitRequest = true
    }

    constructor(bid: BidResponse) {
        url = bid.url
        userAgent = bid.u
        width = bid.width
        height = bid.height
        adUnitId = bid.adUnitId
        explicitRequest = false
    }

    fun toJson(): String {
        val json = JSONObject()
        return try {
            json.put("url", url)
            json.put("userAgent", userAgent)
            json.put("width", width)
            json.put("height", height)
            json.put("adUnitId", adUnitId)
            json.toString()
        } catch (e: JSONException) {
            ""
        }
    }

    fun toHash(): String {
        return url + userAgent + width + height + adUnitId
    }

}