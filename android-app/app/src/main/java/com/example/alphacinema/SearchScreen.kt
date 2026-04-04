@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

// ── UI data class (kept here so AppScreen / other screens can still reference it) ──

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

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onOpenMovieDetail: (String) -> Unit = {},
    viewModel: SearchViewModel = viewModel()
) {
    val trendingMovies = MovieDetailFakeData.searchMovies

    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading    by viewModel.isLoading.collectAsState()
    val isLoadMore   by viewModel.isLoadMore.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }

    val gridState = rememberLazyGridState()

    val isFiltered     = selectedFilter.kind != FilterKind.ALL
    val isSearchActive = searchQuery.length >= 2 || isFiltered

    // Load more when reaching the bottom
    val shouldLoadMore = remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val last = (info.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
            last > 0 && last >= info.totalItemsCount
        }
    }
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && isSearchActive) viewModel.loadMore()
    }

    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPad = navBarHeight + 104.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B16))
    ) {
        // Top gradient decoration
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1A2240).copy(alpha = 0.6f), Color(0xFF070B16))
                    )
                )
        )

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottomPad
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            // ── Search bar ────────────────────────────────────────────────────
            item(span = { GridItemSpan(maxLineSpan) }) {
                SearchHeader(
                    searchQuery = searchQuery,
                    onSearchQueryChange = {
                        searchQuery = it
                        viewModel.onSearchQueryChanged(it)
                    },
                    isFiltered = isFiltered,
                    filterLabel = if (isFiltered) selectedFilter.label else null,
                    onFilterClick = { showFilterSheet = true },
                    applyStatusBarPadding = true
                )
            }

            // ── Active filter badge ───────────────────────────────────────────
            if (isFiltered) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Đang lọc:",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelMedium
                        )
                        // Active filter chip with ✕
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFF6E29A))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { viewModel.setFilter(SEARCH_FILTERS[0]) }
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = selectedFilter.label,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Bỏ lọc",
                                tint = Color.Black.copy(alpha = 0.7f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        // Hint when type filter + keyword (API limitation)
                        if (selectedFilter.kind == FilterKind.MOVIE_TYPE && searchQuery.length >= 2) {
                            Text(
                                text = "* Tìm kiếm không lọc được theo loại phim",
                                color = Color.White.copy(alpha = 0.35f),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ── Section title ─────────────────────────────────────────────────
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
                        text = when {
                            isLoading -> "Đang tải..."
                            isSearchActive -> "Kết quả tìm kiếm"
                            else -> "Được tìm kiếm nhiều"
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (!isSearchActive) {
                        Icon(
                            imageVector = Icons.Outlined.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFFF6E29A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ── Content ───────────────────────────────────────────────────────
            when {
                isLoading -> items(9) { SearchMovieCardSkeleton() }
                !isSearchActive -> items(trendingMovies) { movie ->
                    SearchMovieCard(movie = movie, onClick = { onOpenMovieDetail(movie.movieId) })
                }
                else -> items(searchResults) { movie ->
                    SearchMovieCard(movie = movie, onClick = { onOpenMovieDetail(movie.movieId) })
                }
            }

            if (isLoadMore) items(3) { SearchMovieCardSkeleton() }

            // Empty state
            if (!isLoading && isSearchActive && searchResults.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
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
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Thử từ khóa khác hoặc bỏ bộ lọc",
                            color = Color.White.copy(alpha = 0.3f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // ── Filter bottom sheet ───────────────────────────────────────────────────
    if (showFilterSheet) {
        FilterBottomSheet(
            filters = SEARCH_FILTERS,
            selected = selectedFilter,
            onSelect = { filter ->
                viewModel.setFilter(filter)
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }
}

// ── Search header ─────────────────────────────────────────────────────────────

@Composable
fun SearchHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isFiltered: Boolean = false,
    filterLabel: String? = null,
    onFilterClick: () -> Unit = {},
    applyStatusBarPadding: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (applyStatusBarPadding) Modifier.statusBarsPadding() else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search field
        Row(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF1A1F2E), Color(0xFF1E2438))
                    )
                )
                .border(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color(0xFFF6E29A).copy(alpha = 0.1f)
                        )
                    ),
                    RoundedCornerShape(26.dp)
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
                modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    fontSize = 15.sp
                ),
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Tìm kiếm phim, diễn viên...",
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
            }
        }

        // Filter button – yellow filled when a filter is active
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (isFiltered) Color(0xFFF6E29A) else Color(0xFF1A1F2E))
                .border(
                    1.dp,
                    if (isFiltered) Color.Transparent else Color.White.copy(alpha = 0.15f),
                    CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onFilterClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Tune,
                contentDescription = "Bộ lọc",
                tint = if (isFiltered) Color.Black else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ── Filter Bottom Sheet ───────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    filters: List<SearchFilter>,
    selected: SearchFilter,
    onSelect: (SearchFilter) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val typeFilters  = filters.filter { it.kind == FilterKind.ALL || it.kind == FilterKind.MOVIE_TYPE }
    val genreFilters = filters.filter { it.kind == FilterKind.GENRE }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF10192E),
        scrimColor = Color.Black.copy(alpha = 0.55f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 28.dp)
        ) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bộ lọc",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Chọn loại phim hoặc thể loại",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                if (selected.kind != FilterKind.ALL) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelect(SEARCH_FILTERS[0]) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Đặt lại",
                            color = Color(0xFFF6E29A),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Loại phim section ─────────────────────────────────────────────
            Text(
                text = "Loại phim",
                color = Color.White.copy(alpha = 0.45f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                typeFilters.forEach { filter ->
                    FilterChip(filter = filter, selected = selected, onSelect = onSelect)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Thể loại / genre section ──────────────────────────────────────
            Text(
                text = "Thể loại",
                color = Color.White.copy(alpha = 0.45f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                genreFilters.forEach { filter ->
                    FilterChip(filter = filter, selected = selected, onSelect = onSelect)
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    filter: SearchFilter,
    selected: SearchFilter,
    onSelect: (SearchFilter) -> Unit
) {
    val isSelected = filter == selected
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFF6E29A) else Color(0xFF1A2237),
        animationSpec = tween(200), label = "chipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.Black else Color.White.copy(alpha = 0.80f),
        animationSpec = tween(200), label = "chipText"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(bgColor)
            .then(
                if (!isSelected) Modifier.border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onSelect(filter) }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(13.dp)
            )
        }
        Text(
            text = filter.label,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            style = MaterialTheme.typography.labelLarge,
            fontSize = 13.sp
        )
    }
}

// ── Movie card ────────────────────────────────────────────────────────────────

@Composable
fun SearchMovieCard(movie: SearchMovieUi, onClick: () -> Unit = {}) {
    Box(
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
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
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

// ── Skeleton ──────────────────────────────────────────────────────────────────

@Composable
fun SearchMovieCardSkeleton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.05f), Color.Transparent)
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
