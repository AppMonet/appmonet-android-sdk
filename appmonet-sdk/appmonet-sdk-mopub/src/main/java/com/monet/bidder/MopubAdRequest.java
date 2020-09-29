package com.monet.bidder;

import android.location.Location;
import android.os.Bundle;

import com.monet.bidder.auction.AuctionRequest;
import com.monet.bidder.bid.BidResponse;
import com.mopub.mobileads.MoPubView;
import com.monet.bidder.MoPubRequestUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.monet.bidder.Constants.BIDS_KEY;
import static com.monet.bidder.Constants.Dfp.ADUNIT_KEYWORD_KEY;
import static com.monet.bidder.MoPubRequestUtil.getKeywords;
import static com.monet.bidder.MoPubRequestUtil.mergeKeywords;

/**
 * Created by nbjacob on 6/26/17.
 */

class MopubAdRequest extends AdServerAdRequest {
  static final String CE_AD_WIDTH = "com_mopub_ad_width";
  static final String CE_AD_HEIGHT = "com_mopub_ad_height";
  static final String CE_AD_FORMAT = "__ad_format";
  private final Map<String, Object> mLocalExtras;
  private final MoPubView mAdView;
  private BidResponse mBid = null;

  MopubAdRequest(MoPubView adView) {
    mLocalExtras = adView.getLocalExtras();
    mAdView = adView;

    // set this data so we can read it later
    // on the local extras we get
    mLocalExtras.put(CE_AD_FORMAT, adView.getAdFormat());
    // extract the targeting from the adview
    if (mLocalExtras.containsKey(BIDS_KEY)) {
      try {
        mBid = BidResponse.Mapper.from(new JSONObject((String) mLocalExtras.get(BIDS_KEY)));
      } catch (JSONException e) {
        //do nothing
      }
    }
  }

  MopubAdRequest() {
    mLocalExtras = new HashMap<>();
    mAdView = null;
  }

  static AdServerAdRequest fromAuctionRequest(AuctionRequest request) {
    MopubAdRequest adRequest = new MopubAdRequest();

    for (String key : request.getTargeting().keySet()) {
      adRequest.mLocalExtras.put(key, request.getTargeting().get(key));
    }

    if (request.getBid() != null) {
      adRequest.mBid = request.getBid(); // just pass the bids along
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
    if (mAdView == null) {
      return null;
    }

    return mAdView.getLocation();
  }

  @Override
  public Date getBirthday() {
    if (mLocalExtras.containsKey("birthday")) {
      return (Date) mLocalExtras.get("birthday");
    }
    return null;
  }

  @Override
  public String getContentUrl() {
    if (mLocalExtras.containsKey("content_url")) {
      return (String) mLocalExtras.get("content_url");
    }
    return null;
  }

  @Override
  public String getGender() {
    return (String) mLocalExtras.get("gender");
  }

  @Override
  public AuctionRequest apply(AuctionRequest instance, AdServerAdView adView) {
    // TODO: we should probably mess w/ requestData here
    instance.getTargeting().putAll(
        filterTargeting(getCustomTargeting()));

    return instance;
  }

  @Override
  public Bundle getCustomTargeting() {
    // turn our map into targeting
    Bundle bundle = new Bundle();
    for (Map.Entry<String, Object> kvp : mLocalExtras.entrySet()) {
      Object value = kvp.getValue();
      if (value == null) continue;

      try {
        if (value instanceof List<?>) {
          try {
            bundle.putStringArrayList(
                kvp.getKey(), (ArrayList<String>) kvp.getValue());
          } catch (Exception e) {
            sLogger.warn("failed to set custom targeting", e.getMessage());
          }
          continue;
        }

        if (value instanceof Bundle) {
          bundle.putBundle(kvp.getKey(), (Bundle) value);
          continue;
        }

        bundle.putString(kvp.getKey(), value.toString());
      } catch (Exception e) {
        // do nothing
      }
    }
    return bundle;
  }

  void applyToView(MopubAdView adView) {
    // apply the targeting to the view, as keywords
    MoPubView view = adView.getMopubView();
    if (mBid != null) {
      mLocalExtras.put(BIDS_KEY, BidResponse.Mapper.toJson(mBid).toString());
    }
    mLocalExtras.put(ADUNIT_KEYWORD_KEY, view.getAdUnitId());
    view.setLocalExtras(mLocalExtras);
    String keywords = getKeywords(mLocalExtras);
    if (view.getKeywords() != null) {
      keywords = mergeKeywords(view.getKeywords(), keywords);
    }
    view.setKeywords(keywords);
    view.setLocation(getLocation());
  }
}
