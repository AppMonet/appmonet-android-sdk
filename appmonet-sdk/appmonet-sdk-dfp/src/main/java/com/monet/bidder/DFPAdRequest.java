package com.monet.bidder;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.admob.AdMobExtras;
import com.google.android.gms.ads.mediation.customevent.CustomEventBannerListener;

import com.monet.bidder.auction.AuctionRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;

class DFPAdRequest extends AdServerAdRequest {
  private PublisherAdRequest mPublisherRequest;

  /**
   * Build a DFPAdRequest from a PublisherAdRequest, the DFP representation of an ad request
   *
   * @param adRequest the request constructed by the publisher, to be sent to DFP
   */
  DFPAdRequest(PublisherAdRequest adRequest) {
    mPublisherRequest = adRequest;
  }

  /**
   * Build a "new"/blank request, using an empty PublisherAdRequest
   */
  DFPAdRequest() {
    mPublisherRequest = new PublisherAdRequest.Builder().build();
  }

  /**
   * Build a DFPAdRequest from a MediationAdRequest. The MediationAdRequest
   * is what's passed to use in {@link CustomEventBanner#requestBannerAd(Context,
   * CustomEventBannerListener, String, AdSize, MediationAdRequest, Bundle)}.
   * We instantiate a DFPAdRequest there in order to queue up further bids.
   *
   * @param mediationRequest a MediationAdRequest representing the current request cycle in DFP
   */
  DFPAdRequest(MediationAdRequest mediationRequest) {
    mPublisherRequest = new PublisherAdRequest.Builder()
        .setBirthday(mediationRequest.getBirthday())
        .setGender(mediationRequest.getGender())
        .setLocation(mediationRequest.getLocation())
        .build();
  }

  @SuppressWarnings("deprecation")
  private Bundle getAdMobExtras() {
    try {
      AdMobExtras extras = mPublisherRequest.getNetworkExtras(AdMobExtras.class);
      if (extras != null) {
        return extras.getExtras();
      }
    } catch (Exception e) {
    }

    return new Bundle();
  }

  @Override
  public Bundle getCustomTargeting() {
    // also get the admob extras and merge it here
    Bundle extras = getAdMobExtras();
    Bundle targeting = mPublisherRequest.getCustomTargeting();

    // create a bundle merging both
    Bundle merged = new Bundle();
    merged.putAll(extras);
    merged.putAll(targeting);

    return merged;
  }

  @Override
  public Date getBirthday() {
    return mPublisherRequest.getBirthday();
  }

  @Override
  public String getGender() {
    switch (mPublisherRequest.getGender()) {
      case PublisherAdRequest.GENDER_FEMALE:
        return "female";
      case PublisherAdRequest.GENDER_MALE:
        return "male";
      default:
        return "unknown";
    }
  }

  @Override
  public Location getLocation() {
    return mPublisherRequest.getLocation();
  }

  @Override
  public String getContentUrl() {
    return mPublisherRequest.getContentUrl();
  }

  PublisherAdRequest getDFPRequest() {
    return mPublisherRequest;
  }

  @Override
  public AuctionRequest apply(AuctionRequest request, AdServerAdView adView) {
    // transfer admob extras if they're there
    try {
      Bundle adMob = mPublisherRequest.getNetworkExtrasBundle(AdMobAdapter.class);
      AdMobExtras admobExtras = mPublisherRequest.getNetworkExtras(AdMobExtras.class);
      request.getAdmobExtras().putAll(filterTargeting(
          (admobExtras != null) ? admobExtras.getExtras() : adMob));
    } catch (Exception e) {
      // do nothing
    }

    if (request.getRequestData() == null) {
      request.setRequestData(new RequestData(this, adView));
    }

    request.getTargeting().putAll(filterTargeting(
        mPublisherRequest.getCustomTargeting()));
    return request;
  }

  @Override
  public String getPublisherProvidedId() {
    return mPublisherRequest.getPublisherProvidedId();
  }

  static DFPAdRequest fromAuctionRequest(AuctionRequest request) {
    PublisherAdRequest.Builder builder = new PublisherAdRequest.Builder()
        .addCustomEventExtrasBundle(CustomEventBanner.class, request.getNetworkExtras())
        .addCustomEventExtrasBundle(CustomEventInterstitial.class, request.getNetworkExtras())
        .addCustomEventExtrasBundle(MonetDfpCustomEventInterstitial.class,
            request.getNetworkExtras())
        .addNetworkExtrasBundle(CustomEventBanner.class, request.getNetworkExtras());

    // copy over the targeting
    for (String key : request.getTargeting().keySet()) {
      Object value = request.getTargeting().get(key);
      if (value == null) {
        continue;
      }

      if (value instanceof List) {
        List<String> listValue = (List<String>) value;
        builder.addCustomTargeting(key, listValue);
      } else {
        builder.addCustomTargeting(key, value.toString());
      }
    }

    if (request.getRequestData() != null) {
      if (request.getRequestData().contentURL != null
          && !request.getRequestData().contentURL.isEmpty()) {
        builder.setContentUrl(request.getRequestData().contentURL);
      }
      builder.setLocation(request.getRequestData().location);
      for (Map.Entry<String, String> entry : request.getRequestData().additional.entrySet()) {
        builder.addCustomTargeting(entry.getKey(), entry.getValue());
      }
    }

    // add in the admob extras
    Bundle completeExtras = request.getAdmobExtras();
    completeExtras.putAll(request.getTargeting());
    try {
      builder.addNetworkExtras(new AdMobExtras(completeExtras));
    } catch (Exception e) {
      // do nothing
    }

    return new DFPAdRequest(builder.build());
  }
}