package com.example.alphacinema

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphacinema.data.api.RetrofitClient
import com.example.alphacinema.data.local.SettingsManager
import com.example.alphacinema.data.model.FirestoreMovie
import com.example.alphacinema.data.repository.FirestoreRepository
import com.example.alphacinema.data.repository.MovieRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── Filter model ──────────────────────────────────────────────────────────────

enum class FilterKind {
    ALL,         // No filter → show trending
    MOVIE_TYPE,  // phim-bo / phim-le / hoat-hinh / tv-shows  →  danh-sach/{slug}
    GENRE        // hanh-dong / tinh-cam / …                  →  the-loai/{slug} + category= in search
}

data class SearchFilter(
    val label: String,
    val kind: FilterKind,
    val slug: String? = null   // null only for ALL
)

val SEARCH_FILTERS = listOf(
    SearchFilter("Tất cả",    FilterKind.ALL),
    // ── Loại phim (type) ──
    SearchFilter("Phim bộ",   FilterKind.MOVIE_TYPE, "series"), // Adjusted for firestore type
    SearchFilter("Phim lẻ",   FilterKind.MOVIE_TYPE, "single"), // Adjusted for firestore type
    SearchFilter("Hoạt hình", FilterKind.MOVIE_TYPE, "hoathinh"), // Adjusted for firestore type
    SearchFilter("TV Shows",  FilterKind.MOVIE_TYPE, "tvshows"), // Adjusted for firestore type
    // ── Thể loại (genre) ──
    SearchFilter("Hành động",  FilterKind.GENRE, "Hành động"), // Use natural text for array contains
    SearchFilter("Tình cảm",   FilterKind.GENRE, "Tình cảm"),
    SearchFilter("Tâm lý",     FilterKind.GENRE, "Tâm lý"),
    SearchFilter("Kinh dị",    FilterKind.GENRE, "Kinh dị"),
    SearchFilter("Viễn tưởng", FilterKind.GENRE, "Viễn tưởng"),
    SearchFilter("Hài hước",   FilterKind.GENRE, "Hài hước"),
    SearchFilter("Võ thuật",   FilterKind.GENRE, "Võ thuật"),
    SearchFilter("Cổ trang",   FilterKind.GENRE, "Cổ trang"),
    SearchFilter("Chiến tranh",FilterKind.GENRE, "Chiến tranh"),
    SearchFilter("Thể thao",   FilterKind.GENRE, "Thể thao"),
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class SearchViewModel : ViewModel() {
    private val firestoreRepository = FirestoreRepository()

    private val _searchResults = MutableStateFlow<List<SearchMovieUi>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isLoadMore = MutableStateFlow(false)
    val isLoadMore = _isLoadMore.asStateFlow()

    private val _selectedFilter = MutableStateFlow(SEARCH_FILTERS[0])
    val selectedFilter = _selectedFilter.asStateFlow()

    private var currentPage = 1
    private var isEndReached = false
    private var currentQuery = ""
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            SettingsManager.getInstance().isKidsModeEnabled.collect {
                resetAndFetch()
            }
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun setFilter(filter: SearchFilter) {
        if (_selectedFilter.value == filter) return
        _selectedFilter.value = filter
        resetAndFetch()
    }

    fun onSearchQueryChanged(query: String) {
        if (currentQuery == query) return
        currentQuery = query
        resetAndFetch()
    }

    fun loadMore() {
        if (_isLoading.value || _isLoadMore.value || isEndReached) return
        viewModelScope.launch {
            _isLoadMore.value = true
            currentPage++
            try {
                // Since firestore doesn't easily paginate without cursors in our simple setup, 
                // we just fetch all up to a large limit or use limits. 
                // For simplicity, we just fetch limit * page.
                val items = fetchPage(currentPage)
                if (items.isEmpty()) {
                    isEndReached = true
                } else {
                    _searchResults.value = items // replace to emulate cursor, or append if proper paging
                }
            } catch (_: Exception) {
                currentPage--
            } finally {
                _isLoadMore.value = false
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun resetAndFetch() {
        searchJob?.cancel()
        currentPage = 1
        isEndReached = false

        val filter = _selectedFilter.value
        val query = currentQuery

        if (query.length < 2 && filter.kind == FilterKind.ALL) {
            _searchResults.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            if (query.length >= 2) delay(600)
            _isLoading.value = true
            try {
                _searchResults.value = fetchPage(1)
            } catch (_: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchPage(page: Int): List<SearchMovieUi> {
        val filter = _selectedFilter.value
        val query = currentQuery
        val isKidsMode = SettingsManager.getInstance().isKidsModeEnabled.value

        val limit = page * 20 // Pseudo pagination

        val response = when {
            query.length >= 2 -> {
                val base = firestoreRepository.searchMovies(keyword = query, isKidsMode = isKidsMode, limit = limit)
                when {
                    filter.kind == FilterKind.GENRE && filter.slug != null -> base.filter { it.categories.contains(filter.slug) }
                    filter.kind == FilterKind.MOVIE_TYPE && filter.slug != null -> base.filter { it.type == filter.slug }
                    else -> base
                }
            }
            filter.kind == FilterKind.GENRE && filter.slug != null -> {
                firestoreRepository.getMoviesByCategory(category = filter.slug, isKidsMode = isKidsMode, limit = limit)
            }
            filter.kind == FilterKind.MOVIE_TYPE && filter.slug != null -> {
                firestoreRepository.getMoviesByType(type = filter.slug, isKidsMode = isKidsMode, limit = limit)
            }
            else -> emptyList()
        }

        val items = response
        isEndReached = true // We loaded everything up to the limit

        return items.map { it.toSearchUi() }
    }

    private fun FirestoreMovie.toSearchUi() = SearchMovieUi(
        movieId = slug,
        title = title,
        subtitle = categories.firstOrNull() ?: originName ?: "Phim",
        badge = ageRating,
        badgeColor = if (isKidsFriendly) "green" else "red",
        rating = "9.0", // Fallback for simplicity
        posterUrl = posterUrl
    )
}