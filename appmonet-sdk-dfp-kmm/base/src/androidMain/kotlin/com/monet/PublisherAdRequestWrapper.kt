package com.monet

import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.doubleclick.PublisherAdRequest
import com.google.android.gms.ads.mediation.admob.AdMobExtras
import com.monet.DFPAdRequestUtil.Companion
import com.monet.DFPAdRequestUtil.Companion.CUSTOM_TARGETING
import com.monet.DFPAdRequestUtil.Companion.NETWORK_EXTRAS
import com.monet.DFPAdRequestUtil.Companion.NETWORK_EXTRAS_BUNDLE

class PublisherAdRequestWrapper(override val request: PublisherAdRequest) : RequestWrapper<PublisherAdRequest> {
  override val publisherProvidedId: String?
    get() = request.publisherProvidedId

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
    get() {
      val custom = mutableMapOf<String, Any>()
      request.customTargeting?.let {
        custom[CUSTOM_TARGETING] = it
      }
      // create a bundle merging both
      networkExtras?.let {
        custom.putAll(it)
      }
      return custom
    }

  override val networkExtrasBundle: Map<String, Any>
    get() {
      val extrasBundle = request.getNetworkExtrasBundle(AdMobAdapter::class.java)
      val result = mutableMapOf<String, Any>()
      result[NETWORK_EXTRAS_BUNDLE] = extrasBundle
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