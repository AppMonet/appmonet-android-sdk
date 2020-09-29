package com.monet.bidder;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.webkit.ValueCallback;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.doubleclick.PublisherAdView;
import com.google.android.gms.ads.doubleclick.PublisherInterstitialAd;

import static com.monet.bidder.Constants.MISSING_INIT_ERROR_MESSAGE;

/**
 * Created by jose on 8/28/17.
 */

class SdkManager extends BaseManager {
  private final static Logger sLogger = new Logger("SdkManager");
  private static final Object LOCK = new Object();
  private static SdkManager sInstance;
  boolean isPublisherAdView = true;
  static AppMonetConfiguration appMonetConfiguration;

  protected SdkManager(Context context, String applicationId) {
    super(context, applicationId, new DFPAdServerWrapper());
  }

  static void initialize(Context context, AppMonetConfiguration appMonetConfiguration) {
    try {
      synchronized (LOCK) {
        if (sInstance != null) {
          sLogger.warn("Sdk has already been initialized. No need to initialize it again.");
          return;
        }
        sInstance =
            new SdkManager(context.getApplicationContext(), appMonetConfiguration.applicationId);
        ((DFPAdServerWrapper) sInstance.getAdServerWrapper()).setSdkManager(sInstance);
      }
    } catch (Exception e) {
      if (initRetry < 3) {
        sLogger.error("error initializing ... retrying " + e);
        initRetry += 1;
        initialize(context, appMonetConfiguration);
      }
    }
  }

  static SdkManager get() {
    synchronized (LOCK) {
      if (sInstance == null) {
        sLogger.error(MISSING_INIT_ERROR_MESSAGE);
      }
      return sInstance;
    }
  }

  void addBids(final com.google.android.gms.ads.AdView adView, final AdRequest adRequest,
      final String appMonetAdUnitId, final int timeout, final ValueCallback<AdRequest> onDone) {
    getAuctionManager().timedCallback(timeout, new TimedCallback() {
      @Override
      public void execute(int remainingTime) {
        logState();
        isPublisherAdView = false;
        DFPAdView dfpAdView = new DFPAdView(adView);
        dfpAdView.setAdUnitId(appMonetAdUnitId);
        getAuctionManager().addBids(dfpAdView, new DFPAdViewRequest(adRequest), remainingTime,
            new ValueCallback<AdServerAdRequest>() {
              @Override
              public void onReceiveValue(AdServerAdRequest value) {
                if (value == null) {
                  sLogger.debug("value is null");
                  onDone.onReceiveValue(adRequest);
                  return;
                }
                sLogger.debug("value is valid");
                onDone.onReceiveValue(((DFPAdViewRequest) value).getDFPRequest());
              }
            }
        );
      }

      @Override
      public void timeout() {
        trackTimeoutEvent(appMonetAdUnitId, timeout);
        onDone.onReceiveValue((adRequest == null) ? new AdRequest.Builder().build() : adRequest);
      }
    });
  }

  void addBids(final PublisherAdView adView, final PublisherAdRequest adRequest,
      final String appMonetAdUnitId,
      final int timeout, final ValueCallback<PublisherAdRequest> onDone) {
    getAuctionManager().timedCallback(timeout, new TimedCallback() {
      @Override
      public void execute(int remainingTime) {
        DFPPublisherAdView dfpPublisherAdView = new DFPPublisherAdView(adView);
        dfpPublisherAdView.setAdUnitId(appMonetAdUnitId);
        final AddBidsParams addBidsParams = generateAddBidsParams(dfpPublisherAdView,
            adRequest, remainingTime, onDone);
        onBidManagerReadyCallback(addBidsParams);
      }

      @Override
      public void timeout() {
        trackTimeoutEvent(appMonetAdUnitId, timeout);
        onDone.onReceiveValue(getPublisherAdRequest(adRequest));
      }
    });
  }

  void addBids(final PublisherInterstitialAd interstitialAd, final PublisherAdRequest adRequest,
      final String appMonetAdUnitId, final int timeout, final
  ValueCallback<PublisherAdRequest> onDone) {
    getAuctionManager().timedCallback(timeout, new TimedCallback() {

      @Override
      public void execute(int remainingTime) {
        Context ctx = getContext().get();
        if (ctx == null) {
          sLogger.warn("failed to bind context. Returning");
          onDone.onReceiveValue(adRequest);
          return;
        }

        if (!isInterstitialActivityRegistered(ctx, MonetDfpActivity.class.getName())) {
          String error = "Unable to create activity. Not defined in AndroidManifest.xml. " +
              "Please refer to https://docs.appmonet.com/ for integration infomration.\n";
          sLogger.error(error);
          getAuctionManager().trackEvent("integration_error",
              "missing_interstitial_activity", appMonetAdUnitId, 0f, 0L);
          throw new ActivityNotFoundException(error);
        }

        DFPPublisherAdView dfpPublisherAdView = new DFPPublisherAdView(interstitialAd, ctx);
        dfpPublisherAdView.setAdUnitId(appMonetAdUnitId);
        final AddBidsParams addBidsParams = generateAddBidsParams(dfpPublisherAdView,
            adRequest, remainingTime, onDone);
        onBidManagerReadyCallback(addBidsParams);
      }

      @Override
      public void timeout() {
        trackTimeoutEvent(appMonetAdUnitId, timeout);
        onDone.onReceiveValue(getPublisherAdRequest(adRequest));
      }
    });
  }

