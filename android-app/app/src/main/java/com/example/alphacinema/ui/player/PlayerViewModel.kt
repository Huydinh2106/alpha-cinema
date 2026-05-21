package com.example.alphacinema.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphacinema.data.repository.FirestoreRepository
import com.example.alphacinema.ui.movie.detail.EpisodeUi
import com.example.alphacinema.ui.movie.detail.MovieDetailUi
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class PlayerViewModel : ViewModel() {
    private val firestoreRepository = FirestoreRepository()

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
                firestoreRepository.updateWatchProgress(
                    userId = userId,
                    movieSlug = movie.id,
                    movieName = movie.title,
                    originName = movie.subtitle,
                    posterUrl = movie.posterUrl,
                    episodeId = episode?.id.orEmpty(),
                    episodeName = episode?.name.orEmpty().ifBlank { movie.currentEpisode },
                    progress = progress.coerceAtLeast(0L),
                    duration = duration.coerceAtLeast(0L)
                )
            }
        }
    }
}
