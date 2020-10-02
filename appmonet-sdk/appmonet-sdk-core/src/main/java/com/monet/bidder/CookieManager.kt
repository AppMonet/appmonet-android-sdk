package com.monet.bidder

import android.content.Context
import android.text.TextUtils
import java.util.ArrayList
import java.util.HashSet
import java.util.LinkedHashMap
import java.util.Locale
import java.util.regex.Pattern
import kotlin.collections.MutableMap.MutableEntry

/**
 * Store cookies (in a dumb way), separately from WebView CookieManager
 * to create better privacy isolation from other WebViews in
 * the current Application, since all WV sync with this cookie manager
 */
class CookieManager private constructor() {
  private val mBulkStore: LinkedHashMap<String?, HashSet<Cookie?>?> =
    object : LinkedHashMap<String?, HashSet<Cookie?>?>(
        DOMAIN_LIMIT
    ) {
      override fun removeEldestEntry(eldest: MutableEntry<String?, HashSet<Cookie?>?>?): Boolean {
        return size > DOMAIN_LIMIT
      }
    }

  private class Cookie(header: String) {
    var mDomain: String? = null
    private var mKey // actual cookie
        : String? = null
    private var mValue: String? = null
    val mCookie: String
    private val mHashable: String

    /**
     * Since we don't really care about Expiration,
     * we will compare cookies based on their key, value, and domain
     * @param obj any object to compare
     * @return if it's an equal cookie
     */
    override fun equals(obj: Any?): Boolean {
      if (obj === this) {
        return true
      }
      if (obj !is Cookie) {
        return false
      }
      return obj.mHashable == mHashable
    }

    /**
     * Override the hashcode of the Cookie so it
     * can be stored in a set
     * @return
     */
    override fun hashCode(): Int {
      return mHashable.hashCode()
    }

    companion object {
      private val sKeyValueMath = Pattern.compile("([^;\\s]+)\\s*=\\s*([^;\\s]+)")
      private val sDotSeparator = Pattern.compile("\\.")

      /**
       * Need to normalize all hosts/domains
       * so they can map despite subdomains and stuff
       * @param domain
       * @return
       */
      fun extractDomain(domain: String?): String? {
        // split by '.' and extract just the root domain + tld
        val parts = TextUtils.split(domain, sDotSeparator)

        // return the 2nd-to-last part
        if (parts.size >= 2) {
          val len = parts.size - 2
          return if (parts[len] != null && parts[len].isNotEmpty()) {
            parts[len]
          } else parts[len + 1]

          // return the last part instead.. hmm
          // should never happen
        }
        return "unknown.com"
      }

      fun join(cookies: HashSet<Cookie?>?): String {
        if (cookies == null || cookies.isEmpty()) {
          return ""
        }
        val cookieValues = arrayOfNulls<String>(cookies.size)
        for ((index, cookie) in cookies.withIndex()) {
          cookie?.let {
            cookieValues[index] = it.mKey + "=" + it.mValue
          }
        }
        return TextUtils.join(";", cookieValues)
      }
    }

    init {
      val keyValueMatch = sKeyValueMath.matcher(header)
      while (keyValueMatch.find()) {
        val key = keyValueMatch.group(1).trim { it <= ' ' }
        val value = keyValueMatch.group(2)
        when (key.toLowerCase(Locale.getDefault())) {
          "domain" -> mDomain = extractDomain(value)
          "expires", "path", "max-age", "secure" -> {
          }
          else -> if (mKey == null) {
            mKey = key
            mValue = value
          }
        }
      }
      mCookie = header
      mHashable = mKey + mDomain // only hash on key + domain, since value could be a timestamp
    }
  }

  fun load(context: Context?) {
    val preferences = Preferences(context) ?: return
    val encoded = preferences.getPref(COOKIE_PREF_KEY, "")
    val cookies = deserialize(encoded) ?: return
    for (cookie in cookies) {
      add(cookie)
    }
  }

  fun save(context: Context?) {
    val preferences = Preferences(context) ?: return
    preferences.setPreference(COOKIE_PREF_KEY, serialize(this))
  }

  fun size(): Int {
    return mBulkStore.size
  }

  fun clear() {
    mBulkStore.clear()
  }

  /**
   * Add a cookie from a Set-Cookie header
   * partition by domain, and make unique by the cookie key-value
   * @param cookieHeader the value of Set-Cookie header
   */
  fun add(cookieHeader: String?) {
    if (cookieHeader == null || cookieHeader.length > MAX_COOKIE_HEADER) {
      return
    }
    val cookie = Cookie(cookieHeader)
    if (!mBulkStore.containsKey(cookie.mDomain)) {
      mBulkStore[cookie.mDomain] = HashSet()
    }
    mBulkStore[cookie.mDomain]!!.add(cookie)
  }

  /**
   * Get all available cookies for a given domain, as a string
   * semi-colon separated string.
   * @param domain the domain to lookup cookies for
   * @return all cookies matching that domain
   */
  operator fun get(domain: String?): String {
    val extracted = Cookie.extractDomain(domain)
    return Cookie.join(
        mBulkStore[extracted]
    )
  }

  /**
   * Add the correct cookies for the given host
   * to the Cookie header of the headers map for this request
   * @param headers a map of headers to be made
   * @param host the host the request is for
   */
  fun apply(
    headers: MutableMap<String?, String?>,
    host: String?
  ) {
    var existing = headers[COOKIE_HEADER]
    val cookies = get(host)
    if (existing == null) {
      existing = ""
    }
    if (cookies.isEmpty() && existing.isEmpty()) {
      return
    }
    val found = get(host)
    val complete = if (existing.isNotEmpty()) "$found;$existing" else found
    headers[COOKIE_HEADER] = complete
  }

  companion object {
    private const val MAX_COOKIE_HEADER = 4096
    private const val COOKIE_HEADER = "Cookie"
    const val SET_COOKIE_HEADER = "Set-Cookie"
    private val LOCK = Any()
    private const val DOMAIN_LIMIT = 400 // near the RFC spec
    private const val DELIMITER = ":^#"
    private val DELIMITER_PATTERN = Pattern.compile(":\\^#")
    private const val COOKIE_PREF_KEY = "amBidderDataStore_v23x"
    private var sInstance: CookieManager? = null

    /**
     * Get the singleton instance
     * @return CookieManager instance
     */
    @JvmStatic val instance: CookieManager?
      get() {
        synchronized(LOCK) {
          if (sInstance == null) {
            sInstance = CookieManager()
          }
          return sInstance
        }
      }

    /**
     * Given a cookie manager, produce a serialized (string)
     * representation of all of the cookies contained within.
     * @param instance a cookie manager
     * @return a single obfuscated string
     */
    private fun serialize(instance: CookieManager): String {
      val cookies: MutableList<String?> = ArrayList()
      for ((_, value) in instance.mBulkStore) {
        value?.let {
          for (cookie in it) {
            cookies.add(cookie?.mCookie)
          }
        }
      }
      return RenderingUtils.base64Encode(
          RenderingUtils.encodeStringByXor(TextUtils.join(DELIMITER, cookies))
      )
    }

    private fun deserialize(source: String?): List<String>? {
      if (source == null || source.isEmpty()) {
        return null
      }
      val components = TextUtils.split(
          RenderingUtils.encodeStringByXor(
              RenderingUtils.base64Decode(source)
          ), DELIMITER_PATTERN
      )

      // we should never have persisted @ only 1 key,
      // so this would indicate incorrect formatting
      return if (components.size == 1) {
        null
      } else listOf(*components)
    }
  }
}