  void addBids(final InterstitialAd interstitialAd, final AdRequest adRequest,
      final String appMonetAdUnitId, final int timeout, final ValueCallback<AdRequest> onDone) {
    getAuctionManager().timedCallback(timeout, new TimedCallback() {
      @Override
      public void execute(int remainingTime) {
        logState();
        isPublisherAdView = false;
        DFPInterstitialAdView dfpInterstitialAdView = new DFPInterstitialAdView(interstitialAd);
        dfpInterstitialAdView.setAdUnitId(appMonetAdUnitId);
        getAuctionManager().addBids(dfpInterstitialAdView, new DFPAdViewRequest(adRequest),
            remainingTime,
            new ValueCallback<AdServerAdRequest>() {
              @Override
              public void onReceiveValue(AdServerAdRequest value) {
                if (value == null) {
                  sLogger.debug("value is null");
                  onDone.onReceiveValue(adRequest);
                  return;
                }
                sLogger.debug("value is valid");
                onDone.onReceiveValue(((DFPAdViewRequest) value).getDFPRequest());
              }
            }
        );
      }

      @Override
      public void timeout() {
        trackTimeoutEvent(appMonetAdUnitId, timeout);
        onDone.onReceiveValue((adRequest == null) ? new AdRequest.Builder().build() : adRequest);
      }
    });
  }

  void addBids(final PublisherAdRequest adRequest, final String appMonetAdUnitId, final int timeout,
      final ValueCallback<PublisherAdRequest> onDone) {
    getAuctionManager().timedCallback(timeout, new TimedCallback() {
      @Override
      public void execute(int remainingTime) {
        Context ctx = getContext().get();
        if (ctx == null) {
          sLogger.warn("failed to bind context. Returning");
          onDone.onReceiveValue(adRequest);
          return;
        }
        DFPPublisherAdView dfpPublisherAdView = new DFPPublisherAdView(appMonetAdUnitId);
        final AddBidsParams addBidsParams = generateAddBidsParams(dfpPublisherAdView,
            adRequest, remainingTime, onDone);
        onBidManagerReadyCallback(addBidsParams);
      }

      @Override
      public void timeout() {
        trackTimeoutEvent(appMonetAdUnitId, timeout);
        onDone.onReceiveValue(getPublisherAdRequest(adRequest));
      }
    });
  }

  PublisherAdRequest addBids(PublisherAdView adView, PublisherAdRequest adRequest,
      String appMonetAdUnitId) {
    DFPPublisherAdView dfpPublisherAdView = new DFPPublisherAdView(adView);
    dfpPublisherAdView.setAdUnitId(appMonetAdUnitId);

    PublisherAdRequest localAdRequest = (adRequest == null) ?
        new PublisherAdRequest.Builder().build() : adRequest;

    DFPAdRequest request = (DFPAdRequest) getAuctionManager().addBids(dfpPublisherAdView,
        new DFPAdRequest(localAdRequest));

    // if the request is null, just pass through the original
    return (request != null) ? request.getDFPRequest() : localAdRequest;
  }

  PublisherAdRequest addBids(PublisherAdRequest adRequest, String appMonetAdUnitId) {
    PublisherAdRequest localAdRequest = (adRequest == null) ?
        new PublisherAdRequest.Builder().build() : adRequest;
    DFPPublisherAdView dfpPublisherAdView = new DFPPublisherAdView(appMonetAdUnitId);
    DFPAdRequest request = (DFPAdRequest) getAuctionManager().addBids(
        dfpPublisherAdView, new DFPAdRequest(localAdRequest));
    return (request != null) ? request.getDFPRequest() : localAdRequest;
  }

  private AddBidsParams generateAddBidsParams(AdServerAdView adServerAdView,
      final PublisherAdRequest adRequest, int timeout,
      final ValueCallback<PublisherAdRequest> onDone) {
    return new AddBidsParams(adServerAdView,
        new DFPAdRequest(getPublisherAdRequest(adRequest)),
        timeout, adServerAdRequest -> {
      if (adServerAdRequest == null) {
        sLogger.debug("value null");
        onDone.onReceiveValue(adRequest);
        return;
      }
      sLogger.debug("value is valid");
      onDone.onReceiveValue(((DFPAdRequest) adServerAdRequest).getDFPRequest());
    });
  }

  private void onBidManagerReadyCallback(final AddBidsParams addBidsParams) {
    getAuctionManager().addBids(addBidsParams.getAdView(), addBidsParams.getRequest(),
        addBidsParams.getTimeout(), addBidsParams.getCallback());
  }

  private PublisherAdRequest getPublisherAdRequest(PublisherAdRequest adRequest) {
    return (adRequest == null) ? new PublisherAdRequest.Builder().build() : adRequest;
  }
}
