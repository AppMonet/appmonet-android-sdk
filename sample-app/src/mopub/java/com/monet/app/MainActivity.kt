package com.monet.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import com.monet.bidder.AppMonet
import com.mopub.mobileads.MoPubErrorCode
import com.mopub.mobileads.MoPubInterstitial
import com.mopub.mobileads.MoPubView
import com.monet.ValueCallback

class MainActivity : BaseActivity() {
  private lateinit var interstitial: MoPubInterstitial
  override fun onCreate(savedInstanceState: Bundle?) {
    WebView.setWebContentsDebuggingEnabled(true)
    super.onCreate(savedInstanceState)
    binding.nativeContainer.visibility = View.VISIBLE
  }

  override fun setupMrect() {
    adLayoutBinding.moPubView.setAdUnitId("b03e6dccfe9e4abab02470a39c88d5dc")
    adLayoutBinding.moPubView.bannerAdListener = object : MoPubView.BannerAdListener {
      override fun onBannerExpanded(banner: MoPubView?) {
      }

      override fun onBannerLoaded(banner: MoPubView) {
        showToast("Banner Ad Loaded")
      }

      override fun onBannerCollapsed(banner: MoPubView?) {
      }

      override fun onBannerFailed(
        banner: MoPubView?,
        errorCode: MoPubErrorCode?
      ) {
        showToast("Banner Failed to Load")
      }

      override fun onBannerClicked(banner: MoPubView?) {
        showToast("Banner Clicked")
      }
    }
  }

  override fun setupInterstitial() {
    interstitial = MoPubInterstitial(this, "a49430ee57ee4401a9eda6098726ce54")
    interstitial.interstitialAdListener = object : MoPubInterstitial.InterstitialAdListener {
      override fun onInterstitialLoaded(interstitial: MoPubInterstitial?) {
        showToast("Interstitial Loaded")
      }

      override fun onInterstitialShown(interstitial: MoPubInterstitial?) {
        showToast("Interstitial Shown")
      }

      override fun onInterstitialFailed(
        interstitial: MoPubInterstitial?,
        errorCode: MoPubErrorCode?
      ) {
        showToast("Interstitial Failed")
        interstitial?.destroy()
      }

      override fun onInterstitialDismissed(interstitial: MoPubInterstitial?) {
        showToast("Interstitial Dismissed")
        interstitial?.destroy()
      }

      override fun onInterstitialClicked(interstitial: MoPubInterstitial?) {
        showToast("Interstitial Clicked")
      }
    }
  }

  /**
   * Listener on mrect button that will trigger AppMonet's addBids method.
   */
  override fun setupMrectLoadClickListener() {
    binding.loadMrect.setOnClickListener {
      ValueCallback<MoPubView> { }
      AppMonet.addBids(adLayoutBinding.moPubView, 1500) { value ->
        value.loadAd()
      }
    }
  }

  override fun setupInterstitialLoadClickListener() {
    binding.loadInterstitial.setOnClickListener {
      setupInterstitial()
      interstitial.load()
    }
  }

  override fun setupInterstitialShowClickListener() {
    binding.showInterstitial.setOnClickListener {
      if (interstitial.isReady) {
        interstitial.show()
      } else {
        showToast("Interstitial Is Not Ready")
      }
    }
  }

  override fun setupNativeClickListener() {
    binding.navigateToNativeScreen.setOnClickListener {
      val intent = Intent(this@MainActivity, NativeSampleActivity::class.java)
      startActivity(intent)
    }
  }
}
