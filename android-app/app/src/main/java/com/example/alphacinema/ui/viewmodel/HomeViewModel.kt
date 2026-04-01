package com.example.alphacinema.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphacinema.data.model.MovieItem
import com.example.alphacinema.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val repository = MovieRepository()

    // Loading states
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Movie sections
    private val _heroMovies = MutableStateFlow<List<MovieItem>>(emptyList())
    val heroMovies: StateFlow<List<MovieItem>> = _heroMovies.asStateFlow()

    private val _phimBoMoi = MutableStateFlow<List<MovieItem>>(emptyList())
    val phimBoMoi: StateFlow<List<MovieItem>> = _phimBoMoi.asStateFlow()

    private val _phimLeHot = MutableStateFlow<List<MovieItem>>(emptyList())
    val phimLeHot: StateFlow<List<MovieItem>> = _phimLeHot.asStateFlow()

    private val _phimHanhDong = MutableStateFlow<List<MovieItem>>(emptyList())
    val phimHanhDong: StateFlow<List<MovieItem>> = _phimHanhDong.asStateFlow()

    // Active Category Chip
    private val _selectedChip = MutableStateFlow("Đề xuất")
    val selectedChip: StateFlow<String> = _selectedChip.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Fetch in parallel using async or sequential. Sequential for simplicity right now
                val latest = repository.getLatestMovies(page = 1)
                val phimBo = repository.getMoviesByType(type = "phim-bo", limit = 10)
                val phimLe = repository.getMoviesByType(type = "phim-le", limit = 10)
                val hanhDong = repository.getMoviesByCategory(slug = "hanh-dong", limit = 10)

                _heroMovies.value = latest.take(10) // Top 10 for hero carousel
                _phimBoMoi.value = phimBo
                _phimLeHot.value = phimLe
                _phimHanhDong.value = hanhDong

            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to fetch movies"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setCategory(category: String) {
        _selectedChip.value = category
    }
}
