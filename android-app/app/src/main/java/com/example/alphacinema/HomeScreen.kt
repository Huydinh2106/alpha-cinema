@file:OptIn(ExperimentalFoundationApi::class)
package com.example.alphacinema
    import androidx.compose.foundation.ExperimentalFoundationApi
    import androidx.compose.foundation.Image
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
    import androidx.compose.material.icons.outlined.HeadsetMic
    import androidx.compose.material.icons.outlined.Home
    import androidx.compose.material.icons.outlined.NotificationsNone
    import androidx.compose.material.icons.outlined.PersonOutline
    import androidx.compose.material.icons.outlined.PlayArrow
    import androidx.compose.material.icons.outlined.Search
    import androidx.compose.material.icons.outlined.Settings
    import androidx.compose.material.icons.outlined.Info
    import androidx.compose.material3.*
    import androidx.compose.animation.core.animateFloatAsState
    import androidx.compose.animation.core.tween
    import androidx.compose.runtime.*
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

    data class MovieUi(
        val title: String,
        val subtitle: String,
        val description: String,
        val rating: String,
        val age: String,
        val year: String,
        val season: String,
        val episode: String,
        val posterUrl: String = ""
    )

    data class RecommendMovieUi(
        val title: String,
        val genre: String,
        val rating: String,
        val posterUrl: String = ""
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
        onPlayMovie: (MovieUi) -> Unit = {}
    ) {
        val movies = listOf(
            MovieUi(
                title = "Thế Giới Không Lối Thoát",
                subtitle = "Alice in Borderland",
                description = "Một game thủ lông bông cùng hai người bạn nhận ra họ đã lọt vào thế giới Tokyo song song, nơi họ buộc phải tham gia những trò chơi sinh tồn cực kỳ nguy hiểm...",
                rating = "7.8",
                age = "T18",
                year = "2020",
                season = "Phần 3",
                episode = "Tập 6",
                posterUrl = "https://m.media-amazon.com/images/M/MV5BNGEyOGJiNmEtMmI1OC00MDI4LWIxNzctYzg2YjYyMTk3MTdiXkEyXkFqcGc@._V1_.jpg"
            ),
            MovieUi(
                title = "Dune: Hành Tinh Cát 2",
                subtitle = "Dune: Part Two",
                description = "Paul Atreides liên minh với người Fremen để báo thù những kẻ đã hủy diệt gia đình anh, đồng thời cố gắng ngăn chặn tương lai khủng khiếp mà chỉ mình anh nhìn thấy.",
                rating = "8.6",
                age = "T13",
                year = "2024",
                season = "",
                episode = "",
                posterUrl = "https://m.media-amazon.com/images/M/MV5BN2QyZGU4ZDctOWMzMy00NTc5LThlOGQtODhmNDI1NmY5YzAwXkEyXkFqcGdeQXVyMDM2NDM2MQ@@._V1_.jpg"
            ),
            MovieUi(
                title = "Joker",
                subtitle = "Joker",
                description = "Tại thành phố Gotham năm 1981, Arthur Fleck - một diễn viên hài thất bại - bị xã hội ruồng bỏ và dần dần trượt vào vực thẳm của điên loạn, biến thành tên tội phạm Joker.",
                rating = "8.4",
                age = "T18",
                year = "2019",
                season = "",
                episode = "",
                posterUrl = "https://m.media-amazon.com/images/M/MV5BNGVjNWI4ZGUtNzE0MS00YTJmLWE0ZDctN2ZiYTk2YmI3NTYyXkEyXkFqcGdeQXVyMTkxNjUyNQ@@._V1_.jpg"
            ),
            MovieUi(
                title = "Interstellar",
                subtitle = "Interstellar",
                description = "Khi Trái Đất đang dần trở nên không thể sinh sống, một nhóm phi hành gia được cử đi tìm kiếm hành tinh mới cho nhân loại qua một lỗ sâu bí ẩn.",
                rating = "8.7",
                age = "T13",
                year = "2014",
                season = "",
                episode = "",
                posterUrl = "https://m.media-amazon.com/images/M/MV5BYzdjMDAxZGItMjI2My00ODA1LTlkNzItOWFjMDU5ZDJlYWY3XkEyXkFqcGc@._V1_.jpg"
            ),
            MovieUi(
                title = "Squid Game",
                subtitle = "Squid Game",
                description = "Hàng trăm người chơi đang cần tiền chấp nhận lời mời kỳ lạ tham gia các trò chơi trẻ em. Phần thưởng hấp dẫn đang chờ, nhưng cái giá phải trả rất đắt.",
                rating = "8.0",
                age = "T18",
                year = "2021",
                season = "Phần 2",
                episode = "Tập 7",
                posterUrl = "https://m.media-amazon.com/images/M/MV5BYWE3MDVkN2EtNjQ5MS00ZDQ4LTliNzYtMjc2YWMzMDEwMTA3XkEyXkFqcGdeQXVyMTEzMTI1Mjk3._V1_.jpg"
            )
        )

        val recommendationGroups = listOf(
            RecommendGroupUi(
                title = "Đề xuất cho bạn",
                movies = listOf(
                    RecommendMovieUi("Dark", "Tâm lý", "8.7", "https://m.media-amazon.com/images/M/MV5BOTk2NzUyOTctZDdlMS00MDJlLTgzNTEtNzQzYjFhNzY0ZGFmXkEyXkFqcGdeQXVyMjg1NDcxNDE@._V1_.jpg"),
                    RecommendMovieUi("The Platform", "Sinh tồn", "7.0", "https://m.media-amazon.com/images/M/MV5BMjAzMjlhNGQtMjAzZC00ODIzLWE4YTAtMDkyYmFiZmE2YWYzXkEyXkFqcGc@._V1_.jpg"),
                    RecommendMovieUi("Prison Break", "Hành động", "8.3", "https://m.media-amazon.com/images/M/MV5BMTg3NTkwNzAxOF5BMl5BanBnXkFtZTcwMjM1NjI5MQ@@._V1_.jpg"),
                    RecommendMovieUi("1899", "Bí ẩn", "7.3", "https://m.media-amazon.com/images/M/MV5BYjdkODg1OGItNjc2Yi00YjRiLWI5ZjAtOWMwZjlmMDFiNjBlXkEyXkFqcGc@._V1_.jpg")
                )
            ),
            RecommendGroupUi(
                title = "Top 10 bộ phim bán chạy nhất",
                movies = listOf(
                    RecommendMovieUi("Dune: Part Two", "Viễn tưởng", "8.0", "https://m.media-amazon.com/images/M/MV5BN2QyZGU4ZDctOWMzMy00NTc5LThlOGQtODhmNDI1NmY5YzAwXkEyXkFqcGdeQXVyMDM2NDM2MQ@@._V1_.jpg"),
                    RecommendMovieUi("Joker", "Tâm lý", "8.4", "https://m.media-amazon.com/images/M/MV5BNGVjNWI4ZGUtNzE0MS00YTJmLWE0ZDctN2ZiYTk2YmI3NTYyXkEyXkFqcGdeQXVyMTkxNjUyNQ@@._V1_.jpg"),
                    RecommendMovieUi("John Wick 4", "Hành động", "7.4", "https://m.media-amazon.com/images/M/MV5BMDExZGMyOTMtMDgyYi00NGIwLWJhMTEtOTdkZGFjNmZiMTEwXkEyXkFqcGdeQXVyMjM4NTM5NDY@._V1_.jpg"),
                    RecommendMovieUi("Interstellar", "Khoa học", "8.7", "https://m.media-amazon.com/images/M/MV5BYzdjMDAxZGItMjI2My00ODA1LTlkNzItOWFjMDU5ZDJlYWY3XkEyXkFqcGc@._V1_.jpg")
                )
            ),
            RecommendGroupUi(
                title = "Top 10 bộ phim mới nhất",
                movies = listOf(
                    RecommendMovieUi("Oppenheimer", "Lịch sử", "8.3", "https://m.media-amazon.com/images/M/MV5BN2JkMDc5MGQtZjg3YS00NmFiLWIyZmQtZjZjMjc3MmJlZTQ1XkEyXkFqcGc@._V1_.jpg"),
                    RecommendMovieUi("Rebel Moon", "Phiêu lưu", "5.6", "https://m.media-amazon.com/images/M/MV5BNjMwOWYyYTAtNjE3OS00MzUyLThmNmMtYjI3NDdlNGIzMGI2XkEyXkFqcGc@._V1_.jpg"),
                    RecommendMovieUi("Silo", "Bí ẩn", "8.1", "https://m.media-amazon.com/images/M/MV5BMDI2NjQ1NjctZGI2MC00YzgyLTk3Y2ItMjkwMjNlODg3NTcwXkEyXkFqcGc@._V1_.jpg"),
                    RecommendMovieUi("Shogun", "Lịch sử", "8.8", "https://m.media-amazon.com/images/M/MV5BMWI2N2Q1MjgtMjYxYi00OGJmLTk5NzctMDk5YTljMmE2NjcxXkEyXkFqcGc@._V1_.jpg")
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

        val scrollState = rememberScrollState()
        // Tính collapse fraction: 0 = đầu trang (mở rộng), 1 = đã cuộn (thu gọn)
        val collapseFraction by remember {
            derivedStateOf {
                (scrollState.value / 200f).coerceIn(0f, 1f)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070B16))
        ) {
            BackgroundLayer(posterUrl = movies[currentMovieIndex].posterUrl)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
                    .padding(top = 90.dp, bottom = 110.dp)
            ) {
                CategoryChips()
                Spacer(modifier = Modifier.height(22.dp))
                HeroCarousel(pagerState, movies)
                Spacer(modifier = Modifier.height(20.dp))
                MovieInfoSection(
                    movie = movies[currentMovieIndex],
                    currentMovieIndex = currentMovieIndex,
                    total = movies.size,
                    onPlayClick = { onPlayMovie(movies[currentMovieIndex]) }
                )
                Spacer(modifier = Modifier.height(26.dp))
                RecommendationGroupsSection(groups = recommendationGroups)
                Spacer(modifier = Modifier.height(22.dp))
                InterestSection()
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
fun HeroCarousel(pagerState: PagerState, movies: List<MovieUi>) {
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
                    ),
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onPlayClick,
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
                    color = Color.White.copy(alpha = 0.10f),
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
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = movie.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = movie.genre,
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
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
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(40.dp))
                .background(
                    Color(0xFF1A1D2B).copy(alpha = 0.92f)
                )
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.12f),
                    RoundedCornerShape(40.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
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
                    icon = { Icon(Icons.Outlined.HeadsetMic, contentDescription = null) },
                    label = "Hỗ trợ",
                    selected = currentScreen == ScreenType.SUPPORT,
                    onClick = { onNavigate(ScreenType.SUPPORT) }
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
            targetValue = if (isPressed) 0.88f else 1.0f,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = 0.5f,
                stiffness = 800f
            ),
            label = "pressScale"
        )
        val iconColor by androidx.compose.animation.animateColorAsState(
            targetValue = if (selected) Color(0xFFF6E29A) else Color.White.copy(alpha = 0.55f),
            animationSpec = androidx.compose.animation.core.tween(200),
            label = "iconColor"
        )
        val textColor by androidx.compose.animation.animateColorAsState(
            targetValue = if (selected) Color.White else Color.White.copy(alpha = 0.55f),
            animationSpec = androidx.compose.animation.core.tween(200),
            label = "textColor"
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(36.dp))
                .then(
                    if (selected) Modifier.background(Color.White.copy(alpha = 0.12f))
                    else Modifier
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
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

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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


