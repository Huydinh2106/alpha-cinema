@file:OptIn(ExperimentalFoundationApi::class)
package com.example.alphacinema
    import androidx.compose.foundation.ExperimentalFoundationApi
    import androidx.compose.foundation.background
    import androidx.compose.foundation.border
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.interaction.collectIsPressedAsState
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
    import androidx.compose.material.icons.outlined.CalendarMonth
    import androidx.compose.material.icons.outlined.Home
    import androidx.compose.material.icons.outlined.NotificationsNone
    import androidx.compose.material.icons.outlined.PersonOutline
    import androidx.compose.material.icons.outlined.PlayArrow
    import androidx.compose.material.icons.outlined.Search
    import androidx.compose.material.icons.outlined.Settings
    import androidx.compose.material.icons.outlined.Info
    import androidx.compose.material3.*
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.blur
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.graphics.graphicsLayer
    import androidx.compose.ui.graphics.Brush
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.graphics.lerp
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.text.style.TextOverflow
    import androidx.compose.ui.tooling.preview.Preview
    import androidx.compose.ui.platform.LocalDensity
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp
    import com.example.alphacinema.ui.theme.AlphaCinemaTheme
    import kotlin.math.PI
    import kotlin.math.cos
    import kotlin.math.sin

    data class MovieUi(
        val title: String,
        val subtitle: String,
        val description: String,
        val rating: String,
        val age: String,
        val year: String,
        val season: String,
        val episode: String
    )

    data class RecommendMovieUi(
        val title: String,
        val genre: String,
        val rating: String
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
    fun HomeScreen() {
        val movies = listOf(
            MovieUi(
                title = "Thế Giới Không Lối Thoát",
                subtitle = "Alice in Borderland",
                description = "Một game thủ lông bông cùng hai người bạn nhận ra họ đã lọt vào thế giới Tokyo song song, nơi họ buộc phải tham gia những trò chơi sinh tồn cực kỳ nguy hiểm...",
                rating = "7.8",
                age = "T18",
                year = "2020",
                season = "Phần 3",
                episode = "Tập 6"
            ),
            MovieUi(
                title = "Kẻ Săn Tin Đen",
                subtitle = "Nightcrawler",
                description = "Một phóng viên tự do bước vào thế giới báo chí đêm tối đầy tham vọng, mưu mô và ám ảnh về thành công.",
                rating = "8.0",
                age = "T16",
                year = "2014",
                season = "Phần 1",
                episode = "Tập 1"
            ),
            MovieUi(
                title = "Mật Mã Sống",
                subtitle = "Source Code",
                description = "Một người lính thức dậy trong thân xác người khác và phải tìm ra hung thủ trước khi một vụ nổ khác xảy ra.",
                rating = "7.5",
                age = "T13",
                year = "2011",
                season = "Phần 1",
                episode = "Tập 2"
            )
        )

        val recommendationGroups = listOf(
            RecommendGroupUi(
                title = "Đề xuất cho bạn",
                movies = listOf(
                    RecommendMovieUi("Dark", "Tam ly", "8.7"),
                    RecommendMovieUi("The Platform", "Sinh ton", "7.0"),
                    RecommendMovieUi("Prison Break", "Hanh dong", "8.3"),
                    RecommendMovieUi("1899", "Bi an", "7.3")
                )
            ),
            RecommendGroupUi(
                title = "Top 10 bộ phim bán chạy nhất",
                movies = listOf(
                    RecommendMovieUi("Dune", "Vien tuong", "8.0"),
                    RecommendMovieUi("Joker", "Tam ly", "8.4"),
                    RecommendMovieUi("John Wick", "Hanh dong", "7.4"),
                    RecommendMovieUi("Interstellar", "Khoa hoc", "8.7")
                )
            ),
            RecommendGroupUi(
                title = "Top 10 bộ phim mới nhất",
                movies = listOf(
                    RecommendMovieUi("The Creator", "Vien tuong", "6.7"),
                    RecommendMovieUi("Rebel Moon", "Phieu luu", "5.6"),
                    RecommendMovieUi("Silo", "Bi an", "8.1"),
                    RecommendMovieUi("Shogun", "Lich su", "8.8")
                )
            )
        )

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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070B16))
        ) {
            BackgroundLayer()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 22.dp, bottom = 110.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                TopHeader()
                Spacer(modifier = Modifier.height(20.dp))
                CategoryChips()
                Spacer(modifier = Modifier.height(22.dp))
                HeroCarousel(pagerState)
                Spacer(modifier = Modifier.height(20.dp))
                MovieInfoSection(
                    movie = movies[currentMovieIndex],
                    currentMovieIndex = currentMovieIndex,
                    total = movies.size
                )
                Spacer(modifier = Modifier.height(26.dp))
                RecommendationGroupsSection(groups = recommendationGroups)
                Spacer(modifier = Modifier.height(22.dp))
                InterestSection()
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    @Composable
    fun BackgroundLayer() {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.30f),
                                Color(0xFF070B16).copy(alpha = 0.78f),
                                Color(0xFF070B16)
                            )
                        )
                    )
            )
        }
    }

    @Composable
    fun TopHeader() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFFFFD76A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "AlphaCinema",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Phim hay tẹt ga",
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodyMedium
                    )
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

    @Composable
    fun CategoryChips() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilledChip(text = "Đề xuất", selected = true, modifier = Modifier.weight(1f))
            OutlineChip(text = "Phim bộ", modifier = Modifier.weight(1f))
            OutlineChip(text = "Phim lẻ", modifier = Modifier.weight(1f))
            OutlineChip(text = "Thể loại", modifier = Modifier.weight(1f))

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
fun HeroCarousel(pagerState: PagerState) {
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
            pageSpacing = 0.dp, // Giữ ở mức 0 để tự chỉnh khoảng cách bằng toán học
            modifier = Modifier.fillMaxSize()
        ) { page ->

            // 1. Tính toán Offset (Vị trí tương đối của thẻ so với tâm)
            // Càng gần tâm thì tiến về 0. Trái là số dương, Phải là số âm.
            val pageOffset = (
                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    ).coerceIn(-1.5f, 1.5f)

            // 2. Chuyển đổi Offset sang Radian (1 thẻ = 90 độ = PI/2)
            val angle = pageOffset * (PI / 2f)

            val isCenter = kotlin.math.abs(pageOffset) < 0.3f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Khóa tiêu cự camera để hiệu ứng 3D chuẩn xác
                        cameraDistance = 12f * density

                        // --- ÁP DỤNG COSINE VÀ SINE CHO SỰ MƯỢT MÀ ---

                        // Scale bằng Cosine (Đỉnh vòm mượt)
                        // Giúp scale từ từ đạt 1.0 ở giữa tâm và giảm dần êm ái về 0.75 ở 2 bên
                        val scale = 0.75f + (0.25f * cos(angle).toFloat().coerceAtLeast(0f))
                        scaleX = scale
                        scaleY = scale

                        // Xoay trục Y bằng Sine (Đường cong chữ S)
                        // Xoay dần dần và tối đa đạt 45 độ khi ở hẳn thẻ bên cạnh
                        rotationY = sin(angle).toFloat() * 45f

                        // TranslationX bằng Sine
                        // Kéo các thẻ xích lại gần nhau theo quỹ đạo cong hoàn hảo
                        val maxOverlap = 20.dp.toPx()
                        translationX = sin(angle).toFloat() * maxOverlap

                        // Làm mờ nhẹ thẻ ở xa bằng Cosine
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
                    ),
                contentAlignment = Alignment.Center
            ) {
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

    @Composable
    fun MovieInfoSection(
        movie: MovieUi,
        currentMovieIndex: Int,
        total: Int
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {},
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF6E29A),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Xem Phim", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {},
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.92f),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Thông tin", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            FlowMetaRow(movie)
            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = movie.description,
                color = Color.White.copy(alpha = 0.92f),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

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

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            items(metaItems) { (text, highlight) ->
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
                .clip(RoundedCornerShape(10.dp))
                .border(
                    1.2.dp,
                    if (highlight) Color(0xFFFFD76A) else Color.White.copy(alpha = 0.65f),
                    RoundedCornerShape(10.dp)
                )
                .background(if (highlight) Color(0x22FFD76A) else Color.Transparent)
                .widthIn(min = 52.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = text,
                color = if (highlight) Color(0xFFFFE38E) else Color.White,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    @Composable
    fun RecommendationGroupsSection(groups: List<RecommendGroupUi>) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            groups.forEach { group ->
                RecommendationGroup(group = group)
            }
        }
    }

    @Composable
    fun RecommendationGroup(group: RecommendGroupUi) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = group.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Xem thêm >",
                    color = Color(0xFFF6E29A),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(group.movies) { movie ->
                    RecommendationMovieCard(movie = movie)
                }
            }
        }
    }

    @Composable
    fun RecommendationMovieCard(movie: RecommendMovieUi) {
        Box(
            modifier = Modifier
                .width(132.dp)
                .height(194.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(18.dp)
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2A3354),
                            Color(0xFF131A2F)
                        )
                    )
                )
                .padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.16f),
                modifier = Modifier
                    .size(52.dp)
                    .align(Alignment.Center)
            )

            Column(
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Text(
                    text = movie.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = movie.genre,
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "IMDb ${movie.rating}",
                    color = Color(0xFFFFE08A),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
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
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A0E18).copy(alpha = 0.86f),
                            Color(0xFF0A0E18).copy(alpha = 0.92f),
                            Color(0xFF0A0E18).copy(alpha = 0.97f)
                        )
                    )
                )
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.07f),
                                Color.White.copy(alpha = 0.02f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.TopCenter)
                    .background(Color.White.copy(alpha = 0.11f))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(75.dp)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomItem(
                    icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                    label = "Trang chủ",
                    selected = currentScreen == ScreenType.HOME,
                    onClick = { onNavigate(ScreenType.HOME) }
                )

                BottomItem(
                    icon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    label = "Tìm kiếm",
                    selected = currentScreen == ScreenType.SEARCH,
                    onClick = { onNavigate(ScreenType.SEARCH) }
                )

                BottomItem(
                    icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                    label = "Lịch chiếu",
                    selected = currentScreen == ScreenType.SCHEDULE,
                    onClick = { onNavigate(ScreenType.SCHEDULE) }
                )

                BottomItem(
                    icon = { Icon(Icons.Outlined.PersonOutline, contentDescription = null) },
                    label = "Tài khoản",
                    selected = currentScreen == ScreenType.ACCOUNT,
                    onClick = { onNavigate(ScreenType.ACCOUNT) }
                )
            }
        }
    }

    @Composable
    fun BottomItem(
        icon: @Composable () -> Unit,
        label: String,
        selected: Boolean,
        onClick: () -> Unit = {}
    ) {
        val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()

        val pressScale by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (isPressed) 0.82f else 1.0f,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = 0.5f,
                stiffness = 800f
            ),
            label = "pressScale"
        )
        val iconColor by androidx.compose.animation.animateColorAsState(
            targetValue = if (selected) Color(0xFFF6E29A) else Color.White.copy(alpha = 0.65f),
            animationSpec = androidx.compose.animation.core.tween(200),
            label = "iconColor"
        )
        val textColor by androidx.compose.animation.animateColorAsState(
            targetValue = if (selected) Color(0xFFF6E29A) else Color.White.copy(alpha = 0.65f),
            animationSpec = androidx.compose.animation.core.tween(200),
            label = "textColor"
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
        ) {
            CompositionLocalProvider(
                LocalContentColor provides iconColor
            ) {
                icon()
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp
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

