package com.monet.app

import android.app.Application
import com.monet.bidder.AppMonet
import com.monet.bidder.AppMonetConfiguration

class AppMonetApp : Application() {

  override fun onCreate() {
    super.onCreate()
    //AppMonet initialization.
    val appMonetConfiguration = AppMonetConfiguration.Builder()
        .applicationId("pjdfkud")
        .build()
    AppMonet.init(this, appMonetConfiguration)

    //Use this only during testing in order to get test ads.
    AppMonet.testMode()
  }
}