package com.example.alphacinema

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphacinema.data.api.RetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private val api = RetrofitClient.instance

    private val _searchResults = MutableStateFlow<List<SearchMovieUi>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Thêm các biến mới cho Pagination
    private val _isLoadMore = MutableStateFlow(false)
    val isLoadMore = _isLoadMore.asStateFlow()

    private var currentPage = 1
    private var canLoadMore = true
    private var currentQuery = ""
    private var searchJob: Job? = null
    private var isEndReached = false

    fun onSearchQueryChanged(query: String) {
        if (currentQuery == query) return
        currentQuery = query
        searchJob?.cancel()

        if (query.length < 2) { //chỉ search từ 2 ký tự trở lên
            _searchResults.value = emptyList()
            currentPage = 1
            canLoadMore = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(800)
            _isLoading.value = true
            isEndReached = false
            currentPage = 1
            try {
                val response = api.searchMovies(keyword = query, page = currentPage)
                val items = response.data?.items ?: response.items ?: emptyList()

                val totalPages = response.data?.pagination?.totalPages ?: 1
                canLoadMore = currentPage < totalPages

                _searchResults.value = items.map { it.toSearchUi() }
            } catch (e: Exception) {
                _searchResults.value = emptyList()
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
                val response = api.searchMovies(keyword = currentQuery, page = currentPage)
                val items = response.data?.items ?: response.items ?: emptyList()

                if (items.isEmpty()) {
                    isEndReached = true
                } else {
                    // cộng danh sách cũ với mới
                    _searchResults.value = _searchResults.value + items.map { it.toSearchUi() }
                }
            } catch (e: Exception) {
                currentPage--
            } finally {
                _isLoadMore.value = false
            }
        }
    }

    private fun com.example.alphacinema.data.model.MovieItem.toSearchUi() = SearchMovieUi(
        movieId = slug,
        title = name,
        subtitle = category?.firstOrNull()?.name ?: "Phim",
        badge = episode_current ?: "HD",
        badgeColor = "gray",
        rating = getRating(),
        posterUrl = getFullPosterUrl()
    )
}