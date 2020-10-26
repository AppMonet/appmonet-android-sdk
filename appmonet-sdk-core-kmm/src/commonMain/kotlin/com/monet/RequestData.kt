package com.monet

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class RequestData(
  private val adRequest: AdServerAdRequest,
  private val adView: AdServerAdView
) {
  var birthday: Long? = adRequest.birthday
  var gender: String? = adRequest.gender
  var location: LocationData? = adRequest.location
  var contentURL: String? = adRequest.contentUrl
  var ppid: String? = adRequest.publisherProvidedId
  var adUnitId: String? = adView.adUnitId
  var kvp: Map<String, JsonElement> = buildAdditional(adRequest)

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
          (value as Int?)?.toString()
        } catch (e: Exception) {
          null
        }
      }
      is Double -> {
        return try {
          (value as Double?)?.toString()
        } catch (e: Exception) {
          null
        }
      }
      is Float -> {
        return try {
          (value as Float?)?.toString()
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
          Util.join(",", items)
        } catch (e: Exception) {
          null
        }
      }
      else -> return null
    }
  }

  private fun buildAdditional(adRequest: AdServerAdRequest?): Map<String, JsonElement> {
    val output: MutableMap<String, JsonElement> = mutableMapOf()
    adRequest?.customTargeting?.let { bundle ->
      for (key in bundle.keys) {
        val value = bundle[key]
        value?.let {
          serializeBundleObject(it)?.let { value ->
            output[key] = JsonPrimitive(value)
          }
        }
      }
    }
    return output
  }

  fun toMap(): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    map[DOB_KEY] = birthday
    map[ADUNIT_KEY] = adUnitId
    map[GENDER_KEY] = gender
    map[URL_KEY] = contentURL
    map[PPID_KEY] = ppid
    map[KVP_KEY] = kvp

    location?.let {
      map["location"] =
        mutableMapOf<String, Any?>().apply {
          this["lat"] = it.lat
          this["lon"] = it.lat
          this["accuracy"] = it.accuracy
        }
    }
    return map;
  }

  fun toJson(): String {
    return try {
      JsonObject(
          mutableMapOf<String, JsonElement>().apply {
            this[DOB_KEY] = JsonPrimitive(birthday)
            this[ADUNIT_KEY] = JsonPrimitive(adUnitId)
            this[GENDER_KEY] = JsonPrimitive(gender)
            this[URL_KEY] = JsonPrimitive(contentURL)
            this[PPID_KEY] = JsonPrimitive(ppid)
            this[KVP_KEY] = JsonObject(kvp)
            location?.let {
              this["location"] = JsonObject(
                  mutableMapOf<String, JsonElement>().apply {
                    this["lat"] = JsonPrimitive(it.lat)
                    this["lon"] = JsonPrimitive(it.lat)
                    this["accuracy"] = JsonPrimitive(it.accuracy)
                  }
              )
            }
          }
      ).toString()
    } catch (e: Exception) {
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