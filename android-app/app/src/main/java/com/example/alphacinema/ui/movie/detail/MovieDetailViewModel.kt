package com.example.alphacinema.ui.movie.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphacinema.data.api.RetrofitClient
import com.example.alphacinema.data.api.TmdbConfig
import com.example.alphacinema.data.model.Comment
import com.example.alphacinema.data.model.FirestoreMovie
import com.example.alphacinema.data.model.MovieDetail
import com.example.alphacinema.data.model.MovieDetailResponse
import com.example.alphacinema.data.model.MovieItem
import com.example.alphacinema.data.model.MovieStats
import com.example.alphacinema.data.model.PlaylistMovieItem
import com.example.alphacinema.data.model.UserPlaylist
import com.example.alphacinema.data.repository.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.Normalizer
import com.example.alphacinema.data.local.SettingsManager

class MovieDetailViewModel : ViewModel() {
    private val api = RetrofitClient.instance
    private val tmdbApi = RetrofitClient.tmdbApi
    private val firestoreRepository = FirestoreRepository()
    private val settingsManager = SettingsManager.getInstance()

    private val isKidsMode: Boolean
        get() = settingsManager.isKidsModeEnabled.value

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

    private val _playlists = MutableStateFlow<List<UserPlaylist>>(emptyList())
    val playlists: StateFlow<List<UserPlaylist>> = _playlists.asStateFlow()

    private val _playlistIdsForCurrentMovie = MutableStateFlow<Set<String>>(emptySet())
    val playlistIdsForCurrentMovie: StateFlow<Set<String>> =
        _playlistIdsForCurrentMovie.asStateFlow()

    private val _playlistActionInProgress = MutableStateFlow(false)
    val playlistActionInProgress: StateFlow<Boolean> =
        _playlistActionInProgress.asStateFlow()

    private val _movieStats = MutableStateFlow<MovieStats?>(null)
    val movieStats: StateFlow<MovieStats?> = _movieStats.asStateFlow()

    private val _userRating = MutableStateFlow<Int?>(null)
    val userRating: StateFlow<Int?> = _userRating.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private var activeUserId: String? = null
    private var activeMovieSlug: String? = null
    private var playlistsJob: Job? = null
    private var moviePlaylistIdsJob: Job? = null

