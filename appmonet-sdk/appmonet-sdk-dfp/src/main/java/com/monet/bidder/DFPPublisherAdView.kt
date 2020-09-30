package com.monet.bidder;

import android.content.Context;

import com.google.android.gms.ads.doubleclick.PublisherAdView;
import com.google.android.gms.ads.doubleclick.PublisherInterstitialAd;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nbjacob on 6/26/17.
 */

class DFPPublisherAdView implements AdServerAdView {
    private final PublisherAdView mAdView;
    private String mAdUnitId;
    private Context mContext;

    DFPPublisherAdView(PublisherAdView adView) {
        mAdView = adView;
        mContext = adView.getContext();
        mAdUnitId = adView.getAdUnitId();
    }

    DFPPublisherAdView(PublisherInterstitialAd interstitialAd, Context context) {
        mAdUnitId = interstitialAd.getAdUnitId();
        mContext = context;
        mAdView = null;
    }

    DFPPublisherAdView(String adUnitId){
        mAdUnitId = adUnitId;
        mAdView = null;
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
    public Context getContext() {
        return mContext;
    }

    @Override
    public void loadAd(AdServerAdRequest request) {
        DFPAdRequest adRequest = (DFPAdRequest) request;
        if (mAdView != null) {
            mAdView.loadAd(adRequest.getDFPRequest());
        }
    }
}
