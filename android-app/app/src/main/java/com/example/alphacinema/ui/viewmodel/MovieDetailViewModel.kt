package com.example.alphacinema.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphacinema.CastUi
import com.example.alphacinema.EpisodeUi
import com.example.alphacinema.MovieDetailUi
import com.example.alphacinema.RecommendedMovieUi
import com.example.alphacinema.data.api.RetrofitClient
import com.example.alphacinema.data.model.MovieDetail
import com.example.alphacinema.data.model.MovieDetailResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MovieDetailViewModel : ViewModel() {
    private val api = RetrofitClient.instance

    private val _movieDetail = MutableStateFlow<MovieDetailUi?>(null)
    val movieDetail: StateFlow<MovieDetailUi?> = _movieDetail.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Store episode video URLs (link_m3u8)
    private val _episodeVideoUrls = MutableStateFlow<Map<String, String>>(emptyMap())
    val episodeVideoUrls: StateFlow<Map<String, String>> = _episodeVideoUrls.asStateFlow()

    fun loadMovieDetail(slug: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = api.getMovieDetail(slug)
                val movie = response.movie
                val episodes = response.episodes

                if (movie != null) {
                    val episodeList = mutableListOf<EpisodeUi>()
                    val videoUrlMap = mutableMapOf<String, String>()

                    episodes?.forEach { server ->
                        server.server_data.forEachIndexed { index, ep ->
                            val epId = "${slug}-ep-${index}"
                            episodeList.add(
                                EpisodeUi(
                                    id = epId,
                                    name = ep.name.ifBlank { "Tập ${index + 1}" },
                                    duration = movie.time ?: ""
                                )
                            )
                            if (ep.link_m3u8.isNotBlank()) {
                                videoUrlMap[epId] = ep.link_m3u8
                            } else if (ep.link_embed.isNotBlank()) {
                                videoUrlMap[epId] = ep.link_embed
                            }
                        }
                        // Only use the first server
                        if (episodeList.isNotEmpty()) return@forEach
                    }

                    val posterUrl = movie.getFullPosterUrl()
                    val thumbUrl = movie.thumb_url?.let {
                        if (it.startsWith("http")) it else "https://phimimg.com/$it"
                    } ?: posterUrl

                    _movieDetail.value = MovieDetailUi(
                        id = slug,
                        title = movie.name,
                        subtitle = movie.origin_name ?: "",
                        year = movie.year?.toString() ?: "",
                        ageRating = movie.quality ?: "HD",
                        currentEpisode = movie.episode_current ?: "Full",
                        genres = movie.category?.map { it.name } ?: emptyList(),
                        description = movie.content
                            ?.replace(Regex("<[^>]*>"), "") // strip HTML tags
                            ?.trim()
                            ?: "Chưa có mô tả.",
                        bannerUrl = thumbUrl,
                        posterUrl = posterUrl,
                        episodes = episodeList,
                        cast = movie.actor?.map { CastUi(it, "Diễn viên") } ?: emptyList(),
                        recommendations = emptyList()
                    )
                    _episodeVideoUrls.value = videoUrlMap
                } else {
                    _error.value = "Không tìm thấy phim"
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Lỗi tải chi tiết phim"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
