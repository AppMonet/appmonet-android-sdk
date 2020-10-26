package com.monet

expect class Util {
  companion object {
    fun getUUID(): String
    fun join(
      delimeter: String,
      list: List<*>
    ):String
  }
}