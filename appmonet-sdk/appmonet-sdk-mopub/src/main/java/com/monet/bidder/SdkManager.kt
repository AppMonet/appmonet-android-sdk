package com.monet.bidder

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.webkit.ValueCallback
import com.monet.bidder.AppMonetConfiguration.Builder
import com.monet.bidder.Constants.Configurations.MEDIATION_ENABLED
import com.monet.bidder.Constants.MISSING_INIT_ERROR_MESSAGE
import com.monet.bidder.threading.InternalRunnable
import com.mopub.mobileads.MoPubInterstitial
import com.mopub.mobileads.MoPubView
import com.mopub.nativeads.MoPubNative
import com.mopub.nativeads.RequestParameters
import java.lang.ref.WeakReference
import java.util.HashMap

/**
 * Created by jose on 8/28/17.
 */
internal class SdkManager constructor(
  context: Context?,
  applicationId: String?
) : BaseManager(
    context!!, applicationId, MoPubAdServerWrapper()
) {
  private val mopubAdViews: MutableMap<String, WeakReference<MoPubView>> = HashMap()
  private val positions: MutableMap<String, AppMonetFloatingAdConfiguration> = HashMap()
  var currentActivity: WeakReference<Activity>? = null
  fun getMopubAdView(adUnitId: String?): MoPubView? {
    val mopubAdView = mopubAdViews[adUnitId]
    return mopubAdView?.get()
  }

  fun getFloatingAdPosition(adUnitId: String?): AppMonetFloatingAdConfiguration? {
    return positions[adUnitId]
  }

  fun addBids(
    adView: MoPubView?,
    adUnitId: String
  ): MoPubView? {
    logState()
    if (adView == null) {
      sLogger.warn("attempt to add bids to nonexistent AdView")
      return null
    }
    if (adView.getAdUnitId() == null) {
      sLogger.warn("Mopub adunit id is null. Unable to fetch bids for unit")
      return adView
    }
    val config = sdkConfigurations
    if (config.getBoolean(MEDIATION_ENABLED)) {
      sLogger.debug("Mediation mode is enabled. Ignoring explicit addBids()")
      return adView
    }
    val mpView = MoPubAdView(adView)
    if (adUnitId != adView.getAdUnitId()) {
      mpView.adUnitId = adUnitId
    }
    val baseRequest: AdServerAdRequest
    registerView(adView, adUnitId)
    val request: AdServerAdRequest
    try {
      baseRequest = MoPubAdRequest(adView)
      request = auctionManager.addBids(mpView, baseRequest)
    } catch (exp: NullPointerException) {
      return null
    }
    if (request != null) {
      if (request.hasBid()) {
        sLogger.info("found bids for view. attaching")
      } else {
        sLogger.debug("no bids available for request.")
      }
      (request as MoPubAdRequest).applyToView(mpView)
    }
    mopubAdViews[mpView.adUnitId] = WeakReference(adView)
    return mpView.getMoPubView()
  }

  fun addBids(
    moPubInterstitial: MoPubInterstitial,
    alternateAdUnitId: String?,
    timeout: Int,
    onDone: ValueCallback<MoPubInterstitial?>
  ) {
    auctionManager.timedCallback(timeout, object : TimedCallback {
      override fun execute(remainingTime: Int) {
        if (!isInterstitialActivityRegistered(
                moPubInterstitial.activity.applicationContext,
                MonetActivity::class.java.name
            )
        ) {
          val error = """
                    Unable to create activity. Not defined in AndroidManifest.xml. Please refer to https://docs.appmonet.com/ for integration infomration.

                    """.trimIndent()
          sLogger.error(error)
          //todo refactor this.
          auctionManager.auctionWebView.trackEvent(
              "integration_error",
              "missing_interstitial_activity", alternateAdUnitId!!, 0f, 0L
          )
          throw ActivityNotFoundException(error)
        }
        logState()
        // todo -> do we need this?
        //        if (appMonetBidder == null) {
        //          onDone.onReceiveValue(moPubInterstitial);
        //          return;
        //        }
        val mpView = MoPubInterstitialAdView(moPubInterstitial, alternateAdUnitId!!)
        auctionManager.addBids(mpView, MoPubInterstitialAdRequest(moPubInterstitial),
            remainingTime,
            ValueCallback { value: AdServerAdRequest ->
              // apply the request to the ad view, and pass that back
              val request = value as MoPubInterstitialAdRequest
              request.applyToView(mpView)
              onDone.onReceiveValue(moPubInterstitial)
            }
        )
      }

      override fun timeout() {
        trackTimeoutEvent(alternateAdUnitId, timeout.toFloat())
        onDone.onReceiveValue(moPubInterstitial)
      }
    })
  }

  fun addBids(
    adView: MoPubView,
    alternateAdUnitId: String?,
    timeout: Int,
    onDone: ValueCallback<MoPubView?>
  ) {
    auctionManager.timedCallback(timeout, object : TimedCallback {
      override fun execute(remainingTime: Int) {
        logState()
        val mpView = MoPubAdView(adView)
        if (adView.getAdUnitId() == null) {
          sLogger.warn("Mopub adunit id is null. Unable to fetch bids for unit")
          onDone.onReceiveValue(adView) // can't continue
          return
        }
        val adUnit = alternateAdUnitId ?: adView.getAdUnitId()
        if (adView.getAdUnitId() != adUnit) {
          mpView.adUnitId = adUnit!!
        }
        registerView(adView, adUnit)
        auctionManager.addBids(mpView, MoPubAdRequest(adView), remainingTime,
            ValueCallback { value: AdServerAdRequest ->
              // apply the request to the ad view, and pass that back
              val request = value as MoPubAdRequest
              request.applyToView(mpView)
              mopubAdViews[mpView.adUnitId] = WeakReference(adView)
              onDone.onReceiveValue(adView)
            }
        )
      }

      override fun timeout() {
        val adUnit = alternateAdUnitId ?: adView.getAdUnitId()
        trackTimeoutEvent(adUnit, timeout.toFloat())
        onDone.onReceiveValue(adView)
      }
    })
  }

  fun addBids(
    nativeAd: MoPubNative?,
    requestParameters: RequestParameters?,
    adUnitId: String?,
    timeout: Int,
    onDone: ValueCallback<NativeAddBidsResponse?>
  ) {
    auctionManager.timedCallback(timeout, object : TimedCallback {
      override fun execute(remainingTime: Int) {
        logState()
        val mpView = MopubNativeAdView(nativeAd!!, adUnitId!!)
        auctionManager.addBids(mpView,
            MopubNativeAdRequest(nativeAd, adUnitId, requestParameters), remainingTime,
            ValueCallback { value: AdServerAdRequest ->
              // apply the request to the ad view, and pass that back
              val request = value as MopubNativeAdRequest
              request.applyToView(mpView)
              onDone.onReceiveValue(
                  NativeAddBidsResponse(nativeAd, request.modifiedRequestParameters)
              )
            }
        )
      }

      override fun timeout() {
        trackTimeoutEvent(adUnitId, timeout.toFloat())
        onDone.onReceiveValue(NativeAddBidsResponse(nativeAd!!, requestParameters!!))
      }
    })
  }

  fun registerFloatingAd(
    activity: Activity,
    adConfiguration: AppMonetFloatingAdConfiguration,
    moPubView: MoPubView?
  ) {
    if (moPubView != null) {
      mopubAdViews[adConfiguration.adUnitId] = WeakReference(moPubView)
    }
    positions[adConfiguration.adUnitId] = adConfiguration
    currentActivity = WeakReference(activity)
    auctionManager.registerFloatingAd(adConfiguration.adUnitId, adConfiguration.toJson().toString())
  }

  fun unregisterFloatingAd(
    activity: Activity?,
    moPubView: MoPubView?
  ) {
    if (moPubView != null) {
      mopubAdViews.remove(moPubView.getAdUnitId())
    }
    currentActivity = null
  }

  private fun registerView(
    adView: MoPubView?,
    adUnitIdAlias: String?
  ) {
    if (adView == null) return
    val adUnitId = adUnitIdAlias ?: adView.getAdUnitId()
    if (adUnitId == null) {
      sLogger.warn("adView id is null! Cannot register view")
      return
    }
    val extant = adView.bannerAdListener
    synchronized(LOCK) {
      if (extant !is MopubBannerAdListener &&
          !appMonetConfiguration!!.disableBannerListener && adView.getAdUnitId() != null
      ) {
        sLogger.debug(
            "registering view with internal listener: $adUnitId"
        )
        adView.bannerAdListener = MopubBannerAdListener(adUnitId, extant, this)
      }
    }
  }

  companion object {
    private val sLogger = Logger("SdkManager")
    private val LOCK = Any()
    private const val RE_INIT_DELAY = 1000
    private var sInstance: SdkManager? = null
    var appMonetConfiguration: AppMonetConfiguration? = null
    @JvmStatic fun initializeThreaded(
      context: Context,
      appMonetConfiguration: AppMonetConfiguration?
    ) {
      val handlerThread = HandlerThread("monet-handler", Process.THREAD_PRIORITY_BACKGROUND)
      handlerThread.start()
      val looper = handlerThread.looper
      val handler = Handler(looper)
      handler.post(object : InternalRunnable() {
        override fun runInternal() {
          initialize(
              context,
              appMonetConfiguration ?: Builder().build()
          )
        }

        override fun catchException(e: Exception?) {
          sLogger.error("failed to initialize AppMonet SDK: " + e!!.localizedMessage)
        }
      })
    }

    @SuppressLint("DefaultLocale") fun initialize(
      context: Context,
      appMonetConfiguration: AppMonetConfiguration
    ) {
      val currentVersion = VERSION.SDK_INT
      if (currentVersion < VERSION_CODES.JELLY_BEAN) {
        sLogger.warn(
            String.format(
                "Warning! Sdk v%d is not supported. AppMonet SDK Disabled",
                currentVersion
            )
        )
        return
      }
      try {
        synchronized(LOCK) {
          if (sInstance != null) {
            sLogger.debug("Sdk has already been initialized. No need to initialize it again.")
            return
          }
          Companion.appMonetConfiguration = appMonetConfiguration
          sInstance = SdkManager(context.applicationContext, appMonetConfiguration.applicationId)
        }
      } catch (e: Exception) {
        if (initRetry < 3) {
          sLogger.error(
              "error initializing ... retrying $e"
          )
          initRetry += 1
          val handler = Handler(context.mainLooper)
          handler.postDelayed(object : InternalRunnable() {
            override fun runInternal() {
              initialize(context, appMonetConfiguration)
            }

            override fun catchException(e: Exception?) {
              sLogger.error("Error re-init @ context", e!!.message)
            }
          }, RE_INIT_DELAY.toLong())
        }
      }
    }

    fun get(): SdkManager? {
      return Companion[""]
    }

    @JvmStatic operator fun get(reason: String?): SdkManager? {
      synchronized(LOCK) {
        if (sInstance == null) {
          sLogger.debug(String.format(MISSING_INIT_ERROR_MESSAGE, reason))
        }
        return sInstance
      }
    }
  }
}