package com.monet.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import android.widget.Toast
import com.monet.app.databinding.ActivityMainBinding
import com.monet.app.databinding.AdviewLayoutBinding
import com.monet.app.databinding.NativeLayoutBinding

abstract class BaseActivity : AppCompatActivity() {
  protected lateinit var binding: ActivityMainBinding
  protected lateinit var adLayoutBinding: AdviewLayoutBinding
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WebView.setWebContentsDebuggingEnabled(true)
    binding = ActivityMainBinding.inflate(layoutInflater)
    adLayoutBinding = AdviewLayoutBinding.bind(binding.root)
    setContentView(binding.root)
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
  open fun setupNativeClickListener() {}
  open fun showNativeSection() {}
}
