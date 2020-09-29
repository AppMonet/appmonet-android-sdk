package com.monet.bidder.adview

import com.monet.bidder.bid.BidResponse
import com.monet.bidder.SdkConfigurations

internal interface AdViewPoolManagerCallback {
    fun adViewCreated(uuid: String, adViewContext: AdViewContext)
    fun adViewLoaded(uuid: String)
    fun adViewResponse(vararg args: String)
    fun canReleaseAdViewManager(adViewManager: AdViewManager): Boolean
    fun getReferenceCount(wvUUID: String?): Int
    fun getSdkConfigurations(): SdkConfigurations
    fun impressionEnded()
    fun remove(uuid: String, destroy: Boolean): Boolean
    fun remove(adViewManager: AdViewManager?, destroyWV: Boolean, forceDestroy: Boolean): Boolean
    fun request(bid: BidResponse): AdViewManager?
    fun requestDestroy(wvUUID: String): Boolean
}