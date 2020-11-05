package com.monet.app

import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.doubleclick.PublisherAdRequest
import com.google.android.gms.ads.doubleclick.PublisherInterstitialAd
import com.monet.ValueCallback
import com.monet.bidder.AppMonet
import kotlinx.android.synthetic.dfp.adview_layout.publisherAdView
import kotlinx.android.synthetic.main.activity_main.loadInterstitial
import kotlinx.android.synthetic.main.activity_main.loadMrect
import kotlinx.android.synthetic.main.activity_main.showInterstitial

class MainActivity : BaseActivity() {
  private lateinit var interstitial: PublisherInterstitialAd

  override fun setupMrect() {
    publisherAdView.adListener = object : AdListener() {
      override fun onAdLoaded() {
        showToast("Banner Loaded")
      }

      override fun onAdFailedToLoad(errorCode: Int) {
        showToast("Banner Failed")
      }

      override fun onAdClicked() {
        showToast("Banner Clicked")
      }
    }
  }

  override fun setupInterstitial() {
    interstitial = PublisherInterstitialAd(this)
    interstitial.adUnitId = BuildConfig.INTERSTITIAL_AD_UNIT_ID
    interstitial.adListener = object : AdListener() {
      override fun onAdLoaded() {
        showToast("Interstitial Loaded")
      }

      override fun onAdOpened() {
        showToast("Interstitial Shown")
      }

      override fun onAdFailedToLoad(errorCode: Int) {
        showToast("Interstitial Failed")
      }

      override fun onAdClosed() {
        showToast("Interstitial Dismissed")
      }

      override fun onAdClicked() {
        showToast("Interstitial Clicked")
      }
    }
  }

  /**
   * Listener on mrect button that will trigger AppMonet's addBids method.
   */
  override fun setupMrectLoadClickListener() {
    loadMrect.setOnClickListener {
      val adRequest = PublisherAdRequest.Builder().build()
      AppMonet.addBids(
          publisherAdView, adRequest, 1500
      ) { publisherAdRequest ->
        publisherAdView.loadAd(publisherAdRequest)
      }
    }
  }

  override fun setupInterstitialLoadClickListener() {
    loadInterstitial.setOnClickListener {
      val adRequest = PublisherAdRequest.Builder().build()
      AppMonet.addBids(interstitial, adRequest, 2000, ValueCallback {
        interstitial.loadAd(it)
      })
    }
  }

  override fun setupInterstitialShowClickListener() {
    showInterstitial.setOnClickListener {
      if (interstitial.isLoaded) {
        interstitial.show()
      } else {
        showToast("Interstitial Is Not Ready")
      }
    }
  }
}
