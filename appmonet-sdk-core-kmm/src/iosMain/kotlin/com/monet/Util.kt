package com.monet

import platform.Foundation.NSUUID

actual class Util {
  actual companion object {
    actual fun getUUID(): String {
      return NSUUID.UUID().toString()
    }
  }
}