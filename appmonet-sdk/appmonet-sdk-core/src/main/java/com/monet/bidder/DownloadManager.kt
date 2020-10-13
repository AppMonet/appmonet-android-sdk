package com.monet.bidder

import android.content.Context
import android.net.Uri
import android.net.http.HttpResponseCache
import com.monet.bidder.DownloadManager.DownloadStrategy.CACHE_IF_AVAILABLE
import com.monet.bidder.DownloadManager.DownloadStrategy.NETWORK
import java.io.File
import java.io.IOException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException

internal class DownloadManager(
  context: Context,
  uri: Uri
) {
  internal enum class DownloadStrategy {
    CACHE_IF_AVAILABLE,
    NETWORK
  }

  private val uri: Uri
  @JvmField var timeout = 8000
  @JvmField var downloadStrategy = CACHE_IF_AVAILABLE
  @Throws(KeyManagementException::class, NoSuchAlgorithmException::class) fun get(): HttpRequest {
    val request = HttpRequest(uri.toString(), HttpRequest.METHOD_GET)
    request.useCaches(downloadStrategy == CACHE_IF_AVAILABLE)
    request.disableSslv3()
    request.connectTimeout(timeout)
    if (downloadStrategy == NETWORK) {
      request.parameter("Cache-Control", "no-cache")
    }
    return request
  }

  companion object {
    private val sLogger = Logger("DownloadManager")
  }

  init {
    val httpCacheDir = File(context.cacheDir, "monet")
    val httpCacheSize = 10L * 1024L * 1024L // 10 MB
    try {
      if (HttpResponseCache.getInstalled() == null) {
        HttpResponseCache.install(httpCacheDir, httpCacheSize)
      }
    } catch (e: IOException) {
      sLogger.debug("Error setting http caching: " + e.message)
    }
    this.uri = uri
  }
}