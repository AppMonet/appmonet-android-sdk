package com.monet.bidder;


import com.monet.bidder.threading.InternalRunnable;
import com.mopub.mobileads.DefaultBannerAdListener;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubView;

/**
 * Created by nbjacob on 9/2/17.
 */

class MopubBannerAdListener implements MoPubView.BannerAdListener {
    private final static int REFRESH_TRY_DELAY = 4000;
    private final static Logger sLogger = new Logger("BannerAdListener");
    private final String mAdUnitId;
    private final SdkManager mManager;
    private final MoPubView.BannerAdListener mOriginalListener;
    private Runnable mRefreshTimer;

    MopubBannerAdListener(String adUnitId, MoPubView.BannerAdListener originalListener, SdkManager manager) {
        super();
        mAdUnitId = adUnitId;
        mOriginalListener = originalListener == null ? (new DefaultBannerAdListener()) : originalListener;
        mManager = manager;
    }

    void setBannerRefreshTimer(final MoPubView banner) {
        if (mRefreshTimer != null) {
            mManager.getUiThread().run(mRefreshTimer);
        }

        mRefreshTimer = new InternalRunnable() {
            @Override
            public void runInternal() {
                sLogger.debug("Attaching next bid (after load)");
                mManager.addBids(banner, mAdUnitId);
            }

            @Override
            public void catchException(Exception e) {

            }
        };

        mManager.getUiThread().runDelayed(mRefreshTimer, REFRESH_TRY_DELAY);
    }

    @Override
    public void onBannerLoaded(MoPubView banner) {
        sLogger.debug("banner loaded. Attaching next bid");
        setBannerRefreshTimer(banner);
        mOriginalListener.onBannerLoaded(banner);
    }

    @Override
    public void onBannerFailed(MoPubView banner, MoPubErrorCode errorCode) {
        sLogger.debug("banner failed. Attaching new bid");
        mManager.addBids(banner, mAdUnitId);
        mOriginalListener.onBannerFailed(banner, errorCode);
    }

    @Override
    public void onBannerClicked(MoPubView banner) {
        mOriginalListener.onBannerClicked(banner);
    }

    @Override
    public void onBannerExpanded(MoPubView banner) {
        mOriginalListener.onBannerExpanded(banner);
    }

    @Override
    public void onBannerCollapsed(MoPubView banner) {
        mOriginalListener.onBannerCollapsed(banner);
    }
}
