package com.monet.bidder;

import android.content.Context;
import android.view.View;

import com.mopub.mobileads.MoPubView;

/**
 * Created by nbjacob on 6/26/17.
 */

class MopubAdView implements AdServerAdView {
    private final MoPubView mAdView;
    private final View mContentView;
    private String mAdUnitId;

    MopubAdView(MoPubView adView, View contentView) {
        mAdView = adView;
        mAdUnitId = adView.getAdUnitId();
        mContentView = contentView;
    }

    MopubAdView(MoPubView adView) {
        mAdView = adView;
        mContentView = null;
        mAdUnitId = adView.getAdUnitId();
    }

    MoPubView getMopubView() {
        return mAdView;
    }

    @Override
    public Context getContext() {
        return mAdView.getContext();
    }

    @Override
    public AdServerWrapper.Type getType() {
        return AdServerWrapper.Type.BANNER;
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
        MopubAdRequest adRequest = (MopubAdRequest) request;
        adRequest.applyToView(this);
        if (mContentView != null) {
            this.mAdView.setAdContentView(mContentView);
        }
        this.mAdView.loadAd();
    }
}
