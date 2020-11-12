package com.monet

import com.monet.adview.AdSize

interface IBaseManager {
  fun indicateRequest(
    adUnitId: String,
    adSize: AdSize?,
    adType: AdType,
    floorCpm: Double
  )

  fun indicateRequestAsync(
    adUnitId: String,
    timeout: Int,
    adSize: AdSize?,
    adType: AdType,
    floorCpm: Double,
    callback: Callback<String?>
  )
}