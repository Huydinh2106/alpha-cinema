package com.example.alphacinema.ui.player

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.example.alphacinema.data.local.SettingsManager
import com.example.alphacinema.data.repository.FirestoreRepository
import com.example.alphacinema.ui.movie.detail.EpisodeUi
import com.example.alphacinema.ui.movie.detail.MovieDetailUi
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

internal fun shouldAttachPrerollAds(
    adsEnabled: Boolean,
    adTagUrl: String
): Boolean = adsEnabled && adTagUrl.isNotBlank()

internal fun buildEpisodeMediaItems(
    movie: MovieDetailUi,
    episodeVideoUrls: Map<String, String>,
    adTagUrl: String = "",
    adsEnabled: Boolean = false
): List<MediaItem> {
    val attachAds = shouldAttachPrerollAds(
        adsEnabled = adsEnabled,
        adTagUrl = adTagUrl
    )
    val normalizedAdTagUrl = adTagUrl.trim()

    return movie.episodes.map { episode ->
        val url = episodeVideoUrls[episode.id]
        val builder = MediaItem.Builder()
            .setMediaId(episode.id)
            .setUri(url ?: "")

        if (attachAds) {
            builder.setAdsConfiguration(
                MediaItem.AdsConfiguration.Builder(Uri.parse(normalizedAdTagUrl))
                    .build()
            )
        }

        builder.build()
    }
}

class PlayerViewModel : ViewModel() {
    private val firestoreRepository = FirestoreRepository()

    fun buildEpisodeMediaItems(
        movie: MovieDetailUi,
        episodeVideoUrls: Map<String, String>,
        adTagUrl: String = "",
        adsEnabled: Boolean = false
    ): List<MediaItem> = com.example.alphacinema.ui.player.buildEpisodeMediaItems(
        movie = movie,
        episodeVideoUrls = episodeVideoUrls,
        adTagUrl = adTagUrl,
        adsEnabled = adsEnabled
    )

    fun getEpisodeIndex(
        movie: MovieDetailUi,
        episode: EpisodeUi?
    ): Int {
        return movie.episodes
            .indexOfFirst { it.id == episode?.id }
            .coerceAtLeast(0)
    }

    fun getEpisodeById(
        movie: MovieDetailUi,
        episodeId: String
    ): EpisodeUi? {
        return movie.episodes.find { it.id == episodeId }
    }

    fun saveWatchProgress(
        movie: MovieDetailUi,
        episode: EpisodeUi?,
        progress: Long,
        duration: Long
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (movie.id.isBlank()) return

        viewModelScope.launch {
            runCatching {
                val isKidsMode = runCatching {
                    SettingsManager.getInstance().isKidsModeEnabled.value
                }.getOrDefault(false)

                firestoreRepository.updateWatchProgress(
                    userId = userId,
                    movieSlug = movie.id,
                    movieName = movie.title,
                    originName = movie.subtitle,
                    posterUrl = movie.posterUrl,
                    episodeId = episode?.id.orEmpty(),
                    episodeName = episode?.name.orEmpty().ifBlank { movie.currentEpisode },
                    progress = progress.coerceAtLeast(0L),
                    duration = duration.coerceAtLeast(0L),
                    isKidsMode = isKidsMode
                )
            }
        }
    }
}
