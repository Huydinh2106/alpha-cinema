package com.example.alphacinema

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphacinema.data.api.RetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MovieListViewModel : ViewModel() {
    private val api = RetrofitClient.instance

    private val _movies = MutableStateFlow<List<SearchMovieUi>>(emptyList())
    val movies = _movies.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isLoadMore = MutableStateFlow(false)
    val isLoadMore = _isLoadMore.asStateFlow()

    private var currentKind: FilterKind = FilterKind.ALL
    private var currentSlug: String = ""
    private var currentPage = 1
    private var isEndReached = false
    private var loadJob: Job? = null

    fun load(kind: FilterKind, slug: String) {
        if (currentKind == kind && currentSlug == slug && _movies.value.isNotEmpty()) return
        currentKind = kind
        currentSlug = slug
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            currentPage = 1
            isEndReached = false
            _isLoading.value = true
            try {
                _movies.value = fetchPage(1)
            } catch (_: Exception) {
                _movies.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMore() {
        if (_isLoading.value || _isLoadMore.value || isEndReached) return
        viewModelScope.launch {
            _isLoadMore.value = true
            currentPage++
            try {
                val newItems = fetchPage(currentPage)
                if (newItems.isEmpty()) {
                    isEndReached = true
                } else {
                    _movies.value = _movies.value + newItems
                }
            } catch (_: Exception) {
                currentPage--
            } finally {
                _isLoadMore.value = false
            }
        }
    }

    private suspend fun fetchPage(page: Int): List<SearchMovieUi> {
        val response = when (currentKind) {
            FilterKind.MOVIE_TYPE -> api.getMoviesByType(type = currentSlug, page = page, limit = 21)
            FilterKind.GENRE      -> api.getMoviesByCategory(slug = currentSlug, page = page, limit = 21)
            FilterKind.ALL        -> return emptyList()
        }
        val items = response.data?.items ?: response.items ?: emptyList()
        val totalPages = response.data?.pagination?.totalPages
            ?: response.pagination?.totalPages ?: 1
        isEndReached = page >= totalPages

        return items.map {
            SearchMovieUi(
                movieId = it.slug,
                title = it.name,
                subtitle = it.category?.firstOrNull()?.name ?: it.origin_name ?: "Phim",
                badge = it.episode_current ?: it.quality ?: "HD",
                badgeColor = "gray",
                rating = it.getRating(),
                posterUrl = it.getFullPosterUrl()
            )
        }
    }
}
