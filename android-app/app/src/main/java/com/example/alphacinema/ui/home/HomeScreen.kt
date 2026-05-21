@file:OptIn(ExperimentalFoundationApi::class)
package com.example.alphacinema.ui.home
    import androidx.compose.foundation.ExperimentalFoundationApi
    import androidx.compose.foundation.Image
    import androidx.compose.foundation.background
    import androidx.compose.foundation.border
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.interaction.MutableInteractionSource
    import androidx.compose.foundation.combinedClickable
    import androidx.compose.foundation.lazy.LazyRow
    import androidx.compose.foundation.lazy.items
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.rememberScrollState
    import androidx.compose.foundation.verticalScroll
    import androidx.compose.foundation.pager.HorizontalPager
    import androidx.compose.foundation.pager.PageSize
    import androidx.compose.foundation.pager.PagerState
    import androidx.compose.foundation.pager.rememberPagerState
    import androidx.compose.foundation.shape.CircleShape
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.outlined.HeadsetMic
    import androidx.compose.material.icons.outlined.Home
    import androidx.compose.material.icons.outlined.NotificationsNone
    import androidx.compose.material.icons.outlined.PersonOutline
    import androidx.compose.material.icons.outlined.PlayArrow
    import androidx.compose.material.icons.outlined.KeyboardArrowDown
    import androidx.compose.material.icons.outlined.Search
    import androidx.compose.material.icons.outlined.Settings
    import androidx.compose.material.icons.outlined.Info
    import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
    import androidx.compose.material.icons.filled.Home
    import androidx.compose.material.icons.filled.Search
    import androidx.compose.material.icons.filled.HeadsetMic
    import androidx.compose.material.icons.filled.Person
    import androidx.compose.material3.*
    import androidx.compose.animation.core.animateFloatAsState
    import androidx.compose.animation.core.tween
    import androidx.compose.runtime.*
    import androidx.lifecycle.viewmodel.compose.viewModel
    import android.content.Context
    import android.content.Intent
    import android.net.Uri
    import android.os.Handler
    import android.os.Looper
    import android.webkit.JavascriptInterface
    import android.webkit.WebChromeClient
    import android.webkit.WebResourceError
    import android.webkit.WebResourceRequest
    import android.webkit.WebView
    import android.webkit.WebViewClient
    import androidx.compose.ui.viewinterop.AndroidView
    import com.example.alphacinema.R
    import com.example.alphacinema.data.model.WatchHistoryItem
    import com.example.alphacinema.ui.app.ScreenType
    import com.example.alphacinema.ui.components.GradientPlayButton
    import com.example.alphacinema.ui.account.FilterKind
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.blur
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.draw.clipToBounds
    import androidx.compose.ui.graphics.graphicsLayer
    import androidx.compose.ui.graphics.Brush
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.layout.ContentScale
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.text.style.TextOverflow
    import androidx.compose.ui.tooling.preview.Preview
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.platform.LocalDensity
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.lerp
    import androidx.compose.ui.unit.sp
    import androidx.compose.ui.zIndex
    import coil.compose.AsyncImage
    import androidx.compose.ui.res.painterResource
    import com.example.alphacinema.ui.theme.AlphaCinemaTheme
    import kotlinx.coroutines.delay
    import kotlin.math.PI
    import kotlin.math.cos
    import kotlin.math.sin
    import androidx.compose.foundation.layout.WindowInsets
    import androidx.compose.foundation.layout.asPaddingValues
    import androidx.compose.foundation.layout.navigationBars
    import androidx.compose.foundation.layout.statusBars

    data class MovieUi(
        val title: String,
        val subtitle: String,
        val description: String,
        val rating: String,
        val age: String,
        val year: String,
        val season: String,
        val episode: String,
        val posterUrl: String = "",
        val slug: String = ""
    )

    data class RecommendMovieUi(
        val title: String,
        val originName: String,
        val quality: String,
        val genre: String,
        val rating: String,
        val posterUrl: String = "",
        val slug: String = "",
        val description: String = "",
        val age: String = "",
        val year: String = "",
        val episode: String = "",
        val genres: List<String> = emptyList(),
        val tmdbId: String? = null,
        val tmdbType: String? = null
    )

    data class RecommendGroupUi(
        val title: String,
        val movies: List<RecommendMovieUi>
    )

    data class ContinueWatchingMovieUi(
        val title: String,
        val originName: String,
        val posterUrl: String,
        val slug: String,
        val episodeId: String?,
        val startPositionMs: Long,
        val progressFraction: Float
    )

    private fun Int.loopedIndex(size: Int): Int {
        val mod = this % size
        return if (mod < 0) mod + size else mod
    }

    private fun WatchHistoryItem.toContinueWatchingMovieUi(): ContinueWatchingMovieUi? {
        val safeDuration = duration.coerceAtLeast(0L)
        if (movieId.isBlank() || movieName.isBlank() || safeDuration == 0L) return null

        val safeProgress = progress.coerceIn(0L, safeDuration)
        if (safeProgress == 0L) return null

        return ContinueWatchingMovieUi(
            title = movieName,
            originName = originName,
            posterUrl = posterUrl,
            slug = movieId,
            episodeId = episodeId.takeIf { it.isNotBlank() },
            startPositionMs = (safeProgress - CONTINUE_WATCHING_REWIND_MS).coerceAtLeast(0L),
            progressFraction = (safeProgress.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
        )
    }

    private const val CONTINUE_WATCHING_REWIND_MS = 3_000L

    @Composable
    fun HomeScreen(
        viewModel: HomeViewModel = viewModel(),
        initialShowNotification: Boolean = false,
        onPlayMovie: (MovieUi) -> Unit = {},
        onSeeMore: (FilterKind, String, String) -> Unit = { _, _, _ -> },
        onOpenMovieDetail: (String) -> Unit = {},
        onNavigateToPlan: () -> Unit = {},
        onNavigateToMovieType: (type: String, title: String) -> Unit = { _, _ -> },
        onContinueWatchingMovie: (movieSlug: String, episodeId: String?, startPositionMs: Long) -> Unit = { _, _, _ -> }
    ) {
        val isLoading by viewModel.isLoading.collectAsState()
        val error by viewModel.error.collectAsState()
        var showNotificationScreen by remember { mutableStateOf(initialShowNotification) }
        val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
        var currentUid by remember { mutableStateOf(auth.currentUser?.uid) }
        var unreadCount by remember { mutableStateOf(0) }

        DisposableEffect(Unit) {
            val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
                currentUid = firebaseAuth.currentUser?.uid
            }
            auth.addAuthStateListener(listener)
            onDispose {
                auth.removeAuthStateListener(listener)
            }
        }

        DisposableEffect(currentUid) {
            val uid = currentUid
            if (uid == null) {
                unreadCount = 0
                return@DisposableEffect onDispose {}
            }
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val listener = db.collection("users").document(uid).collection("notifications")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("HomeScreen", "Listen notifications failed.", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        unreadCount = snapshot.documents.count { doc ->
                            !(doc.getBoolean("isRead") ?: false)
                        }
                    }
                }
            onDispose {
                listener.remove()
            }
        }


        var showGenreSheet by remember { mutableStateOf(false) }
        
        val heroItems by viewModel.heroMovies.collectAsState()
        val phimBoMoi by viewModel.phimBoMoi.collectAsState()
        val phimLeHot by viewModel.phimLeHot.collectAsState()
        val normalHanhDong by viewModel.normalHanhDong.collectAsState()
        val normalTinhCam by viewModel.normalTinhCam.collectAsState()
        val normalAuMy by viewModel.normalAuMy.collectAsState()
        val normalHinhSu by viewModel.normalHinhSu.collectAsState()
        val normalVienTuong by viewModel.normalVienTuong.collectAsState()
        val normalHaiHuoc by viewModel.normalHaiHuoc.collectAsState()
        val normalKinhDi by viewModel.normalKinhDi.collectAsState()
        val normalCoTrang by viewModel.normalCoTrang.collectAsState()

        val selectedChip by viewModel.selectedChip.collectAsState()
        val heroDescriptions by viewModel.heroDescriptions.collectAsState()
        val previewTrailerKeys by viewModel.previewTrailerKeys.collectAsState()
        val previewTrailerLoading by viewModel.previewTrailerLoading.collectAsState()
        val isKidsMode by viewModel.isKidsMode.collectAsState()
        val continueWatchingItems by viewModel.continueWatching.collectAsState()

        val kidsHoatHinh by viewModel.kidsHoatHinh.collectAsState()
        val kidsAnime by viewModel.kidsAnime.collectAsState()
        val kidsGiaDinh by viewModel.kidsGiaDinh.collectAsState()
        val kidsPhieuLuu by viewModel.kidsPhieuLuu.collectAsState()
        val kidsHaiHuoc by viewModel.kidsHaiHuoc.collectAsState()
        val kidsKhoaHoc by viewModel.kidsKhoaHoc.collectAsState()

        val continueWatchingMovies = remember(continueWatchingItems) {
            continueWatchingItems.mapNotNull { it.toContinueWatchingMovieUi() }
        }

        val movies = remember(heroItems, heroDescriptions) {
            heroItems.map {
                MovieUi(
                    title = it.name,
                    subtitle = it.origin_name ?: "",
                    description = heroDescriptions[it.slug] ?: "",
                    rating = it.getRating(),
                    age = it.ageRating ?: "",
                    year = it.year?.toString() ?: "",
                    season = it.tmdb?.season?.let { s -> if (s > 0) "Season $s" else "" } ?: "",
                    episode = it.episode_current
                        ?: when (it.tmdb?.type) {
                            "tv" -> "Phim bộ"
                            "movie" -> "Phim lẻ"
                            else -> ""
                        },
                    posterUrl = it.getFullPosterUrl(),
                    slug = it.slug
                )
            }.ifEmpty { // fallback to prevent empty state crashes if the list is empty during initial load
                List(5) {
                    MovieUi("Đang tải...", "Đang kết nối dữ liệu", "Vui lòng chờ trong giây lát...", "-", "", "", "", "")
                }
            }
        }

        val recommendationGroups = remember(
            phimBoMoi, phimLeHot, 
            normalHanhDong, normalTinhCam, normalAuMy, normalHinhSu, normalVienTuong, normalHaiHuoc, normalKinhDi, normalCoTrang,
            kidsHoatHinh, kidsAnime, kidsGiaDinh, kidsPhieuLuu, kidsHaiHuoc, kidsKhoaHoc,
            selectedChip, isLoading, isKidsMode
        ) {
            val allGroups = mutableListOf<RecommendGroupUi>()

            fun mapToUi(list: List<com.example.alphacinema.data.model.MovieItem>) = list.map { 
                RecommendMovieUi(
                    title = it.name, 
                    originName = it.origin_name ?: "", 
                    quality = it.quality ?: "HD", 
                    genre = it.category?.firstOrNull()?.name ?: "", 
                    rating = it.getRating(), 
                    posterUrl = it.getFullPosterUrl(), 
                    slug = it.slug,
                    description = "Bộ phim cực kì hấp dẫn, lôi cuốn. Cùng thưởng thức ngay nhé!",
                    age = it.ageRating ?: "",
                    year = it.year?.toString() ?: "2024",
                    episode = it.episode_current ?: "Tập 1",
                    genres = it.category?.map { c -> c.name } ?: emptyList(),
                    tmdbId = it.tmdb?.id,
                    tmdbType = it.tmdb?.type
                ) 
            }.ifEmpty { if (isLoading) List(5) { RecommendMovieUi("Đang tải...", "", "", "", "-", "", "") } else emptyList() }

            fun addGroup(title: String, mappedList: List<RecommendMovieUi>) {
                if (mappedList.isNotEmpty()) {
                    allGroups.add(RecommendGroupUi(title, mappedList))
                }
            }

            if (isKidsMode) {
                val hoatHinhList = mapToUi(kidsHoatHinh)
                val animeList = mapToUi(kidsAnime)
                val giaDinhList = mapToUi(kidsGiaDinh)
                val phieuLuuList = mapToUi(kidsPhieuLuu)
                val haiHuocList = mapToUi(kidsHaiHuoc)
                val khoaHocList = mapToUi(kidsKhoaHoc)

                if (selectedChip == "Đề xuất" || selectedChip == "Hoạt hình") {
                    addGroup("Thế giới Hoạt Hình", hoatHinhList)
                    addGroup("Anime dễ thương", animeList)
                }
                if (selectedChip == "Đề xuất" || selectedChip == "Gia đình") {
                    addGroup("Phim Gia Đình ấm áp", giaDinhList)
                    addGroup("Phim Hài Hước vui nhộn", haiHuocList)
                }
                if (selectedChip == "Đề xuất" || selectedChip == "Phiêu lưu") {
                    addGroup("Khám phá & Phiêu lưu", phieuLuuList)
                    addGroup("Khoa học & Bí ẩn", khoaHocList)
                }
            } else {
                val boMoiList = mapToUi(phimBoMoi)
                val leHotList = mapToUi(phimLeHot)
                
                val hanhDongList = mapToUi(normalHanhDong)
                val tinhCamList = mapToUi(normalTinhCam)
                val auMyList = mapToUi(normalAuMy)
                val hinhSuList = mapToUi(normalHinhSu)
                val vienTuongList = mapToUi(normalVienTuong)
                val haiHuocList = mapToUi(normalHaiHuoc)
                val kinhDiList = mapToUi(normalKinhDi)
                val coTrangList = mapToUi(normalCoTrang)

                if (selectedChip == "Đề xuất" || selectedChip == "Phim bộ") {
                    addGroup("Phim bộ đang thịnh hành", boMoiList)
                    addGroup("Cổ Trang & Tiên Hiệp Đặc Sắc", coTrangList)
                }
                if (selectedChip == "Đề xuất" || selectedChip == "Phim lẻ") {
                    addGroup("Phim lẻ nổi bật", leHotList)
                    addGroup("Kinh Dị Lạnh Sống Lưng", kinhDiList)
                }
                if (selectedChip == "Đề xuất" || selectedChip == "Thể loại") {
                    addGroup("Kỳ Án & Phá Án Đỉnh Cao", hinhSuList)
                    addGroup("Khoa Học & Viễn Tưởng Đột Phá", vienTuongList)
                    addGroup("Hài Hước Cười Ra Nước Mắt", haiHuocList)
                }
                if (selectedChip == "Đề xuất") {
                    addGroup("Hành Động Khai Mở Nhãn Quan", hanhDongList)
                    addGroup("Tình Cảm Ngọt Ngào & Lãng Mạn", tinhCamList)
                    addGroup("Siêu Phẩm Điện Ảnh Âu Mỹ", auMyList)
                }
            }
            allGroups
        }

        val top10InsertIndex = remember(recommendationGroups) {
            if (recommendationGroups.isEmpty()) {
                0
            } else {
                (recommendationGroups.size / 2).coerceAtLeast(1)
            }
        }
        val recommendationGroupsBeforeTop10 = remember(recommendationGroups, top10InsertIndex) {
            recommendationGroups.take(top10InsertIndex)
        }
        val recommendationGroupsAfterTop10 = remember(recommendationGroups, top10InsertIndex) {
            recommendationGroups.drop(top10InsertIndex)
        }

        val top10Movies = remember {
            listOf(
                RecommendMovieUi(
                    title = "Thoát Khỏi Tận Thế", 
                    originName = "Project Hail Mary", 
                    quality = "HD", genre = "Trending", rating = "9.0", 
                    posterUrl = "https://phimimg.com/upload/vod/20260425-1/972224cff3b934fa1aa8781de7822a0d.jpg", 
                    slug = "thoat-khoi-tan-the"
                ),
                RecommendMovieUi(
                    title = "Avatar: Lửa và Tro Tàn", 
                    originName = "Avatar: Fire and Ash", 
                    quality = "HD", genre = "Trending", rating = "9.0", 
                    posterUrl = "https://phimimg.com/upload/vod/20251221-1/687373bb9894616507f27c74c0eaa598.jpg", 
                    slug = "avatar-lua-va-tro-tan"
                ),
                RecommendMovieUi(
                    title = "Thảm Họa Thiên Thạch: Di Tản", 
                    originName = "Greenland 2: Migration", 
                    quality = "HD", genre = "Trending", rating = "9.0", 
                    posterUrl = "https://phimimg.com/upload/vod/20260128-1/227d553ec342486d0cbdc34b0b1fc31d.jpg", 
                    slug = "tham-hoa-thien-thach-di-tan"
                ),
                RecommendMovieUi(
                    title = "Đại Hồng Thủy", 
                    originName = "The Great Flood", 
                    quality = "HD", genre = "Trending", rating = "9.0", 
                    posterUrl = "https://phimimg.com/upload/vod/20251220-1/aa015742d7890f5bf4fc3af9e1bb59f4.jpg", 
                    slug = "dai-hong-thuy"
                ),
                RecommendMovieUi(
                    title = "Nguyên Thủ Đối Đầu (Nguyên Thủ Quốc Gia)", 
                    originName = "Heads of State", 
                    quality = "HD", genre = "Trending", rating = "9.0", 
                    posterUrl = "https://phimimg.com/upload/vod/20250708-1/d7eff5448de409988466ff0c3f48bcaf.jpg", 
                    slug = "nguyen-thu-doi-dau-nguyen-thu-quoc-gia"
                ),
                RecommendMovieUi(
                    title = "Elio: Cậu Bé Đến Từ Trái Đất", 
                    originName = "Elio", 
                    quality = "HD", genre = "Trending", rating = "9.0", 
                    posterUrl = "https://phimimg.com/upload/vod/20250820-1/718b5ba60d0b5dec1852e00ce8822665.jpg", 
                    slug = "elio-cau-be-den-tu-trai-dat"
                ),
                RecommendMovieUi(
                    title = "Phi Vụ Động Trời 2", 
                    originName = "Zootopia 2", 
                    quality = "HD", genre = "Trending", rating = "9.0", 
                    posterUrl = "https://phimimg.com/upload/vod/20251223-1/6bc1d549b86e490274d93bab66a3654d.jpg", 
                    slug = "phi-vu-dong-troi-2"
                ),
                RecommendMovieUi(
                    title = "Cơn Say Mùa Xuân", 
                    originName = "Spring Fever", 
                    quality = "HD", genre = "Trending", rating = "9.0", 
                    posterUrl = "https://phimimg.com/upload/vod/20260106-1/deeaf96194e9eb72cc990cb4747536e6.jpg", 
                    slug = "con-say-mua-xuan"
                ),
                RecommendMovieUi(
                    title = "Tên Trộm Dấu Yêu", 
                    originName = "To My Beloved Thief", 
                    quality = "HD", genre = "Trending", rating = "9.0", 
                    posterUrl = "https://phimimg.com/upload/vod/20260105-1/0adea2e823ede3d6a2bca22d1f0ac286.jpg", 
                    slug = "ten-trom-dau-yeu"
                ),
                RecommendMovieUi(
                    title = "Liều Thuốc Cho Tình Yêu", 
                    originName = "Recipe for Love", 
                    quality = "HD", genre = "Trending", rating = "9.0", 
                    posterUrl = "https://phimimg.com/upload/vod/20260203-1/c65d018cf7b9b5ec469fc42e15a0c60f.jpg", 
                    slug = "lieu-thuoc-cho-tinh-yeu"
                )
            )
        }
        val movieCount = movies.size
        val initialPage = remember(movieCount) {
            val middle = Int.MAX_VALUE / 2
            middle - middle.loopedIndex(movieCount)
        }
        val pagerState = rememberPagerState(
            initialPage = initialPage,
            pageCount = { Int.MAX_VALUE }
        )
        val currentMovieIndex by remember {
            derivedStateOf { pagerState.currentPage.loopedIndex(movieCount) }
        }

        val scrollState = rememberScrollState()
        val collapseFraction by remember {
            derivedStateOf {
                (scrollState.value / 200f).coerceIn(0f, 1f)
            }
        }

        var previewMovie by remember { mutableStateOf<RecommendMovieUi?>(null) }
        LaunchedEffect(previewMovie?.slug) {
            previewMovie?.let { movie ->
                viewModel.loadPreviewTrailer(movie.slug, movie.tmdbId, movie.tmdbType)
            }
        }

        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val topPad = statusBarHeight + 80.dp
        val bottomPad = navBarHeight + 96.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            BackgroundLayer(posterUrl = movies[currentMovieIndex].posterUrl)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    // Bỏ padding horizontal tại đây
                    .padding(top = topPad, bottom = bottomPad)
            ) {
                Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                    CategoryChips(
                        selectedChip = selectedChip,
                        isKidsMode = isKidsMode,
                        onChipSelected = { chip ->
                            if (!isKidsMode && (chip == "Phim bộ" || chip == "Phim lẻ")) {
                                val type = if (chip == "Phim bộ") "phim-bo" else "phim-le"
                                onNavigateToMovieType(type, chip)
                            } else if (!isKidsMode && chip == "Thể loại") {
                                showGenreSheet = true
                            } else {
                                viewModel.setCategory(chip)
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(22.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    HeroCarousel(pagerState, movies, onMovieClick = { onOpenMovieDetail(it.slug) })
                    if (isLoading && heroItems.isEmpty()) {
                        com.example.alphacinema.ui.components.LottieLoadingIndicator(size = 100.dp)
                    } else if (error != null && heroItems.isEmpty()) {
                        Text(
                            text = "Lỗi tải phim",
                            color = Color.Red,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Box(modifier = Modifier.padding(horizontal = 38.dp)) {
                    MovieInfoSection(
                        movie = movies[currentMovieIndex],
                        currentMovieIndex = currentMovieIndex,
                        total = movies.size,
                        onPlayClick = { onPlayMovie(movies[currentMovieIndex]) },
                        onInfoClick = { onOpenMovieDetail(movies[currentMovieIndex].slug) }
                    )
                }
                Spacer(modifier = Modifier.height(26.dp))

                if (continueWatchingMovies.isNotEmpty()) {
                    ContinueWatchingSection(
                        movies = continueWatchingMovies,
                        onMovieClick = { movie ->
                            onContinueWatchingMovie(
                                movie.slug,
                                movie.episodeId,
                                movie.startPositionMs
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                }

                RecommendationGroupsSection(
                    groups = recommendationGroupsBeforeTop10,
                    onMovieClick = { recommendMovie ->
                        onOpenMovieDetail(recommendMovie.slug)
                    },
                    onMovieLongClick = {
                        previewMovie = it
                    },
                    onSeeMore = onSeeMore
                )
                if (recommendationGroupsBeforeTop10.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(22.dp))
                }

                Top10Section(
                    movies = top10Movies,
                    onMovieClick = { recommendMovie ->
                        onOpenMovieDetail(recommendMovie.slug)
                    }
                )
                Spacer(modifier = Modifier.height(22.dp))

                RecommendationGroupsSection(
                    groups = recommendationGroupsAfterTop10,
                    onMovieClick = { recommendMovie ->
                        onOpenMovieDetail(recommendMovie.slug)
                    },
                    onMovieLongClick = {
                        previewMovie = it
                    },
                    onSeeMore = onSeeMore
                )
            }

            // Sticky Top Header - nằm trên cùng, không bị cuộn
            TopHeader(
                collapseFraction = collapseFraction,
                unreadCount = unreadCount,

                onNotificationClick = { showNotificationScreen = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .zIndex(10f)
            )

            // Movie Preview Dialog Overlays
            if (previewMovie != null) {
                MoviePreviewDialog(
                    movie = previewMovie!!,
                    trailerKeys = previewTrailerKeys[previewMovie!!.slug],
                    isTrailerLoading = previewMovie!!.slug in previewTrailerLoading,
                    onDismiss = { previewMovie = null },
                    onPlayClick = {
                        val m = previewMovie!!
                        previewMovie = null
                        onPlayMovie(MovieUi(
                            title = m.title, subtitle = m.originName, description = m.description,
                            rating = m.rating, age = m.age, year = m.year, season = "", episode = m.episode,
                            posterUrl = m.posterUrl, slug = m.slug
                        ))
                    },
                    onDetailClick = {
                        val m = previewMovie!!
                        previewMovie = null
                        onOpenMovieDetail(m.slug)
                    }
                )
            }

            // Notification Screen Overlay
            if (showNotificationScreen) {
                NotificationScreen(
                    modifier = Modifier.zIndex(100f),
                    onClose = { showNotificationScreen = false },
                    onNavigateToMovie = { slug -> 
                        showNotificationScreen = false
                        onOpenMovieDetail(slug)
                    },
                    onNavigateToPlan = {
                        showNotificationScreen = false
                        onNavigateToPlan()
                    }
                )
            }
        }

        // Genre bottom sheet
        if (showGenreSheet) {
            GenreBottomSheet(
                onGenreSelected = { genres, sortField ->
                    showGenreSheet = false
                    val slugs = genres.joinToString(",") { it.slug }
                    val label = genres.joinToString(", ") { it.label }
                    onNavigateToMovieType("the-loai/$slugs|sort=$sortField", label)
                },
                onDismiss = { showGenreSheet = false }
            )
        }
    }

    @Composable
    fun BackgroundLayer(posterUrl: String) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Ảnh background tĩnh từ drawable - chỉ chiếm 1/3 trên màn hình
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(700.dp)
                    .align(Alignment.TopStart)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bg_home),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    alpha = 0.25f
                )
                // Gradient fade ra nền tối
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.Black
                                )
                            )
                        )
                )
            }
            // Ảnh poster mờ phía sau
            if (posterUrl.isNotEmpty()) {
                com.example.alphacinema.ui.components.AlphaCinemaImage(
                    model = posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(480.dp)
                        .blur(30.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(480.dp)
                        .blur(20.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1F1F1F),
                                    Color(0xFF0B0B0B)
                                )
                            )
                        )
                )
            }

            // Gradient overlay để fade vào nền tối
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.6f),
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black
                            )
                        )
                    )
            )
        }
    }

    @Composable
    fun TopHeader(
        collapseFraction: Float,
        unreadCount: Int,

        onNotificationClick: () -> Unit = {},
        modifier: Modifier = Modifier
    ) {
        // Animate các giá trị dựa trên scroll
        val animatedFraction by animateFloatAsState(
            targetValue = collapseFraction,
            animationSpec = tween(durationMillis = 150),
            label = "headerCollapse"
        )

        val logoSize = lerp(36.dp, 26.dp, animatedFraction)
        val iconInsideSize = lerp(20.dp, 14.dp, animatedFraction)
        val titleFontSize = lerp(18.sp, 15.sp, animatedFraction)
        val subtitleAlpha = (1f - animatedFraction * 2.5f).coerceIn(0f, 1f)
        val bgAlpha = animatedFraction
        val verticalPadding = lerp(16.dp, 14.dp, animatedFraction)
        val headerHeight = lerp(80.dp, 56.dp, animatedFraction)

        Column(
            modifier = modifier
                .background(
                    Color(0xFF141414).copy(alpha = bgAlpha * 0.95f)
                )
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.alphacinema),
                        contentDescription = "Logo",
                        modifier = Modifier.size(logoSize),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "AlphaCinema",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = titleFontSize
                        )
                        if (subtitleAlpha > 0f) {
                            Text(
                                text = "Phim hay tẹt ga",
                                color = Color.White.copy(alpha = 0.75f * subtitleAlpha),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.graphicsLayer { alpha = subtitleAlpha }
                            )
                        }
                    }
                }

                IconButton(onClick = onNotificationClick) {
                    Box {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsNone,
                            contentDescription = "Thông báo",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        if (unreadCount > 0) {
                            val badgeSize = if (unreadCount > 9) 18.dp else 16.dp
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .defaultMinSize(minWidth = badgeSize, minHeight = badgeSize)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = if (unreadCount > 9) 9.sp else 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    lineHeight = 10.sp,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }

                }

                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }

    @Composable
    fun CategoryChips(
        selectedChip: String,
        isKidsMode: Boolean,
        onChipSelected: (String) -> Unit
    ) {
        val categories = if (isKidsMode) listOf("Đề xuất", "Hoạt hình", "Gia đình", "Phiêu lưu") else listOf("Đề xuất", "Phim bộ", "Phim lẻ", "Thể loại")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { category ->
                if (selectedChip == category) {
                    FilledChip(text = category, selected = true, modifier = Modifier.weight(1f).clickable { onChipSelected(category) })
                } else if (category == "Thể loại") {
                    GenreOutlineChip(text = category, modifier = Modifier.weight(1f).clickable { onChipSelected(category) })
                } else {
                    OutlineChip(text = category, modifier = Modifier.weight(1f).clickable { onChipSelected(category) })
                }
            }
        }
    }

    @Composable
    fun FilledChip(text: String, selected: Boolean, modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (selected) Color.White else Color.Transparent)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (selected) Color.Black else Color.White,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }

	    @Composable
	    fun OutlineChip(text: String, modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .border(1.2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
	        }
	    }

    @Composable
    fun GenreOutlineChip(text: String, modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .border(1.2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

@Composable
fun HeroCarousel(pagerState: PagerState, movies: List<MovieUi>, onMovieClick: (MovieUi) -> Unit = {}) {
    val movieCount = movies.size
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        val posterWidth = 170.dp
        val sidePadding = if (maxWidth > posterWidth) (maxWidth - posterWidth) / 2 else 0.dp

        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(posterWidth),
            contentPadding = PaddingValues(horizontal = sidePadding),
            pageSpacing = 0.dp,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val movieIndex = page.loopedIndex(movieCount)
            val movie = movies[movieIndex]

            val pageOffset = (
                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    ).coerceIn(-1.5f, 1.5f)

            val angle = pageOffset * (PI / 2f)
            val isCenter = kotlin.math.abs(pageOffset) < 0.3f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        cameraDistance = 12f * density
                        val scale = 0.75f + (0.25f * cos(angle).toFloat().coerceAtLeast(0f))
                        scaleX = scale
                        scaleY = scale
                        rotationY = sin(angle).toFloat() * 45f
                        val maxOverlap = 20.dp.toPx()
                        translationX = sin(angle).toFloat() * maxOverlap
                        alpha = 0.5f + (0.5f * cos(angle).toFloat().coerceAtLeast(0f))
                    }
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        1.5.dp,
                        Color.White.copy(alpha = if (isCenter) 0.85f else 0.2f),
                        RoundedCornerShape(20.dp)
                    )
                    .background(
                        if (isCenter) Color(0xFF262626) else Color(0xFF202020)
                    )
                    .clickable { onMovieClick(movie) },
                contentAlignment = Alignment.Center
            ) {
                // Hiển thị poster từ URL
                if (movie.posterUrl.isNotEmpty()) {
                    com.example.alphacinema.ui.components.AlphaCinemaImage(
                        model = movie.posterUrl,
                        contentDescription = movie.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = if (isCenter) 0.6f else 0.1f),
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }
    }
}

    @Composable
    fun MovieInfoSection(
        movie: MovieUi,
        currentMovieIndex: Int,
        total: Int,
        onPlayClick: () -> Unit = {},
        onInfoClick: () -> Unit = {}
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = movie.title,
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = movie.subtitle,
                color = Color.White.copy(alpha = 0.60f),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(22.dp))

            val actionButtonHeight = 40.dp
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                GradientPlayButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(actionButtonHeight)
                )

                Button(
                    onClick = onInfoClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(actionButtonHeight),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.92f),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Thông tin", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            FlowMetaRow(movie)
            Spacer(modifier = Modifier.height(18.dp))

            if (movie.description.isNotBlank()) {
                Text(
                    text = movie.description,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(total) { index ->
                    val active = index == currentMovieIndex
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (active) Color.White else Color.White.copy(alpha = 0.35f))
                            .size(width = if (active) 28.dp else 10.dp, height = 10.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun FlowMetaRow(movie: MovieUi) {
        val metaItems = mutableListOf<Pair<String, Boolean>>()
        if (movie.rating != "N/A" && movie.rating.isNotBlank()) {
            metaItems.add("TMDB  ${movie.rating}" to true)
        }
        metaItems.addAll(
            listOf(
                movie.age to false,
                movie.year to false,
                movie.season to false,
                movie.episode to false
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            metaItems.filter { it.first.isNotBlank() }.forEach { (text, highlight) ->
                MetaTag(
                    text = text,
                    highlight = highlight
                )
            }
        }
    }

    @Composable
    fun MetaTag(
        text: String,
        highlight: Boolean = false,
        modifier: Modifier = Modifier
    ) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .border(
                    1.dp,
                    if (highlight) Color(0xFFFFD76A) else Color.White.copy(alpha = 0.65f),
                    RoundedCornerShape(8.dp)
                )
                .background(if (highlight) Color(0x22FFD76A) else Color.Transparent)
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Text(
                text = text,
                color = if (highlight) Color(0xFFFFE38E) else Color.White,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    @Composable
    fun ContinueWatchingSection(
        movies: List<ContinueWatchingMovieUi>,
        onMovieClick: (ContinueWatchingMovieUi) -> Unit = {}
    ) {
        if (movies.isEmpty()) return

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Tiếp tục xem",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(start = 12.dp, end = 20.dp)
            ) {
                items(movies) { movie ->
                    ContinueWatchingMovieCard(
                        movie = movie,
                        onClick = { onMovieClick(movie) }
                    )
                }
            }
        }
    }

    @Composable
    fun ContinueWatchingMovieCard(
        movie: ContinueWatchingMovieUi,
        onClick: () -> Unit = {}
    ) {
        Column(
            modifier = Modifier
                .width(132.dp)
                .clickable(onClick = onClick)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF2F2F2F),
                                Color(0xFF1A1A1A)
                            )
                        )
                    )
            ) {
                if (movie.posterUrl.isNotBlank()) {
                    com.example.alphacinema.ui.components.AlphaCinemaImage(
                        model = movie.posterUrl,
                        contentDescription = movie.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.16f),
                        modifier = Modifier
                            .size(52.dp)
                            .align(Alignment.Center)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f)
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.22f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(movie.progressFraction)
                            .height(4.dp)
                            .background(Color(0xFFF6E29A))
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = movie.title,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (movie.originName.isNotBlank() && movie.originName != movie.title) {
                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = movie.originName,
                    color = Color.White.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    @Composable
    fun RecommendationGroupsSection(
        groups: List<RecommendGroupUi>,
        onMovieClick: (RecommendMovieUi) -> Unit = {},
        onMovieLongClick: (RecommendMovieUi) -> Unit = {},
        onSeeMore: (FilterKind, String, String) -> Unit = { _, _, _ -> }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            groups.forEach { group ->
                RecommendationGroup(
                    group = group,
                    onMovieClick = onMovieClick,
                    onMovieLongClick = onMovieLongClick,
                    onSeeMore = onSeeMore
                )
            }
        }
    }

    @Composable
    fun RecommendationGroup(
        group: RecommendGroupUi,
        onMovieClick: (RecommendMovieUi) -> Unit = {},
        onMovieLongClick: (RecommendMovieUi) -> Unit = {},
        onSeeMore: (FilterKind, String, String) -> Unit = { _, _, _ -> }
    ) {
        // Map group title -> (FilterKind, slug) for the "See more" action
        val (seeMoreKind, seeMoreSlug) = when (group.title) {
            "Phim bộ đang thịnh hành",
            "Phim bộ mới tải lên" -> FilterKind.CUSTOM_CATEGORY to "phim-bo-moi"
            "Phim lẻ nổi bật" -> FilterKind.CUSTOM_CATEGORY to "phim-le-hot"
            "Cổ Trang & Tiên Hiệp Đặc Sắc" -> FilterKind.CUSTOM_CATEGORY to "normal-co-trang"
            "Kinh Dị Lạnh Sống Lưng" -> FilterKind.CUSTOM_CATEGORY to "normal-kinh-di"
            "Kỳ Án & Phá Án Đỉnh Cao" -> FilterKind.CUSTOM_CATEGORY to "normal-hinh-su"
            "Khoa Học & Viễn Tưởng Đột Phá" -> FilterKind.CUSTOM_CATEGORY to "normal-vien-tuong"
            "Hài Hước Cười Ra Nước Mắt" -> FilterKind.CUSTOM_CATEGORY to "normal-hai-huoc"
            "Hành Động Khai Mở Nhãn Quan" -> FilterKind.CUSTOM_CATEGORY to "normal-hanh-dong"
            "Tình Cảm Ngọt Ngào & Lãng Mạn" -> FilterKind.CUSTOM_CATEGORY to "normal-tinh-cam"
            "Siêu Phẩm Điện Ảnh Âu Mỹ",
            "Siêu phẩm Âu Mỹ" -> FilterKind.CUSTOM_CATEGORY to "normal-au-my"
            "Thế giới Hoạt Hình",
            "Hoạt hình 3D" -> FilterKind.CUSTOM_CATEGORY to "kids-hoat-hinh"
            "Anime dễ thương",
            "Kho tàng Anime mới nhất" -> FilterKind.CUSTOM_CATEGORY to "kids-anime"
            "Phim Gia Đình ấm áp" -> FilterKind.CUSTOM_CATEGORY to "kids-gia-dinh"
            "Phim Hài Hước vui nhộn" -> FilterKind.CUSTOM_CATEGORY to "kids-hai-huoc"
            "Khám phá & Phiêu lưu" -> FilterKind.CUSTOM_CATEGORY to "kids-phieu-luu"
            "Khoa học & Bí ẩn" -> FilterKind.CUSTOM_CATEGORY to "kids-khoa-hoc"
            "Phim Hàn Quốc mới" -> FilterKind.CUSTOM_CATEGORY to "phim-han-quoc"
            "Phim Trung Quốc mới" -> FilterKind.CUSTOM_CATEGORY to "phim-trung-quoc"
            "Phim điện ảnh mới cóng" -> FilterKind.CUSTOM_CATEGORY to "phim-chieu-rap"
            else -> FilterKind.ALL to ""
        }
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), // Giảm padding tiêu đề và nút > xuống 12dp
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = group.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                // Minimal "›" arrow button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.07f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSeeMore(seeMoreKind, seeMoreSlug, group.title) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = "Xem thêm",
                        tint = Color(0xFFF6E29A),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(start = 12.dp, end = 20.dp) // Đồng bộ lề trái thẻ phim là 12dp
            ) {
                items(group.movies) { movie ->
                    RecommendationMovieCard(
                        movie = movie,
                        onClick = { onMovieClick(movie) },
                        onLongClick = { onMovieLongClick(movie) }
                    )
                }
            }
        }
    }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable
    fun RecommendationMovieCard(movie: RecommendMovieUi, onClick: () -> Unit = {}, onLongClick: () -> Unit = {}) {
        Column(
            modifier = Modifier
                .width(132.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF2F2F2F),
                                Color(0xFF1A1A1A)
                            )
                        )
                    )
            ) {
                // Poster image
                if (movie.posterUrl.isNotEmpty()) {
                    com.example.alphacinema.ui.components.AlphaCinemaImage(
                        model = movie.posterUrl,
                        contentDescription = movie.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.16f),
                        modifier = Modifier
                            .size(52.dp)
                            .align(Alignment.Center)
                    )
                }

                // Gradient overlay for text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.4f)
                                )
                            )
                        )
                )

            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = movie.title,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            if (movie.originName.isNotEmpty()) {
                Text(
                    text = movie.originName,
                    color = Color.White.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    @Composable
    fun GlassBottomBar(
        modifier: Modifier = Modifier,
        currentScreen: ScreenType = ScreenType.HOME,
        onNavigate: (ScreenType) -> Unit = {}
    ) {
        NavigationBar(
            modifier = modifier,
            containerColor = Color(0xFF141414).copy(alpha = 0.95f),
            contentColor = Color.White,
            tonalElevation = 0.dp
        ) {
            NavigationBarItem(
                selected = currentScreen == ScreenType.HOME,
                onClick = { onNavigate(ScreenType.HOME) },
                icon = {
                    Icon(
                        imageVector = if (currentScreen == ScreenType.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                        contentDescription = "Trang chủ"
                    )
                },
                label = { Text("Trang chủ", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFFF6E29A),
                    selectedTextColor = Color.White,
                    unselectedIconColor = Color.White.copy(alpha = 0.55f),
                    unselectedTextColor = Color.White.copy(alpha = 0.55f),
                    indicatorColor = Color(0xFFF6E29A).copy(alpha = 0.15f)
                )
            )

            NavigationBarItem(
                selected = currentScreen == ScreenType.SEARCH,
                onClick = { onNavigate(ScreenType.SEARCH) },
                icon = {
                    Icon(
                        imageVector = if (currentScreen == ScreenType.SEARCH) Icons.Filled.Search else Icons.Outlined.Search,
                        contentDescription = "Tìm kiếm"
                    )
                },
                label = { Text("Tìm kiếm", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFFF6E29A),
                    selectedTextColor = Color.White,
                    unselectedIconColor = Color.White.copy(alpha = 0.55f),
                    unselectedTextColor = Color.White.copy(alpha = 0.55f),
                    indicatorColor = Color(0xFFF6E29A).copy(alpha = 0.15f)
                )
            )

            NavigationBarItem(
                selected = currentScreen == ScreenType.SUPPORT,
                onClick = { onNavigate(ScreenType.SUPPORT) },
                icon = {
                    Icon(
                        imageVector = if (currentScreen == ScreenType.SUPPORT) Icons.Filled.HeadsetMic else Icons.Outlined.HeadsetMic,
                        contentDescription = "Hỗ trợ"
                    )
                },
                label = { Text("Hỗ trợ", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFFF6E29A),
                    selectedTextColor = Color.White,
                    unselectedIconColor = Color.White.copy(alpha = 0.55f),
                    unselectedTextColor = Color.White.copy(alpha = 0.55f),
                    indicatorColor = Color(0xFFF6E29A).copy(alpha = 0.15f)
                )
            )

            NavigationBarItem(
                selected = currentScreen == ScreenType.ACCOUNT,
                onClick = { onNavigate(ScreenType.ACCOUNT) },
                icon = {
                    Icon(
                        imageVector = if (currentScreen == ScreenType.ACCOUNT) Icons.Filled.Person else Icons.Outlined.PersonOutline,
                        contentDescription = "Tài khoản"
                    )
                },
                label = { Text("Tài khoản", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFFF6E29A),
                    selectedTextColor = Color.White,
                    unselectedIconColor = Color.White.copy(alpha = 0.55f),
                    unselectedTextColor = Color.White.copy(alpha = 0.55f),
                    indicatorColor = Color(0xFFF6E29A).copy(alpha = 0.15f)
                )
            )
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun HomeScreenPreview() {
        AlphaCinemaTheme {
            HomeScreen()
        }
    }

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MoviePreviewDialog(
    movie: RecommendMovieUi,
    trailerKeys: List<String>?,
    isTrailerLoading: Boolean,
    onDismiss: () -> Unit,
    onPlayClick: () -> Unit,
    onDetailClick: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF262626))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Capture touches inside the card to avoid dismiss
                    )
            ) {
                // Header Player area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color(0xFF2F2F2F)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        !trailerKeys.isNullOrEmpty() -> {
                            TrailerPreviewWebView(
                                youtubeKeys = trailerKeys,
                                posterUrl = movie.posterUrl,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        trailerKeys == null || isTrailerLoading -> {
                            CircularProgressIndicator(
                                color = Color(0xFFF6E29A),
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        else -> {
                            val context = LocalContext.current
                            Column(
                                modifier = Modifier
                                    .clickable { openYoutubeTrailerSearch(context, movie) }
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PlayArrow,
                                    contentDescription = "Preview Play",
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier
                                        .size(70.dp)
                                        .border(3.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                        .padding(14.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tìm trailer trên YouTube",
                                    color = Color.White.copy(alpha = 0.55f),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = movie.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tags row
                    val tags = mutableListOf<String>()
                    if (movie.age.isNotEmpty()) tags.add(movie.age)
                    if (movie.year.isNotEmpty()) tags.add(movie.year)
                    tags.add("Phần 1")
                    if (movie.episode.isNotEmpty()) tags.add(movie.episode)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = tag,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Genres FlowRow
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        movie.genres.forEach { genre ->
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF4A4A4A), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = genre,
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = movie.description,
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GradientPlayButton(
                            onClick = onPlayClick,
                            modifier = Modifier.weight(1f).height(48.dp),
                            label = "Xem Ngay",
                            shape = RoundedCornerShape(8.dp)
                        )

                        Button(
                            onClick = onDetailClick,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Chi tiết", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrailerPreviewWebView(
    youtubeKeys: List<String>,
    posterUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var currentIndex by remember(youtubeKeys) { mutableStateOf(0) }
    var playerReady by remember(youtubeKeys, currentIndex) { mutableStateOf(false) }
    var loadFailed by remember(youtubeKeys, currentIndex) { mutableStateOf(false) }
    val currentKey = youtubeKeys.getOrNull(currentIndex)
    val hasMoreKeys = currentIndex < youtubeKeys.lastIndex
    val previewHtml = remember(currentKey) { currentKey?.let { buildYoutubePreviewHtml(it) } }

    LaunchedEffect(currentIndex, playerReady, loadFailed) {
        if (currentKey != null && !playerReady && !loadFailed) {
            delay(7_000)
            if (!playerReady) {
                if (hasMoreKeys) {
                    currentIndex += 1
                } else {
                    loadFailed = true
                }
            }
        }
    }

    val webView = remember(currentKey) {
        WebView(context).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        if (hasMoreKeys) {
                            currentIndex += 1
                        } else {
                            loadFailed = true
                        }
                    }
                }
            }
            addJavascriptInterface(
                object {
                    @JavascriptInterface
                    fun onPlayerPlaying() {
                        mainHandler.post {
                            playerReady = true
                            loadFailed = false
                        }
                    }

                    @JavascriptInterface
                    fun onPlayerError(errorCode: String) {
                        mainHandler.post {
                            if (hasMoreKeys) {
                                currentIndex += 1
                            } else {
                                loadFailed = true
                            }
                        }
                    }
                },
                "PreviewBridge"
            )
            isHorizontalScrollBarEnabled = false
            isVerticalScrollBarEnabled = false
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.loadsImagesAutomatically = true
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            if (previewHtml != null) {
                loadDataWithBaseURL(
                    "https://www.youtube-nocookie.com",
                    previewHtml,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        if (currentKey != null && previewHtml != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { webView },
                update = { view ->
                    if (view.url == null) {
                        view.loadDataWithBaseURL(
                            "https://www.youtube-nocookie.com",
                            previewHtml,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                }
            )
        }

        if (!playerReady || loadFailed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF171717)),
                contentAlignment = Alignment.Center
            ) {
                if (posterUrl.isNotBlank()) {
                    com.example.alphacinema.ui.components.AlphaCinemaImage(
                        model = posterUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.48f))
                    )
                }

                if (loadFailed) {
                    Column(
                        modifier = Modifier
                            .clickable { currentKey?.let { openYoutubeVideo(context, it) } }
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(64.dp)
                                .border(2.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                                .padding(14.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Mở trailer trên YouTube",
                            color = Color.White.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    CircularProgressIndicator(
                        color = Color(0xFFF6E29A),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
        }
    }
}

private fun buildYoutubePreviewHtml(youtubeKey: String): String {
    return """
        <!doctype html>
        <html>
          <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
              html, body {
                margin: 0;
                padding: 0;
                width: 100%;
                height: 100%;
                overflow: hidden;
                background: #000;
              }
              iframe {
                position: fixed;
                inset: 0;
                width: 100%;
                height: 100%;
                border: 0;
                background: #000;
              }
              #player {
                position: fixed;
                inset: 0;
                width: 100%;
                height: 100%;
                background: #000;
                pointer-events: none;
              }
            </style>
          </head>
          <body>
            <div id="player"></div>
            <script src="https://www.youtube.com/iframe_api"></script>
            <script>
              var player;
              function onYouTubeIframeAPIReady() {
                player = new YT.Player('player', {
                  host: 'https://www.youtube-nocookie.com',
                  videoId: '$youtubeKey',
                  width: '100%',
                  height: '100%',
                  playerVars: {
                    autoplay: 1,
                    mute: 1,
                    playsinline: 1,
                    controls: 0,
                    disablekb: 1,
                    fs: 0,
                    iv_load_policy: 3,
                    rel: 0,
                    modestbranding: 1,
                    origin: 'https://www.youtube-nocookie.com'
                  },
                  events: {
                    onReady: function(event) {
                      event.target.mute();
                      event.target.playVideo();
                    },
                    onStateChange: function(event) {
                      if (event.data === YT.PlayerState.PLAYING && window.PreviewBridge) {
                        window.setTimeout(function() {
                          window.PreviewBridge.onPlayerPlaying();
                        }, 700);
                      }
                    },
                    onError: function(event) {
                      if (window.PreviewBridge) {
                        window.PreviewBridge.onPlayerError(String(event.data));
                      }
                    }
                  }
                });
              }
            </script>
          </body>
        </html>
    """.trimIndent()
}

private fun openYoutubeVideo(context: Context, youtubeKey: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$youtubeKey"))
    context.startActivity(intent)
}

private fun openYoutubeTrailerSearch(context: Context, movie: RecommendMovieUi) {
    val queryTitle = movie.originName.ifBlank { movie.title }
    val query = "$queryTitle trailer"
    val uri = Uri.Builder()
        .scheme("https")
        .authority("www.youtube.com")
        .path("results")
        .appendQueryParameter("search_query", query)
        .build()
    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
}

    @Composable
    fun Top10Section(
        movies: List<RecommendMovieUi>,
        onMovieClick: (RecommendMovieUi) -> Unit
    ) {
        if (movies.isEmpty()) return

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "Top 10 Hôm Nay",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(start = 0.dp, end = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(movies.size) { index ->
                    Top10Item(
                        movie = movies[index],
                        rank = index + 1,
                        onClick = { onMovieClick(movies[index]) }
                    )
                }
            }
        }
    }

    @Composable
    fun Top10Item(
        movie: RecommendMovieUi,
        rank: Int,
        onClick: () -> Unit
    ) {
        Column(
            modifier = Modifier.clickable { onClick() },
            horizontalAlignment = Alignment.End
        ) {
            Box(
                modifier = Modifier
                    .width(190.dp)
                    .height(185.dp)
                    .clipToBounds(),
                contentAlignment = Alignment.BottomEnd
            ) {
                // Keep the rank as a single outline text layer to avoid duplicated edges.
                RankOutlineNumber(
                    rank = rank,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = 0.dp)
                        .width(if (rank >= 10) 190.dp else 150.dp)
                        .height(176.dp)
                )

                // Poster
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .height(185.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                ) {
                    com.example.alphacinema.ui.components.AlphaCinemaImage(
                        model = movie.posterUrl,
                        contentDescription = movie.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = movie.title,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(130.dp),
                textAlign = TextAlign.Start
            )
            if (movie.originName.isNotBlank() && movie.originName != movie.title) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = movie.originName,
                    color = Color.White.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(130.dp),
                    textAlign = TextAlign.Start
                )
            }
        }
    }

    @Composable
    private fun RankOutlineNumber(
        rank: Int,
        modifier: Modifier = Modifier
    ) {
        val rankText = rank.toString()
        val rankFontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
        val rankFontWeight = FontWeight.Black
        val rankFillColor = Color.Black

        Box(
            modifier = modifier,
            contentAlignment = Alignment.BottomStart
        ) {
            if (rankText == "10") {
                RankDigitOutline(
                    text = "1",
                    fontSize = 172.sp,
                    fontFamily = rankFontFamily,
                    fontWeight = rankFontWeight,
                    fillColor = rankFillColor,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .width(92.dp)
                )
                RankDigitOutline(
                    text = "0",
                    fontSize = 172.sp,
                    fontFamily = rankFontFamily,
                    fontWeight = rankFontWeight,
                    fillColor = rankFillColor,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = 48.dp)
                        .width(140.dp)
                )
            } else {
                RankDigitOutline(
                    text = rankText,
                    fontSize = 172.sp,
                    fontFamily = rankFontFamily,
                    fontWeight = rankFontWeight,
                    fillColor = rankFillColor
                )
            }
        }
    }

    @Composable
    private fun RankDigitOutline(
        text: String,
        fontSize: androidx.compose.ui.unit.TextUnit,
        fontFamily: androidx.compose.ui.text.font.FontFamily,
        fontWeight: FontWeight,
        fillColor: Color,
        modifier: Modifier = Modifier
    ) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = fontSize,
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                maxLines = 1,
                style = androidx.compose.ui.text.TextStyle(
                    drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 6.4f,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )
            )
            Text(
                text = text,
                color = fillColor,
                fontSize = fontSize,
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                maxLines = 1
            )
        }
    }
