package com.monet.bidder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class InterstitialData {
  InterstitialMetadata metadata;
  ArrayList<InterstitialContent> content;

  InterstitialData(String rawData) throws JSONException {
    JSONObject jsonObject = new JSONObject(rawData);
    metadata = new InterstitialMetadata(jsonObject.getJSONObject("meta"));
    retrieveInterstitialContent(jsonObject.getJSONArray("content"));
  }

  private void retrieveInterstitialContent(JSONArray jsonArray) throws JSONException {
    content = new ArrayList<>(jsonArray.length());
    for (int i = 0; i < jsonArray.length(); i++) {
      content.add(new InterstitialContent(jsonArray.getJSONObject(i)));
    }
  }

  static class InterstitialMetadata {
    private JSONObject meta;

    InterstitialMetadata(JSONObject meta) {
      this.meta = meta;
    }

    String title() {
      return meta.optString("title");
    }

    String navColor() {
      return meta.optString("nav_color");
    }

    String initialOffset() {
      return meta.optString("initial_offset");
    }

    String initialDuration() {
      return meta.optString("initial_duration");
    }
  }

  static class InterstitialContent {
    private final JSONObject content;
    @Nullable private final Media media;

    InterstitialContent(JSONObject content) throws JSONException {
      this.content = content;
      JSONObject jsonMedia = content.optJSONObject("media");
      this.media = jsonMedia != null ? new Media(jsonMedia) : null;
    }

    String type() {
      return content.optString("type");
    }

    String id() {
      return content.optString("id");
    }

    String title() {
      return content.optString("title");
    }

    Media media() {
      return media;
    }

    static class Media {
      private final JSONObject jsonMedia;

      Media(@NonNull JSONObject media) {
        this.jsonMedia = media;
      }

      String mime() {
        return jsonMedia.optString("mime");
      }

      String source() {
        return jsonMedia.optString("src");
      }

      String thumbnail() {
        return jsonMedia.optString("thumbnail");
      }

      String quality() {
        return jsonMedia.optString("quality");
      }

      String duration() {
        return jsonMedia.optString("duration");
      }

      String width() {
        return jsonMedia.optString("width");
      }

      String height() {
        return jsonMedia.optString("height");
      }
    }
  }
}
