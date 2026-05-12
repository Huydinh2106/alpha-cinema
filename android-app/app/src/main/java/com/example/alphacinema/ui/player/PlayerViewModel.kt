package com.example.alphacinema.ui.player

import androidx.lifecycle.ViewModel
import com.example.alphacinema.ui.movie.detail.EpisodeUi
import com.example.alphacinema.ui.movie.detail.MovieDetailUi

class PlayerViewModel : ViewModel() {

    fun buildEpisodeMediaItems(
        movie: MovieDetailUi,
        episodeVideoUrls: Map<String, String>
    ): List<androidx.media3.common.MediaItem> {
        return movie.episodes.map { episode ->
            val url = episodeVideoUrls[episode.id]

            androidx.media3.common.MediaItem.Builder()
                .setMediaId(episode.id)
                .setUri(url ?: "")
                .build()
        }
    }

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
}