    fun initFirebaseListeners(slug: String, userId: String?) {
        activeMovieSlug = slug
        activeUserId = userId
        
        viewModelScope.launch {
            firestoreRepository.getMovieStats(slug)
                .catch { e -> Log.e(TAG, "Error collecting movie stats", e) }
                .collect {
                    _movieStats.value = it
                }
        }
        viewModelScope.launch {
            firestoreRepository.getComments(slug)
                .catch { e -> Log.e(TAG, "Error collecting comments", e) }
                .collect {
                    _comments.value = it
                }
        }
        if (userId != null) {
            viewModelScope.launch {
                firestoreRepository.getFavorites(userId)
                    .catch { e -> Log.e(TAG, "Error collecting favorites", e) }
                    .collect { favorites ->
                        _isFavorite.value = favorites.any { it.movieId == slug }
                    }
            }
            viewModelScope.launch {
                firestoreRepository.getUserRating(slug, userId)
                    .catch { e -> Log.e(TAG, "Error collecting user rating", e) }
                    .collect { rating ->
                        _userRating.value = rating?.score
                    }
            }
            playlistsJob?.cancel()
            playlistsJob = viewModelScope.launch {
                firestoreRepository.getPlaylists(userId, isKidsMode)
                    .catch { e -> Log.e(TAG, "Error collecting playlists", e) }
                    .collect { playlists ->
                        _playlists.value = playlists
                    }
            }
            moviePlaylistIdsJob?.cancel()
            moviePlaylistIdsJob = viewModelScope.launch {
                firestoreRepository.getPlaylistIdsForMovie(userId, slug, isKidsMode)
                    .catch { e -> Log.e(TAG, "Error collecting playlist IDs", e) }
                    .collect { playlistIds ->
                        _playlistIdsForCurrentMovie.value = playlistIds
                    }
            }
        } else {
            _userRating.value = null
            _playlists.value = emptyList()
            _playlistIdsForCurrentMovie.value = emptySet()
            playlistsJob?.cancel()
            moviePlaylistIdsJob?.cancel()
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

    fun createPlaylistAndAddMovie(
        playlistName: String,
        movie: MovieDetailUi,
        onResult: (Boolean, String) -> Unit
    ) {
        val uid = activeUserId
        if (uid == null) {
            onResult(false, "Vui lòng đăng nhập để lưu vào danh sách phát")
            return
        }
        val normalizedName = validatePlaylistName(playlistName, onResult) ?: return

        viewModelScope.launch {
            _playlistActionInProgress.value = true
            try {
                firestoreRepository.createPlaylist(
                    userId = uid,
                    name = normalizedName,
                    firstMovie = movie.toPlaylistMovieItem(),
                    isKidsMode = isKidsMode
                )
                onResult(true, "Đã lưu vào $normalizedName")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                onResult(
                    false,
                    playlistErrorMessage(
                        operation = "createPlaylistAndAddMovie",
                        error = e,
                        fallback = "Không thể tạo danh sách phát"
                    )
                )
            } finally {
                _playlistActionInProgress.value = false
            }
        }
    }

    fun toggleMovieInPlaylist(
        playlistId: String,
        movie: MovieDetailUi,
        isInPlaylist: Boolean,
        onResult: (Boolean, String) -> Unit
    ) {
        val uid = activeUserId
        if (uid == null) {
            onResult(false, "Vui lòng đăng nhập để lưu vào danh sách phát")
            return
        }
        val playlist = _playlists.value.firstOrNull { it.id == playlistId }
        val playlistName = playlist?.name.orEmpty().ifBlank { "danh sách phát" }
        val previousIds = _playlistIdsForCurrentMovie.value

        viewModelScope.launch {
            _playlistActionInProgress.value = true
            _playlistIdsForCurrentMovie.value = if (isInPlaylist) {
                previousIds - playlistId
            } else {
                previousIds + playlistId
            }
            try {
                if (isInPlaylist) {
                    firestoreRepository.removeMovieFromPlaylist(uid, playlistId, movie.id, isKidsMode)
                    onResult(true, "Đã bỏ khỏi $playlistName")
                } else {
                    firestoreRepository.addMovieToPlaylist(
                        userId = uid,
                        playlistId = playlistId,
                        movie = movie.toPlaylistMovieItem(),
                        isKidsMode = isKidsMode
                    )
                    onResult(true, "Đã lưu vào $playlistName")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _playlistIdsForCurrentMovie.value = previousIds
                onResult(
                    false,
                    playlistErrorMessage(
                        operation = "toggleMovieInPlaylist",
                        error = e,
                        fallback = "Lỗi cập nhật danh sách phát"
                    )
                )
            } finally {
                _playlistActionInProgress.value = false
            }
        }
    }

    fun renamePlaylist(
        playlistId: String,
        playlistName: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val uid = activeUserId
        if (uid == null) {
            onResult(false, "Vui lòng đăng nhập để sửa danh sách phát")
            return
        }
        val normalizedName = validatePlaylistName(playlistName, onResult) ?: return

        viewModelScope.launch {
            _playlistActionInProgress.value = true
            try {
                firestoreRepository.renamePlaylist(uid, playlistId, normalizedName, isKidsMode)
                onResult(true, "Đã đổi tên danh sách phát")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                onResult(
                    false,
                    playlistErrorMessage(
                        operation = "renamePlaylist",
                        error = e,
                        fallback = "Không thể đổi tên danh sách phát"
                    )
                )
            } finally {
                _playlistActionInProgress.value = false
            }
        }
    }

    fun deletePlaylist(
        playlistId: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val uid = activeUserId
        if (uid == null) {
            onResult(false, "Vui lòng đăng nhập để xóa danh sách phát")
            return
        }

        viewModelScope.launch {
            _playlistActionInProgress.value = true
            val previousIds = _playlistIdsForCurrentMovie.value
            _playlistIdsForCurrentMovie.value = previousIds - playlistId
            try {
                firestoreRepository.deletePlaylist(uid, playlistId, isKidsMode)
                onResult(true, "Đã xóa danh sách phát")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _playlistIdsForCurrentMovie.value = previousIds
                onResult(
                    false,
                    playlistErrorMessage(
                        operation = "deletePlaylist",
                        error = e,
                        fallback = "Không thể xóa danh sách phát"
                    )
                )
            } finally {
                _playlistActionInProgress.value = false
            }
        }
    }

    private fun MovieDetailUi.toPlaylistMovieItem(): PlaylistMovieItem {
        return PlaylistMovieItem(
            movieId = id,
            movieName = title,
            posterUrl = posterUrl
        )
    }

    private fun validatePlaylistName(
        playlistName: String,
        onResult: (Boolean, String) -> Unit
    ): String? {
        val normalizedName = playlistName.trim()
        return when {
            normalizedName.isBlank() -> {
                onResult(false, "Vui lòng nhập tên danh sách phát")
                null
            }
            normalizedName.length > PLAYLIST_NAME_MAX_LENGTH -> {
                onResult(false, "Tên danh sách phát tối đa $PLAYLIST_NAME_MAX_LENGTH ký tự")
                null
            }
            else -> normalizedName
        }
    }

    private fun playlistErrorMessage(
        operation: String,
        error: Exception,
        fallback: String
    ): String {
        Log.e(TAG, "$operation failed", error)
        val message = error.message.orEmpty()
        val firestoreError = error as? FirebaseFirestoreException
        return when {
            firestoreError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Không có quyền lưu danh sách phát. Vui lòng đăng nhập lại"
            firestoreError?.code == FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại"
            message.contains("API key", ignoreCase = true) ||
                message.contains("SERVICE_DISABLED", ignoreCase = true) ||
                message.contains("PROJECT_DISABLED", ignoreCase = true) ->
                "Cấu hình Firebase đang bị chặn. Vui lòng kiểm tra lại project"
            else -> fallback
        }
    }

    fun postComment(
        userName: String,
        userAvatar: String,
        content: String,
        parentCommentId: String = "",
        replyToUserName: String = "",
        onResult: (Boolean, String) -> Unit
    ) {
        val uid = activeUserId
        if (uid == null) {
            onResult(false, "Vui lòng đăng nhập để bình luận")
            return
        }
        val slug = activeMovieSlug ?: return
        if (content.isBlank()) return
        
        viewModelScope.launch {
            try {
                val profile = firestoreRepository.getUserProfile(uid)
                val resolvedUserName = profile?.displayName
                    ?.takeIf { it.isNotBlank() }
                    ?: userName.ifBlank { "Người dùng" }
                val resolvedUserAvatar = profile?.photoUrl
                    ?.takeIf { it.isNotBlank() }
                    ?: userAvatar

                firestoreRepository.postComment(
                    slug,
                    uid,
                    resolvedUserName,
                    resolvedUserAvatar,
                    content,
                    parentCommentId,
                    replyToUserName
                )
                onResult(true, if (parentCommentId.isBlank()) "Đã gửi bình luận" else "Đã trả lời bình luận")
            } catch (e: Exception) {
               onResult(false, "Không thể gửi bình luận")
            }
        }
    }

    fun toggleCommentLike(commentId: String, onResult: (Boolean, String) -> Unit) {
        val uid = activeUserId
        if (uid == null) {
            onResult(false, "Vui lòng đăng nhập để thả tim")
            return
        }
        val slug = activeMovieSlug ?: return
        if (commentId.isBlank()) return

        viewModelScope.launch {
            try {
                firestoreRepository.toggleCommentLike(slug, commentId, uid)
            } catch (e: Exception) {
                onResult(false, "Không thể cập nhật thả tim")
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
                val response = loadMovieDetailWithFallback(slug)
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

    private suspend fun loadMovieDetailWithFallback(slug: String): MovieDetailResponse {
        runCatching { api.getMovieDetail(slug) }
            .getOrNull()
            ?.takeIf { it.movie != null }
            ?.let { return it }

        val firestoreMovie = firestoreRepository.getMovieBySlug(slug)
        val searchKeywords = listOfNotNull(
            firestoreMovie?.title,
            firestoreMovie?.originName,
            slug.replace("-", " ")
        )
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        for (keyword in searchKeywords) {
            val candidates = runCatching {
                val searchResponse = api.searchMovies(keyword = keyword, limit = 10)
                searchResponse.data?.items ?: searchResponse.items ?: emptyList()
            }.getOrDefault(emptyList())

            val resolved = resolveFallbackCandidate(
                candidates = candidates,
                firestoreMovie = firestoreMovie,
                requestedSlug = slug
            ) ?: continue

            runCatching { api.getMovieDetail(resolved.slug) }
                .getOrNull()
                ?.takeIf { it.movie != null }
                ?.let { return it }
        }

        return MovieDetailResponse(status = false, movie = null, episodes = null)
    }

    private fun resolveFallbackCandidate(
        candidates: List<MovieItem>,
        firestoreMovie: FirestoreMovie?,
        requestedSlug: String
    ): MovieItem? {
        if (candidates.isEmpty()) return null

        val targetKeys = listOfNotNull(
            firestoreMovie?.title,
            firestoreMovie?.originName,
            requestedSlug.replace("-", " ")
        )
            .map { it.normalizedMovieKey() }
            .filter { it.isNotBlank() }

        return candidates
            .maxByOrNull { candidate ->
                val candidateKeys = listOf(
                    candidate.name,
                    candidate.origin_name.orEmpty(),
                    candidate.slug.replace("-", " ")
                ).map { it.normalizedMovieKey() }

                when {
                    candidate.slug == requestedSlug -> 100
                    candidateKeys.any { it in targetKeys } -> 90
                    candidateKeys.any { candidateKey ->
                        targetKeys.any { targetKey ->
                            candidateKey.contains(targetKey) || targetKey.contains(candidateKey)
                        }
                    } -> 80
                    else -> 0
                }
            }
            ?.takeIf { candidate ->
                val candidateKeys = listOf(
                    candidate.name,
                    candidate.origin_name.orEmpty(),
                    candidate.slug.replace("-", " ")
                ).map { it.normalizedMovieKey() }

                candidate.slug == requestedSlug || candidateKeys.any { candidateKey ->
                    targetKeys.any { targetKey ->
                        candidateKey == targetKey || candidateKey.contains(targetKey) || targetKey.contains(candidateKey)
                    }
                }
            }
            ?: candidates.firstOrNull()
    }

    private fun String.normalizedMovieKey(): String {
        return Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
    }
}

private const val PLAYLIST_NAME_MAX_LENGTH = 60
private const val TAG = "MovieDetailViewModel"
