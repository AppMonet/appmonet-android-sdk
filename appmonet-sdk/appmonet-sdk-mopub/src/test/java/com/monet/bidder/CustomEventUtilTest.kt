package com.monet.bidder

import com.monet.bidder.Constants.Dfp.ADUNIT_KEYWORD_KEY
import junit.framework.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CustomEventUtilTest {
  @Test
  fun `Given getAdUnit is called and localExtras contains ADUNIT_KEYWORD_KEY key then return the value`() {
    val serverExtras = mutableMapOf<String, String?>()
    val localExtras = mutableMapOf<String, Any?>()
    val adSize = AdSize(0, 0)
    localExtras[ADUNIT_KEYWORD_KEY] = "adUnit"
    val adUnit = CustomEventUtil.getAdUnitId(serverExtras, localExtras.toMap(), adSize)
    assertEquals(adUnit, "adUnit")
  }

  @Test
  fun `Given getAdUnit is called and serverExtras contains ADUNIT_KEYWORD_KEY key then return the value`() {
    val serverExtras = mutableMapOf<String, String?>()
    val adSize = AdSize(0, 0)
    serverExtras[ADUNIT_KEYWORD_KEY] = "ADUNIT_KEYWORD_KEY"
    serverExtras["adunitId"] = "SERVER_EXTRA_ADUNIT_KEY"
    serverExtras["tagid"] = "SERVER_EXTRA_LOWER_TAG_ID_KEY"
    serverExtras["tagId"] = "SERVER_EXTRA_TAG_ID_KEY"
    val adUnit = CustomEventUtil.getAdUnitId(serverExtras, adSize)
    assertEquals(adUnit, "ADUNIT_KEYWORD_KEY")
  }

  @Test
  fun `Given getAdUnit is called and serverExtras contains SERVER_EXTRA_ADUNIT_KEY key then return the value`() {
    val serverExtras = mutableMapOf<String, String?>()
    val adSize = AdSize(0, 0)
    serverExtras["adunitId"] = "SERVER_EXTRA_ADUNIT_KEY"
    serverExtras["tagid"] = "SERVER_EXTRA_LOWER_TAG_ID_KEY"
    serverExtras["tagId"] = "SERVER_EXTRA_TAG_ID_KEY"
    val adUnit = CustomEventUtil.getAdUnitId(serverExtras, adSize)
    assertEquals(adUnit, "SERVER_EXTRA_ADUNIT_KEY")
  }

  @Test
  fun `Given getAdUnit is called and serverExtras contains SERVER_EXTRA_TAG_ID_KEY key then return the value`() {
    val serverExtras = mutableMapOf<String, String?>()
    val adSize = AdSize(0, 0)
    serverExtras["tagid"] = "SERVER_EXTRA_LOWER_TAG_ID_KEY"
    serverExtras["tagId"] = "SERVER_EXTRA_TAG_ID_KEY"
    val adUnit = CustomEventUtil.getAdUnitId(serverExtras, adSize)
    assertEquals(adUnit, "SERVER_EXTRA_TAG_ID_KEY")
  }

  @Test
  fun `Given getAdUnit is called and serverExtras contains SERVER_EXTRA_LOWER_TAG_ID_KEY key then return the value`() {
    val serverExtras = mutableMapOf<String, String?>()
    val adSize = AdSize(0, 0)
    serverExtras["tagid"] = "SERVER_EXTRA_LOWER_TAG_ID_KEY"
    val adUnit = CustomEventUtil.getAdUnitId(serverExtras, adSize)
    assertEquals(adUnit, "SERVER_EXTRA_LOWER_TAG_ID_KEY")
  }

  @Test
  fun `Given getAdUnit is called and serverExtras does not contain any keys then return width x height`() {
    val serverExtras = mutableMapOf<String, String?>()
    val adSize = AdSize(300, 250)
    val adUnit = CustomEventUtil.getAdUnitId(serverExtras, adSize)
    assertEquals(adUnit, "300x250")
  }

  @Test
  fun `Given getServerExtraCpm is called and serverExtras does not contain SERVER_EXTRA_CPM_KEY return default value`() {
    val serverExtras = mapOf<String, String>()
    val cpm = CustomEventUtil.getServerExtraCpm(serverExtras, 0.0)
    assertEquals(cpm, 0.0)
  }

  @Test
  fun `Given getServerExtraCpm is called and serverExtras contains SERVER_EXTRA_CPM_KEY return value`() {
    val serverExtras = mutableMapOf<String, String>()
    serverExtras["cpm"] = "1"
    val cpm = CustomEventUtil.getServerExtraCpm(serverExtras, 0.0)
    assertEquals(cpm, 1.0)
  }

  @Test
  fun `Given getServerExtraCpm is called and serverExtras contains SERVER_EXTRA_CPM_KEY and its not a double string value return default value`() {
    val serverExtras = mutableMapOf<String, String>()
    serverExtras["cpm"] = "notADouble"
    val cpm = CustomEventUtil.getServerExtraCpm(serverExtras, 0.0)
    assertEquals(cpm, 0.0)
  }
}