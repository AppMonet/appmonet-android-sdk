package com.monet.app

import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.monet.bidder.AppMonet
import kotlinx.android.synthetic.admob.adview_layout.adView
import kotlinx.android.synthetic.main.activity_main.loadInterstitial
import kotlinx.android.synthetic.main.activity_main.loadMrect
import kotlinx.android.synthetic.main.activity_main.showInterstitial

class MainActivity : BaseActivity() {
  private lateinit var interstitial: InterstitialAd

  override fun setupMrect() {
    adView.adListener = object : AdListener() {
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
    interstitial = InterstitialAd(this)
    interstitial.adUnitId = "ca-app-pub-2737306441907340/2005444833"
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
      val adRequest = AdRequest.Builder().build()
      AppMonet.addBids(
          adView, adRequest, 1500
      ) { customRequest ->
        adView.loadAd(customRequest)
      }
    }
  }

  override fun setupInterstitialLoadClickListener() {
    loadInterstitial.setOnClickListener {
      val adRequest = AdRequest.Builder().build()
      interstitial.loadAd(adRequest)
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
