package com.monet

import co.touchlab.stately.freeze
import com.monet.BidResponse.Interstitial
import com.monet.BidResponse.InterstitialSerializer
import com.monet.Constants.DEFAULT_BIDDER_KEY
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.Json

@Serializable
data class BidResponses(
  val adUnitId: String,
  val bids: List<BidResponse>
) {
  object Mapper {
    fun from(json: String): BidResponses {
      return Json { ignoreUnknownKeys = true }.decodeFromString(json)
    }
  }
}

internal object InterstitialDeserializer : DeserializationStrategy<Interstitial?> {
  override val descriptor: SerialDescriptor =
    buildClassSerialDescriptor("Interstitial") {
      element<String>("format")
      element<Boolean>("closed")
      element<Boolean>("trusted")
    }

  override fun deserialize(decoder: Decoder): Interstitial? {
    var format = ""
    var closed = true
    var trusted = false
    decoder.decodeStructure(descriptor) {
      while (true) {
        when (val index = decodeElementIndex(descriptor)) {
          0 -> format = decodeStringElement(descriptor, 0)
          1 -> closed = decodeBooleanElement(descriptor, 1)
          2 -> trusted = decodeBooleanElement(descriptor, 2)
          CompositeDecoder.DECODE_DONE -> break
          else -> {
            error("Unexpected index: $index")
          }
        }
      }
    }
    return Interstitial(format, closed, trusted)
  }
}

@Serializer(forClass = BidResponse::class)
internal object BidResponseSerializer : KSerializer<BidResponse> {
  private val stringMapSerializer =
    MapSerializer(String.serializer(), String.serializer())

  override val descriptor: SerialDescriptor =
    buildClassSerialDescriptor("BidResponse") {
      element<String>("adm")
      element<String>("id")
      element<String>("code")
      element<Int>("width")
      element<Int>("height")
      element<Long>("ts")
      element<Double>("cpm")
      element<String>("bidder")
      element<String>("adUnitId")
      element<String>("keywords")
      element<String>("renderPixel")
      element<String>("clickPixel")
      element<String>("u")
      element<String>("uuid")
      element<Int>("cdown")
      element<Boolean>("naRender")
      element<String>("wvUUID")
      element<Int>("duration")
      element<Long>("expiration")
      element<Boolean>("mega")
      element<String>("adType")
      element<Int>("refresh")
      element<Interstitial?>("interstitial")
      element<Map<String, String>>("extras")
      element<Boolean>("nativeInvalidated")
      element<Boolean>("queueNext")
      element<Boolean>("flexSize")
      element<String>("url")
      element<String>("inst")
    }

  override fun serialize(
    encoder: Encoder,
    value: BidResponse
  ) {
    val map: MutableMap<String, String> = mutableMapOf()

    encoder.encodeStructure(descriptor) {
      encodeStringElement(descriptor, 0, value.adm)
      encodeStringElement(descriptor, 1, value.id)
      encodeStringElement(
          descriptor, 2, if (value.code.isNotEmpty()) value.code else DEFAULT_BIDDER_KEY
      )
      encodeIntElement(descriptor, 3, value.width)
      encodeIntElement(descriptor, 4, value.height)
      encodeLongElement(descriptor, 5, value.ts)
      encodeDoubleElement(descriptor, 6, value.cpm)
      encodeStringElement(descriptor, 7, value.bidder)
      encodeStringElement(descriptor, 8, value.adUnitId)
      encodeStringElement(descriptor, 9, value.keywords)
      encodeStringElement(descriptor, 10, value.renderPixel)
      encodeStringElement(descriptor, 11, value.clickPixel)
      encodeStringElement(descriptor, 12, value.u)
      encodeStringElement(descriptor, 13, value.uuid)
      encodeIntElement(descriptor, 14, value.cdown)
      encodeBooleanElement(descriptor, 15, value.naRender)
      encodeStringElement(descriptor, 16, value.wvUUID)
      encodeIntElement(descriptor, 17, value.duration)
      encodeLongElement(descriptor, 18, value.expiration)
      encodeBooleanElement(descriptor, 19, value.mega)
      encodeStringElement(descriptor, 20, value.adType)
      encodeIntElement(descriptor, 21, value.refresh)
      encodeNullableSerializableElement(
          descriptor, 22, InterstitialSerializer, value.interstitial
      )
      value.extras?.let { map.putAll(it) }
      encodeNullableSerializableElement(descriptor, 23, stringMapSerializer, map)

      encodeBooleanElement(descriptor, 24, value.nativeInvalidated)
      encodeIntElement(descriptor, 25, value.queueNext)
      encodeBooleanElement(descriptor, 26, value.flexSize)
      encodeStringElement(descriptor, 27, value.url)
      value.inst?.let {
        encodeStringElement(descriptor, 28, it)
      }
    }
  }

