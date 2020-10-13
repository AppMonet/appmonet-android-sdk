package com.monet.bidder;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.widget.VideoView;

import java.util.UUID;

public class MonetVideoView extends VideoView implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener {
    boolean isBuffered;
    boolean isFocused;
    String videoUrl;
    String videoId;
    private VideoListener listener;
    private InterstitialAnalyticsTracker analyticsTracker;
    private String impressionId;

    public MonetVideoView(Context context) {
        super(context);
        setOnPreparedListener(this);
        setOnCompletionListener(this);
    }

    void setVideoListener(VideoListener listener) {
        this.listener = listener;
    }

    void setAnalyticsTracker(InterstitialAnalyticsTracker analyticsTracker) {
        this.analyticsTracker = analyticsTracker;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        analyticsTracker.trackVideoCompleted(videoId, impressionId);
        listener.onVideoCompleted();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        isBuffered = true;
        playVideo();
    }

    void loadVideo() {
        setVideoURI(Uri.parse(videoUrl));
    }

    void playVideo() {
        if (!isPlaying() && isBuffered && isFocused) {
            impressionId = UUID.randomUUID().toString();
            start();
            analyticsTracker.trackVideoEventStart(videoId, impressionId);
            listener.onVideoPlaying();
        }
    }

    void resetVideo() {
        analyticsTracker.trackVideoEventStop(videoId, impressionId);
        isFocused = false;
        pause();
        seekTo(0);
        listener.onResetVideo();
    }

    void trackVideoAttached() {
        analyticsTracker.trackVideoAttached(videoId, impressionId);
    }

    void trackVideoDetached() {
        analyticsTracker.trackVideoDetached(videoId, impressionId);
    }

    interface VideoListener {
        void onVideoPlaying();

        void onVideoCompleted();

        void onResetVideo();
    }
}
