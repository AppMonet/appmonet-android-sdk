package com.monet.bidder.bid

import android.annotation.SuppressLint
import android.os.Bundle
import com.monet.bidder.Constants
import com.monet.bidder.Logger
import com.monet.bidder.bid.BidResponse.Constant.ADM_KEY
import com.monet.bidder.bid.BidResponse.Constant.ADUNIT_ID_KEY
import com.monet.bidder.bid.BidResponse.Constant.AD_TYPE_KEY
import com.monet.bidder.bid.BidResponse.Constant.BIDDER_KEY
import com.monet.bidder.bid.BidResponse.Constant.BID_BUNDLE_KEY
import com.monet.bidder.bid.BidResponse.Constant.CLICK_PIXEL_KEY
import com.monet.bidder.bid.BidResponse.Constant.CODE_KEY
import com.monet.bidder.bid.BidResponse.Constant.COOL_KEY
import com.monet.bidder.bid.BidResponse.Constant.CPM_KEY
import com.monet.bidder.bid.BidResponse.Constant.DURATION_KEY
import com.monet.bidder.bid.BidResponse.Constant.EXPIRATION_KEY
import com.monet.bidder.bid.BidResponse.Constant.EXTRAS_KEY
import com.monet.bidder.bid.BidResponse.Constant.FLEX_SIZE_KEY
import com.monet.bidder.bid.BidResponse.Constant.HEIGHT_KEY
import com.monet.bidder.bid.BidResponse.Constant.ID_KEY
import com.monet.bidder.bid.BidResponse.Constant.INTERSTITIAL_CLOSE_KEY
import com.monet.bidder.bid.BidResponse.Constant.INTERSTITIAL_FORMAT_KEY
import com.monet.bidder.bid.BidResponse.Constant.INTERSTITIAL_KEY
import com.monet.bidder.bid.BidResponse.Constant.INTERSTITIAL_TRUSTED_KEY
import com.monet.bidder.bid.BidResponse.Constant.KEYWORDS_KEY
import com.monet.bidder.bid.BidResponse.Constant.MEGA_KEY
import com.monet.bidder.bid.BidResponse.Constant.NATIVE_RENDER
import com.monet.bidder.bid.BidResponse.Constant.QUEUE_NEXT_KEY
import com.monet.bidder.bid.BidResponse.Constant.REFRESH_KEY
import com.monet.bidder.bid.BidResponse.Constant.RENDER_PIXEL_KEY
import com.monet.bidder.bid.BidResponse.Constant.TS_KEY
import com.monet.bidder.bid.BidResponse.Constant.URL_KEY
import com.monet.bidder.bid.BidResponse.Constant.UUID_KEY
import com.monet.bidder.bid.BidResponse.Constant.U_KEY
import com.monet.bidder.bid.BidResponse.Constant.WIDTH_KEY
import com.monet.bidder.bid.BidResponse.Constant.WV_UUID
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable
import java.util.*

