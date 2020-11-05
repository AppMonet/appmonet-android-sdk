package com.monet

import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.doubleclick.PublisherAdRequest
import com.google.android.gms.ads.mediation.admob.AdMobExtras
import com.monet.DFPAdRequestUtil.Companion.CUSTOM_TARGETING
import com.monet.DFPAdRequestUtil.Companion.NETWORK_EXTRAS
import com.monet.DFPAdRequestUtil.Companion.NETWORK_EXTRAS_BUNDLE
import com.monet.DFPAdViewRequestUtil.Companion

class AdRequestWrapper(override val request: AdRequest) : RequestWrapper<AdRequest> {
  companion object {
    const val ADMOB_EXTRAS = "admob_extras"
  }

  override val publisherProvidedId: String?
    get() = null

  override val location: LocationData?
    get() = request.location?.let {
      LocationData(it.latitude, it.longitude, it.accuracy.toDouble(), it.provider)
    }
  override val contentUrl: String?
    get() = request.contentUrl

  override val gender: String
    get() = when (request.gender) {
      PublisherAdRequest.GENDER_FEMALE -> "female"
      PublisherAdRequest.GENDER_MALE -> "male"
      else -> "unknown"
    }
  override val birthday: Long?
    get() = request.birthday?.time

  override val customTargeting: Map<String, Any>
    get() = networkExtrasBundle

  override val networkExtrasBundle: Map<String, Any>
    get() {
      val extrasBundle = request.getNetworkExtrasBundle(AdMobAdapter::class.java)
      val result = mutableMapOf<String, Any>()
      if (extrasBundle != null) {
        result[ADMOB_EXTRAS] = extrasBundle
      }
      return result
    }

  override val networkExtras: Map<String, Any>?
    get() {
      val extras = request.getNetworkExtras(AdMobExtras::class.java)
      extras?.extras?.let {
        return mutableMapOf<String, Any>().apply {
          this[NETWORK_EXTRAS] = it
        }
      }
      return null
    }
}