package com.monet.bidder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class InterstitialAnalyticsTracker {
    private static final String BID_ID = "bidId";
    private static final String EVENT_TYPE = "eventType";
    private static final String TIMESTAMP = "timestamp";
    private static final String VIDEO_ID = "videoId";
    private static final String IMPRESSION_ID = "impressionId";
    private static final String TIME_DURATION_IN_MILLIS = "durationInMillis";
    private String bidId;
    private final BaseManager sdkManager;
    private long startTime;
    private long totalTimeInInterstitial;
    private JSONArray analyticsContent;

    InterstitialAnalyticsTracker(BaseManager sdkManager, String bidId) {
        this.sdkManager = sdkManager;
        analyticsContent = new JSONArray();
        this.bidId = bidId;
    }

    void trackVideoEventStart(String id, String impressionId) {
        buildVideoEvent("videoStart", id, impressionId);
    }

    void trackVideoEventStop(String id, String impressionId) {
        buildVideoEvent("videoStop", id, impressionId);
    }

    void trackVideoCompleted(String id, String impressionId) {
        buildVideoEvent("videoCompleted", id, impressionId);
    }

    private void buildVideoEvent(String event, String id, String impressionId) {
        try {
            JSONObject object = new JSONObject();
            object.put(BID_ID, bidId);
            object.put(VIDEO_ID, id);
            object.put(IMPRESSION_ID, impressionId);
            object.put(EVENT_TYPE, event);
            object.put(TIMESTAMP, System.currentTimeMillis());
            analyticsContent.put(object);
        } catch (JSONException e) {
           //Nothing to do.
        }
    }

    void send() {
        timeSpentInInterstitial();
        //todo refactor this.
        sdkManager.getAuctionManager().auctionWebView.executeJs("logEvents", analyticsContent.toString());
    }

    void interstitialViewAttached() {
        startTime = System.currentTimeMillis();
    }

    void interstitialViewDetached() {
        totalTimeInInterstitial = System.currentTimeMillis() - startTime;
    }

    void trackVideoDetached(String videoId, String impressionId) {
        buildVideoEvent("videoDetached", videoId, impressionId);
    }

    void trackVideoAttached(String videoId, String impressionId) {
        buildVideoEvent("videoAttached", videoId, impressionId);
    }

    void trackVideoClickEvent(String videoId, String impressionId) {
        buildVideoEvent("videoClicked", videoId, impressionId);
    }

    private void timeSpentInInterstitial() {
        try {
            JSONObject object = new JSONObject();
            object.put(BID_ID, bidId);
            object.put(EVENT_TYPE, "duration");
            object.put(TIME_DURATION_IN_MILLIS, totalTimeInInterstitial);
            analyticsContent.put(object);
        } catch (Exception e) {
            //Nothing to do.
        }
    }
}
