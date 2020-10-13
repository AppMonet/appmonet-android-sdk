package com.monet.bidder;

import android.content.Context;
import android.net.Uri;
import android.net.http.HttpResponseCache;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

class DownloadManager {
  enum DownloadStrategy {
    CACHE_IF_AVAILABLE,
    NETWORK
  }

  private final static Logger sLogger = new Logger("DownloadManager");

  private Uri uri;
  int timeout = 8000;
  DownloadStrategy downloadStrategy = DownloadStrategy.CACHE_IF_AVAILABLE;

  DownloadManager(Context context, Uri uri) {
    File httpCacheDir = new File(context.getCacheDir(), "monet");
    long httpCacheSize = 10L * 1024L * 1024L; // 10 MB
    try {
      if (HttpResponseCache.getInstalled() == null) {
        HttpResponseCache.install(httpCacheDir, httpCacheSize);
      }
    } catch (IOException e) {
      sLogger.debug("Error setting http caching: " + e.getMessage());
    }
    this.uri = uri;
  }

  HttpRequest get() throws KeyManagementException, NoSuchAlgorithmException {
    HttpRequest request = new HttpRequest(uri.toString(), HttpRequest.METHOD_GET);
    request.useCaches(downloadStrategy == DownloadStrategy.CACHE_IF_AVAILABLE);
    request.disableSslv3();
    request.connectTimeout(timeout);
    if (downloadStrategy == DownloadStrategy.NETWORK) {
      request.parameter("Cache-Control", "no-cache");
    }
    return request;
  }
}
