package com.monet.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import android.widget.Toast

abstract class BaseActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WebView.setWebContentsDebuggingEnabled(true)
    setContentView(R.layout.activity_main)
    setupMrect()
    setupInterstitial()
    showNativeSection()

    setupMrectLoadClickListener()
    setupInterstitialLoadClickListener()
    setupInterstitialShowClickListener()
    setupNativeClickListener()
  }

  protected fun showToast(message: String) {
    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
  }

  abstract fun setupMrect()
  abstract fun setupInterstitial()
  abstract fun setupMrectLoadClickListener()
  abstract fun setupInterstitialLoadClickListener()
  abstract fun setupInterstitialShowClickListener()
  open fun setupNativeClickListener(){}
  open fun showNativeSection(){}
}
