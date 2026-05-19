package com.example.alphacinema.ui.movie.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphacinema.data.api.RetrofitClient
import com.example.alphacinema.data.api.TmdbConfig
import com.example.alphacinema.data.model.Comment
import com.example.alphacinema.data.model.MovieDetail
import com.example.alphacinema.data.model.MovieDetailResponse
import com.example.alphacinema.data.model.MovieStats
import com.example.alphacinema.data.repository.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MovieDetailViewModel : ViewModel() {
    private val api = RetrofitClient.instance
    private val tmdbApi = RetrofitClient.tmdbApi
    private val firestoreRepository = FirestoreRepository()

    private val _movieDetail = MutableStateFlow<MovieDetailUi?>(null)
    val movieDetail: StateFlow<MovieDetailUi?> = _movieDetail.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Store episode video URLs (link_m3u8)
    private val _episodeVideoUrls = MutableStateFlow<Map<String, String>>(emptyMap())
    val episodeVideoUrls: StateFlow<Map<String, String>> = _episodeVideoUrls.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _movieStats = MutableStateFlow<MovieStats?>(null)
    val movieStats: StateFlow<MovieStats?> = _movieStats.asStateFlow()

    private val _userRating = MutableStateFlow<Int?>(null)
    val userRating: StateFlow<Int?> = _userRating.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private var activeUserId: String? = null
    private var activeMovieSlug: String? = null

    fun initFirebaseListeners(slug: String, userId: String?) {
        activeMovieSlug = slug
        activeUserId = userId
        
        viewModelScope.launch {
            firestoreRepository.getMovieStats(slug).collect {
                _movieStats.value = it
            }
        }
        viewModelScope.launch {
            firestoreRepository.getComments(slug).collect {
                _comments.value = it
            }
        }
        if (userId != null) {
            viewModelScope.launch {
                firestoreRepository.getFavorites(userId).collect { favorites ->
                    _isFavorite.value = favorites.any { it.movieId == slug }
                }
            }
            viewModelScope.launch {
                firestoreRepository.getUserRating(slug, userId).collect { rating ->
                    _userRating.value = rating?.score
                }
            }
        } else {
            _userRating.value = null
        }
    }

    fun toggleFavorite(movieName: String, posterUrl: String, onResult: (Boolean, String) -> Unit) {
        val uid = activeUserId
        if (uid == null) {
            onResult(false, "Vui lòng đăng nhập để thao tác")
            return
        }
        val slug = activeMovieSlug ?: return
        val currentFavState = _isFavorite.value
        viewModelScope.launch {
            _isFavorite.value = !currentFavState
            try {
                firestoreRepository.toggleFavorite(uid, slug, movieName, posterUrl, !currentFavState)
                onResult(true, if (!currentFavState) "Đã thêm vào yêu thích" else "Đã bỏ yêu thích")
            } catch (e: Exception) {
                _isFavorite.value = currentFavState
                onResult(false, "Lỗi cập nhật yêu thích")
            }
        }
    }

    fun postComment(userName: String, userAvatar: String, content: String, onResult: (Boolean, String) -> Unit) {
        val uid = activeUserId
        if (uid == null) {
            onResult(false, "Vui lòng đăng nhập để bình luận")
            return
        }
        val slug = activeMovieSlug ?: return
        if (content.isBlank()) return
        
        viewModelScope.launch {
            try {
                firestoreRepository.postComment(slug, uid, userName, userAvatar, content)
                onResult(true, "Đã gửi bình luận")
            } catch (e: Exception) {
               onResult(false, "Không thể gửi bình luận")
            }
        }
    }
    
    fun submitRating(score: Int, onResult: (Boolean, String) -> Unit) {
        val uid = activeUserId
        if (uid == null) {
            onResult(false, "Vui lòng đăng nhập để đánh giá")
            return
        }
        val slug = activeMovieSlug ?: return
        
        viewModelScope.launch {
            try {
                firestoreRepository.submitRating(slug, uid, score)
                onResult(true, "Cảm ơn bạn đã đánh giá")
            } catch (e: Exception) {
                 onResult(false, "Lỗi khi lưu đánh giá")
            }
        }
    }

    fun loadMovieDetail(slug: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            initFirebaseListeners(slug, FirebaseAuth.getInstance().currentUser?.uid)
            try {
                val response = api.getMovieDetail(slug)
                val movie = response.movie
                val episodes = response.episodes

                if (movie != null) {
                    val episodeList = mutableListOf<EpisodeUi>()
                    val videoUrlMap = mutableMapOf<String, String>()

                    episodes
                        ?.firstOrNull { server -> server.server_data.isNotEmpty() }
                        ?.server_data
                        ?.forEachIndexed { index, ep ->
                            val episodeName = ep.name.ifBlank { "Tập ${index + 1}" }
                            val epId = "${slug}-ep-${episodeList.size}"
                            episodeList.add(
                                EpisodeUi(
                                    id = epId,
                                    name = episodeName,
                                    duration = movie.time ?: ""
                                )
                            )
                            if (ep.link_m3u8.isNotBlank()) {
                                videoUrlMap[epId] = ep.link_m3u8
                            } else if (ep.link_embed.isNotBlank()) {
                                videoUrlMap[epId] = ep.link_embed
                            }
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

                    // Fetch cast photos from TMDB
                    val tmdbId = movie.tmdb?.id
                    val tmdbType = movie.tmdb?.type
                    if (tmdbId != null) {
                        viewModelScope.launch {
                            try {
                                val credits = if (tmdbType == "tv") {
                                    tmdbApi.getTvCredits(tmdbId)
                                } else {
                                    tmdbApi.getMovieCredits(tmdbId)
                                }
                                val tmdbCast = credits.cast?.take(20)?.map { cast ->
                                    CastUi(
                                        name = cast.name ?: cast.original_name ?: "",
                                        role = cast.character ?: "Diễn viên",
                                        profileUrl = cast.profile_path?.let { "${TmdbConfig.IMAGE_BASE_URL}$it" }
                                    )
                                } ?: emptyList()
                                if (tmdbCast.isNotEmpty()) {
                                    _movieDetail.value = _movieDetail.value?.copy(cast = tmdbCast)
                                }
                            } catch (e: Exception) {
                                // Keep original cast from phimapi if TMDB fails
                            }
                        }
                    }

                    // Thêm logic tìm kiếm phụ các Phần khác
                    viewModelScope.launch {
                        try {
                            val baseName = movie.name.replace(Regex("(?i)\\(?\\s*(Phần|Mùa|Season|Part)\\s*\\d+\\)?"), "").trim()
                            if (baseName.isNotBlank()) {
                                val searchResponse = api.searchMovies(keyword = baseName, limit = 10)
                                val related = searchResponse.data?.items ?: searchResponse.items ?: emptyList()
                                val processedRelated = related.filter {
                                    it.slug != slug && (it.name.contains(baseName, ignoreCase = true) || it.origin_name?.contains(baseName, ignoreCase = true) == true)
                                }.map { rel ->
                                    RecommendedMovieUi(
                                        id = rel.slug,
                                        title = rel.name,
                                        year = rel.year?.toString() ?: "",
                                        posterUrl = rel.getFullPosterUrl()
                                    )
                                }
                                if (processedRelated.isNotEmpty()) {
                                    _movieDetail.value = _movieDetail.value?.copy(relatedSeasons = processedRelated.sortedBy { it.title })
                                }
                            }
                        } catch (e: Exception) {
                            // Bỏ qua lỗi nếu không thể tải các phần khác
                        }
                    }
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
