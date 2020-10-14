package com.monet

import kotlin.native.Platform

internal actual object BuildConfiguration {
  actual val STAGING_URL = BuildKonfig.STAGING_SERVER_HOST
  actual val isDebug: Boolean = Platform.isDebugBinary
}