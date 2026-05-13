package com.example.alphacinema.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphacinema.data.repository.FirestoreRepository
import com.example.alphacinema.data.model.MovieItem
import com.example.alphacinema.data.repository.MovieRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val repository = MovieRepository()
    private val firestoreRepo = FirestoreRepository()

    // Loading states
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isKidsMode = MutableStateFlow(false)
    val isKidsMode: StateFlow<Boolean> = _isKidsMode.asStateFlow()

    // Movie sections
    private val _heroMovies = MutableStateFlow<List<MovieItem>>(emptyList())
    val heroMovies: StateFlow<List<MovieItem>> = _heroMovies.asStateFlow()

    // Hero movie descriptions from TMDB (slug -> overview)
    private val _heroDescriptions = MutableStateFlow<Map<String, String>>(emptyMap())
    val heroDescriptions: StateFlow<Map<String, String>> = _heroDescriptions.asStateFlow()

    private val _phimBoMoi = MutableStateFlow<List<MovieItem>>(emptyList())
    val phimBoMoi: StateFlow<List<MovieItem>> = _phimBoMoi.asStateFlow()

    private val _phimLeHot = MutableStateFlow<List<MovieItem>>(emptyList())
    val phimLeHot: StateFlow<List<MovieItem>> = _phimLeHot.asStateFlow()

    private val _normalHanhDong = MutableStateFlow<List<MovieItem>>(emptyList())
    val normalHanhDong: StateFlow<List<MovieItem>> = _normalHanhDong.asStateFlow()

    private val _normalTinhCam = MutableStateFlow<List<MovieItem>>(emptyList())
    val normalTinhCam: StateFlow<List<MovieItem>> = _normalTinhCam.asStateFlow()

    private val _normalAuMy = MutableStateFlow<List<MovieItem>>(emptyList())
    val normalAuMy: StateFlow<List<MovieItem>> = _normalAuMy.asStateFlow()

    private val _normalHinhSu = MutableStateFlow<List<MovieItem>>(emptyList())
    val normalHinhSu: StateFlow<List<MovieItem>> = _normalHinhSu.asStateFlow()

    private val _normalVienTuong = MutableStateFlow<List<MovieItem>>(emptyList())
    val normalVienTuong: StateFlow<List<MovieItem>> = _normalVienTuong.asStateFlow()

    private val _normalHaiHuoc = MutableStateFlow<List<MovieItem>>(emptyList())
    val normalHaiHuoc: StateFlow<List<MovieItem>> = _normalHaiHuoc.asStateFlow()

    private val _normalKinhDi = MutableStateFlow<List<MovieItem>>(emptyList())
    val normalKinhDi: StateFlow<List<MovieItem>> = _normalKinhDi.asStateFlow()

    private val _normalCoTrang = MutableStateFlow<List<MovieItem>>(emptyList())
    val normalCoTrang: StateFlow<List<MovieItem>> = _normalCoTrang.asStateFlow()

    // Kids sections
    private val _kidsHoatHinh = MutableStateFlow<List<MovieItem>>(emptyList())
    val kidsHoatHinh: StateFlow<List<MovieItem>> = _kidsHoatHinh.asStateFlow()

    private val _kidsAnime = MutableStateFlow<List<MovieItem>>(emptyList())
    val kidsAnime: StateFlow<List<MovieItem>> = _kidsAnime.asStateFlow()

    private val _kidsGiaDinh = MutableStateFlow<List<MovieItem>>(emptyList())
    val kidsGiaDinh: StateFlow<List<MovieItem>> = _kidsGiaDinh.asStateFlow()

    private val _kidsPhieuLuu = MutableStateFlow<List<MovieItem>>(emptyList())
    val kidsPhieuLuu: StateFlow<List<MovieItem>> = _kidsPhieuLuu.asStateFlow()

    private val _kidsHaiHuoc = MutableStateFlow<List<MovieItem>>(emptyList())
    val kidsHaiHuoc: StateFlow<List<MovieItem>> = _kidsHaiHuoc.asStateFlow()

    private val _kidsKhoaHoc = MutableStateFlow<List<MovieItem>>(emptyList())
    val kidsKhoaHoc: StateFlow<List<MovieItem>> = _kidsKhoaHoc.asStateFlow()

    // Active Category Chip
    private val _selectedChip = MutableStateFlow("Đề xuất")
    val selectedChip: StateFlow<String> = _selectedChip.asStateFlow()

    init {
        viewModelScope.launch {
            com.example.alphacinema.data.local.SettingsManager.getInstance().isKidsModeEnabled.collect { kidsMode ->
                _isKidsMode.value = kidsMode
                loadData()
            }
        }
    }

    private fun fetchHeroDescriptions(heroes: List<MovieItem>) {
        viewModelScope.launch {
            val descMap = mutableMapOf<String, String>()
            heroes.forEach { movie ->
                launch {
                    try {
                        val firestoreMovie = firestoreRepo.getMovieBySlug(movie.slug)
                        val content = firestoreMovie?.content
                        if (!content.isNullOrBlank()) {
                            // Strip HTML tags
                            val cleanContent = content.replace(Regex("<[^>]*>"), "").trim()
                            if (cleanContent.isNotBlank()) {
                                descMap[movie.slug] = cleanContent
                                _heroDescriptions.value = descMap.toMap()
                            }
                        }
                    } catch (_: Exception) {
                        // Skip if Firestore fails for this movie
                    }
                }
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

                // Fetch descriptions from TMDB for hero movies
                fetchHeroDescriptions(latest.take(10))

                // Collect slugs used so far for dedup
                val heroSlugs = latest.take(10).map { it.slug }.toSet()

                // Load remaining sections in parallel using curated categories
                val safeRun = { block: suspend () -> List<MovieItem> -> async { try { block() } catch(e:Exception){ emptyList() } } }
                val heroSet = heroSlugs.toSet()
                fun dedup(list: List<MovieItem>): List<MovieItem> = list.filter { it.slug !in heroSet }
                
                if (_isKidsMode.value) {
                    val hoatHinhDef = safeRun { repository.getMoviesByHomeCategory("kids-hoat-hinh", limit = 15) }
                    val animeDef = safeRun { repository.getMoviesByHomeCategory("kids-anime", limit = 15) }
                    val giaDinhDef = safeRun { repository.getMoviesByHomeCategory("kids-gia-dinh", limit = 15) }
                    val phieuLuuDef = safeRun { repository.getMoviesByHomeCategory("kids-phieu-luu", limit = 15) }
                    val haiHuocDef = safeRun { repository.getMoviesByHomeCategory("kids-hai-huoc", limit = 15) }
                    val khoaHocDef = safeRun { repository.getMoviesByHomeCategory("kids-khoa-hoc", limit = 15) }

                    _kidsHoatHinh.value = dedup(hoatHinhDef.await())
                    _kidsAnime.value = dedup(animeDef.await())
                    _kidsGiaDinh.value = dedup(giaDinhDef.await())
                    _kidsPhieuLuu.value = dedup(phieuLuuDef.await())
                    _kidsHaiHuoc.value = dedup(haiHuocDef.await())
                    _kidsKhoaHoc.value = dedup(khoaHocDef.await())
                } else {
                    val phimBoDeferred = safeRun { repository.getMoviesByHomeCategory("phim-bo-moi", limit = 10) }
                    val phimLeDeferred = safeRun { repository.getMoviesByHomeCategory("phim-le-hot", limit = 10) }
                    
                    val hanhDongDef = safeRun { repository.getMoviesByHomeCategory("normal-hanh-dong", limit = 10) }
                    val tinhCamDef = safeRun { repository.getMoviesByHomeCategory("normal-tinh-cam", limit = 10) }
                    val auMyDef = safeRun { repository.getMoviesByHomeCategory("normal-au-my", limit = 10) }
                    val hinhSuDef = safeRun { repository.getMoviesByHomeCategory("normal-hinh-su", limit = 10) }
                    val vienTuongDef = safeRun { repository.getMoviesByHomeCategory("normal-vien-tuong", limit = 10) }
                    val haiHuocDef = safeRun { repository.getMoviesByHomeCategory("normal-hai-huoc", limit = 10) }
                    val kinhDiDef = safeRun { repository.getMoviesByHomeCategory("normal-kinh-di", limit = 10) }
                    val coTrangDef = safeRun { repository.getMoviesByHomeCategory("normal-co-trang", limit = 10) }

                    _phimBoMoi.value = dedup(phimBoDeferred.await())
                    _phimLeHot.value = dedup(phimLeDeferred.await())
                    
                    _normalHanhDong.value = dedup(hanhDongDef.await())
                    _normalTinhCam.value = dedup(tinhCamDef.await())
                    _normalAuMy.value = dedup(auMyDef.await())
                    _normalHinhSu.value = dedup(hinhSuDef.await())
                    _normalVienTuong.value = dedup(vienTuongDef.await())
                    _normalHaiHuoc.value = dedup(haiHuocDef.await())
                    _normalKinhDi.value = dedup(kinhDiDef.await())
                    _normalCoTrang.value = dedup(coTrangDef.await())
                }

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
