package com.monet.bidder;

import android.location.Location;
import android.os.Bundle;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.admob.AdMobExtras;
import com.monet.bidder.auction.AuctionRequest;
import java.util.Date;

class DFPAdViewRequest extends AdServerAdRequest {
  private static AdRequest mAdRequest;

  DFPAdViewRequest() {
    mAdRequest = new AdRequest.Builder().build();
  }

  DFPAdViewRequest(AdRequest adRequest) {
    mAdRequest = adRequest;
  }

  DFPAdViewRequest(MediationAdRequest mediationAdRequest) {
    mAdRequest = new AdRequest.Builder()
        .setBirthday(mediationAdRequest.getBirthday())
        .setGender(mediationAdRequest.getGender())
        .setLocation(mediationAdRequest.getLocation())
        .build();
  }

  private Bundle getAdMobExtras() {
    try {
      Bundle extras = mAdRequest.getNetworkExtrasBundle(AdMobAdapter.class);
      //      AdMobExtras extras = mAdRequest.getNetworkExtras(AdMobExtras.class);
      if (extras != null) {
        return extras;
      }
    } catch (Exception e) {
    }

    return new Bundle();
  }

  @Override
  public Bundle getCustomTargeting() {
    // also get the admob extras and merge it here
    Bundle extras = getAdMobExtras();

    // create a bundle merging both
    Bundle merged = new Bundle();
    merged.putAll(extras);

    return merged;
  }

  @Override
  public Date getBirthday() {
    return mAdRequest.getBirthday();
  }

  @Override
  public String getGender() {
    switch (mAdRequest.getGender()) {
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
    return mAdRequest.getLocation();
  }

  @Override
  public String getContentUrl() {
    return mAdRequest.getContentUrl();
  }

  AdRequest getDFPRequest() {
    return mAdRequest;
  }

  @Override
  public AuctionRequest apply(AuctionRequest request, AdServerAdView adView) {
    // transfer admob extras if they're there
    try {
      Bundle adMob = mAdRequest.getNetworkExtrasBundle(AdMobAdapter.class);
      AdMobExtras admobExtras = mAdRequest.getNetworkExtras(AdMobExtras.class);
      request.getAdmobExtras().putAll(filterTargeting(
          (admobExtras != null) ? admobExtras.getExtras() : adMob));
    } catch (Exception e) {
      // do nothing
    }

    if (request.getRequestData() == null) {
      request.setRequestData(new RequestData(this, adView));
    }
    return request;
  }

  @Override
  public String getPublisherProvidedId() {
    return "";
  }

  static DFPAdViewRequest fromAuctionRequest(AuctionRequest request) {
    AdRequest.Builder builder = new AdRequest.Builder()
        .addCustomEventExtrasBundle(CustomEventBanner.class, request.getNetworkExtras())
        .addCustomEventExtrasBundle(CustomEventInterstitial.class, request.getNetworkExtras())
        .addCustomEventExtrasBundle(MonetDfpCustomEventInterstitial.class,
            request.getNetworkExtras())
        .addNetworkExtrasBundle(CustomEventBanner.class, request.getNetworkExtras());

    // add in the admob extras
    Bundle completeExtras =
        request.getAdmobExtras() == null ? new Bundle() : request.getAdmobExtras();
    completeExtras.putAll(request.getTargeting());
    try {
      builder.addNetworkExtrasBundle(AdMobAdapter.class, completeExtras);
    } catch (Exception e) {
      sLogger.error("excetion " + e);
      // do nothing
    }
    if (request.getRequestData() != null) {
      if (request.getRequestData().contentURL != null
          && !request.getRequestData().contentURL.isEmpty()) {
        builder.setContentUrl(request.getRequestData().contentURL);
      }
      builder.setLocation(request.getRequestData().location);
    }
    AdRequest adRequest = builder.build();
    return new DFPAdViewRequest(adRequest);
  }
}