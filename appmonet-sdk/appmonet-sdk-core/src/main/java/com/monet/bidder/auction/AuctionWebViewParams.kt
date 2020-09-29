package com.monet.bidder.auction

import com.monet.bidder.*

internal class AuctionWebViewParams constructor(defaultAuctionUrl: String,
                                                preferences: Preferences,
                                                val appMonetContext: AppMonetContext) {
    private val sLogger = Logger("AuctionWebViewParams")
    private val WEBVIEW_HTML = "<html><head><link rel=\"icon\" href=\"data:;base64,=\"><title>" + Constants.SDK_VERSION + " (cx) </title>"
    private val WEBVIEW_FOOTER = "</script></head><body></body></html>"
    var auctionHtml: String
    var auctionUrl: String
    var webViewClient: MonetWebViewClient

    init {
        auctionUrl = preferences.getPref(Constants.AUCTION_URL_KEY, defaultAuctionUrl)
        // javascript can override what we've set here..
        // note that  we should check the auction html
        // to make sure it's good

        // javascript can override what we've set here..
        // note that  we should check the auction html
        // to make sure it's good
        var auctionHTMLHeader: String = preferences.getPref(
                Constants.AUCTION_HTML_KEY, WEBVIEW_HTML)
        var auctionJs: String = preferences.getPref(
                Constants.AUCTION_JS_KEY, Constants.AUCTION_JS_URL)

        if (!RenderingUtils.isValidUrl(auctionUrl)) {
            sLogger.warn("bad auction url configured", auctionUrl)
            auctionUrl = defaultAuctionUrl // in the event that JS sets a bad value
        }

        // protect against invalid saved values..

        // protect against invalid saved values..
        if (!auctionHTMLHeader.contains("<html")) {
            auctionHTMLHeader = WEBVIEW_HTML
        }

        if (!RenderingUtils.isValidUrl(auctionJs)) {
            sLogger.warn("invalid auction JS configured. Defaulting")
            auctionJs = Constants.AUCTION_JS_URL
        }

        // append the application id to the bidder

        // append the application id to the bidder
        auctionJs = RenderingUtils.appendQueryParam(
                auctionJs, "aid", appMonetContext.applicationId)

        // save it here so we can consider putting it through later

        // save it here so we can consider putting it through later
        auctionHtml = (auctionHTMLHeader +
                "<script src=\"" + auctionJs + "\">" + WEBVIEW_FOOTER)

        webViewClient = MonetWebViewClient(auctionHtml, auctionUrl)
    }
}
