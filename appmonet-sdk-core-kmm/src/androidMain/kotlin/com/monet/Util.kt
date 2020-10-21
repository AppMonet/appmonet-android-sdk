package com.monet

import java.util.UUID

actual class Util {
  actual companion object {
    actual fun getUUID(): String {
      return UUID.randomUUID().toString()
    }
  }
}