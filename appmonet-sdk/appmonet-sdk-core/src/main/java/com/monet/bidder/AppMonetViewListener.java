package com.monet.bidder;

import android.view.View;

interface AppMonetViewListener {
  void onAdRefreshed(View view);
  AppMonetViewLayout getCurrentView();
}
