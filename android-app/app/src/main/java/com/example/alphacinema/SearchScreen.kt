@file:OptIn(ExperimentalFoundationApi::class)

package com.example.alphacinema

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

data class SearchMovieUi(
    val movieId: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val badgeColor: String,
    val rating: String = "",
    val year: String = "",
    val posterUrl: String = ""
)

@Composable
fun SearchScreen(
    onOpenMovieDetail: (String) -> Unit = {},
    viewModel: SearchViewModel = viewModel()
) {
    val trendingMovies = MovieDetailFakeData.searchMovies
    val categories = listOf("Tất cả", "Phim bộ", "Phim lẻ", "Anime", "TV Show")

    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Tất cả") }

    val isLoadMore by viewModel.isLoadMore.collectAsState()
    val gridState = rememberLazyGridState()

    val filteredMovies = if (searchQuery.isEmpty()) {
        trendingMovies
    } else {
        trendingMovies.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.subtitle.contains(searchQuery, ignoreCase = true)
        }
    }

    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

            //load khi thấy item cuối
            lastVisibleItemIndex > 0 && lastVisibleItemIndex >= totalItemsNumber
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            println("DEBUG: Reached bottom, calling loadMore(). Current results: ${searchResults.size}")
            viewModel.loadMore()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B16))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A2240).copy(alpha = 0.6f),
                            Color(0xFF070B16)
                        )
                    )
                )
        )

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 120.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SearchHeader(
                    searchQuery = searchQuery,
                    onSearchQueryChange = {
                        searchQuery = it
                        viewModel.onSearchQueryChanged(it)
                    }
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SearchCategoryChips(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFFF6B6B),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (searchQuery.isEmpty()) "Được tìm kiếm nhiều" else "Kết quả tìm kiếm",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (searchQuery.isEmpty()) {
                        Icon(
                            imageVector = Icons.Outlined.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFFF6E29A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (isLoading) {
                items(9) {
                    SearchMovieCardSkeleton()
                }
            } else if (searchQuery.isEmpty()) {
                items(trendingMovies) { movie ->
                    SearchMovieCard(
                        movie = movie,
                        onClick = { onOpenMovieDetail(movie.movieId) }
                    )
                }
            } else {
                items(searchResults) { movie ->
                    SearchMovieCard(
                        movie = movie,
                        onClick = { onOpenMovieDetail(movie.movieId) }
                    )
                }
            }

            if (isLoadMore) {
                items(3) { // hiển thị thêm 3 skeleton ở cuối
                    SearchMovieCardSkeleton()
                }
            }

            if (!isLoading && searchQuery.isNotEmpty() && searchResults.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Không tìm thấy kết quả",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Thử tìm kiếm với từ khóa khác",
                            color = Color.White.copy(alpha = 0.3f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF1A1F2E),
                        Color(0xFF1E2438)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color(0xFFF6E29A).copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(26.dp)
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = Color(0xFFF6E29A).copy(alpha = 0.7f),
            modifier = Modifier.size(22.dp)
        )

        BasicTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White,
                fontSize = 15.sp
            ),
            decorationBox = { innerTextField ->
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Tìm kiếm phim, diễn viên, thể loại...",
                        color = Color.White.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 15.sp
                    )
                }
                innerTextField()
            },
            singleLine = true
        )

        if (searchQuery.isNotEmpty()) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Xóa",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable { onSearchQueryChange("") }
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Tune,
                contentDescription = "Bộ lọc",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SearchCategoryChips(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            val isSelected = category == selectedCategory
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFFF6E29A) else Color.Transparent,
                animationSpec = tween(250),
                label = "chipBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.Black else Color.White.copy(alpha = 0.75f),
                animationSpec = tween(250),
                label = "chipText"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .then(
                        if (isSelected) Modifier.background(bgColor)
                        else Modifier.border(
                            1.dp,
                            Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(20.dp)
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onCategorySelected(category) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category,
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun SearchMovieCard(
    movie: SearchMovieUi,
    onClick: () -> Unit = {}
) {    Box(
    modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(0.68f)
        .clip(RoundedCornerShape(18.dp))
        .background(Color(0xFF131A2F))
        .clickable(onClick = onClick)
) {
    AsyncImage(
        model = movie.posterUrl,
        contentDescription = movie.title,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        placeholder = painterResource(id = R.drawable.logo_app),
        error = painterResource(id = R.drawable.logo_app)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.2f),
                        Color.Black.copy(alpha = 0.85f)
                    ),
                    startY = 100f
                )
            )
    )

    if (movie.rating.isNotEmpty() && movie.rating != "N/A") {
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                tint = Color(0xFFFFD76A),
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = movie.rating,
                color = Color(0xFFFFE08A),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }

    // 4. Thông tin Phim (Căn dưới)
    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Badge (Chất lượng/Số tập) - Dùng nền xám mờ đồng bộ
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.25f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = movie.badge,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = movie.title,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 15.sp
        )

        Text(
            text = movie.subtitle,
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
}

@Composable
fun SearchMovieCardSkeleton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f)) // Màu nền mờ
    ) {
        // Hiệu ứng loading đơn giản
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
    SearchScreen()
}
