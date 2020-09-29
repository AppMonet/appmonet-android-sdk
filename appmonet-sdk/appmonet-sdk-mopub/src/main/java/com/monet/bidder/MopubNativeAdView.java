package com.monet.bidder;

import android.content.Context;

import com.mopub.nativeads.MoPubNative;


class MopubNativeAdView implements AdServerAdView {
    private final MoPubNative mNativeAd;
    private String mAdUnitId;


    MopubNativeAdView(MoPubNative nativeAd, String adUnitId) {
        mNativeAd = nativeAd;
        mAdUnitId = adUnitId;
    }

    MoPubNative getMopubNative() {
        return mNativeAd;
    }

    @Override
    public Context getContext() {
        return null;
    }

    @Override
    public AdServerWrapper.Type getType() {
        return AdServerWrapper.Type.NATIVE;
    }

    @Override
    public String getAdUnitId() {
        return mAdUnitId;
    }

    @Override
    public void setAdUnitId(String adUnitId) {
        mAdUnitId = adUnitId;
    }

    @Override
    public void loadAd(AdServerAdRequest request) {
    }
}
