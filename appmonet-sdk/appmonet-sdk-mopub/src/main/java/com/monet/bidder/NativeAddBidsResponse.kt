package com.monet.bidder

import com.mopub.nativeads.MoPubNative
import com.mopub.nativeads.RequestParameters

class NativeAddBidsResponse internal constructor(
  val moPubNative: MoPubNative,
  val requestParameters: RequestParameters?
)