package com.example.alphacinema

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphacinema.data.api.RetrofitClient
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
    SearchFilter("Phim bộ",   FilterKind.MOVIE_TYPE, "phim-bo"),
    SearchFilter("Phim lẻ",   FilterKind.MOVIE_TYPE, "phim-le"),
    SearchFilter("Hoạt hình", FilterKind.MOVIE_TYPE, "hoat-hinh"),
    SearchFilter("TV Shows",  FilterKind.MOVIE_TYPE, "tv-shows"),
    // ── Thể loại (genre) ──
    SearchFilter("Hành động",  FilterKind.GENRE, "hanh-dong"),
    SearchFilter("Tình cảm",   FilterKind.GENRE, "tinh-cam"),
    SearchFilter("Tâm lý",     FilterKind.GENRE, "tam-ly"),
    SearchFilter("Kinh dị",    FilterKind.GENRE, "kinh-di"),
    SearchFilter("Viễn tưởng", FilterKind.GENRE, "vien-tuong"),
    SearchFilter("Hài hước",   FilterKind.GENRE, "hai-huoc"),
    SearchFilter("Võ thuật",   FilterKind.GENRE, "vo-thuat"),
    SearchFilter("Cổ trang",   FilterKind.GENRE, "co-trang"),
    SearchFilter("Chiến tranh",FilterKind.GENRE, "chien-tranh"),
    SearchFilter("Thể thao",   FilterKind.GENRE, "the-thao"),
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class SearchViewModel : ViewModel() {
    private val api = RetrofitClient.instance

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
                val items = fetchPage(currentPage)
                if (items.isEmpty()) {
                    isEndReached = true
                } else {
                    _searchResults.value = _searchResults.value + items
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

        // Nothing to fetch: no keyword AND no filter active
        if (query.length < 2 && filter.kind == FilterKind.ALL) {
            _searchResults.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            // Debounce only for text search to avoid hammering the API
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

    /**
     * Fetch a single page based on the current query + filter combination.
     *
     * Decision table:
     * ┌─────────────┬──────────────┬──────────────────────────────────────────────────┐
     * │ query ≥ 2   │ filter kind  │ endpoint used                                    │
     * ├─────────────┼──────────────┼──────────────────────────────────────────────────┤
     * │ yes         │ GENRE        │ searchMovies(keyword, category=slug)              │
     * │ yes         │ MOVIE_TYPE   │ searchMovies(keyword)  ← API has no type param   │
     * │ yes         │ ALL          │ searchMovies(keyword)                             │
     * │ no          │ GENRE        │ getMoviesByCategory(slug)                        │
     * │ no          │ MOVIE_TYPE   │ getMoviesByType(slug)                            │
     * │ no          │ ALL          │ never reached (guarded above)                    │
     * └─────────────┴──────────────┴──────────────────────────────────────────────────┘
     */
    private suspend fun fetchPage(page: Int): List<SearchMovieUi> {
        val filter = _selectedFilter.value
        val query = currentQuery

        val response = when {
            // ── Text search ──────────────────────────────────────────────────
            query.length >= 2 -> {
                val genreCategorySlug = if (filter.kind == FilterKind.GENRE) filter.slug else null
                api.searchMovies(
                    keyword = query,
                    page = page,
                    category = genreCategorySlug
                )
            }
            // ── Browse by genre (no keyword) ─────────────────────────────────
            filter.kind == FilterKind.GENRE && filter.slug != null -> {
                api.getMoviesByCategory(slug = filter.slug, page = page)
            }
            // ── Browse by type (no keyword) ──────────────────────────────────
            filter.kind == FilterKind.MOVIE_TYPE && filter.slug != null -> {
                api.getMoviesByType(type = filter.slug, page = page)
            }
            // Fallback – should not happen
            else -> return emptyList()
        }

        val items = response.data?.items ?: response.items ?: emptyList()
        val totalPages = response.data?.pagination?.totalPages
            ?: response.pagination?.totalPages ?: 1
        isEndReached = page >= totalPages

        return items.map { it.toSearchUi() }
    }

    private fun com.example.alphacinema.data.model.MovieItem.toSearchUi() = SearchMovieUi(
        movieId = slug,
        title = name,
        subtitle = category?.firstOrNull()?.name ?: origin_name ?: "Phim",
        badge = episode_current ?: quality ?: "HD",
        badgeColor = "gray",
        rating = getRating(),
        posterUrl = getFullPosterUrl()
    )
}