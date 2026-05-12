@file:OptIn(ExperimentalFoundationApi::class)
package com.example.alphacinema.ui.home
    import androidx.compose.foundation.ExperimentalFoundationApi
    import androidx.compose.foundation.Image
    import androidx.compose.foundation.background
    import androidx.compose.foundation.border
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.interaction.MutableInteractionSource
    import androidx.compose.foundation.interaction.collectIsPressedAsState
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
    import com.example.alphacinema.R
    import com.example.alphacinema.data.model.MovieItem
    import com.example.alphacinema.ui.app.ScreenType
    import com.example.alphacinema.ui.components.GradientPlayButton
    import com.example.alphacinema.ui.account.FilterKind
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.blur
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.graphics.graphicsLayer
    import androidx.compose.ui.graphics.Brush
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.graphics.lerp
    import androidx.compose.ui.layout.ContentScale
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.text.style.TextOverflow
    import androidx.compose.ui.tooling.preview.Preview
    import androidx.compose.ui.platform.LocalDensity
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.lerp
    import androidx.compose.ui.unit.sp
    import androidx.compose.ui.zIndex
    import coil.compose.AsyncImage
    import androidx.compose.ui.res.painterResource
    import com.example.alphacinema.ui.theme.AlphaCinemaTheme
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
        val genres: List<String> = emptyList()
    )

    data class RecommendGroupUi(
        val title: String,
        val movies: List<RecommendMovieUi>
    )

    private fun Int.loopedIndex(size: Int): Int {
        val mod = this % size
        return if (mod < 0) mod + size else mod
    }

    @Composable
    fun HomeScreen(
        viewModel: HomeViewModel = viewModel(),
        onPlayMovie: (MovieUi) -> Unit = {},
        onSeeMore: (FilterKind, String, String) -> Unit = { _, _, _ -> }
    ) {
        val isLoading by viewModel.isLoading.collectAsState()
        val error by viewModel.error.collectAsState()
        
        val heroItems by viewModel.heroMovies.collectAsState()
        val phimBoMoi by viewModel.phimBoMoi.collectAsState()
        val phimLeHot by viewModel.phimLeHot.collectAsState()
        val phimHanhDong by viewModel.phimHanhDong.collectAsState()
        
        val pTrungQuoc by viewModel.phimTrungQuoc.collectAsState()
        val pAuMy by viewModel.phimAuMy.collectAsState()
        val pHanQuoc by viewModel.phimHanQuoc.collectAsState()
        val pDienAnh by viewModel.phimDienAnh.collectAsState()
        val pAnime by viewModel.animeMoi.collectAsState()

        val selectedChip by viewModel.selectedChip.collectAsState()
        val heroDescriptions by viewModel.heroDescriptions.collectAsState()

        val movies = remember(heroItems, heroDescriptions) {
            heroItems.map {
                MovieUi(
                    title = it.name,
                    subtitle = it.origin_name ?: "",
                    description = heroDescriptions[it.slug] ?: "",
                    rating = it.getRating(),
                    age = it.ageRating ?: "",
                    year = it.year?.toString() ?: "",
                    season = "",
                    episode = it.episode_current ?: "",
                    posterUrl = it.getFullPosterUrl(),
                    slug = it.slug
                )
            }.ifEmpty { // fallback to prevent empty state crashes if the list is empty during initial load
                List(5) {
                    MovieUi("Đang tải...", "Đang kết nối dữ liệu", "Vui lòng chờ trong giây lát...", "-", "", "", "", "")
                }
            }
        }

        val recommendationGroups = remember(phimBoMoi, phimLeHot, phimHanhDong, pTrungQuoc, pAuMy, pHanQuoc, pDienAnh, pAnime, selectedChip, isLoading) {
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
                    genres = it.category?.map { c -> c.name } ?: emptyList()
                ) 
            }.ifEmpty { if (isLoading) List(5) { RecommendMovieUi("Đang tải...", "", "", "", "-", "", "") } else emptyList() }

            val boMoiList = mapToUi(phimBoMoi)
            val leHotList = mapToUi(phimLeHot)
            val hanhDongList = mapToUi(phimHanhDong)
            
            val tqList = mapToUi(pTrungQuoc)
            val amList = mapToUi(pAuMy)
            val hqList = mapToUi(pHanQuoc)
            val dienAnhList = mapToUi(pDienAnh)
            val animeList = mapToUi(pAnime)

            fun addGroup(title: String, mappedList: List<RecommendMovieUi>) {
                if (mappedList.isNotEmpty()) {
                    allGroups.add(RecommendGroupUi(title, mappedList))
                }
            }

            if (selectedChip == "Đề xuất" || selectedChip == "Phim bộ") {
                addGroup("Phim bộ mới tải lên", boMoiList)
                addGroup("Phim Hàn Quốc mới", hqList)
                addGroup("Phim Trung Quốc mới", tqList)
                addGroup("Siêu phẩm Âu Mỹ", amList)
            }
            if (selectedChip == "Đề xuất" || selectedChip == "Phim lẻ") {
                addGroup("Phim lẻ nổi bật", leHotList)
                addGroup("Phim điện ảnh mới cóng", dienAnhList)
            }
            if (selectedChip == "Đề xuất" || selectedChip == "Thể loại") {
                addGroup("Kho tàng Anime mới nhất", animeList)
                addGroup("Hoạt hình 3D", hanhDongList)
            }
            allGroups
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070B16))
        ) {
            BackgroundLayer(posterUrl = movies[currentMovieIndex].posterUrl)

        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        // header height is approx 80dp (logo+title+subtitle+padding) + status bar
        val topPad = statusBarHeight + 80.dp
        // bottom bar (GlassBottomBar) is ~72dp pill + 12dp vertical padding each side + nav bar
        val bottomPad = navBarHeight + 96.dp

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
                        onChipSelected = { viewModel.setCategory(it) }
                    )
                }
                Spacer(modifier = Modifier.height(22.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    HeroCarousel(pagerState, movies, onMovieClick = onPlayMovie)
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
                        onPlayClick = { onPlayMovie(movies[currentMovieIndex]) }
                    )
                }
                Spacer(modifier = Modifier.height(26.dp))
                
                // RecommendationGroupsSection sẽ quản lý padding của riêng nó để tràn viền
                RecommendationGroupsSection(
                    groups = recommendationGroups,
                    onMovieClick = { recommendMovie ->
                        onPlayMovie(MovieUi(
                            title = recommendMovie.title,
                            subtitle = recommendMovie.originName,
                            description = recommendMovie.description,
                            rating = recommendMovie.rating,
                            age = recommendMovie.age,
                            year = recommendMovie.year,
                            season = "",
                            episode = recommendMovie.episode,
                            posterUrl = recommendMovie.posterUrl,
                            slug = recommendMovie.slug
                        ))
                    },
                    onMovieLongClick = {
                        previewMovie = it
                    },
                    onSeeMore = onSeeMore
                )
                Spacer(modifier = Modifier.height(22.dp))
                Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                    InterestSection()
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Sticky Top Header - nằm trên cùng, không bị cuộn
            TopHeader(
                collapseFraction = collapseFraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .zIndex(10f)
            )

            // Movie Preview Dialog Overlays
            if (previewMovie != null) {
                MoviePreviewDialog(
                    movie = previewMovie!!,
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
                        onPlayMovie(MovieUi(
                            title = m.title, subtitle = m.originName, description = m.description,
                            rating = m.rating, age = m.age, year = m.year, season = "", episode = m.episode,
                            posterUrl = m.posterUrl, slug = m.slug
                        ))
                    }
                )
            }
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
                                    Color(0xFF070B16).copy(alpha = 0.3f),
                                    Color(0xFF070B16).copy(alpha = 0.6f),
                                    Color(0xFF070B16).copy(alpha = 0.85f),
                                    Color(0xFF070B16)
                                )
                            )
                        )
                )
            }
            // Ảnh poster mờ phía sau
            if (posterUrl.isNotEmpty()) {
                AsyncImage(
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
                                    Color(0xFF1A2237),
                                    Color(0xFF0B1120)
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
                                Color(0xFF070B16).copy(alpha = 0.3f),
                                Color(0xFF070B16).copy(alpha = 0.6f),
                                Color(0xFF070B16).copy(alpha = 0.85f),
                                Color(0xFF070B16)
                            )
                        )
                    )
            )
        }
    }

    @Composable
    fun TopHeader(
        collapseFraction: Float,
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
                    Color(0xFF1A1D2B).copy(alpha = bgAlpha * 0.95f)
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
                        painter = painterResource(id = R.drawable.logo_app),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(logoSize)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
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

                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsNone,
                        contentDescription = null,
                        tint = Color.White
                    )
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
        onChipSelected: (String) -> Unit
    ) {
        val categories = listOf("Đề xuất", "Phim bộ", "Phim lẻ", "Thể loại")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { category ->
                if (selectedChip == category) {
                    FilledChip(text = category, selected = true, modifier = Modifier.weight(1f).clickable { onChipSelected(category) })
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
                        if (isCenter) Color(0xFF20253A) else Color(0xFF1B2031)
                    )
                    .clickable { onMovieClick(movie) },
                contentAlignment = Alignment.Center
            ) {
                // Hiển thị poster từ URL
                if (movie.posterUrl.isNotEmpty()) {
                    AsyncImage(
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
        onPlayClick: () -> Unit = {}
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
                    onClick = onPlayClick,
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
        val metaItems = listOf(
            "IMDb  ${movie.rating}" to true,
            movie.age to false,
            movie.year to false,
            movie.season to false,
            movie.episode to false
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
        // Map group title → (FilterKind, slug) for the "See more" action
        val (seeMoreKind, seeMoreSlug) = when (group.title) {
            "Phim bộ mới"    -> FilterKind.MOVIE_TYPE to "series"
            "Phim lẻ hot"   -> FilterKind.MOVIE_TYPE to "single"
            "Phim hoạt hình" -> FilterKind.MOVIE_TYPE to "hoathinh"
            else             -> FilterKind.GENRE      to group.title.lowercase().replace(" ", "-")
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
                                Color(0xFF2A3354),
                                Color(0xFF131A2F)
                            )
                        )
                    )
            ) {
                // Poster image
                if (movie.posterUrl.isNotEmpty()) {
                    AsyncImage(
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

                if (movie.quality.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF5C6273).copy(alpha = 0.95f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = movie.quality,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                if (movie.age.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (movie.age == "P" || movie.age == "G") Color(0xFF4CAF50).copy(alpha = 0.9f) else Color(0xFFE53935).copy(alpha = 0.9f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = movie.age,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
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
    fun InterestSection() {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ban dang quan tam gi?",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = ">",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SuggestCard("Tinh cam")
                SuggestCard("Tam ly")
            }
        }
    }

    @Composable
    fun SuggestCard(title: String) {
        Box(
            modifier = Modifier
                .width(170.dp)
                .height(110.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF5B6CFF),
                            Color(0xFFD75FA7)
                        )
                    )
                )
                .padding(16.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
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
            containerColor = Color(0xFF1A1D2B).copy(alpha = 0.95f),
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
                    .background(Color(0xFF212534))
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
                        .background(Color(0xFF282C3D)),
                    contentAlignment = Alignment.Center
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
                                    .background(Color(0xFF383C4D), RoundedCornerShape(4.dp))
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

