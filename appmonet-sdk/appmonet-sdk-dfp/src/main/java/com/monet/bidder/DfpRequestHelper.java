package com.monet.bidder;

import android.os.Bundle;

import static com.monet.bidder.Constants.Dfp.ADUNIT_KEYWORD_KEY;

class DfpRequestHelper {

  private DfpRequestHelper() {
  }

  static String getAdUnitID(Bundle customEventExtras, String serverParameter, AdSize adSize) {
    if (customEventExtras != null && customEventExtras.containsKey(ADUNIT_KEYWORD_KEY)) {
      return customEventExtras.getString(ADUNIT_KEYWORD_KEY);
    }

    String adUnit = null;
    if (serverParameter != null && !serverParameter.equals("default") && !serverParameter.equals(
        "AMAdSize")) {
      if (serverParameter.startsWith("$")) {
        adUnit = getWidthHeightAdUnit(adSize);
      } else {
        adUnit = parseServerParameter(serverParameter)[0];
      }
    }

    if (adUnit == null || adUnit.isEmpty()) {
      adUnit = getWidthHeightAdUnit(adSize);
    }

    return adUnit;
  }

  private static String getWidthHeightAdUnit(AdSize adSize) {
    String adUnit = null;
    if (adSize != null && adSize.getHeight() != 0 && adSize.getWidth() != 0) {
      adUnit = adSize.getWidth() + "x" + adSize.getHeight();
    }
    return adUnit;
  }

  static double getCpm(String serverParameter) {
    if (serverParameter == null || serverParameter.isEmpty()) {
      return 0;
    }

    // another option: a server parameter
    // is *only* the floor, e.g. $5.00...
    if (serverParameter.startsWith("$")) {
      try {
        return Double.parseDouble(serverParameter.substring(1));
      } catch (NumberFormatException e) {
        return 0;
      }
    }

    try {
      return Double.parseDouble(parseServerParameter(serverParameter)[1]);
    } catch (Exception e) {
      // error
    }
    return 0;
  }

  private static String[] parseServerParameter(String serverParameter) {
    return serverParameter.split("@\\$");
  }
}
