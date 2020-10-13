package com.monet.bidder

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class InterstitialAnalyticsTracker(
  private val sdkManager: BaseManager,
  private val bidId: String
) {
  private var startTime: Long = 0
  private var totalTimeInInterstitial: Long = 0
  private val analyticsContent: JSONArray = JSONArray()
  fun trackVideoEventStart(
    id: String,
    impressionId: String
  ) {
    buildVideoEvent("videoStart", id, impressionId)
  }

  fun trackVideoEventStop(
    id: String,
    impressionId: String
  ) {
    buildVideoEvent("videoStop", id, impressionId)
  }

  fun trackVideoCompleted(
    id: String,
    impressionId: String
  ) {
    buildVideoEvent("videoCompleted", id, impressionId)
  }

  private fun buildVideoEvent(
    event: String,
    id: String,
    impressionId: String
  ) {
    try {
      val `object` = JSONObject()
      `object`.put(BID_ID, bidId)
      `object`.put(VIDEO_ID, id)
      `object`.put(IMPRESSION_ID, impressionId)
      `object`.put(EVENT_TYPE, event)
      `object`.put(TIMESTAMP, System.currentTimeMillis())
      analyticsContent.put(`object`)
    } catch (e: JSONException) {
      //Nothing to do.
    }
  }

  fun send() {
    timeSpentInInterstitial()
    //todo refactor this.
    sdkManager.auctionManager.auctionWebView.executeJs("logEvents", analyticsContent.toString())
  }

  fun interstitialViewAttached() {
    startTime = System.currentTimeMillis()
  }

  fun interstitialViewDetached() {
    totalTimeInInterstitial = System.currentTimeMillis() - startTime
  }

  fun trackVideoDetached(
    videoId: String,
    impressionId: String
  ) {
    buildVideoEvent("videoDetached", videoId, impressionId)
  }

  fun trackVideoAttached(
    videoId: String,
    impressionId: String
  ) {
    buildVideoEvent("videoAttached", videoId, impressionId)
  }

  fun trackVideoClickEvent(
    videoId: String,
    impressionId: String
  ) {
    buildVideoEvent("videoClicked", videoId, impressionId)
  }

  private fun timeSpentInInterstitial() {
    try {
      val `object` = JSONObject()
      `object`.put(BID_ID, bidId)
      `object`.put(EVENT_TYPE, "duration")
      `object`.put(TIME_DURATION_IN_MILLIS, totalTimeInInterstitial)
      analyticsContent.put(`object`)
    } catch (e: Exception) {
      //Nothing to do.
    }
  }

  companion object {
    private const val BID_ID = "bidId"
    private const val EVENT_TYPE = "eventType"
    private const val TIMESTAMP = "timestamp"
    private const val VIDEO_ID = "videoId"
    private const val IMPRESSION_ID = "impressionId"
    private const val TIME_DURATION_IN_MILLIS = "durationInMillis"
  }

}