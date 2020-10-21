package com.monet.app

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.monet.app.R.id
import com.monet.app.R.layout
import com.monet.app.databinding.ActivityNativeSampleBinding
import com.monet.bidder.AppMonet
import com.monet.bidder.AppMonetNativeAdRenderer
import com.monet.bidder.AppMonetNativeViewBinder.Builder
import com.mopub.nativeads.MoPubNative
import com.mopub.nativeads.MoPubNative.MoPubNativeNetworkListener
import com.mopub.nativeads.NativeAd
import com.mopub.nativeads.NativeErrorCode
import com.mopub.nativeads.RequestParameters

class NativeSampleActivity : AppCompatActivity() {
  companion object {
    private val TAG = NativeSampleActivity::class.java.canonicalName
  }

  private var nativeAd: NativeAd? = null
  private var mMoPubNative: MoPubNative? = null
  private lateinit var binding : ActivityNativeSampleBinding
  private val adUnit = "8fd7a8bae7c84236b465537b55fc80a7"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityNativeSampleBinding.inflate(layoutInflater)
    setContentView(binding.root)
    setupLoadAdListener()
    val moPubNativeListener: MoPubNativeNetworkListener = object : MoPubNativeNetworkListener {
      override fun onNativeLoad(nativeAd: NativeAd) {
        this@NativeSampleActivity.nativeAd = nativeAd
        binding.nativeAdContainer.removeAllViews()
        val adViewRender = nativeAd.createAdView(this@NativeSampleActivity, binding.nativeAdContainer)
        binding.nativeAdContainer.addView(adViewRender)
        nativeAd.prepare(adViewRender)
        nativeAd.renderAdView(adViewRender)
      }

      override fun onNativeFail(errorCode: NativeErrorCode) {
        Log.d(TAG, errorCode.name)
      }
    }

    val appMonetNativeAdRenderer = AppMonetNativeAdRenderer(
        Builder(layout.native_layout)
            .mediaLayoutId(id.native_ad_video)
            .callToActionId(id.native_ad_action_button)
            .titleId(id.native_ad_title)
            .iconId(id.native_icon_image)
            .build()
    )

    mMoPubNative = MoPubNative(this, adUnit, moPubNativeListener)
    mMoPubNative?.registerAdRenderer(appMonetNativeAdRenderer)

  }

  override fun onDestroy() {
    super.onDestroy()
    nativeAd?.destroy()
    mMoPubNative?.destroy()
  }

  private fun setupLoadAdListener() {
    binding.loadNativeAd.setOnClickListener {
      val childView = binding.nativeAdContainer.getChildAt(0)
      if (childView != null && nativeAd != null) {
        childView.visibility = View.GONE
        nativeAd!!.clear(childView)
      }
      val requestParameters =
        RequestParameters.Builder().keywords("key1:value1,key2:value2").build()
      AppMonet.addNativeBids(
          mMoPubNative, requestParameters, adUnit, 4000
      ) { value -> value?.moPubNative?.makeRequest(value.requestParameters) }
    }
  }

}