package com.monet.bidder

import android.location.Location
import android.text.TextUtils
import org.json.JSONException
import org.json.JSONObject
import java.util.ArrayList
import java.util.Date
import java.util.HashMap

class RequestData(
  adRequest: AdServerAdRequest?,
  adView: AdServerAdView
) {
  var birthday: Date? = adRequest?.birthday
  var gender: String? = adRequest?.gender
  var location: Location? = adRequest?.location
  var contentURL: String = adRequest?.contentUrl ?: ""
  var ppid: String = adRequest?.publisherProvidedId ?: ""
  var adUnitId: String = adView.adUnitId
  var additional: Map<String?, String?> = buildAdditional(adRequest)


  private fun serializeBundleObject(value: Any?): String? {
    if (value == null) {
      return null
    }
    when (value) {
      is String -> {
        return value
      }
      is Int -> {
        return try {
          ((value as Int?)!!).toString()
        } catch (e: Exception) {
          null
        }
      }
      is Double -> {
        return try {
          java.lang.Double.toString((value as Double?)!!)
        } catch (e: Exception) {
          null
        }
      }
      is Float -> {
        return try {
          java.lang.Float.toString((value as Float?)!!)
        } catch (e: Exception) {
          null
        }
      }
      is List<*> -> {
        val items: MutableList<String?> = ArrayList()
        return try {
          for (listItem in value) {
            val serialized = serializeBundleObject(listItem)
            if (serialized != null) {
              items.add(serialized)
            }
          }
          TextUtils.join(",", items)
        } catch (e: Exception) {
          null
        }
      }
      else -> return null
    }
  }

  private fun buildAdditional(adRequest: AdServerAdRequest?): Map<String?, String?> {
    val output: MutableMap<String?, String?> = HashMap()
    val bundle = adRequest!!.customTargeting
    for (key in bundle.keySet()) {
      val value = bundle[key]
      val serialized = serializeBundleObject(value)
      if (serialized != null) {
        output[key] = serialized
      }
    }
    return output
  }

  fun toJson(): String {
    val json = JSONObject()
    return try {
      json.put(DOB_KEY, birthday)
      json.put(ADUNIT_KEY, adUnitId)
      json.put(GENDER_KEY, gender)
      json.put(URL_KEY, contentURL)
      json.put(PPID_KEY, ppid)
      json.put(KVP_KEY, JSONObject(additional))
      val locationJson = JSONObject()
      if (location != null) {
        locationJson.put("lat", location!!.latitude)
        locationJson.put("lon", location!!.longitude)
        locationJson.put("accuracy", location!!.accuracy.toDouble())
      }
      json.put("location", locationJson)
      json.toString()
    } catch (e: JSONException) {
      "{}"
    }
  }

  companion object {
    private const val DOB_KEY = "dob"
    private const val GENDER_KEY = "gender"
    private const val PPID_KEY = "ppid"
    private const val KVP_KEY = "kvp"
    private const val URL_KEY = "url"
    private const val ADUNIT_KEY = "adunit_id"
  }
}