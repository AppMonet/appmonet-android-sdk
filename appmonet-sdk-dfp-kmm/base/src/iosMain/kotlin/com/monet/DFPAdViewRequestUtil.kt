package com.monet

import cocoapods.GoogleMobileAds.GADCustomEventExtras
import cocoapods.GoogleMobileAds.GADExtras
import cocoapods.GoogleMobileAds.GADRequest
import com.monet.auction.AuctionRequest
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970

actual class DFPAdViewRequestUtil {
  actual companion object {
    actual fun apply(
      adRequestWrapper: RequestWrapper<*>,
      request: AuctionRequest,
      adView: AdServerAdView,
      adServerAdRequest: AdServerAdRequest
    ): AuctionRequest {
      try {
        val requestExtras = request.admobExtras.toMutableMap()
        val admobExtras =
          adServerAdRequest.filterTargeting(adRequestWrapper.networkExtrasBundle.toMutableMap())
        requestExtras.putAll(admobExtras)
        request.admobExtras = requestExtras
      } catch (e: Exception) {
        // do nothing
      }
      if (request.requestData == null) {
        request.requestData = RequestData(adServerAdRequest, adView)
      }
      return request
    }

    fun fromAuctionRequest(
      request: AuctionRequest,
      adServerLabel: String,
      currentExtrasLabel: String
    ): DFPAdViewRequest {
      val gadRequest = GADRequest.request()
      val defaultExtras = getCustomEventExtras(request, adServerLabel)
      if (defaultExtras != null) {
        gadRequest.registerAdNetworkExtras(defaultExtras)
      }
      val addlExtras = getCustomEventExtras(request, currentExtrasLabel)
      if (addlExtras != null) {
        gadRequest.registerAdNetworkExtras(addlExtras)
      }

      if (request.requestData != null) {
        if (!request.requestData!!.contentURL.isNullOrEmpty()) {
          gadRequest.contentURL = request.requestData!!.contentURL
        }
        if (request.requestData!!.birthday != null) {
          gadRequest.birthday =
            NSDate.dateWithTimeIntervalSince1970(request.requestData!!.birthday!!.toDouble())
        }
      }


      if (request.admobExtras != null) {
        val completeExtras = request.admobExtras.toMutableMap()
        completeExtras.putAll(request.targeting)
        val adMobExtras = GADExtras()
        val adtlParams = mutableMapOf<Any?, Any>().apply { putAll(completeExtras) }
        adMobExtras.additionalParameters = adtlParams
        gadRequest.registerAdNetworkExtras(adMobExtras)
      }
      return DFPAdViewRequest(GADAdRequestWrapper(gadRequest))
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
  }
}