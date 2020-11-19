package com.monet

const val ANDROID = "Android"
const val IOS = "iOS"
expect object Platform {
  val platform: String
}