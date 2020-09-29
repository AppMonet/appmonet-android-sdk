package com.monet.bidder;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.webkit.ValueCallback;

import androidx.annotation.NonNull;

import com.monet.bidder.auction.AuctionManagerCallback;
import com.monet.bidder.auction.AuctionRequest;
import com.monet.bidder.bid.BidManager;
import com.monet.bidder.threading.BackgroundThread;
import com.monet.bidder.threading.InternalRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.monet.bidder.bid.BidResponse;

import static com.monet.bidder.BaseManager.isTestMode;
import static com.monet.bidder.Constants.TEST_MODE_WARNING;
import static com.monet.bidder.bid.BidResponse.Constant.BID_BUNDLE_KEY;

/**
 * Created by jose on 8/28/17.
 */

public class AppMonetBidder {
  private static final Logger sLogger = new Logger("Bdr");
  private final Map<String, AdServerAdView> mAdViews;
  private final Map<String, AuctionRequest> mExtantExtras;
  private final Handler mMainHandler;
  private final AdServerWrapper mAdServerWrapper;
  private final BackgroundThread backgroundThread;
  private final BaseManager sdkManager;
  private final BidManager bidManager;
  private final AuctionManagerCallback auctionManagerCallback;

  public AppMonetBidder(Context context, @NonNull BaseManager sdkManager, BidManager bidManager,
      AdServerWrapper adServerWrapper, AuctionManagerCallback auctionManagerCallback,
      BackgroundThread backgroundThread) {
    this.sdkManager = sdkManager;
    this.bidManager = bidManager;
    this.auctionManagerCallback = auctionManagerCallback;
    mAdViews = new HashMap<>();
    mExtantExtras = new HashMap<>();
    mMainHandler = new Handler(context.getMainLooper());
    mAdServerWrapper = adServerWrapper;
    this.backgroundThread = backgroundThread;
  }

  /**
   * Add header bids to the given PublisherAdRequest. This sets customTargeting
   * on the ad request, which will target line items set up in DFP.
   *
   * @param adView PublisherAdView instance that will be loading the ad request
   * @param adRequest PublisherAdRequest for this adUnit
   * @return a new PublisherAdRequest with the custom targeting required for header bidding
   */
  public AdServerAdRequest addBids(AdServerAdView adView, AdServerAdRequest adRequest) {
    try {
      return addBidsToPublisherAdRequest(adView, adRequest);
    } catch (Exception e) {
      return adRequest;
    }
  }

  protected AdServerAdRequest addBids(AdServerAdView adView, AdServerAdRequest adRequest,
      String adUnitId) {
    adView.setAdUnitId(adUnitId);
    return addBids(adView, adRequest);
  }

  /**
   * Add header bids to the given PublisherAdRequest, blocking for the given timeout to allow
   * AppMonetBidder time to fetch bids if no bids are present in the bid cache.
   *
   * @param adView PublisherAdView that will load the PublisherAdRequest
   * @param adRequest PublisherAdRequest request instance for the given adView
   * @param timeout int milliseconds to wait for a bid from AppMonetBidder bidder
   * @param onDone ValueCallback to receiev the request with bids attached
   */
  public void addBids(AdServerAdView adView, AdServerAdRequest adRequest, int timeout,
      ValueCallback<AdServerAdRequest> onDone) {
    try {
      addBidsToPublisherAdRequest(adView, adRequest, timeout, onDone);
    } catch (Exception e) {
      onDone.onReceiveValue(adRequest);
    }
  }

  protected void addBids(AdServerAdView adView, AdServerAdRequest adRequest, String adUnitId,
      int timeout, ValueCallback<AdServerAdRequest> onDone) {
    adView.setAdUnitId(adUnitId);
    addBids(adView, adRequest, timeout, onDone);
  }

  private AdServerAdRequest addBidsToPublisherAdRequest(AdServerAdView adView,
      AdServerAdRequest otherRequest) {
    // fetch a bid from our backend & attach it's KVPs to the
    // request
    if (isTestMode) {
      sLogger.warn(TEST_MODE_WARNING);
    }
    registerView(adView, otherRequest);
    AuctionRequest request = attachBid(adView, otherRequest);
    auctionManagerCallback.trackRequest(adView.getAdUnitId(), "addBids");

    if (request == null) {
      addBidsNoFill(adView.getAdUnitId());
      sLogger.debug("no bid received");
      return otherRequest;
    }

    return buildRequest(request, adView.getType());
  }

