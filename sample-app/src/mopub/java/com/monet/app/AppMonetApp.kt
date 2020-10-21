package com.monet.app

import android.app.Application
import android.widget.Toast
import com.monet.bidder.AppMonet
import com.monet.bidder.AppMonetConfiguration
import com.mopub.common.MoPub
import com.mopub.common.SdkConfiguration

class AppMonetApp : Application() {

  override fun onCreate() {
    super.onCreate()
    //MoPub initialization
    val sdkConfiguration = SdkConfiguration.Builder("b03e6dccfe9e4abab02470a39c88d5dc").build()

    MoPub.initializeSdk(this, sdkConfiguration) {
      Toast.makeText(applicationContext, "MoPub Init", Toast.LENGTH_SHORT).show()
    }

    //AppMonet initialization.
//    val appMonetConfiguration = AppMonetConfiguration.Builder().applicationId("pjdfkud").build()
    val appMonetConfiguration = AppMonetConfiguration.Builder().applicationId("3zeuyua").build()

    AppMonet.init(this, appMonetConfiguration)

    //Use this only during testing in order to get test ads.
    AppMonet.testMode()
  }
}