  override fun deserialize(decoder: Decoder): BidResponse =
    decoder.decodeStructure(descriptor) {
      var adm = ""
      var id = ""
      var code = DEFAULT_BIDDER_KEY
      var width = 0
      var height = 0
      var ts = 0L
      var cpm = 0.0
      var bidder: String = ""
      var adUnitId: String = ""
      var keyWords: String = ""
      var renderPixel: String = ""
      var clickPixel: String = ""
      var u: String = ""
      var uuid: String = Util.getUUID()
      var cdown: Int = 0
      var naRender: Boolean = false
      var wvUUID: String = ""
      var duration: Int = 0
      var expiration: Long = 0L
      var mega: Boolean = false
      var adType: String = ""
      var refresh: Int = 0
      var interstitial: Interstitial? = null
      var extras: Map<String, String> = mapOf()
      var nativeInvalidated: Boolean = false
      var queueNext: Int = 0
      var flexSize: Boolean = false
      var url: String = ""
      var inst: String? = null
      while (true) {
        when (val index = decodeElementIndex(descriptor)) {
          0 -> adm = decodeStringElement(descriptor, 0)
          1 -> id = decodeStringElement(descriptor, 1)
          2 -> {
            decodeStringElement(descriptor, 2)?.let {
              if (it.isNotEmpty()) code = it
            }
          }
          3 -> width = decodeIntElement(descriptor, 3)
          4 -> height = decodeIntElement(descriptor, 4)
          5 -> ts = decodeLongElement(descriptor, 5)
          6 -> cpm = decodeDoubleElement(descriptor, 6)
          7 -> bidder = decodeStringElement(descriptor, 7)
          8 -> adUnitId = decodeStringElement(descriptor, 8)
          9 -> keyWords = decodeStringElement(descriptor, 9)
          10 -> renderPixel = decodeStringElement(descriptor, 10)
          11 -> clickPixel = decodeStringElement(descriptor, 11)
          12 -> u = decodeStringElement(descriptor, 12)
          13 -> {
            decodeStringElement(descriptor, 13).let {
              if (it.isNotEmpty()) uuid = it
            }
          }
          14 -> cdown = decodeIntElement(descriptor, 14)
          15 -> naRender = decodeBooleanElement(descriptor, 15)
          16 -> wvUUID = decodeStringElement(descriptor, 16)
          17 -> duration = decodeIntElement(descriptor, 17)
          18 -> expiration = decodeLongElement(descriptor, 18)
          19 -> mega = decodeBooleanElement(descriptor, 19)
          20 -> adType = decodeStringElement(descriptor, 20)
          21 -> refresh = decodeIntElement(descriptor, 21)
          22 -> interstitial =
            decodeNullableSerializableElement(descriptor, 22, InterstitialDeserializer)
          23 -> extras = stringMapSerializer.deserialize(decoder)
          24 -> nativeInvalidated = decodeBooleanElement(descriptor, 24)
          25 -> queueNext = decodeIntElement(descriptor, 25)
          26 -> flexSize = decodeBooleanElement(descriptor, 26)
          27 -> url = decodeStringElement(descriptor, 27)
          28 -> inst = decodeStringElement(descriptor, 28)
          CompositeDecoder.DECODE_DONE -> break
          else -> {
            error("Unexpected index: $index")
          }
        }
      }

      BidResponse(
          adm, id, code, width, height, ts, cpm, bidder, adUnitId, keyWords, renderPixel,
          clickPixel, u, uuid, cdown, naRender, wvUUID, duration, normalizeExpiration(expiration),
          mega, adType,
          refresh, interstitial, extras, nativeInvalidated, queueNext, flexSize, url, inst
      )
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

@Serializable(with = BidResponseSerializer::class)
class BidResponse(
  val adm: String,
  val id: String,
  val code: String,
  val width: Int,
  val height: Int,
  val ts: Long,
  val cpm: Double,
  val bidder: String,
  val adUnitId: String,
  val keywords: String,
  val renderPixel: String,
  val clickPixel: String,
  val u: String,
  val uuid: String,
  val cdown: Int,
  val naRender: Boolean,
  val wvUUID: String,
  val duration: Int,
  val expiration: Long,
  val mega: Boolean,
  val adType: String,
  val refresh: Int,
  val interstitial: Interstitial?,
  val extras: Map<String, String> = hashMapOf(),
  var nativeInvalidated: Boolean = false,
  var queueNext: Int = 0,
  var flexSize: Boolean = false,
  var url: String = "",
  var inst: String? = null
) {
  val orientation = inst
  val createdAt = ts
  val cool = if (refresh <= 0) cdown else 0
  val nativeRender = naRender
  val nextQueue = queueNext != 0
  fun freezeBid() {
    this.freeze()
  }

  companion object {
//    private val sLogger = Logger("BidResponse")
  }

  @Serializer(forClass = Interstitial::class)
  object InterstitialSerializer

  @Serializable
  class Interstitial(
    val format: String,
    val close: Boolean,
    val trusted: Boolean
  )

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
    fun from(jsonString: String): BidResponse? {
      return try {
        Json { ignoreUnknownKeys = true }.decodeFromString(jsonString)
      } catch (e: Exception) {
        null
      }
    }

    //      return try {
//        var code = json.getString(CODE_KEY)
//        if (code == null) {
////                    sLogger.warn("json missing bid code: defaulting")
//          code = Constants.Dfp.DEFAULT_BIDDER_KEY
//        }
//        val refreshTime = json.optInt(REFRESH_KEY, 0)
//        val interstitialJSON = json.optJSONObject(INTERSTITIAL_KEY)
//        var interstitial: Interstitial? = null
//        if (interstitialJSON != null) {
//          interstitial = Interstitial(
//              interstitialJSON.getString(INTERSTITIAL_FORMAT_KEY),
//              interstitialJSON.optBoolean(INTERSTITIAL_CLOSE_KEY, true),
//              interstitialJSON.optBoolean(INTERSTITIAL_TRUSTED_KEY, false)
//          )
//        }
//        val bid = BidResponse(
//            json.optString(ADM_KEY),
//            json.optString(ID_KEY),
//            code,
//            json.optInt(WIDTH_KEY),
//            json.optInt(HEIGHT_KEY),
//            json.optLong(TS_KEY),
//            json.optDouble(CPM_KEY),
//            json.optString(BIDDER_KEY),
//            json.optString(ADUNIT_ID_KEY),
//            json.optString(KEYWORDS_KEY),
//            json.optString(RENDER_PIXEL_KEY),
//            json.optString(CLICK_PIXEL_KEY),
//            json.optString(U_KEY),
////                        json.optString(UUID_KEY),
//            UUID.randomUUID().toString(),
//            if (refreshTime <= 0) json.optInt(COOL_KEY) else 0,
//            json.optBoolean(NATIVE_RENDER),
//            json.optString(WV_UUID),
//            json.optInt(DURATION_KEY),
//            normalizeExpiration(json.optLong(EXPIRATION_KEY)),
//            json.optBoolean(MEGA_KEY),
//            json.optString(AD_TYPE_KEY, ""),
//            refreshTime,
//            interstitial
//        )
//        bid.url = json.optString(URL_KEY)
//        bid.queueNext = json.optInt(QUEUE_NEXT_KEY) != 0
//        bid.flexSize = json.optBoolean(FLEX_SIZE_KEY)
//        bid.extras = toMap(json.optJSONObject(EXTRAS_KEY))
//        bid
//      } catch (e: Exception) {
//        sLogger.error("malformed bid: ", e.message)
//        null
//      }
//    }
//
//    @JvmStatic
    fun toJsonString(bid: BidResponse?): String {
      return try {
        return bid?.let { Json { ignoreUnknownKeys = true }.encodeToString(it) } ?: "{}"
      } catch (e: Exception) {
        "{}"
      }
    }
//      val json = JSONObject()
//      bid?.let {
//        try {
//          json.putOpt(ADM_KEY, bid.adm)
//          json.putOpt(ID_KEY, bid.id)
//          json.putOpt(
//              CODE_KEY, if (bid.code.isNotEmpty()) bid.code else Constants.Dfp.DEFAULT_BIDDER_KEY
//          )
//          json.putOpt(WIDTH_KEY, bid.width)
//          json.putOpt(HEIGHT_KEY, bid.height)
//          json.putOpt(TS_KEY, bid.createdAt)
//          json.putOpt(CPM_KEY, bid.cpm)
//          json.putOpt(BIDDER_KEY, bid.bidder)
//          json.putOpt(ADUNIT_ID_KEY, bid.adUnitId)
//          json.putOpt(KEYWORDS_KEY, bid.keyWords)
//          json.putOpt(RENDER_PIXEL_KEY, bid.renderPixel)
//          json.putOpt(CLICK_PIXEL_KEY, bid.clickPixel)
//          json.putOpt(U_KEY, bid.u)
//          json.putOpt(UUID_KEY, bid.uuid)
//          json.putOpt(COOL_KEY, bid.cool)
//          json.putOpt(NATIVE_RENDER, bid.nativeRender)
//          json.putOpt(WV_UUID, bid.wvUUID)
//          json.putOpt(DURATION_KEY, bid.duration)
//          json.putOpt(EXPIRATION_KEY, bid.expiration)
//          json.putOpt(MEGA_KEY, bid.mega)
//          json.putOpt(AD_TYPE_KEY, bid.adType)
//          json.putOpt(REFRESH_KEY, bid.refresh)
//          if (bid.interstitial != null) {
//            json.putOpt(INTERSTITIAL_KEY, Interstitial.Mapper.toJson(bid.interstitial))
//          }
//          json.putOpt(URL_KEY, bid.url)
//          json.putOpt(QUEUE_NEXT_KEY, bid.queueNext)
//          json.putOpt(FLEX_SIZE_KEY, bid.flexSize)
//          json.putOpt(EXTRAS_KEY, bid.extras)
//        } catch (e: java.lang.Exception) {
//          //do nothing
//        }
//      }
//      return json
//    }
//
//    @Throws(JSONException::class)
//    private fun toList(array: JSONArray): List<Any> {
//      val list: MutableList<Any> = ArrayList()
//      for (i in 0 until array.length()) {
//        var value = array[i]
//        if (value is JSONArray) {
//          value = toList(value)
//        } else if (value is JSONObject) {
//          value = toMap(value)
//        }
//        list.add(value)
//      }
//      return list
//    }
//
//    @Throws(JSONException::class)
//    private fun toMap(obj: JSONObject?): Map<String, Any> {
//      val map: MutableMap<String, Any> = HashMap()
//      if (obj != null) {
//        val keysItr = obj.keys()
//        while (keysItr.hasNext()) {
//          val key = keysItr.next()
//          var value = obj[key]
//          if (value is JSONArray) {
//            value = toList(value)
//          } else if (value is JSONObject) {
//            value = toMap(value)
//          }
//          map[key] = value
//        }
//      }
//      return map
//    }
//

  }

  //
  override fun toString(): String {
    return "<BidResponse cpm=$cpm bidder=$bidder width=$width height=$height id=$id auid=$adUnitId url=$url nativeRender=$nativeRender adm=$adm/>"
  }
}