data class BidResponse(
    val adm: String,
    val id: String,
    val code: String,
    val width: Int,
    val height: Int,
    val createdAt: Long,
    val cpm: Double,
    val bidder: String,
    val adUnitId: String,
    val keyWords: String,
    val renderPixel: String,
    val clickPixel: String,
    val u: String,
    val uuid: String,
    val cool: Int,
    val nativeRender: Boolean,
    val wvUUID: String,
    val duration: Int,
    val expiration: Long,
    val mega: Boolean,
    val adType: String,
    val refresh: Int,
    val interstitial: Interstitial?,
    var extras: Map<String, Any> = hashMapOf(),
    var nativeInvalidated: Boolean = false,
    var queueNext: Boolean = true,
    var flexSize: Boolean = false,
    var url: String = ""
) : Serializable {

  companion object {
    private val sLogger = Logger("BidResponse")
  }

  data class Interstitial(
      val format: String,
      val close: Boolean,
      val trusted: Boolean
  ) : Serializable {
    object Mapper {
      fun toJson(interstitial: Interstitial?): JSONObject? {
        return try {
          val obj = JSONObject()
          obj.putOpt(INTERSTITIAL_FORMAT_KEY, interstitial?.format)
          obj.putOpt(INTERSTITIAL_CLOSE_KEY, interstitial?.close)
          obj.putOpt(INTERSTITIAL_TRUSTED_KEY, interstitial?.trusted)
          obj
        } catch (e: java.lang.Exception) {
          null
        }
      }
    }
  }

  object Constant {
    const val FLOATING_AD_TYPE = "FLOATING"

    // constants
    const val CODE_KEY = "code"
    const val ADM_KEY = "adm"
    const val WIDTH_KEY = "width"
    const val URL_KEY = "url"
    const val HEIGHT_KEY = "height"
    const val ID_KEY = "id"
    const val TS_KEY = "ts"
    const val CPM_KEY = "cpm"
    const val BIDDER_KEY = "bidder"
    const val UUID_KEY = "uuid"
    const val ADUNIT_ID_KEY = "adUnitId"
    const val FLEX_SIZE_KEY = "flexSize"
    const val KEYWORDS_KEY = "keywords"
    const val RENDER_PIXEL_KEY = "renderPixel"
    const val CLICK_PIXEL_KEY = "clickPixel"
    const val COOL_KEY = "cdown"
    const val APPLICATION_ID_KEY = "appId"
    const val QUEUE_NEXT_KEY = "queueNext"
    const val NATIVE_RENDER = "naRender"
    const val EXPIRATION_KEY = "expiration"
    const val WV_UUID = "wvUUID"
    const val U_KEY = "u"
    const val DURATION_KEY = "duration"
    const val MEGA_KEY = "mega"
    const val BID_BUNDLE_KEY = "__bid__"
    const val AD_TYPE_KEY = "adType"
    const val REFRESH_KEY = "refresh"
    const val INTERSTITIAL_KEY = "interstitial"
    const val INTERSTITIAL_FORMAT_KEY = "format"
    const val INTERSTITIAL_CLOSE_KEY = "close"
    const val INTERSTITIAL_TRUSTED_KEY = "trusted"
    const val EXTRAS_KEY = "extras"
  }

  object Mapper {

    @JvmStatic
    fun from(bundle: Bundle?): BidResponse? {
      if (bundle == null) {
        return null
      }
      if (!bundle.containsKey(BID_BUNDLE_KEY)) {
        return null
      }
      var bid: BidResponse? = null
      try {
        bid = bundle.getSerializable(BID_BUNDLE_KEY) as BidResponse?
      } catch (e: java.lang.Exception) {
        sLogger.debug("bid response is not serializable")
      }
      return bid
    }

    @JvmStatic
    fun from(json: JSONObject): BidResponse? {
      return try {
        var code = json.getString(CODE_KEY)
        if (code == null) {
//                    sLogger.warn("json missing bid code: defaulting")
          code = Constants.Dfp.DEFAULT_BIDDER_KEY
        }
        val refreshTime = json.optInt(REFRESH_KEY, 0)
        val interstitialJSON = json.optJSONObject(INTERSTITIAL_KEY)
        var interstitial: Interstitial? = null
        if (interstitialJSON != null) {
          interstitial = Interstitial(
              interstitialJSON.getString(INTERSTITIAL_FORMAT_KEY),
              interstitialJSON.optBoolean(INTERSTITIAL_CLOSE_KEY, true),
              interstitialJSON.optBoolean(INTERSTITIAL_TRUSTED_KEY, false)
          )
        }
        val bid = BidResponse(
            json.optString(ADM_KEY),
            json.optString(ID_KEY),
            code,
            json.optInt(WIDTH_KEY),
            json.optInt(HEIGHT_KEY),
            json.optLong(TS_KEY),
            json.optDouble(CPM_KEY),
            json.optString(BIDDER_KEY),
            json.optString(ADUNIT_ID_KEY),
            json.optString(KEYWORDS_KEY),
            json.optString(RENDER_PIXEL_KEY),
            json.optString(CLICK_PIXEL_KEY),
            json.optString(U_KEY),
//                        json.optString(UUID_KEY),
            UUID.randomUUID().toString(),
            if (refreshTime <= 0) json.optInt(COOL_KEY) else 0,
            json.optBoolean(NATIVE_RENDER),
            json.optString(WV_UUID),
            json.optInt(DURATION_KEY),
            normalizeExpiration(json.optLong(EXPIRATION_KEY)),
            json.optBoolean(MEGA_KEY),
            json.optString(AD_TYPE_KEY, ""),
            refreshTime,
            interstitial
        )
        bid.url = json.optString(URL_KEY)
        bid.queueNext = json.optInt(QUEUE_NEXT_KEY) != 0
        bid.flexSize = json.optBoolean(FLEX_SIZE_KEY)
        bid.extras = toMap(json.optJSONObject(EXTRAS_KEY))
        bid
      } catch (e: Exception) {
        sLogger.error("malformed bid: ", e.message)
        null
      }
    }

    @JvmStatic
    fun toJson(bid: BidResponse?): JSONObject {
      val json = JSONObject()
      bid?.let {
        try {
          json.putOpt(ADM_KEY, bid.adm)
          json.putOpt(ID_KEY, bid.id)
          json.putOpt(
              CODE_KEY, if (bid.code.isNotEmpty()) bid.code else Constants.Dfp.DEFAULT_BIDDER_KEY
          )
          json.putOpt(WIDTH_KEY, bid.width)
          json.putOpt(HEIGHT_KEY, bid.height)
          json.putOpt(TS_KEY, bid.createdAt)
          json.putOpt(CPM_KEY, bid.cpm)
          json.putOpt(BIDDER_KEY, bid.bidder)
          json.putOpt(ADUNIT_ID_KEY, bid.adUnitId)
          json.putOpt(KEYWORDS_KEY, bid.keyWords)
          json.putOpt(RENDER_PIXEL_KEY, bid.renderPixel)
          json.putOpt(CLICK_PIXEL_KEY, bid.clickPixel)
          json.putOpt(U_KEY, bid.u)
          json.putOpt(UUID_KEY, bid.uuid)
          json.putOpt(COOL_KEY, bid.cool)
          json.putOpt(NATIVE_RENDER, bid.nativeRender)
          json.putOpt(WV_UUID, bid.wvUUID)
          json.putOpt(DURATION_KEY, bid.duration)
          json.putOpt(EXPIRATION_KEY, bid.expiration)
          json.putOpt(MEGA_KEY, bid.mega)
          json.putOpt(AD_TYPE_KEY, bid.adType)
          json.putOpt(REFRESH_KEY, bid.refresh)
          if (bid.interstitial != null) {
            json.putOpt(INTERSTITIAL_KEY, Interstitial.Mapper.toJson(bid.interstitial))
          }
          json.putOpt(URL_KEY, bid.url)
          json.putOpt(QUEUE_NEXT_KEY, bid.queueNext)
          json.putOpt(FLEX_SIZE_KEY, bid.flexSize)
          json.putOpt(EXTRAS_KEY, bid.extras)
        } catch (e: java.lang.Exception) {
          //do nothing
        }
      }
      return json
    }

    @Throws(JSONException::class)
    private fun toList(array: JSONArray): List<Any> {
      val list: MutableList<Any> = ArrayList()
      for (i in 0 until array.length()) {
        var value = array[i]
        if (value is JSONArray) {
          value = toList(value)
        } else if (value is JSONObject) {
          value = toMap(value)
        }
        list.add(value)
      }
      return list
    }

    @Throws(JSONException::class)
    private fun toMap(obj: JSONObject?): Map<String, Any> {
      val map: MutableMap<String, Any> = HashMap()
      if (obj != null) {
        val keysItr = obj.keys()
        while (keysItr.hasNext()) {
          val key = keysItr.next()
          var value = obj[key]
          if (value is JSONArray) {
            value = toList(value)
          } else if (value is JSONObject) {
            value = toMap(value)
          }
          map[key] = value
        }
      }
      return map
    }

    private fun normalizeExpiration(exp: Long): Long {
      if (exp <= 0) {
        return 0
      }
      // javascript forgot to send it in ms...
      return if (exp < 1000) {
        exp * 1000
      } else exp
    }
  }

  @SuppressLint("DefaultLocale")
  override fun toString(): String {
    return String.format(
        "<BidResponse cpm=%.2f bidder=%s width=%d height=%d id=%s auid=%s />",
        cpm, bidder, width, height, id, adUnitId
    )
  }
}
