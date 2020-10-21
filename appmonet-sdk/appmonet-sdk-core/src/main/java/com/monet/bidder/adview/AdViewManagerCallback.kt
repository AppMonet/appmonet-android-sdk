package com.monet.bidder.adview

import android.net.Uri
import android.os.Message
import android.view.MotionEvent
import android.webkit.*
import com.monet.BidResponse

interface AdViewManagerCallback {
    val adViewEnvironment: String
    val adViewVisibility: String

    var bid: BidResponse?
    var isBrowserOpening: Boolean
    var isLoaded: Boolean
    var networkRequestCount: Int
    fun ajax(request: String): String
    fun callFinishLoad(bidToBeTracked: BidResponse)
    fun checkOverride(webView: WebView, uri: Uri): Boolean
    fun destroy()
    fun destroy(invalidate: Boolean)
    fun enableThirdPartyCookies(enabled: Boolean)
    fun executeJs(method: String, vararg args: String)
    fun getBooleanValue(key: String): String
    fun handleAdInteraction(view: WebView, url: String)
    fun interceptWindowOpen(webView: WebView, resultMsg: Message)
    fun isAdViewAttachedToLayout(): String
    fun loadAppStoreUrl(view: WebView, parse: Uri): Boolean
    fun markBidRendered(bidId: String): String
    fun nativePlacement(key: String, value: String)
    fun onAdViewTouchEvent(event: MotionEvent): Boolean
    fun openUrl(url: String)
    fun openUrlInBrowser(url: String)
    fun ready()
    fun setBackgroundColor(color: String)
    fun setBooleanValue(key: String, value: String): String
    fun shouldInterceptRequest(webView: WebView, url: String): WebResourceResponse?
    fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse?
    fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean
    var urlOpeningMethod: String
    val uuid: String
    var wasAdViewClicked: Boolean
}