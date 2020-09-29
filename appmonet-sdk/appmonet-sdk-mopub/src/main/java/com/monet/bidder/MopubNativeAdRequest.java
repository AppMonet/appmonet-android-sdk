package com.monet.bidder;

import android.location.Location;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.monet.bidder.auction.AuctionRequest;
import com.monet.bidder.bid.BidResponse;
import com.mopub.nativeads.MoPubNative;
import com.mopub.nativeads.RequestParameters;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.monet.bidder.Constants.BIDS_KEY;
import static com.monet.bidder.Constants.Dfp.ADUNIT_KEYWORD_KEY;
import static com.monet.bidder.MoPubRequestUtil.getKeywords;
import static com.monet.bidder.MoPubRequestUtil.mergeKeywords;

class MopubNativeAdRequest extends AdServerAdRequest {
  private final Map<String, Object> mLocalExtras;
  private final String adUnitId;

  private MoPubNative moPubNative;
  private final RequestParameters requestParameters;
  RequestParameters modifiedRequestParameters;
  @Nullable
  private BidResponse mBid = null;

  MopubNativeAdRequest(MoPubNative moPubNative, String adUnitId,
      RequestParameters requestParameters) {
    this.requestParameters = requestParameters;
    mLocalExtras = new HashMap<>();
    this.moPubNative = moPubNative;
    this.adUnitId = adUnitId;
    if (mLocalExtras.containsKey(BIDS_KEY)) {
      try {
        mBid = BidResponse.Mapper.from(new JSONObject((String) mLocalExtras.get(BIDS_KEY)));
      } catch (JSONException e) {
        //do nothing
      }
    }
  }

  MopubNativeAdRequest(String adUnitId) {
    this.requestParameters = new RequestParameters.Builder().build();
    mLocalExtras = new HashMap<>();
    this.adUnitId = adUnitId;
  }

  static MopubNativeAdRequest fromAuctionRequest(AuctionRequest request) {
    MopubNativeAdRequest adRequest = new MopubNativeAdRequest(request.getAdUnitId());
    for (String key : request.getTargeting().keySet()) {
      Object targeting = request.getTargeting().get(key);
      if (targeting != null) {
        adRequest.mLocalExtras.put(key, targeting);
      }
    }

    if (request.getBid() != null) {
      adRequest.mBid = request.getBid();
    }

    return adRequest;
  }

  @Override
  public boolean hasBid() {
    return mBid != null;
  }

  @Override
  public BidResponse getBid() {
    return mBid;
  }

  @Override
  public Location getLocation() {
    if (requestParameters == null) {
      return null;
    }
    return requestParameters.getLocation();
  }

  @Override
  public Date getBirthday() {
    return null;
  }

  @Override
  public String getContentUrl() {
    return null;
  }

  @Override
  public String getGender() {
    return null;
  }

  @Override
  public AuctionRequest apply(AuctionRequest instance, AdServerAdView adView) {
    return instance;
  }

  @Override
  public Bundle getCustomTargeting() {
    return null;
  }

  void applyToView(MopubNativeAdView mopubNativeAdView) {
    // apply the targeting to the view, as keywords
    MoPubNative nativeAd = mopubNativeAdView.getMopubNative();
    mLocalExtras.put(BIDS_KEY, BidResponse.Mapper.toJson(mBid).toString());
    mLocalExtras.put(ADUNIT_KEYWORD_KEY, adUnitId);
    nativeAd.setLocalExtras(mLocalExtras);
    String keywords = getKeywords(mLocalExtras);
    if (requestParameters != null) {
      keywords = mergeKeywords(requestParameters.getKeywords(), keywords);
    }
    modifiedRequestParameters = setKeywords(keywords);
  }

  private RequestParameters setKeywords(String keywords) {
    if (requestParameters != null) {
      return new RequestParameters.Builder()
          .keywords(keywords)
          .location(requestParameters.getLocation())
          .userDataKeywords(requestParameters.getUserDataKeywords()).build();
    } else {
      return new RequestParameters.Builder()
          .keywords(keywords)
          .build();
    }
  }
}