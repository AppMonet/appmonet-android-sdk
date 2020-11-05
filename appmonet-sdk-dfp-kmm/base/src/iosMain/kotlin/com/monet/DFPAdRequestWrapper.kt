package com.monet

import cocoapods.GoogleMobileAds.DFPRequest
import cocoapods.GoogleMobileAds.GADAdNetworkExtrasProtocol
import cocoapods.GoogleMobileAds.GADExtras
import cocoapods.GoogleMobileAds.GADGender.kGADGenderFemale
import cocoapods.GoogleMobileAds.GADGender.kGADGenderMale
import kotlinx.cinterop.ObjCClassOf
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.getOriginalKotlinClass
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap.alloc
import platform.Foundation.NSLog
import platform.Foundation.timeIntervalSince1970
import kotlin.reflect.KClass

class DFPAdRequestWrapper(override val request: DFPRequest) : RequestWrapper<DFPRequest> {
  override val publisherProvidedId: String?
    get() = request.publisherProvidedID

  override val location: LocationData?
    get() = null
  override val contentUrl: String?
    get() = request.contentURL

  override val gender: String
    get() = when (request.gender) {
      kGADGenderFemale -> "female"
      kGADGenderMale -> "male"
      else -> "unknown"
    }
  override val birthday: Long?
    get() = request.birthday?.timeIntervalSince1970?.toLong()

  override val customTargeting: Map<String, Any>
    get() {
      val custom = mutableMapOf<String, Any>()
      request.customTargeting?.map {
        if (it.key is String && it.value != null) {
          custom[it.key as String] = it.value!!
        }
      }
      return custom
    }

  override val networkExtrasBundle: Map<String, Any>
    get() {
      val extras = mutableMapOf<String, Any>()
      (getOriginalKotlinClass(GADExtras.Companion) as GADAdNetworkExtrasProtocol?)?.let {
        val admobExtras = request.adNetworkExtrasFor(it) as GADExtras
        admobExtras.additionalParameters?.map { params ->
          if (params.key is String && params.value != null) {
            extras[params.key as String] = params.value!!
          }
        }
      }
      return extras
    }

  override val networkExtras: Map<String, Any>?
    get() = null

}