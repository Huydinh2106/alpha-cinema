package com.example.alphacinema.ui.movie.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphacinema.data.local.SettingsManager
import com.example.alphacinema.data.repository.FirestoreRepository
import com.example.alphacinema.ui.account.FilterKind
import com.example.alphacinema.ui.search.SearchMovieUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MovieListViewModel : ViewModel() {
    private val firestoreRepository = FirestoreRepository()

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

    init {
        viewModelScope.launch {
            SettingsManager.getInstance().isKidsModeEnabled.collect {
                if (_movies.value.isNotEmpty()) {
                    load(currentKind, currentSlug, forceReload = true)
                }
            }
        }
    }

    fun load(kind: FilterKind, slug: String, forceReload: Boolean = false) {
        if (!forceReload && currentKind == kind && currentSlug == slug && _movies.value.isNotEmpty()) return
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
        val limit = page * 20
        val isKidsMode = SettingsManager.getInstance().isKidsModeEnabled.value

        val baseMovies = when (currentKind) {
            FilterKind.MOVIE_TYPE -> firestoreRepository.getMovies(isKidsMode, limit).filter { it.type == currentSlug }
            FilterKind.GENRE      -> firestoreRepository.getMoviesByCategory(currentSlug, isKidsMode, limit)
            FilterKind.ALL        -> firestoreRepository.getMovies(isKidsMode, limit)
        }
        
        isEndReached = true // Simplified pagination

        return baseMovies.map {
            SearchMovieUi(
                movieId = it.slug,
                title = it.title,
                subtitle = it.categories.firstOrNull() ?: it.originName ?: "Phim",
                badge = it.ageRating,
                badgeColor = if (it.isKidsFriendly) "green" else "red",
                rating = "9.0", // Hardcoded rating since we store simplified data now
                posterUrl = it.posterUrl
            )
        }
    }
}
