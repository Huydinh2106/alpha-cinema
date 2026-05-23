package com.example.alphacinema.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrerollAdsLogicTest {
    @Test
    fun enabledAdsWithAdTagAttachesPrerollAds() {
        assertTrue(
            shouldAttachPrerollAds(
                adsEnabled = true,
                adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads"
            )
        )
    }

    @Test
    fun disabledAdsDoNotAttachPrerollAds() {
        assertFalse(
            shouldAttachPrerollAds(
                adsEnabled = false,
                adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads"
            )
        )
    }

    @Test
    fun blankAdTagDoesNotAttachPrerollAds() {
        assertFalse(shouldAttachPrerollAds(adsEnabled = true, adTagUrl = ""))
        assertFalse(shouldAttachPrerollAds(adsEnabled = true, adTagUrl = "   "))
    }
}
