package com.monet.bidder

import android.content.ActivityNotFoundException
import android.content.Context
import android.webkit.ValueCallback
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdRequest.Builder
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.doubleclick.PublisherAdRequest
import com.google.android.gms.ads.doubleclick.PublisherAdView
import com.google.android.gms.ads.doubleclick.PublisherInterstitialAd
import com.monet.bidder.Constants.MISSING_INIT_ERROR_MESSAGE

/**
 * Created by jose on 8/28/17.
 */
internal class SdkManager constructor(
  context: Context,
  applicationId: String?
) : BaseManager(
    context, applicationId, DFPAdServerWrapper()
) {
  var isPublisherAdView = true
  fun addBids(
    adView: AdView?,
    adRequest: AdRequest?,
    appMonetAdUnitId: String?,
    timeout: Int,
    onDone: ValueCallback<AdRequest?>
  ) {
    auctionManager.timedCallback(timeout, object : TimedCallback {
      override fun execute(remainingTime: Int) {
        logState()
        isPublisherAdView = false
        val dfpAdView = DFPAdView(adView)
        dfpAdView.adUnitId = appMonetAdUnitId!!
        auctionManager.addBids(dfpAdView, DFPAdViewRequest(adRequest!!), remainingTime,
            ValueCallback { value ->
              if (value == null) {
                onDone.onReceiveValue(adRequest)
                return@ValueCallback
              }
              onDone.onReceiveValue((value as DFPAdViewRequest).dfpRequest)
            }
        )
      }

      override fun timeout() {
        trackTimeoutEvent(appMonetAdUnitId, timeout.toFloat())
        onDone.onReceiveValue(
            adRequest ?: Builder().build()
        )
      }
    })
  }

  fun addBids(
    adView: PublisherAdView?,
    adRequest: PublisherAdRequest,
    appMonetAdUnitId: String?,
    timeout: Int,
    onDone: ValueCallback<PublisherAdRequest>
  ) {
    auctionManager.timedCallback(timeout, object : TimedCallback {
      override fun execute(remainingTime: Int) {
        val dfpPublisherAdView = DFPPublisherAdView(adView!!)
        dfpPublisherAdView.adUnitId = appMonetAdUnitId!!
        val addBidsParams = generateAddBidsParams(
            dfpPublisherAdView,
            adRequest, remainingTime, onDone
        )
        onBidManagerReadyCallback(addBidsParams)
      }

      override fun timeout() {
        trackTimeoutEvent(appMonetAdUnitId, timeout.toFloat())
        onDone.onReceiveValue(getPublisherAdRequest(adRequest))
      }
    })
  }

  fun addBids(
    interstitialAd: PublisherInterstitialAd?,
    adRequest: PublisherAdRequest,
    appMonetAdUnitId: String?,
    timeout: Int,
    onDone: ValueCallback<PublisherAdRequest>
  ) {
    auctionManager.timedCallback(timeout, object : TimedCallback {
      override fun execute(remainingTime: Int) {
        val ctx = context.get()
        if (ctx == null) {
          sLogger.warn("failed to bind context. Returning")
          onDone.onReceiveValue(adRequest)
          return
        }
        if (!isInterstitialActivityRegistered(ctx, MonetDfpActivity::class.java.name)) {
          val error = """
                    Unable to create activity. Not defined in AndroidManifest.xml. Please refer to https://docs.appmonet.com/ for integration infomration.

                    """.trimIndent()
          sLogger.error(error)
          auctionManager.trackEvent(
              "integration_error",
              "missing_interstitial_activity", appMonetAdUnitId!!, 0f, 0L
          )
          throw ActivityNotFoundException(error)
        }
        val dfpPublisherInterstitialAdView = DFPPublisherInterstitialAdView(interstitialAd)
        dfpPublisherInterstitialAdView.adUnitId = appMonetAdUnitId!!
        val addBidsParams = generateAddBidsParams(
            dfpPublisherInterstitialAdView,
            adRequest, remainingTime, onDone
        )
        onBidManagerReadyCallback(addBidsParams)
      }

      override fun timeout() {
        trackTimeoutEvent(appMonetAdUnitId, timeout.toFloat())
        onDone.onReceiveValue(getPublisherAdRequest(adRequest))
      }
    })
  }

  fun addBids(
    interstitialAd: InterstitialAd?,
    adRequest: AdRequest?,
    appMonetAdUnitId: String?,
    timeout: Int,
    onDone: ValueCallback<AdRequest?>
  ) {
    auctionManager.timedCallback(timeout, object : TimedCallback {
      override fun execute(remainingTime: Int) {
        logState()
        isPublisherAdView = false
        val dfpInterstitialAdView = DFPInterstitialAdView(interstitialAd)
        dfpInterstitialAdView.adUnitId = appMonetAdUnitId!!
        auctionManager.addBids(dfpInterstitialAdView, DFPAdViewRequest(adRequest!!),
            remainingTime,
            ValueCallback { value ->
              if (value == null) {
                sLogger.debug("value is null")
                onDone.onReceiveValue(adRequest)
                return@ValueCallback
              }
              sLogger.debug("value is valid")
              onDone.onReceiveValue((value as DFPAdViewRequest).dfpRequest)
            }
        )
      }

      override fun timeout() {
        trackTimeoutEvent(appMonetAdUnitId, timeout.toFloat())
        onDone.onReceiveValue(
            adRequest ?: Builder().build()
        )
      }
    })
  }

  fun addBids(
    adRequest: PublisherAdRequest,
    appMonetAdUnitId: String,
    timeout: Int,
    onDone: ValueCallback<PublisherAdRequest>
  ) {
    auctionManager.timedCallback(timeout, object : TimedCallback {
      override fun execute(remainingTime: Int) {
        val ctx = context.get()
        if (ctx == null) {
          sLogger.warn("failed to bind context. Returning")
          onDone.onReceiveValue(adRequest)
          return
        }
        val dfpPublisherAdView = DFPPublisherAdView(appMonetAdUnitId)
        val addBidsParams = generateAddBidsParams(
            dfpPublisherAdView,
            adRequest, remainingTime, onDone
        )
        onBidManagerReadyCallback(addBidsParams)
      }

      override fun timeout() {
        trackTimeoutEvent(appMonetAdUnitId, timeout.toFloat())
        onDone.onReceiveValue(getPublisherAdRequest(adRequest))
      }
    })
  }

  fun addBids(
    adView: PublisherAdView,
    adRequest: PublisherAdRequest?,
    appMonetAdUnitId: String
  ): PublisherAdRequest {
    val dfpPublisherAdView = DFPPublisherAdView(adView)
    dfpPublisherAdView.adUnitId = appMonetAdUnitId
    val localAdRequest = adRequest ?: PublisherAdRequest.Builder().build()
    val request = auctionManager.addBids(
        dfpPublisherAdView, DFPAdRequest(localAdRequest)
    ) as DFPAdRequest

    // if the request is null, just pass through the original
    return request.dfpRequest
  }

  fun addBids(
    adRequest: PublisherAdRequest?,
    appMonetAdUnitId: String
  ): PublisherAdRequest {
    val localAdRequest = adRequest ?: PublisherAdRequest.Builder().build()
    val dfpPublisherAdView = DFPPublisherAdView(appMonetAdUnitId)
    val request = auctionManager.addBids(
        dfpPublisherAdView, DFPAdRequest(localAdRequest)
    ) as DFPAdRequest
    return request.dfpRequest
  }

  private fun generateAddBidsParams(
    adServerAdView: AdServerAdView,
    adRequest: PublisherAdRequest,
    timeout: Int,
    onDone: ValueCallback<PublisherAdRequest>
  ): AddBidsParams {
    return AddBidsParams(adServerAdView,
        DFPAdRequest(getPublisherAdRequest(adRequest)),
        timeout, ValueCallback { adServerAdRequest: AdServerAdRequest? ->
      if (adServerAdRequest == null) {
        sLogger.debug("value null")
        onDone.onReceiveValue(adRequest)
        return@ValueCallback
      }
      sLogger.debug("value is valid")
      onDone.onReceiveValue((adServerAdRequest as DFPAdRequest).dfpRequest)
    })
  }

  private fun onBidManagerReadyCallback(addBidsParams: AddBidsParams) {
    auctionManager.addBids(
        addBidsParams.adView, addBidsParams.request,
        addBidsParams.timeout, addBidsParams.callback
    )
  }

  private fun getPublisherAdRequest(adRequest: PublisherAdRequest?): PublisherAdRequest {
    return adRequest ?: PublisherAdRequest.Builder().build()
  }

  companion object {
    private val sLogger = Logger("SdkManager")
    private val LOCK = Any()
    private var sInstance: SdkManager? = null
    @JvmStatic fun initialize(
      context: Context,
      appMonetConfiguration: AppMonetConfiguration
    ) {
      try {
        synchronized(LOCK) {
          if (sInstance != null) {
            sLogger.warn("Sdk has already been initialized. No need to initialize it again.")
            return
          }
          sInstance = SdkManager(context.applicationContext, appMonetConfiguration.applicationId)
          (sInstance!!.adServerWrapper as DFPAdServerWrapper).setSdkManager(sInstance)
        }
      } catch (e: Exception) {
        if (initRetry < 3) {
          sLogger.error(
              "error initializing ... retrying $e"
          )
          initRetry += 1
          initialize(context, appMonetConfiguration)
        }
      }
    }

    @JvmStatic fun get(): SdkManager? {
      synchronized(LOCK) {
        if (sInstance == null) {
          sLogger.error(MISSING_INIT_ERROR_MESSAGE)
        }
        return sInstance
      }
    }
  }
}