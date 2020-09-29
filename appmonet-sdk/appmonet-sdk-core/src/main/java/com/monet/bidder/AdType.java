package com.monet.bidder;

import androidx.annotation.NonNull;

/**
 * Created by jose on 8/29/17.
 */

public enum AdType {
  BANNER("banner"),
  INTERSTITIAL("interstitial"),
  NATIVE("native");
  private String type;

  AdType(String type) {
    this.type = type;
  }

  @NonNull @Override
  public String toString() {
    return type;
  }
}
