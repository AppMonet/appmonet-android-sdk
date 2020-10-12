package com.monet.bidder

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.monet.bidder.Constants.APPMONET_BROADCAST_MESSAGE
import com.monet.bidder.Constants.INTERSTITIAL_ACTIVITY_BROADCAST
import com.monet.bidder.Constants.INTERSTITIAL_ACTIVITY_CLOSE

class MonetActivity : TrustedInterstitialActivity() {
  private val messageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(
      context: Context,
      intent: Intent
    ) {
      val message = intent.getStringExtra(APPMONET_BROADCAST_MESSAGE)
      if (INTERSTITIAL_ACTIVITY_CLOSE == message) {
        finish()
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val sdkManager = SdkManager.get()
    if (sdkManager == null) {
      LocalBroadcastManager.getInstance(this)
          .sendBroadcast(getBroadcastIntent(INTERNAL_ERROR))
      return
    }
    try {
      LocalBroadcastManager.getInstance(this).registerReceiver(
          messageReceiver,
          IntentFilter(INTERSTITIAL_ACTIVITY_BROADCAST)
      )
      if (uuid != null) {
        val view: View = InterstitialView(this, sdkManager, uuid)
        setContentView(
            view, LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT
        )
        )
      }
      LocalBroadcastManager.getInstance(this)
          .sendBroadcast(getBroadcastIntent(INTERSTITIAL_SHOWN))
    } catch (e: Exception) {
      LocalBroadcastManager.getInstance(this)
          .sendBroadcast(getBroadcastIntent(INTERNAL_ERROR))
    }
  }

  override fun onBackPressed() {}
  override fun onDestroy() {
    super.onDestroy()
    LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver)
  }

  private fun getBroadcastIntent(message: String): Intent {
    return Intent(APPMONET_BROADCAST)
        .putExtra("message", message)
  }

  companion object {
    private val logger = MonetLogger("MonetActivity")
    private const val BID_ID = "bidId"
    private const val APPMONET_BROADCAST = "appmonet-broadcast"
    private const val INTERNAL_ERROR = "internal_error"
    const val INTERSTITIAL_SHOWN = "interstitial_shown"
    @JvmStatic @Throws(ActivityNotFoundException::class) internal fun start(
      context: Context,
      sdkManager: SdkManager,
      uuid: String?,
      url: String?
    ) {
      val intent = Intent(context, MonetActivity::class.java)
      intent.putExtra(UUID, uuid)
      intent.putExtra(URL, url)
      try {
        context.startActivity(intent)
      } catch (e: ActivityNotFoundException) {
        sdkManager.auctionManager.auctionWebView.trackEvent(
            "integration_error",
            "missing_interstitial_activity", "onStart", 0f, 0L
        )
        logger.error(
            """
  Unable to create activity. Not defined in AndroidManifest.xml. Please refer to https://docs.appmonet.com/ for integration infomration.
  ${e.message}
  """.trimIndent()
        )
        throw e
      }
    }
  }
}