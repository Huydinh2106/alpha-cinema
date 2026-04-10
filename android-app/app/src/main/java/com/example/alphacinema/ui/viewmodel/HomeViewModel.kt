package com.example.alphacinema.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphacinema.data.model.MovieItem
import com.example.alphacinema.data.repository.MovieRepository
import kotlinx.coroutines.async
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

    private val _phimTrungQuoc = MutableStateFlow<List<MovieItem>>(emptyList())
    val phimTrungQuoc: StateFlow<List<MovieItem>> = _phimTrungQuoc.asStateFlow()

    private val _phimAuMy = MutableStateFlow<List<MovieItem>>(emptyList())
    val phimAuMy: StateFlow<List<MovieItem>> = _phimAuMy.asStateFlow()

    private val _phimHanQuoc = MutableStateFlow<List<MovieItem>>(emptyList())
    val phimHanQuoc: StateFlow<List<MovieItem>> = _phimHanQuoc.asStateFlow()

    private val _phimDienAnh = MutableStateFlow<List<MovieItem>>(emptyList())
    val phimDienAnh: StateFlow<List<MovieItem>> = _phimDienAnh.asStateFlow()

    private val _animeMoi = MutableStateFlow<List<MovieItem>>(emptyList())
    val animeMoi: StateFlow<List<MovieItem>> = _animeMoi.asStateFlow()

    // Active Category Chip
    private val _selectedChip = MutableStateFlow("Đề xuất")
    val selectedChip: StateFlow<String> = _selectedChip.asStateFlow()

    init {
        viewModelScope.launch {
            com.example.alphacinema.data.local.SettingsManager.getInstance().isKidsModeEnabled.collect {
                loadData()
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // 1. Hero carousel: latest movies (mixed types)
                val latest = try {
                    repository.getLatestMovies(page = 1)
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "Hero load failed", e)
                    emptyList()
                }
                _heroMovies.value = latest.take(10)

                // Collect slugs used so far for dedup
                val heroSlugs = latest.take(10).map { it.slug }.toSet()

                // Load remaining sections in parallel
                val safeRun = { block: suspend () -> List<MovieItem> -> async { try { block() } catch(e:Exception){ emptyList() } } }
                
                val phimBoDeferred = safeRun { repository.getMoviesByType("series", limit = 8, excludeSlugs = heroSlugs) }
                val phimLeDeferred = safeRun { repository.getMoviesByType("single", limit = 8, excludeSlugs = heroSlugs) }
                val hoatHinhDeferred = safeRun { repository.getMoviesByType("hoathinh", limit = 8, excludeSlugs = heroSlugs) }

                val hqDeferred = safeRun { repository.getMoviesByCountry("Hàn Quốc", limit = 8) }
                val tqDeferred = safeRun { repository.getMoviesByCountry("Trung Quốc", limit = 8) }
                val amDeferred = safeRun { repository.getMoviesByCountry("Âu Mỹ", limit = 8) }
                val dienAnhDeferred = safeRun { repository.getMoviesByType("single", limit = 15) } // Fetch a bit more to filter single/theatrical better
                val animeDeferred = safeRun { repository.getMoviesByType("hoathinh", limit = 15) }

                val phimBo = phimBoDeferred.await()
                val phimLe = phimLeDeferred.await()
                val hoatHinh = hoatHinhDeferred.await()
                
                val pHanQuoc = hqDeferred.await()
                val pTrungQuoc = tqDeferred.await()
                val pAuMy = amDeferred.await()
                val pDienAnh = dienAnhDeferred.await()
                val pAnime = animeDeferred.await()

                val usedSlugs = heroSlugs.toMutableSet()
                
                fun dedup(list: List<MovieItem>): List<MovieItem> = list.filter { usedSlugs.add(it.slug) }

                _phimBoMoi.value = dedup(phimBo)
                _phimLeHot.value = dedup(phimLe)
                _phimHanhDong.value = dedup(hoatHinh)
                
                _phimHanQuoc.value = dedup(pHanQuoc)
                _phimTrungQuoc.value = dedup(pTrungQuoc)
                _phimAuMy.value = dedup(pAuMy)
                _phimDienAnh.value = dedup(pDienAnh)
                _animeMoi.value = dedup(pAnime)

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
