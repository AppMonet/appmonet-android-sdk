package com.monet.bidder;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.VisibleForTesting;
import android.util.Base64;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class RemoteConfiguration {
  private final static Logger sLogger = new Logger("RemoteConfiguration");
  private DownloadManager downloadManager;

  public RemoteConfiguration(Context context, String applicationId) {
    Uri configurationUri = Uri.parse(Constants.AUCTION_MANAGER_CONFIG_URL)
        .buildUpon()
        .appendPath("hb")
        .appendPath("c1")
        .appendPath(applicationId)
        .build();
    this.downloadManager = new DownloadManager(context, configurationUri);
    this.downloadManager.timeout = 8000; // 8s timeout
  }

  @VisibleForTesting
  RemoteConfiguration(DownloadManager downloadManager) {
    this.downloadManager = downloadManager;
  }

  public synchronized String getRemoteConfiguration(boolean forceServer) {
    String response = null;
    try {
      downloadManager.downloadStrategy = (forceServer) ? DownloadManager.DownloadStrategy.NETWORK
          : DownloadManager.DownloadStrategy.CACHE_IF_AVAILABLE;
      HttpRequest request = downloadManager.get();
      String httpResponse = request.body();
      request.disconnect();
      byte[] decodedResponse = Base64.decode(httpResponse, Base64.DEFAULT);
      byte[] output = decompress(decodedResponse);
      String decompressedResponse = new String(output, 0, output.length, "UTF-8");
      JSONObject body = new JSONObject(decompressedResponse);
      JSONObject main = new JSONObject();
      main.put("remoteAddr", request.header("X-Remote-Addr"));
      main.put("country", request.header("X-Client-Country"));
      main.put("isCached", request.header("X-Android-Response-Source").equals("CONDITIONAL_CACHE 304"));
      main.put("body", body);
      response = main.toString();
    } catch (Exception e) {
      sLogger.debug("error getting remote configuration");
      sLogger.debug(e.getMessage());
    }
    return response;
  }

  private byte[] decompress(byte[] data) throws IOException, DataFormatException {
    Inflater inflater = new Inflater();
    inflater.setInput(data);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
    byte[] buffer = new byte[1024];
    while (!inflater.finished()) {
      int count = inflater.inflate(buffer);
      outputStream.write(buffer, 0, count);
    }
    outputStream.close();
    inflater.end();
    return outputStream.toByteArray();
  }
}
