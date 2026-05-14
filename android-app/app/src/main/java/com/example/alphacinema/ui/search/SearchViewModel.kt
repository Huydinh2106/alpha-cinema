package com.example.alphacinema.ui.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphacinema.data.api.RetrofitClient
import com.example.alphacinema.data.local.SearchHistoryManager
import com.example.alphacinema.data.model.MovieItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── Filter Models (New System) ──────────────────────────────────────────────────

data class FilterOption(val label: String, val slug: String)

data class SelectedFilterState(
    val country: FilterOption? = null,
    val category: FilterOption? = null,
    val sortLang: FilterOption? = null,
    val year: FilterOption? = null
) {
    fun isAnyFilterApplied(): Boolean = !(country == null && category == null && sortLang == null && year == null)
}

val COUNTRY_OPTIONS = listOf(
    "Việt Nam" to "viet-nam", "Trung Quốc" to "trung-quoc", "Thái Lan" to "thai-lan",
    "Hồng Kông" to "hong-kong", "Pháp" to "phap", "Đức" to "duc", "Hà Lan" to "ha-lan",
    "Mexico" to "mexico", "Thụy Điển" to "thuy-dien", "Philippines" to "philippines",
    "Đan Mạch" to "dan-mach", "Thụy Sĩ" to "thuy-si", "Ukraina" to "ukraina",
    "Hàn Quốc" to "han-quoc", "Âu Mỹ" to "au-my", "Ấn Độ" to "an-do", "Canada" to "canada",
    "Tây Ban Nha" to "tay-ban-nha", "Indonesia" to "indonesia", "Ba Lan" to "ba-lan",
    "Malaysia" to "malaysia", "Bồ Đào Nha" to "bo-dao-nha", "UAE" to "uae",
    "Châu Phi" to "chau-phi", "Ả Rập Xê Út" to "a-rap-xe-ut", "Nhật Bản" to "nhat-ban",
    "Đài Loan" to "dai-loan", "Anh" to "anh", "Thổ Nhĩ Kỳ" to "tho-nhi-ky", "Nga" to "nga",
    "Úc" to "uc", "Brazil" to "brazil", "Ý" to "y", "Na Uy" to "na-uy", "Nam Phi" to "nam-phi"
).map { FilterOption(it.first, it.second) }

val CATEGORY_OPTIONS = listOf(
    "Hành động" to "hanh-dong", "Miền Tây" to "mien-tay", "Trẻ em" to "tre-em",
    "Lịch sử" to "lich-su", "Cổ trang" to "co-trang", "Chiến tranh" to "chien-tranh",
    "Viễn tưởng" to "vien-tuong", "Kinh dị" to "kinh-di", "Tài liệu" to "tai-lieu",
    "Bí ẩn" to "bi-an", "Phim 18+" to "phim-18", "Tình cảm" to "tinh-cam",
    "Tâm lý" to "tam-ly", "Thể thao" to "the-thao", "Phiêu lưu" to "phieu-luu",
    "Âm nhạc" to "am-nhac", "Gia đình" to "gia-dinh", "Học đường" to "hoc-duong",
    "Hài hước" to "hai-huoc", "Hình sự" to "hinh-su", "Võ thuật" to "vo-thuat",
    "Khoa học" to "khoa-hoc", "Thần thoại" to "than-thoai", "Chính kịch" to "chinh-kich",
    "Kinh điển" to "kinh-dien", "Phim ngắn" to "phim-ngan"
).map { FilterOption(it.first, it.second) }

val VERSION_OPTIONS = listOf(
    FilterOption("Vietsub", "vietsub"),
    FilterOption("Thuyết Minh", "thuyet-minh"),
    FilterOption("Lồng Tiếng", "long-tieng")
)

val YEAR_OPTIONS = (2026 downTo 1970).map { FilterOption(it.toString(), it.toString()) }

// ── ViewModel ─────────────────────────────────────────────────────────────────

class SearchViewModel : ViewModel() {
    private val api = RetrofitClient.instance
    private var historyManager: SearchHistoryManager? = null

    private val _searchResults = MutableStateFlow<List<SearchMovieUi>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory = _searchHistory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isLoadMore = MutableStateFlow(false)
    val isLoadMore = _isLoadMore.asStateFlow()

    // Filter states
    private val _appliedFilters = MutableStateFlow(SelectedFilterState())
    val appliedFilters = _appliedFilters.asStateFlow()