  private void addBidsToPublisherAdRequest(final AdServerAdView adView,
      final AdServerAdRequest otherRequest, final int timeout,
      final ValueCallback<AdServerAdRequest> onDone) {
    registerView(adView, otherRequest);
    if (isTestMode) {
      sLogger.warn(TEST_MODE_WARNING);
    }
    backgroundThread.execute(new InternalRunnable() {
      @Override
      public void runInternal() {
        attachBidAsync(adView, otherRequest, timeout,
            value -> mMainHandler.post(new InternalRunnable() {
              @Override
              public void runInternal() {
                auctionManagerCallback.trackRequest(adView.getAdUnitId(), "addBidsAsync");

                if (value == null) {
                  sLogger.info("no bid returned from js");
                  addBidsNoFill(adView.getAdUnitId());
                  onDone.onReceiveValue(otherRequest);
                  return;
                }

                AdServerAdRequest newRequest = buildRequest(value, adView.getType());
                sLogger.debug("passing bid to main thread");
                if (newRequest == null) {
                  addBidsNoFill(adView.getAdUnitId());
                }
                onDone.onReceiveValue(newRequest);
              }

              @Override
              public void catchException(Exception e) {
                onDone.onReceiveValue(otherRequest);
              }
            }));
      }

      @Override
      public void catchException(Exception e) {
        onDone.onReceiveValue(otherRequest);
      }
    });
  }

  public void cancelRequest(String adUnitId, AdServerAdRequest adRequest, BidResponse bid) {
    if (adUnitId == null || adRequest == null) {
      return;
    }
    if (!mAdViews.containsKey(adUnitId)) {
      return;
    }

    AdServerAdView adView = mAdViews.get(adUnitId);
    if (adView == null) {
      sLogger.warn("could not associate adview for next request");
      return;
    }

    // the request that we get has minimal targeting.
    // we need to create a new request from merging that
    // with what we have in sExtantExtras
    AuctionRequest extant = mExtantExtras.get(adUnitId);
    extant = extant != null ? extant : AuctionRequest.Companion.from(adView, adRequest);
    AdServerAdRequest request =
        mAdServerWrapper.newAdRequest(adRequest.apply(extant, adView));

    auctionManagerCallback.trackRequest(adUnitId, "addBidRefresh");

    if (bid != null) {
      sLogger.info("attaching next bid", bid.toString());

      AuctionRequest req = addRawBid(adView, request, bid);
      adView.loadAd(buildRequest(req, adView.getType()));
    } else {
      sLogger.debug("passing request");
      adView.loadAd(request);
    }
  }

  /**
   * Given a JSON response from our AuctionManager WebView, retrieve the bids we have for that
   * adunit and attach them to the given adView/adRequest
   *
   * @param adView the adView where the ad will be rendered
   * @param adRequest the request for ad impressions
   * @return an AuctionRequest with the demand attached
   */
  AuctionRequest attachBidResponse(AdServerAdView adView, AdServerAdRequest adRequest) {
    BidResponse bid = bidManager.getBidForAdUnit(adView.getAdUnitId());
    return attachBid(adView, adRequest, bid);
  }

  /**
   * Add a bid to the given request/AdView pair.
   *
   * @param adView the view the bid will be rendered in
   * @param adRequest the request for an ad
   * @return an AuctionRequest representing the bid & the initial request (adRequest)
   */
  private AuctionRequest attachBid(AdServerAdView adView, AdServerAdRequest adRequest) {
    BidResponse bidResponse = !bidManager.needsNewBids(adView, adRequest) ? adRequest.getBid()
        : bidManager.getLocalBid(adView.getAdUnitId());
    RequestData data = new RequestData(adRequest, adView);
    if (bidResponse != null) {
      sLogger.debug("(sync) attaching bids to request");
      sLogger.debug("\t[sync/request] attaching:$bidResponse");
      return attachBid(adView, adRequest, bidResponse);
    }
    auctionManagerCallback.executeJs(Constants.JSMethods.FETCH_BIDS,
        WebViewUtils.quote(adView.getAdUnitId()), data.toJson());
    return attachBidResponse(adView, adRequest);
  }

  private AuctionRequest attachBid(AdServerAdView adView, AdServerAdRequest adRequest,
      BidResponse bidResponse) {
    if (bidResponse == null) {
      return null;
    }
    AuctionRequest auctionRequest = AuctionRequest.Companion.from(adView, adRequest);
    List<String> kwStrings = new ArrayList();
    kwStrings.add(bidResponse.getKeyWords());
    attachBidToNetworkExtras(auctionRequest.getNetworkExtras(), bidResponse);
    auctionRequest.setBid(bidResponse);
    Bundle kwTargeting = keywordStringToBundle(TextUtils.join(",", kwStrings));
    auctionRequest.getTargeting().putAll(kwTargeting);
    return auctionRequest;
  }

