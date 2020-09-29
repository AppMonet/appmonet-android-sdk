package com.monet.app

import com.monet.bidder.AppMonetAdSize
import com.monet.bidder.AppMonetErrorCode
import com.monet.bidder.AppMonetInterstitial
import com.monet.bidder.AppMonetView
import kotlinx.android.synthetic.appmonet.adview_layout.appMonetView
import kotlinx.android.synthetic.main.activity_main.loadInterstitial
import kotlinx.android.synthetic.main.activity_main.loadMrect
import kotlinx.android.synthetic.main.activity_main.showInterstitial

class MainActivity : BaseActivity() {
  private lateinit var interstitial: AppMonetInterstitial

  override fun setupMrect() {
    appMonetView.adUnitId = "b03e6dccfe9e4abab02470a39c88d5dc"
    appMonetView.setAdSize(AppMonetAdSize(300, 250))
    appMonetView.bannerAdListener = object : AppMonetView.BannerAdListener {
      override fun onBannerLoaded(banner: AppMonetView?) {
        showToast("Banner Ad Loaded")
      }

      override fun onBannerFailed(
        banner: AppMonetView?,
        error: AppMonetErrorCode?
      ) {
        showToast("Banner Failed to Load")
      }

      override fun onBannerClicked(banner: AppMonetView?) {
        showToast("Banner Clicked")
      }
    }
  }

  override fun setupInterstitial() {
    interstitial = AppMonetInterstitial(this, "a49430ee57ee4401a9eda6098726ce54")
    interstitial.interstitialAdListener = object : AppMonetInterstitial.InterstitialAdListener {
      override fun onInterstitialLoaded(interstitial: AppMonetInterstitial?) {
        showToast("Interstitial Loaded")
      }

      override fun onInterstitialShown(interstitial: AppMonetInterstitial?) {
        showToast("Interstitial Shown")
      }

      override fun onInterstitialFailed(
        interstitial: AppMonetInterstitial?,
        errorCode: AppMonetErrorCode?
      ) {
        showToast("Interstitial Failed")
        interstitial?.destroy()
      }

      override fun onInterstitialDismissed(interstitial: AppMonetInterstitial?) {
        showToast("Interstitial Dismissed")
        interstitial?.destroy()
      }

      override fun onInterstitialClicked(interstitial: AppMonetInterstitial?) {
        showToast("Interstitial Clicked")
      }
    }
  }

  /**
   * Listener on mrect button that will trigger AppMonet's addBids method.
   */
  override fun setupMrectLoadClickListener() {
    loadMrect.setOnClickListener {
      appMonetView.loadAd()
    }
  }

  /**
   * Clean up
   */
  override fun onDestroy() {
    appMonetView.destroy()
    interstitial.destroy()
    super.onDestroy()
  }

  override fun setupInterstitialLoadClickListener() {
    loadInterstitial.setOnClickListener {
      setupInterstitial()
      interstitial.load()
    }
  }

  override fun setupInterstitialShowClickListener() {
    showInterstitial.setOnClickListener {
      if (interstitial.isReady) {
        interstitial.show()
      } else {
        showToast("Interstitial Is Not Ready")
      }
    }
  }
}
