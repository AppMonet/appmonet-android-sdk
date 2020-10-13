package com.monet.bidder;

import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestData {

  private static String DOB_KEY = "dob";
  private static String GENDER_KEY = "gender";
  private static String PPID_KEY = "ppid";
  private static String KVP_KEY = "kvp";
  private static String URL_KEY = "url";
  private static String SIZES_KEY = "sizes";
  private static String ADUNIT_KEY = "adunit_id";
  Date birthday;
  String gender;
  Location location;
  String contentURL;
  String ppid;
  String adUnitId;
  Map<String, String> additional;

  public RequestData(AdServerAdRequest adRequest, AdServerAdView adView) {
    if (adRequest == null) {
      return;
    }

    birthday = adRequest.getBirthday();
    adUnitId = adView.getAdUnitId();
    gender = adRequest.getGender();
    location = adRequest.getLocation();
    contentURL = adRequest.getContentUrl();
    ppid = adRequest.getPublisherProvidedId();
    additional = buildAdditional(adRequest);

    if (ppid == null) {
      ppid = "";
    }

    if (contentURL == null) {
      contentURL = "";
    }
  }

  private String serializeBundleObject(Object value) {
    if (value == null) {
      return null;
    }

    if (value instanceof String) {
      return (String) value;
    } else if (value instanceof Integer) {
      try {
        return Integer.toString((Integer) value);
      } catch (Exception e) {
        return null;
      }
    } else if (value instanceof Double) {
      try {
        return Double.toString((Double) value);
      } catch (Exception e) {
        return null;
      }
    } else if (value instanceof Float) {
      try {
        return Float.toString((Float) value);
      } catch (Exception e) {
        return null;
      }
    } else if (value instanceof List<?>) {
      List<String> items = new ArrayList<>();

      try {
        for (Object listItem : ((List<Object>) value)) {
          String serialized = serializeBundleObject(listItem);
          if (serialized != null) {
            items.add(serialized);
          }
        }

        return TextUtils.join(",", items);
      } catch (Exception e) {
        return null;
      }
    }

    return null;
  }

  private Map<String, String> buildAdditional(AdServerAdRequest adRequest) {
    Map<String, String> output = new HashMap<>();
    Bundle bundle = adRequest.getCustomTargeting();
    for (String key : bundle.keySet()) {
      Object value = bundle.get(key);
      String serialized = serializeBundleObject(value);
      if (serialized != null) {
        output.put(key, serialized);
      }
    }
    return output;
  }

  public String toJson() {
    JSONObject json = new JSONObject();

    try {
      json.put(DOB_KEY, birthday);
      json.put(ADUNIT_KEY, adUnitId);
      json.put(GENDER_KEY, gender);
      json.put(URL_KEY, contentURL);
      json.put(PPID_KEY, ppid);
      json.put(KVP_KEY, new JSONObject(additional));

      JSONObject locationJson = new JSONObject();
      if (location != null) {
        locationJson.put("lat", location.getLatitude());
        locationJson.put("lon", location.getLongitude());
        locationJson.put("accuracy", location.getAccuracy());
      }

      json.put("location", locationJson);
      return json.toString();
    } catch (JSONException e) {
      return "{}";
    }
  }
}