    private val _tempFilters = MutableStateFlow(SelectedFilterState())
    val tempFilters = _tempFilters.asStateFlow()

    private var currentPage = 1
    private var isEndReached = false
    private var currentQuery = ""
    private var searchJob: Job? = null

    fun initHistory(context: Context) {
        if (historyManager == null) {
            historyManager = SearchHistoryManager(context)
            _searchHistory.value = historyManager?.getHistory() ?: emptyList()
        }
    }

    // ── Filter Logic ──────────────────────────────────────────────────────────

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
        resetAndFetch()
    }

    fun resetTempFilters() {
        _tempFilters.value = _appliedFilters.value
    }

    fun clearAppliedFilters() {
        _appliedFilters.value = SelectedFilterState()
        _tempFilters.value = SelectedFilterState()
        resetAndFetch()
    }

    // ── Search Logic ──────────────────────────────────────────────────────────

    fun onSearchQueryChanged(query: String) {
        if (currentQuery == query) return
        currentQuery = query
        resetAndFetch()
    }

    fun onPerformSearch(query: String) {
        if (query.isNotBlank()) {
            historyManager?.addHistory(query)
            _searchHistory.value = historyManager?.getHistory() ?: emptyList()
        }
    }

    fun removeHistoryItem(query: String) {
        historyManager?.removeHistory(query)
        _searchHistory.value = historyManager?.getHistory() ?: emptyList()
    }

    fun clearAllHistory() {
        historyManager?.clearHistory()
        _searchHistory.value = emptyList()
    }

    fun loadMore() {
        if (_isLoading.value || _isLoadMore.value || isEndReached) return

        val isFilterActive = _appliedFilters.value.isAnyFilterApplied()
        if (currentQuery.length < 1 && !isFilterActive) return

        viewModelScope.launch {
            _isLoadMore.value = true
            currentPage++
            try {
                val items = fetchFilteredResults(currentPage)
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

    private fun resetAndFetch() {
        searchJob?.cancel()
        currentPage = 1
        isEndReached = false

        val isFilterActive = _appliedFilters.value.isAnyFilterApplied()

        if (currentQuery.trim().isEmpty() && !isFilterActive) {
            _searchResults.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {

            if (currentQuery.length >= 1) delay(600)
            _isLoading.value = true
            try {
                val results = fetchFilteredResults(1)
                _searchResults.value = results
                isEndReached = results.isEmpty()
            } catch (_: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchFilteredResults(page: Int): List<SearchMovieUi> {
        val isKidsMode = com.example.alphacinema.data.local.SettingsManager.getInstance().isKidsModeEnabled.value
        val filters = _appliedFilters.value
        val keywordParam = currentQuery.trim()

        if (isKidsMode) {
            // Kids mode: use Firestore to ensure only isKidsFriendly movies are returned
            // Note: Firestore search here only supports keyword filtering, not the advanced API filters
            val repository = com.example.alphacinema.data.repository.FirestoreRepository()
            val movies = repository.searchMovies(keywordParam, isKidsMode = true, limit = 20)
            return movies.map { firestoreMovie ->
                SearchMovieUi(
                    movieId = firestoreMovie.slug,
                    title = firestoreMovie.title,
                    subtitle = firestoreMovie.categories.firstOrNull() ?: firestoreMovie.originName,
                    badge = "Full",
                    badgeColor = "gray",
                    rating = if (firestoreMovie.tmdbVoteAverage > 0.0) firestoreMovie.tmdbVoteAverage.toString() else "N/A",
                    posterUrl = firestoreMovie.posterUrl
                )
            }
        } else {
            // Normal mode: use API for full catalog search
            val response = api.searchMovies(
                keyword = keywordParam,
                page = page,
                limit = 21,
                category = filters.category?.slug,
                country = filters.country?.slug,
                year = filters.year?.slug,
                sortLang = filters.sortLang?.slug
            )

            val items = response.data?.items ?: response.items ?: emptyList()
            return items.map { it.toSearchUi() }
        }
    }

    private fun MovieItem.toSearchUi() = SearchMovieUi(
        movieId = slug,
        title = name,
        subtitle = category?.firstOrNull()?.name ?: origin_name ?: "Phim",
        badge = episode_current ?: "HD",
        badgeColor = "gray",
        rating = getRating(),
        posterUrl = getFullPosterUrl()
    )
}
