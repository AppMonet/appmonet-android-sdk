package com.monet.bidder

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.annotation.VisibleForTesting
import com.monet.bidder.DownloadManager.DownloadStrategy.CACHE_IF_AVAILABLE
import com.monet.bidder.DownloadManager.DownloadStrategy.NETWORK
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8
import java.util.zip.DataFormatException
import java.util.zip.Inflater

class RemoteConfiguration {
  private var downloadManager: DownloadManager

  constructor(
    context: Context?,
    applicationId: String?
  ) {
    val configurationUri = Uri.parse(Constants.AUCTION_MANAGER_CONFIG_URL)
        .buildUpon()
        .appendPath("hb")
        .appendPath("c1")
        .appendPath(applicationId)
        .build()
    downloadManager = DownloadManager(context!!, configurationUri)
    downloadManager.timeout = 8000 // 8s timeout
  }

  @VisibleForTesting
  internal constructor(downloadManager: DownloadManager) {
    this.downloadManager = downloadManager
  }

  @Synchronized fun getRemoteConfiguration(forceServer: Boolean): String? {
    var response: String? = null
    try {
      downloadManager.downloadStrategy = if (forceServer) NETWORK else CACHE_IF_AVAILABLE
      val request = downloadManager.get()
      val httpResponse = request.body()
      request.disconnect()
      val decodedResponse = Base64.decode(httpResponse, Base64.DEFAULT)
      val output = decompress(decodedResponse)
      val decompressedResponse = String(output, 0, output.size, UTF_8)
      val body = JSONObject(decompressedResponse)
      val main = JSONObject()
      main.put("remoteAddr", request.header("X-Remote-Addr"))
      main.put("country", request.header("X-Client-Country"))
      main.put("isCached", request.header("X-Android-Response-Source") == "CONDITIONAL_CACHE 304")
      main.put("body", body)
      response = main.toString()
    } catch (e: Exception) {
      sLogger.debug("error getting remote configuration")
      sLogger.debug(e.message)
    }
    return response
  }

  @Throws(
      IOException::class, DataFormatException::class
  ) private fun decompress(data: ByteArray): ByteArray {
    val inflater = Inflater()
    inflater.setInput(data)
    val outputStream = ByteArrayOutputStream(data.size)
    val buffer = ByteArray(1024)
    while (!inflater.finished()) {
      val count = inflater.inflate(buffer)
      outputStream.write(buffer, 0, count)
    }
    outputStream.close()
    inflater.end()
    return outputStream.toByteArray()
  }

  companion object {
    private val sLogger = Logger("RemoteConfiguration")
  }
}