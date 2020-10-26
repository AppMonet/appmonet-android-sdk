package com.monet

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
  }
}