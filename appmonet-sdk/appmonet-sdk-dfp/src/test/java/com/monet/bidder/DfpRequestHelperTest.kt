package com.monet.bidder

import android.os.Bundle
import com.monet.adview.AdSize
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DfpRequestHelperTest {
    private val adSize = AdSize(300, 250)
    @Test
    fun `Given ADUNIT_KEYWORD_KEY is present in Bundle  get adUnitId from customEventExtras`() {
        val adUnitId = "adUnitId"
        val serverParameter = "serverParam"
        val customEventExtras = Bundle()
        customEventExtras.putString(Constants.Dfp.ADUNIT_KEYWORD_KEY, adUnitId)
        val helperAdUnit = DfpRequestHelper.getAdUnitID(customEventExtras, serverParameter, adSize)
        Assert.assertEquals(helperAdUnit, adUnitId)
    }

    @Test
    fun `Given ADUNIT_KEYWORD_KEY is not present and serverParameter is not null, 'default' or 'AMAdSize' and it starts with $ sign`() {
        val adUnit = DfpRequestHelper.getAdUnitID(Bundle(), "$100", adSize);
        Assert.assertEquals("300x250", adUnit)
    }

    @Test
    fun `Given ADUNIT_KEYWORD_KEY is not present and serverParameter is not null, 'default' or 'AMAdSize' and it does not start with $ sign (adunit cpm format)`() {
        val serverParam = "test_interstitial@$100"
        val adUnit = DfpRequestHelper.getAdUnitID(Bundle(), serverParam, adSize)
        Assert.assertEquals(adUnit, "test_interstitial")
    }

    @Test
    fun `Given ADUNIT_KEYWORD_KEY is not present and serverParameter is not null, 'default' or 'AMAdSize' and it does not start with $ sign (normal format)`() {
        val normalAdUnitServerParam = "only_ad_unit"
        val adUnit = DfpRequestHelper.getAdUnitID(Bundle(), normalAdUnitServerParam, adSize)
        Assert.assertEquals(adUnit, normalAdUnitServerParam)
    }

    @Test
    fun `Given ADUNIT_KEYWORD_KEY is not present and serverParameters default or AMAdSize`() {
        val defaultServerParam = "default"
        val adUnit = DfpRequestHelper.getAdUnitID(Bundle(), defaultServerParam, adSize)
        Assert.assertEquals(adUnit, "300x250")
        val amAdSizeServerParam = "AMAdSize"
        val unit = DfpRequestHelper.getAdUnitID(Bundle(), amAdSizeServerParam, adSize)
        Assert.assertEquals(unit, "300x250")
    }

    @Test
    fun `Given ADUNIT_KEYWORD_KEY is not present and serverParameter is empty get ad size as adUnit`() {
        val serverParam = ""
        val adUnit = DfpRequestHelper.getAdUnitID(Bundle(), serverParam, adSize)
        Assert.assertEquals(adUnit, "300x250")
    }

    @Test
    fun `Given serverParameter is empty or null return 0 cpm`() {
        Assert.assertEquals(DfpRequestHelper.getCpm(""), 0.0, 0.0)
        Assert.assertEquals(DfpRequestHelper.getCpm(null), 0.0, 0.0)
    }

    @Test
    fun `Given serverParameter starts with $ sign followed by a number`() {
        val cpm = DfpRequestHelper.getCpm("$5.00")
        Assert.assertEquals(cpm, 5.00, 5.00)
    }

    @Test
    fun `Given serverParameter starts with $sign followed by not a number`() {
        val cpm = DfpRequestHelper.getCpm("\$JOSE")
        Assert.assertEquals(cpm, 0.0, 0.0)
    }

    @Test
    fun `Given serverParameter has adUnitId and cpm`(){
        val cpm = DfpRequestHelper.getCpm("test_adunit@$5.00")
        Assert.assertEquals(cpm, 5.0, 5.0)
    }

    @Test
    fun `Given serverParameter has adUnitId and cpm not as a number`(){
        val cpm = DfpRequestHelper.getCpm("test_adunit@\$JOSE")
        Assert.assertEquals(cpm, 0.0, 0.0)
    }
}