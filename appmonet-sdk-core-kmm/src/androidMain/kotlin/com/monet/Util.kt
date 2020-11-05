package com.monet

import android.os.Bundle
import android.text.TextUtils
import java.util.UUID

actual class Util {
  actual companion object {
    actual fun getUUID(): String {
      return UUID.randomUUID().toString()
    }

    actual fun join(
      delimeter: String,
      list: List<*>
    ): String {
      return TextUtils.join(delimeter, list)
    }

    actual fun isAdRequestExtra(
      map: MutableMap<String, Any>,
      key: String,
      extra: Any
    ): Boolean {
      if (extra is Bundle) {
        map[key] = extra
        return true
      }
      return false
    }
  }
}