package com.monet

import cocoapods.GoogleMobileAds.DFPRequest
import cocoapods.GoogleMobileAds.GADCustomEventExtras
import com.monet.auction.AuctionRequest
import platform.Foundation.NSDate
import platform.Foundation.NSLog
import platform.Foundation.NSMutableArray
import platform.Foundation.dateWithTimeIntervalSince1970

actual class DFPAdRequestUtil {
  actual companion object {
    const val ADMOB_EXTRAS = "admob_extras"
    const val CUSTOM_TARGETING = "custom_targeting"
    const val NETWORK_EXTRAS_BUNDLE = "network_extras_bundle"
    const val NETWORK_EXTRAS = "network_extras"

    actual fun apply(
      adRequestWrapper: RequestWrapper<*>,
      request: AuctionRequest,
      adView: AdServerAdView,
      adServerAdRequest: AdServerAdRequest
    ): AuctionRequest {
      try {
        val requestExtras = request.admobExtras.toMutableMap()
        val admobExtras = adRequestWrapper.networkExtrasBundle.toMutableMap()
        requestExtras.putAll(admobExtras)
        request.admobExtras = requestExtras
      } catch (e: Exception) {
        // do nothing
      }
      if (request.requestData == null) {
        request.requestData = RequestData(adServerAdRequest, adView)
      }
      if (adRequestWrapper.request is DFPRequest) {
        val targeting = request.targeting.toMutableMap()
        targeting.putAll(
            adServerAdRequest.filterTargeting(
                adRequestWrapper.customTargeting
            )
        )
        request.targeting = targeting
      }
      return request
    }

    private fun getCustomEventExtras(
      request: AuctionRequest,
      label: String?
    ): GADCustomEventExtras? {
      if (label != null) {
        val extras = GADCustomEventExtras()
        extras.setExtras(mutableMapOf<Any?, Any>().apply {
          putAll(request.networkExtras)
        }, label)
        return extras
      }
      return null
    }

    fun fromAuctionRequest(
      request: AuctionRequest,
      adServerLabel: String,
      currentExtrasLabel: String
    ): DFPAdRequest {
      val dfpRequest = DFPRequest.request()
      val defaultExtras = getCustomEventExtras(request, adServerLabel)
      if (defaultExtras != null) {
        dfpRequest.registerAdNetworkExtras(defaultExtras)
      }
      val addlExtras = getCustomEventExtras(request, currentExtrasLabel)
      if (addlExtras != null) {
        dfpRequest.registerAdNetworkExtras(addlExtras)
      }
      // copy over the targeting
      val targetingCopy = dfpRequest.customTargeting?.toMutableMap() ?: mutableMapOf()
      for ((key, value) in request.targeting) {
        if (value is List<*>) {
          targetingCopy[key] = value
        } else {
          targetingCopy[key] = value.toString()
        }
      }
      dfpRequest.customTargeting = targetingCopy


      if (request.requestData != null) {
        if (!request.requestData!!.contentURL.isNullOrEmpty()) {
          dfpRequest.contentURL = request.requestData!!.contentURL
        }
        if (request.requestData!!.birthday != null) {
          dfpRequest.birthday =
            NSDate.dateWithTimeIntervalSince1970(request.requestData!!.birthday!!.toDouble())
        }
        val customTargeting = mutableMapOf<Any?, Any>()

        request.requestData?.getStringMapFromKvp()?.map {
          customTargeting[it.key] = it.value
        }
        val merge = dfpRequest.customTargeting?.toMutableMap() ?: mutableMapOf()
        merge.putAll(customTargeting)
        dfpRequest.customTargeting = merge
      }
      return DFPAdRequest(DFPAdRequestWrapper(dfpRequest))
    }
  }

}

