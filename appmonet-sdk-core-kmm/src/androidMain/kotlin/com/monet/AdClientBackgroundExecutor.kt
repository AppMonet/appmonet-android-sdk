package com.monet

import android.content.Context
import android.os.AsyncTask
import android.provider.Settings.Secure
import java.lang.ref.WeakReference

internal class AdClientBackgroundExecutor constructor(
  context: Context?,
  private val valueCallback: Callback<AdInfo>
) : AsyncTask<Void?, Void?, AdInfo>() {
  private val context: WeakReference<Context?> = WeakReference(context)
  override fun doInBackground(vararg params: Void?): AdInfo {
    return adClientInfoInner
  }

  override fun onPostExecute(adInfo: AdInfo) {
    valueCallback(adInfo)
  }

  private val adClientInfoInner: AdInfo
    get() {
      val adInfo = AdInfo()
      if (context.get() != null) {
        try {
          val clazz = Class.forName(GMS_ADS_CLIENT_CLASS)
          val infoClazz = Class.forName("$GMS_ADS_CLIENT_CLASS\$Info")

          val getAdvertisingInfo = clazz.getDeclaredMethod(
              "getAdvertisingIdInfo", Context::class.java
          )
          val getId = infoClazz.getMethod("getId")
          val isLimitAdTrackingEnabled = infoClazz.getMethod("isLimitAdTrackingEnabled")
          val info = getAdvertisingInfo.invoke(null, context.get())
          if (info == null) {
//            sLogger.warn("unable to get advertising info from GMS")
            return amazonAdInfo
          }
          adInfo.advertisingId = getId.invoke(info) as String
          adInfo.isLimitAdTrackingEnabled = isLimitAdTrackingEnabled.invoke(info) as Boolean
        } catch (e: Exception) {
//          sLogger.warn("gms not detected. Using next method")
          return amazonAdInfo
        }
      }
      return adInfo
    }

  private val amazonAdInfo: AdInfo
    get() {
      val adInfo = AdInfo()
      try {
        val cr = context.get()?.contentResolver
        adInfo.isLimitAdTrackingEnabled = Secure.getInt(cr, "limit_ad_tracking") != 0
        adInfo.advertisingId = Secure.getString(cr, "advertising_id")
      } catch (e: Exception) {
        return adInfo
      }
      return adInfo
    }

  companion object {
//    private val sLogger = Logger("AdClientBackgroundExecutor")
    private const val GMS_ADS_CLIENT_CLASS =
      "com.google.android.gms.ads.identifier.AdvertisingIdClient"
  }
}