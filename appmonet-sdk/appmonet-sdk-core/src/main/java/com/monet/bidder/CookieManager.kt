package com.monet.bidder;

import android.content.Context;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Store cookies (in a dumb way), separately from WebView CookieManager
 * to create better privacy isolation from other WebViews in
 * the current Application, since all WV sync with this cookie manager
 */
public class CookieManager {
    private static final int MAX_COOKIE_HEADER = 4096;
    private static final String COOKIE_HEADER = "Cookie";
    static final String SET_COOKIE_HEADER = "Set-Cookie";
    private static final Object LOCK = new Object();
    private static final int DOMAIN_LIMIT = 400; // near the RFC spec
    private final static String DELIMITER = ":^#";
    private final static Pattern DELIMITER_PATTERN = Pattern.compile(":\\^#");
    private final static String COOKIE_PREF_KEY = "amBidderDataStore_v23x";
    private static CookieManager sInstance;
    private final Map<String, HashSet<Cookie>> mBulkStore = new LinkedHashMap<String, HashSet<Cookie>>(DOMAIN_LIMIT) {
        @Override
        protected boolean removeEldestEntry(Entry eldest) {
            return size() > DOMAIN_LIMIT;
        }
    };

    private static class Cookie {
        private static Pattern sKeyValueMath = Pattern.compile("([^;\\s]+)\\s*=\\s*([^;\\s]+)");
        private static Pattern sDotSeparator = Pattern.compile("\\.");

        private String mDomain;
        private String mKey; // actual cookie
        private String mValue;
        private String mCookie;
        private String mHashable;

        /**
         * Need to normalize all hosts/domains
         * so they can map despite subdomains and stuff
         * @param domain
         * @return
         */
        static String extractDomain(String domain) {
            // split by '.' and extract just the root domain + tld
            String[] parts = TextUtils.split(domain, sDotSeparator);

            // return the 2nd-to-last part
            if (parts.length >= 2) {
                int len = parts.length - 2;
                if (parts[len] != null && parts[len].length() > 0) {
                    return parts[len];
                }

                // return the last part instead.. hmm
                // should never happen
                return parts[len + 1];
            }

            return "unknown.com";
        }

        static String join(Set<Cookie> cookies) {
            if (cookies == null || cookies.size() == 0) {
                return "";
            }

            String[] cookieValues = new String[cookies.size()];

            int index = 0;
            for (Cookie cookie : cookies) {
                cookieValues[index] = cookie.mKey + "=" + cookie.mValue;
                index++;
            }

            return TextUtils.join(";", cookieValues);
        }

        private Cookie(String header) {
            Matcher keyValueMatch = sKeyValueMath.matcher(header);
            while (keyValueMatch.find()) {
                String key = keyValueMatch.group(1).trim();
                String value = keyValueMatch.group(2);

                switch (key.toLowerCase()) {
                    case "domain":
                        mDomain = extractDomain(value);
                        break;
                    case "expires":
                    case "path":
                    case "max-age":
                    case "secure":
                        break;
                    default:
                        if (mKey == null) {
                            mKey = key;
                            mValue = value;
                        }
                        break;
                }
            }

            mCookie = header;
            mHashable = mKey + mDomain; // only hash on key + domain, since value could be a timestamp
        }

        /**
         * Since we don't really care about Expiration,
         * we will compare cookies based on their key, value, and domain
         * @param obj any object to compare
         * @return if it's an equal cookie
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (!(obj instanceof Cookie)) {
                return false;
            }

            Cookie other = (Cookie) obj;
            return other.mHashable.equals(mHashable);
        }

        /**
         * Override the hashcode of the Cookie so it
         * can be stored in a set
         * @return
         */
        @Override
        public int hashCode() {
            return mHashable.hashCode();
        }
    }

    private CookieManager() {
    }

    /**
     * Get the singleton instance
     * @return CookieManager instance
     */
    public static CookieManager getInstance() {
        synchronized (LOCK) {
            if (sInstance == null) {
                sInstance = new CookieManager();
            }
            return sInstance;
        }
    }

    /**
     * Given a cookie manager, produce a serialized (string)
     * representation of all of the cookies contained within.
     * @param instance a cookie manager
     * @return a single obfuscated string
     */
    private static String serialize(CookieManager instance) {
        List<String> cookies = new ArrayList<>();
        for (Map.Entry<String, HashSet<Cookie>> kvp : instance.mBulkStore.entrySet()) {
            for (Cookie cookie : kvp.getValue()) {
                cookies.add(cookie.mCookie);
            }
        }

        return RenderingUtils.base64Encode(
                RenderingUtils.encodeStringByXor(TextUtils.join(DELIMITER, cookies)));
    }

    private static List<String> deserialize(String source) {
        if (source == null || source.isEmpty()) {
            return null;
        }

        String[] components = TextUtils.split(RenderingUtils.encodeStringByXor(
                RenderingUtils.base64Decode(source)), DELIMITER_PATTERN);

        // we should never have persisted @ only 1 key,
        // so this would indicate incorrect formatting
        if (components.length == 1) {
            return null;
        }

        return Arrays.asList(components);
    }

    public void load(Context context) {
        Preferences preferences = new Preferences(context);
        if (preferences == null) {
            return;
        }

        String encoded = preferences.getPref(COOKIE_PREF_KEY, "");
        List<String> cookies = deserialize(encoded);
        if (cookies == null) {
            return;
        }

        for (String cookie : cookies) {
            add(cookie);
        }
    }

    public void save(Context context) {
        Preferences preferences = new Preferences(context);
        if (preferences == null) {
            return;
        }

        preferences.setPreference(COOKIE_PREF_KEY, serialize(this));
    }

    int size() {
        return mBulkStore != null ? mBulkStore.size() : 0;
    }

    public void clear() {
        if (mBulkStore == null) {
            return;
        }

        mBulkStore.clear();
    }

    /**
     * Add a cookie from a Set-Cookie header
     * partition by domain, and make unique by the cookie key-value
     * @param cookieHeader the value of Set-Cookie header
     */
    void add(String cookieHeader) {
        if (cookieHeader == null || cookieHeader.length() > MAX_COOKIE_HEADER) {
            return;
        }
        Cookie cookie = new Cookie(cookieHeader);
        if (!mBulkStore.containsKey(cookie.mDomain)) {
            mBulkStore.put(cookie.mDomain, new HashSet<Cookie>());
        }
        mBulkStore.get(cookie.mDomain).add(cookie);
    }

    /**
     * Get all available cookies for a given domain, as a string
     * semi-colon separated string.
     * @param domain the domain to lookup cookies for
     * @return all cookies matching that domain
     */
    String get(String domain) {
        String extracted = Cookie.extractDomain(domain);
        return Cookie.join(
                mBulkStore.get(extracted));
    }

    /**
     * Add the correct cookies for the given host
     * to the Cookie header of the headers map for this request
     * @param headers a map of headers to be made
     * @param host the host the request is for
     */
    void apply(Map<String, String> headers, String host) {
        String existing = headers.get(COOKIE_HEADER);
        String cookies = get(host);

        if (existing  == null) {
            existing = "";
        }

        if (cookies.isEmpty() && existing.isEmpty()) {
            return;
        }

        String found = get(host);
        String complete = (existing != null && !existing.isEmpty()) ? (found + ";" + existing) : found;
        headers.put(COOKIE_HEADER, complete);
    }

}
