package com.example.alphacinema.ui.player

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.alphacinema.ui.movie.detail.EpisodeUi
import com.example.alphacinema.ui.movie.detail.MovieDetailUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerMediaItemAdsInstrumentedTest {
    private val movie = MovieDetailUi(
        id = "demo-movie",
        title = "Demo Movie",
        subtitle = "Demo",
        year = "2026",
        ageRating = "HD",
        currentEpisode = "Tập 1",
        genres = emptyList(),
        description = "Demo",
        bannerUrl = "",
        posterUrl = "",
        episodes = listOf(EpisodeUi(id = "ep-1", name = "Tập 1", duration = "45 phút")),
        cast = emptyList(),
        recommendations = emptyList()
    )

    @Test
    fun freeUserWithAdTagBuildsMediaItemsWithAdsConfiguration() {
        val adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads"
        val items = buildEpisodeMediaItems(
            movie = movie,
            episodeVideoUrls = mapOf("ep-1" to "https://cdn.example.com/video.m3u8"),
            adTagUrl = adTagUrl,
            adsEnabled = true
        )

        val adsConfiguration = items.single().localConfiguration?.adsConfiguration
        assertNotNull(adsConfiguration)
        assertEquals(adTagUrl, adsConfiguration?.adTagUri.toString())
    }

    @Test
    fun paidUserBuildsMediaItemsWithoutAdsConfiguration() {
        val items = buildEpisodeMediaItems(
            movie = movie,
            episodeVideoUrls = mapOf("ep-1" to "https://cdn.example.com/video.m3u8"),
            adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads",
            adsEnabled = false
        )

        assertNull(items.single().localConfiguration?.adsConfiguration)
    }

    @Test
    fun blankAdTagBuildsMediaItemsWithoutAdsConfiguration() {
        val items = buildEpisodeMediaItems(
            movie = movie,
            episodeVideoUrls = mapOf("ep-1" to "https://cdn.example.com/video.m3u8"),
            adTagUrl = "",
            adsEnabled = true
        )

        assertNull(items.single().localConfiguration?.adsConfiguration)
    }
}
