package com.monet.bidder

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.ArrayList

internal class InterstitialData(rawData: String?) {
  var metadata: InterstitialMetadata
  var content   = arrayListOf<InterstitialContent>()

  @Throws(
      JSONException::class
  ) private fun retrieveInterstitialContent(jsonArray: JSONArray) {
    content = ArrayList(jsonArray.length())
    for (i in 0 until jsonArray.length()) {
      content.add(InterstitialContent(jsonArray.getJSONObject(i)))
    }
  }

  internal class InterstitialMetadata(private val meta: JSONObject) {
    fun title(): String {
      return meta.optString("title")
    }

    fun navColor(): String {
      return meta.optString("nav_color")
    }

    fun initialOffset(): String {
      return meta.optString("initial_offset")
    }

    fun initialDuration(): String {
      return meta.optString("initial_duration")
    }
  }

  internal class InterstitialContent(private val content: JSONObject) {
    private val media: Media?
    fun type(): String {
      return content.optString("type")
    }

    fun id(): String {
      return content.optString("id")
    }

    fun title(): String {
      return content.optString("title")
    }

    fun media(): Media? {
      return media
    }

    internal class Media(private val jsonMedia: JSONObject) {
      fun mime(): String {
        return jsonMedia.optString("mime")
      }

      fun source(): String {
        return jsonMedia.optString("src")
      }

      fun thumbnail(): String {
        return jsonMedia.optString("thumbnail")
      }

      fun quality(): String {
        return jsonMedia.optString("quality")
      }

      fun duration(): String {
        return jsonMedia.optString("duration")
      }

      fun width(): String {
        return jsonMedia.optString("width")
      }

      fun height(): String {
        return jsonMedia.optString("height")
      }
    }

    init {
      val jsonMedia = content.optJSONObject("media")
      media = if (jsonMedia != null) Media(jsonMedia) else null
    }
  }

  init {
    val jsonObject = JSONObject(rawData)
    metadata = InterstitialMetadata(jsonObject.getJSONObject("meta"))
    retrieveInterstitialContent(jsonObject.getJSONArray("content"))
  }
}