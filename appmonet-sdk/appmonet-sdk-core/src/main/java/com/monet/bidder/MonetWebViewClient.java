package com.monet.bidder;

import android.net.Uri;
import android.os.Build;
import androidx.annotation.RequiresApi;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import static com.monet.bidder.WebViewUtils.buildResponse;
import static com.monet.bidder.WebViewUtils.looseUrlMatch;

/**
 * This WebViewClient subclass handles intercepting requests to the auction URL,
 * which sends a preconfigured static HTML.
 */
public class MonetWebViewClient extends BaseWebViewClient {
    private final String mHtml;
    private final String mAuctionURL;

    public MonetWebViewClient(String stageHtml, String auctionUrl) {
        super();
        mHtml = stageHtml;
        mAuctionURL = auctionUrl;
    }

    @Override
    public WebResourceResponse shouldInterceptRequestInner(WebView view, String url) {
        if (looseUrlMatch(url, mAuctionURL)) {
            return buildResponse(mHtml);
        }

        if (url.contains("favicon.ico")) {
            return getBlankPixelResponse();
        }

        try {
            if (url.contains(Constants.AUCTION_WEBVIEW_HOOK)) {
                WebResourceResponse resp = hookResponse(url);
                if (resp != null) {
                    return resp;
                }
            }
        } catch (Exception e) {
            // do nothing
        }
        return super.shouldInterceptRequestInner(view, url);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WebResourceResponse shouldInterceptRequestInner(WebView view, WebResourceRequest request) {
        Uri uri = request.getUrl();
        String url = uri.toString();

        if (looseUrlMatch(url, mAuctionURL)) {
            return buildResponse(mHtml);
        }

        if (url.contains("favicon.ico")) {
            return getBlankPixelResponse();
        }

        return super.shouldInterceptRequestInner(view, request);
    }

    /**
     * This method returns a {@link WebResourceResponse}.
     *
     * @param url This contains the script url as a query parameter which is to be added as a src
     *            on the html web resource.
     * @return {@link WebResourceResponse}
     */
    private WebResourceResponse hookResponse(String url) {
        try {
            Uri uri = Uri.parse(url);
            return (uri == null) ? null :
                    buildResponse(String.format("<html><body><script src=\"%s\"></script></body></html>",
                            uri.getQueryParameter(Constants.AUCTION_WV_HK_PARAM)));
        } catch (Exception e) {
            return null;
        }
    }
}
