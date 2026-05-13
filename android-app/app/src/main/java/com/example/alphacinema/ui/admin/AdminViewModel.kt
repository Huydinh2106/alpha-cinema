package com.example.alphacinema.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphacinema.data.api.PhimApiService
import com.example.alphacinema.data.model.FirestoreMovie
import com.example.alphacinema.data.model.MovieItem
import com.example.alphacinema.data.repository.FirestoreRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AdminUiState {
    object Idle : AdminUiState()
    object Loading : AdminUiState()
    data class Success(val movies: List<MovieItem>) : AdminUiState()
    data class Error(val message: String) : AdminUiState()
}

class AdminViewModel(
    private val apiService: PhimApiService,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminUiState>(AdminUiState.Idle)
    val uiState: StateFlow<AdminUiState> = _uiState

    private val _addMovieStatus = MutableStateFlow<String?>(null)
    val addMovieStatus: StateFlow<String?> = _addMovieStatus

    fun searchMovies(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.value = AdminUiState.Loading
            try {
                val response = apiService.searchMovies(query)
                val items = response.data?.items ?: response.items ?: emptyList()
                _uiState.value = AdminUiState.Success(items)
            } catch (e: Exception) {
                _uiState.value = AdminUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun addMovieToDatabase(movieItem: MovieItem) {
        viewModelScope.launch {
            _addMovieStatus.value = "Đang thêm phim ${movieItem.name}..."
            try {
                // 1. Fetch full details to get categories, countries, content, etc.
                val detailResponse = apiService.getMovieDetail(movieItem.slug)
                val m = detailResponse.movie ?: throw Exception("Không lấy được chi tiết phim")

                // 2. Map categories and evaluate age rating
                val categories = m.category?.map { it.name } ?: emptyList()
                val (ageRating, isKidsFriendly) = evaluateAgeRating(categories)

                // 3. Create keywords for search
                val keywords = generateKeywords(m.name, m.origin_name ?: "")

                // 4. Create FirestoreMovie object
                val firestoreMovie = FirestoreMovie(
                    slug = m.slug,
                    title = m.name,
                    originName = m.origin_name ?: "",
                    type = m.type ?: "single",
                    status = m.status ?: "completed",
                    posterUrl = m.getFullPosterUrl(),
                    thumbUrl = m.thumb_url ?: "",
                    year = m.year?.toLong() ?: 0L,
                    content = m.content ?: "",
                    categories = categories,
                    countries = m.country?.map { it.name } ?: emptyList(),
                    actors = m.actor ?: emptyList(),
                    directors = m.director ?: emptyList(),
                    searchKeywords = keywords,
                    ageRating = ageRating,
                    isKidsFriendly = isKidsFriendly,
                    modifiedTime = Timestamp.now()
                )

                // 5. Save to Firestore
                firestoreRepository.saveMovie(firestoreMovie)
                _addMovieStatus.value = "Đã thêm thành công: ${m.name}"
            } catch (e: Exception) {
                _addMovieStatus.value = "Lỗi khi thêm phim: ${e.message}"
            }
        }
    }

    fun clearStatus() {
        _addMovieStatus.value = null
    }

    private fun evaluateAgeRating(categories: List<String>): Pair<String, Boolean> {
        val cats = categories.map { it.lowercase() }
        var ageRating = "13+"
        var isKidsFriendly = false

        val isAdult = cats.any { it.contains("18+") || it.contains("kinh dị") || it.contains("người lớn") || it.contains("tình cảm") }
        val isMature = cats.any { it.contains("chiến tranh") || it.contains("bạo lực") || it.contains("hành động") || it.contains("giật gân") || it.contains("tội phạm") }
        val isKids = cats.any { it.contains("hoạt hình") || it.contains("gia đình") || it.contains("học đường") || it.contains("âm nhạc") }

        if (isAdult) {
            ageRating = "18+"
        } else if (isMature) {
            ageRating = "16+"
        } else if (isKids) {
            ageRating = "P" // Changed "G" to "P" as requested in previous turns
            isKidsFriendly = true
        }

        return Pair(ageRating, isKidsFriendly)
    }

    private fun generateKeywords(title: String, originName: String): List<String> {
        val keywords = mutableSetOf<String>()
        val addWords = { str: String ->
            val norm = str.lowercase().trim()
            if (norm.isNotBlank()) {
                keywords.add(norm)
                norm.split(Regex("\\s+")).forEach { if (it.length > 1) keywords.add(it) }
            }
        }
        addWords(title)
        addWords(originName)
        return keywords.toList()
    }

    // --- CATEGORY MANAGEMENT ---
    
    private val _categories = MutableStateFlow<List<com.example.alphacinema.data.model.HomeCategory>>(emptyList())
    val categories: StateFlow<List<com.example.alphacinema.data.model.HomeCategory>> = _categories

    private val _categoryStatus = MutableStateFlow<String?>(null)
    val categoryStatus: StateFlow<String?> = _categoryStatus

    private val _categorySearchState = MutableStateFlow<AdminUiState>(AdminUiState.Idle)
    val categorySearchState: StateFlow<AdminUiState> = _categorySearchState

    fun searchMoviesForCategory(query: String) {
        if (query.isBlank()) {
            _categorySearchState.value = AdminUiState.Idle
            return
        }
        viewModelScope.launch {
            _categorySearchState.value = AdminUiState.Loading
            try {
                val response = apiService.searchMovies(query)
                val items = response.data?.items ?: response.items ?: emptyList()
                _categorySearchState.value = AdminUiState.Success(items)
            } catch (e: Exception) {
                _categorySearchState.value = AdminUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun clearCategorySearch() {
        _categorySearchState.value = AdminUiState.Idle
    }

    fun addMovieToCategory(movieItem: MovieItem, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _categoryStatus.value = "Đang tải dữ liệu phim ${movieItem.name}..."
            try {
                val detailResponse = apiService.getMovieDetail(movieItem.slug)
                val m = detailResponse.movie ?: throw Exception("Không lấy được chi tiết phim")

                val categories = m.category?.map { it.name } ?: emptyList()
                val (ageRating, isKidsFriendly) = evaluateAgeRating(categories)
                val keywords = generateKeywords(m.name, m.origin_name ?: "")

                val firestoreMovie = FirestoreMovie(
                    slug = m.slug,
                    title = m.name,
                    originName = m.origin_name ?: "",
                    type = m.type ?: "single",
                    status = m.status ?: "completed",
                    posterUrl = m.getFullPosterUrl(),
                    thumbUrl = m.thumb_url ?: "",
                    year = m.year?.toLong() ?: 0L,
                    content = m.content ?: "",
                    categories = categories,
                    countries = m.country?.map { it.name } ?: emptyList(),
                    actors = m.actor ?: emptyList(),
                    directors = m.director ?: emptyList(),
                    searchKeywords = keywords,
                    ageRating = ageRating,
                    isKidsFriendly = isKidsFriendly,
                    modifiedTime = Timestamp.now()
                )

                firestoreRepository.saveMovie(firestoreMovie)
                _categoryStatus.value = null // clear status smoothly
                onSuccess(m.slug)
            } catch (e: Exception) {
                _categoryStatus.value = "Lỗi khi thêm phim: ${e.message}"
            }
        }
    }

    fun fetchCategories() {
        viewModelScope.launch {
            _categories.value = firestoreRepository.getAllHomeCategories()
        }
    }

    fun saveCategory(category: com.example.alphacinema.data.model.HomeCategory) {
        viewModelScope.launch {
            _categoryStatus.value = "Đang lưu danh mục..."
            try {
                firestoreRepository.saveHomeCategory(category)
                _categoryStatus.value = "Lưu danh mục thành công!"
                fetchCategories() // Refresh list
            } catch (e: Exception) {
                _categoryStatus.value = "Lỗi khi lưu: ${e.message}"
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            _categoryStatus.value = "Đang xóa danh mục..."
            try {
                firestoreRepository.deleteHomeCategory(categoryId)
                _categoryStatus.value = "Xóa danh mục thành công!"
                fetchCategories() // Refresh list
            } catch (e: Exception) {
                _categoryStatus.value = "Lỗi khi xóa: ${e.message}"
            }
        }
    }

    fun clearCategoryStatus() {
        _categoryStatus.value = null
    }
}
