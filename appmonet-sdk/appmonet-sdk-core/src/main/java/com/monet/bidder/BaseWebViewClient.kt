package com.monet.bidder;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.monet.bidder.WebViewUtils.buildResponse;

public class BaseWebViewClient extends WebViewClient {
    private Map<String, String> mDefaultHeaders = new HashMap<>();
    private WebResourceResponse mBlankResponse = buildResponse("", "text/plain");

    public void setHeaders(Map<String, String> headers) {
        if (headers != null) {
            mDefaultHeaders = headers;
        }
    }

    public WebResourceResponse getBlankPixelResponse() {
        return mBlankResponse;
    }

    public WebResourceResponse shouldInterceptRequestInner(WebView view, WebResourceRequest request) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return super.shouldInterceptRequest(view, request);
        }

        Uri uri = request.getUrl();
        return super.shouldInterceptRequest(view, uri.toString());
    }

    // exceptions in these methods propagate to
    // the chromium webView code (they're not handled well by other try/catch)
    // we create new signatures that wrap in try/catch so we don't accidentally
    // crash the app if we mess this up.

    @SuppressWarnings("deprecation")
    public WebResourceResponse shouldInterceptRequestInner(WebView view, String url) {
        return super.shouldInterceptRequest(view, url);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        try {
            return shouldInterceptRequestInner(view, request);
        } catch (Exception e) {
            return super.shouldInterceptRequest(view, request);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        try {
            return shouldInterceptRequestInner(view, url);
        } catch (Exception e) {
            return super.shouldInterceptRequest(view, url);
        }
    }

    @SuppressWarnings("deprecation")
    public boolean shouldOverrideUrlLoadingInner(WebView view, WebResourceRequest request) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return super.shouldOverrideUrlLoading(view, request);
        }

        return shouldOverrideUrlLoading(view, request.getUrl().toString());
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        try {
            return shouldOverrideUrlLoadingInner(view, request);
        } catch (Exception e) {
            return super.shouldOverrideUrlLoading(view, request);
        }
    }

    @SuppressWarnings("deprecation")
    public boolean shouldOverrideUrlLoadingInner(WebView view, String url) {
        return super.shouldOverrideUrlLoading(view, url);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        try {
            return shouldOverrideUrlLoadingInner(view, url);
        } catch (Exception e) {
            return super.shouldOverrideUrlLoading(view, url);
        }
    }

    public void onPageStartedInner(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        try {
            onPageStartedInner(view, url, favicon);
        } catch (Exception e) {
            super.onPageStarted(view, url, favicon);
        }
    }
}