  /**
   * Convert a formatted string into a bundle (of string:string mappings). The input string should
   * be in this format:
   *
   *
   * key1:value,key2:value2,key3:value3
   *
   * @param kwString a string of keywords in the expected format
   * @return a Bundle with string keys & String values
   */
  private Bundle keywordStringToBundle(String kwString) {
    Bundle bundle = new Bundle();
    for (String kvp : TextUtils.split(kwString, ",")) {
      String[] pair = TextUtils.split(kvp, ":");
      if (pair.length != 2) {
        continue;
      }
      bundle.putString(pair[0], pair[1]);
    }
    return bundle;
  }

  /**
   * Attach the bid to the network Extras bundle, eg the bundle that will be passed in the
   * customEventBanner.
   *
   * @param bundle the Bundle the bid should be attached to
   * @param bid the BidResponse to be passed into the adserver.
   */
  private void attachBidToNetworkExtras(Bundle bundle, BidResponse bid) {
    if (bid == null || bundle == null) {
      return;
    }
    bundle.putSerializable(BID_BUNDLE_KEY, bid);
  }

  private void attachBidAsync(AdServerAdView adView, AdServerAdRequest adRequest, int timeout,
      ValueCallback<AuctionRequest> callback) {
    String adUnitId = adView.getAdUnitId();
    RequestData data = new RequestData(adRequest, adView);
    if (!bidManager.needsNewBids(adView, adRequest)) {
      sLogger.debug("keeping current bids");
      callback.onReceiveValue(attachBid(adView, adRequest, adRequest.getBid()));
      return;
    }
    SdkConfigurations config = auctionManagerCallback.getSdkConfigurations();
    if (config.getBoolean(Constants.Configurations.SKIP_FETCH)
        && bidManager.hasLocalBids(adView.getAdUnitId())) {
      sLogger.debug("Skipping fetch wait (latency reduction)");
      BidResponse bid = bidManager.getLocalBid(adView.getAdUnitId());
      callback.onReceiveValue(attachBid(adView, adRequest, bid));
    } else {
      // get the timeout
      int realTimeout = resolveTimeout(config, timeout);
      auctionManagerCallback.executeJs(realTimeout, Constants.JSMethods.FETCH_BIDS_BLOCKING,
          s -> {
            BidResponse bid = bidManager.getLocalBid(adView.getAdUnitId());
            if (bid != null) {
              sLogger.debug("attaching bids to request");
            }
            callback.onReceiveValue(attachBid(adView, adRequest, bid));
          },
          WebViewUtils.quote(adUnitId), Integer.toString(timeout),
          data.toJson(), "'addBids'"
      );
    }
  }

  /**
   * Get a raw bid available for this adunit
   */
  BidResponse fetchRawBid(String adUnitId) {
    return bidManager.getLocalBid(adUnitId);
  }

  private int resolveTimeout(SdkConfigurations config, int timeout) {
    int realTimeout =
        (config != null && config.hasKey(Constants.Configurations.FETCH_TIMEOUT_OVERRIDE)) ?
            config.getInt(Constants.Configurations.FETCH_TIMEOUT_OVERRIDE) : timeout;
    return (realTimeout <= 0) ? timeout : realTimeout;
  }

  private void registerView(AdServerAdView adView, AdServerAdRequest request) {
    if (adView == null) {
      return;
    }

    String adUnitId = adView.getAdUnitId();
    mAdViews.put(adUnitId, adView);

    if (request == null) {
      return;
    }

    mExtantExtras.put(adUnitId, AuctionRequest.Companion.from(adView, request));
  }

  private AuctionRequest addRawBid(AdServerAdView adView, AdServerAdRequest baseRequest,
      BidResponse bid) {
    return attachBid(adView, baseRequest, bid);
  }

  private void addBidsNoFill(String adUnitId) {
    auctionManagerCallback.trackEvent("addbids_nofill", "null",
        adUnitId, 0f, System.currentTimeMillis());
  }

  private AdServerAdRequest buildRequest(AuctionRequest req, AdServerWrapper.Type type) {
    return mAdServerWrapper.newAdRequest(req, type);
  }
}
