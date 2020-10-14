package com.monet

internal actual object BuildConfiguration {
  actual val STAGING_URL = BuildKonfig.STAGING_SERVER_HOST
  actual val isDebug = BuildConfig.DEBUG
}