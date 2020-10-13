package com.monet.bidder

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.WindowManager.LayoutParams
import androidx.browser.customtabs.CustomTabColorSchemeParams.Builder
import androidx.browser.trusted.TrustedWebActivityDisplayMode
import androidx.browser.trusted.TrustedWebActivityDisplayMode.ImmersiveMode
import androidx.browser.trusted.TrustedWebActivityIntentBuilder
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.androidbrowserhelper.trusted.ChromeUpdatePrompt
import com.google.androidbrowserhelper.trusted.LauncherActivityMetadata
import com.google.androidbrowserhelper.trusted.TwaLauncher
import com.google.androidbrowserhelper.trusted.TwaLauncher.FallbackStrategy
import com.google.androidbrowserhelper.trusted.TwaSharedPreferencesManager
import com.monet.R.style
import com.monet.bidder.Constants.APPMONET_BROADCAST
import com.monet.bidder.Constants.APPMONET_BROADCAST_MESSAGE

open class TrustedInterstitialActivity : Activity() {
  private var mMetadata: LauncherActivityMetadata? = null
  private var mBrowserWasLaunched = false
  @JvmField protected var uuid: String? = null
  protected var url: String? = null
  private var mTwaLauncher: TwaLauncher? = null
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    uuid = intent.getStringExtra(UUID)
    url = intent.getStringExtra(URL)
    if (isTrustedActivity) {
      setTheme(style.TrustedInterstitialActivityTheme)
      if (restartInNewTask()) {
        finish()
      } else if (savedInstanceState != null && savedInstanceState.getBoolean(
              BROWSER_WAS_LAUNCHED_KEY
          )
      ) {
        finish()
      } else {
        mMetadata = LauncherActivityMetadata.parse(this)
        val darkModeColorScheme = Builder().setToolbarColor(
            getColorCompat(mMetadata!!.statusBarColorDarkId)
        )
            .setNavigationBarColor(getColorCompat(mMetadata!!.navigationBarColorDarkId))
            .build()
        val twaBuilder = TrustedWebActivityIntentBuilder(launchingUrl)
            .setToolbarColor(getColorCompat(mMetadata!!.statusBarColorId))
            .setNavigationBarColor(getColorCompat(mMetadata!!.navigationBarColorId))
            .setColorScheme(0).setColorSchemeParams(2, darkModeColorScheme)
            .setDisplayMode(displayMode)
        mMetadata!!.additionalTrustedOrigins?.let {
          twaBuilder.setAdditionalTrustedOrigins(it)
        }
        mTwaLauncher = TwaLauncher(this)
        mTwaLauncher!!.launch(
            twaBuilder, null, Runnable { mBrowserWasLaunched = true }, fallbackStrategy
        )
        if (!sChromeVersionChecked) {
          ChromeUpdatePrompt.promptIfNeeded(this, mTwaLauncher!!.providerPackage)
          sChromeVersionChecked = true
        }
        TwaSharedPreferencesManager(this).writeLastLaunchedProviderPackageName(
            mTwaLauncher!!.providerPackage
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    if (isTrustedActivity) {
      finish()
    }
  }

  private fun getColorCompat(splashScreenBackgroundColorId: Int): Int {
    return ContextCompat.getColor(this, splashScreenBackgroundColorId)
  }

  override fun onRestart() {
    super.onRestart()
    if (mBrowserWasLaunched) {
      finish()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    if (isTrustedActivity) {
      if (mTwaLauncher != null) {
        mTwaLauncher!!.destroy()
      }
      LocalBroadcastManager.getInstance(this).sendBroadcast(
          Intent(APPMONET_BROADCAST)
              .putExtra(
                  APPMONET_BROADCAST_MESSAGE,
                  "interstitial_dismissed"
              )
      )
    }
  }

  override fun onBackPressed() {
    //do nothing
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    if (isTrustedActivity) {
      outState.putBoolean("BROWSER_WAS_LAUNCHED_KEY", mBrowserWasLaunched)
    }
  }

  protected val launchingUrl: Uri
    get() = Uri.parse(url)
  protected val fallbackStrategy: FallbackStrategy
    get() = if (FALLBACK_TYPE_WEBVIEW.equals(
            mMetadata!!.fallbackStrategyType, ignoreCase = true
        )
    ) TwaLauncher.WEBVIEW_FALLBACK_STRATEGY else TwaLauncher.CCT_FALLBACK_STRATEGY
  protected val displayMode: TrustedWebActivityDisplayMode
    get() = ImmersiveMode(true, LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT)

  private fun restartInNewTask(): Boolean {
    val hasNewTask = intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0
    var hasNewDocument = false
    if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
      hasNewDocument = intent.flags and Intent.FLAG_ACTIVITY_NEW_DOCUMENT != 0
    }
    if (hasNewTask && !hasNewDocument) return false
    val newIntent = Intent(intent)
    var flags = intent.flags
    flags = flags or Intent.FLAG_ACTIVITY_NEW_TASK
    if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
      flags = flags and Intent.FLAG_ACTIVITY_NEW_DOCUMENT.inv()
    }
    newIntent.flags = flags
    startActivity(newIntent)
    return true
  }

  private val isTrustedActivity: Boolean
    get() = uuid == null

  companion object {
    const val URL = "url"
    const val UUID = "uuid"
    private const val BROWSER_WAS_LAUNCHED_KEY =
      "android.support.customtabs.trusted.BROWSER_WAS_LAUNCHED_KEY"
    private const val FALLBACK_TYPE_WEBVIEW = "webview"
    private var sChromeVersionChecked = false
  }
}