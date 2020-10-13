package com.monet.bidder

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.RelativeLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.monet.bidder.Constants.APPMONET_BROADCAST_MESSAGE
import com.monet.bidder.Constants.INTERSTITIAL_ACTIVITY_BROADCAST
import com.monet.bidder.Constants.INTERSTITIAL_ACTIVITY_CLOSE
import org.json.JSONException

class MonetDfpActivity : TrustedInterstitialActivity() {
  private var sdkManager: SdkManager? = null
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
    sdkManager = SdkManager.get()
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
        val view = if (intent.getStringExtra(BID_ID) == null) {
          createSingleAdView()
        } else {
          createInteractiveView()
        }
        if (view == null) {
          LocalBroadcastManager.getInstance(this)
              .sendBroadcast(getBroadcastIntent(INTERNAL_ERROR))
          return
        }
        setContentView(
            view, RelativeLayout.LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        )
      }
    } catch (e: Exception) {
      LocalBroadcastManager.getInstance(this)
          .sendBroadcast(getBroadcastIntent(INTERNAL_ERROR))
    }
  }

  override fun onBackPressed() {
    // do nothing
  }

  override fun onDestroy() {
    super.onDestroy()
    LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver)
  }

  private fun createInteractiveView(): View? {
    return try {
      sdkManager?.let {
        InterstitialView(this, it, intent.getStringExtra(UUID))
      }
    } catch (e: JSONException) {
      null
    }
  }

  private fun createSingleAdView(): View? {
    return try {
      sdkManager?.let {
        InterstitialView(this, it, intent.getStringExtra(UUID))
      }
    } catch (e: JSONException) {
      null
    }
  }

  companion object {
    private val logger = Logger("MonetDfpActivity")
    private const val BID_ID = "bidId"
    private const val APPMONET_BROADCAST = "appmonet-broadcast"
    private const val INTERNAL_ERROR = "internal_error"
    internal fun start(
      context: Context,
      sdkManager: SdkManager,
      uuid: String?,
      url: String?
    ) {
      val intent = Intent(context, MonetDfpActivity::class.java)
      intent.putExtra(UUID, uuid)
      intent.putExtra(URL, url)
      startActivity(context, sdkManager, intent)
    }

    private fun startActivity(
      context: Context,
      sdkManager: SdkManager,
      intent: Intent
    ) {
      try {
        context.startActivity(intent)
      } catch (e: ActivityNotFoundException) {
        sdkManager.auctionManager.trackEvent(
            "integration_error",
            "missing_interstitial_activity", "onStart", 0f, 0L
        )
        logger.error("Unable to create activity. Not defined in AndroidManifest.xml.")
      }
    }

    private fun getBroadcastIntent(message: String): Intent {
      return Intent(APPMONET_BROADCAST)
          .putExtra("message", message)
    }
  }
}