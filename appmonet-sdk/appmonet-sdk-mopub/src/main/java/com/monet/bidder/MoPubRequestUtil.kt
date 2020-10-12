package com.monet.bidder

import java.util.HashMap

internal object MoPubRequestUtil {
  @JvmStatic
  fun mergeKeywords(
    viewKeywords: String?,
    newKeywords: String
  ): String {
    val viewKVMap =
      keywordsToMap(viewKeywords)
    val newKVMap: Map<String, Any> =
      keywordsToMap(newKeywords)
    viewKVMap.putAll(newKVMap)
    return getKeywords(viewKVMap)
  }

  fun keywordsToMap(keyWords: String?): MutableMap<String, Any> {
    val kvMap: MutableMap<String, Any> = mutableMapOf()
    keyWords?.let {
      val keyValueArr = it.split(",".toRegex()).toTypedArray()
      for (kv in keyValueArr) {
        val splitKV = kv.split(":".toRegex()).toTypedArray()
        if (splitKV.size == 2) {
          kvMap[splitKV[0]] = splitKV[1]
        }
      }
    }
    return kvMap
  }

  @JvmStatic
  fun getKeywords(kv: Map<String, Any>): String {
    // get keywords out of the local extras
    val buffer = StringBuilder()
    for ((key, value) in kv) {
      if (value is String) {
        buffer.append(key)
        buffer.append(":")
        buffer.append(value)
        buffer.append(",")
      }
    }
    return buffer.toString()
  }
}