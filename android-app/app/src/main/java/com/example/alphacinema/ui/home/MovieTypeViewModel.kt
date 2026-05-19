package com.example.alphacinema.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphacinema.data.api.RetrofitClient
import com.example.alphacinema.data.model.MovieItem
import com.example.alphacinema.data.model.MovieListResponse
import com.example.alphacinema.ui.search.FilterOption
import com.example.alphacinema.ui.search.SelectedFilterState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MovieTypeViewModel : ViewModel() {
    private val api = RetrofitClient.instance

    private val _movies = MutableStateFlow<List<MovieItem>>(emptyList())
    val movies: StateFlow<List<MovieItem>> = _movies.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _totalPages = MutableStateFlow(1)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    // Sort: "modified.time" (mới nhất) or "_id" (xem nhiều)
    private val _sortField = MutableStateFlow("modified.time")
    val sortField: StateFlow<String> = _sortField.asStateFlow()

    // Reuse SelectedFilterState from SearchScreen for consistency
    private val _appliedFilters = MutableStateFlow(SelectedFilterState())
    val appliedFilters: StateFlow<SelectedFilterState> = _appliedFilters.asStateFlow()

    private val _tempFilters = MutableStateFlow(SelectedFilterState())
    val tempFilters: StateFlow<SelectedFilterState> = _tempFilters.asStateFlow()

    private var currentType = ""
    private var loadJob: Job? = null

    /** true when type starts with "the-loai/" → use genre API */
    private val isGenreMode: Boolean get() = currentType.startsWith("the-loai/")
    private val routeSortField: String?
        get() = currentType.substringAfter("|sort=", missingDelimiterValue = "")
            .takeIf { it.isNotBlank() }
    private val genreSlugs: List<String>
        get() = currentType
            .removePrefix("the-loai/")
            .substringBefore("|sort=")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

    fun loadMovies(type: String, sort: String = "modified.time") {
        if (currentType == type && _movies.value.isNotEmpty() && !_isLoading.value) return
        currentType = type
        _sortField.value = routeSortField ?: sort
        _appliedFilters.value = SelectedFilterState()
        _tempFilters.value = SelectedFilterState()
        fetchPage(1)
    }

    // ── Pagination ──

    fun goToPage(page: Int) {
        if (page < 1 || page > _totalPages.value || _isLoading.value) return
        fetchPage(page)
    }

    fun nextPage() = goToPage(_currentPage.value + 1)
    fun prevPage() = goToPage(_currentPage.value - 1)

    // ── Filter logic ──

    fun updateTempCountry(option: FilterOption?) {
        _tempFilters.update { it.copy(country = if (it.country == option) null else option) }
    }

    fun updateTempCategory(option: FilterOption?) {
        _tempFilters.update { it.copy(category = if (it.category == option) null else option) }
    }

    fun updateTempVersion(option: FilterOption?) {
        _tempFilters.update { it.copy(sortLang = if (it.sortLang == option) null else option) }
    }

    fun updateTempYear(option: FilterOption?) {
        _tempFilters.update { it.copy(year = if (it.year == option) null else option) }
    }

    fun applyFilters() {
        _appliedFilters.value = _tempFilters.value
        fetchPage(1)
    }

    fun resetTempFilters() {
        _tempFilters.value = _appliedFilters.value
    }

    private fun fetchPage(page: Int) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                val filters = _appliedFilters.value
                val response: MovieListResponse = if (isGenreMode) {
                    fetchGenrePage(page, filters)
                } else {
                    api.getMoviesByType(
                        type = currentType,
                        page = page,
                        limit = 24,
                        sortField = _sortField.value,
                        sortLang = filters.sortLang?.slug,
                        category = filters.category?.slug,
                        country = filters.country?.slug,
                        year = filters.year?.slug
                    )
                }
                val items = response.data?.items ?: response.items ?: emptyList()
                val pagination = response.data?.params?.pagination
                    ?: response.data?.pagination
                    ?: response.pagination
                _totalPages.value = pagination?.totalPages ?: 1
                _currentPage.value = page
                _movies.value = items
            } catch (e: Exception) {
                android.util.Log.e("MovieTypeVM", "Load failed page=$page type=$currentType", e)
                _movies.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchGenrePage(
        page: Int,
        filters: SelectedFilterState
    ): MovieListResponse {
        val responses = genreSlugs.map { slug ->
            api.getMoviesByCategory(
                slug = slug,
                page = page,
                limit = 24,
                sortField = _sortField.value,
                sortLang = filters.sortLang?.slug,
                country = filters.country?.slug,
                year = filters.year?.slug
            )
        }

        if (responses.size <= 1) {
            return responses.firstOrNull() ?: MovieListResponse(
                status = false,
                items = emptyList(),
                data = null,
                pagination = null
            )
        }

        val mergedItems = responses
            .flatMap { it.data?.items ?: it.items ?: emptyList() }
            .distinctBy { it.slug }
        val maxTotalPages = responses.maxOfOrNull { response ->
            response.data?.params?.pagination?.totalPages
                ?: response.data?.pagination?.totalPages
                ?: response.pagination?.totalPages
                ?: 1
        } ?: 1
        val base = responses.first()
        return base.copy(
            items = mergedItems,
            data = base.data?.copy(
                items = mergedItems,
                params = base.data.params?.copy(
                    pagination = base.data.params.pagination?.copy(totalPages = maxTotalPages)
                ),
                pagination = base.data.pagination?.copy(totalPages = maxTotalPages)
            ),
            pagination = base.pagination?.copy(totalPages = maxTotalPages)
        )
    }
}
