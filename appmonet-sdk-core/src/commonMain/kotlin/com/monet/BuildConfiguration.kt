package com.monet

internal expect object BuildConfiguration {
  val STAGING_URL: String
  val isDebug: Boolean
}