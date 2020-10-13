package com.monet.bidder

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.media.MediaPlayer.OnPreparedListener
import android.net.Uri
import android.widget.VideoView
import java.util.UUID

class MonetVideoView(context: Context?) : VideoView(context),
    OnCompletionListener,
    OnPreparedListener {
  var isBuffered = false
  var focused = false
  var videoUrl: String? = null
  var videoId: String? = null
  private var listener: VideoListener? = null
  private var analyticsTracker: InterstitialAnalyticsTracker? = null
  private var impressionId: String? = null
  fun setVideoListener(listener: VideoListener?) {
    this.listener = listener
  }

  fun setAnalyticsTracker(analyticsTracker: InterstitialAnalyticsTracker?) {
    this.analyticsTracker = analyticsTracker
  }

  override fun onCompletion(mp: MediaPlayer) {
    analyticsTracker!!.trackVideoCompleted(videoId!!, impressionId!!)
    listener!!.onVideoCompleted()
  }

  override fun onPrepared(mp: MediaPlayer) {
    isBuffered = true
    playVideo()
  }

  fun loadVideo() {
    setVideoURI(Uri.parse(videoUrl))
  }

  fun playVideo() {
    if (!isPlaying && isBuffered && focused) {
      impressionId = UUID.randomUUID().toString()
      start()
      analyticsTracker!!.trackVideoEventStart(videoId!!, impressionId!!)
      listener!!.onVideoPlaying()
    }
  }

  fun resetVideo() {
    analyticsTracker!!.trackVideoEventStop(videoId!!, impressionId!!)
    focused = false
    pause()
    seekTo(0)
    listener!!.onResetVideo()
  }

  fun trackVideoAttached() {
    analyticsTracker!!.trackVideoAttached(videoId!!, impressionId!!)
  }

  fun trackVideoDetached() {
    analyticsTracker!!.trackVideoDetached(videoId!!, impressionId!!)
  }

  interface VideoListener {
    fun onVideoPlaying()
    fun onVideoCompleted()
    fun onResetVideo()
  }

  init {
    setOnPreparedListener(this)
    setOnCompletionListener(this)
  }
}