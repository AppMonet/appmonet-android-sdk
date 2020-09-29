package com.monet.bidder;

import android.location.Location;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.monet.bidder.auction.AuctionRequest;
import com.monet.bidder.bid.BidResponse;
import java.util.Date;


public class AdServerAdRequest {
    protected static final Logger sLogger = new Logger("AdRequest");

    public AdServerAdRequest() {

    }

    @Nullable
    public Bundle getCustomTargeting() {
        return null;
    }

    Date getBirthday() {
        return null;
    }


    String getGender() {
        return null;
    }

    @Nullable
    public BidResponse getBid() {
        return null;
    }

    public boolean hasBid() {
        return false;
    }

    Location getLocation() {
        return null;
    }

    String getContentUrl() {
        return null;
    }


    public AuctionRequest apply(AuctionRequest instance, AdServerAdView adView) {
        return instance; // nothing
    }

    String getPublisherProvidedId() {
        return null;
    }

    private boolean shouldRemoveKey(String dynamicKeyPrefix, String key) {
        if (key == null) {
            return false;
        }

        return key.startsWith(Constants.KW_KEY_PREFIX) || (dynamicKeyPrefix != null && key.startsWith(dynamicKeyPrefix));
    }

    Bundle filterTargeting(Bundle targeting) {
        if (targeting == null) {
            return new Bundle();
        }

        Bundle filteredBundle = new Bundle();
        filteredBundle.putAll(targeting);

        String dynamicKeyPrefix = targeting.getString(Constants.CUSTOM_KW_PREFIX_KEY);
        for (String key : targeting.keySet()) {
            if (shouldRemoveKey(dynamicKeyPrefix, key)) {
                filteredBundle.remove(key);
            }
        }

        return filteredBundle;
    }
}
