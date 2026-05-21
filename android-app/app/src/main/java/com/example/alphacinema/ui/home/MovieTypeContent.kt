package com.example.alphacinema.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alphacinema.data.model.MovieItem
import com.example.alphacinema.ui.search.FilterBottomSheet
import com.example.alphacinema.ui.search.SearchMovieCardSkeleton
import kotlinx.coroutines.launch

private val AccentGold = Color(0xFFF6E29A)

@Composable
fun MovieTypeScreen(
    type: String,
    title: String,
    onBack: () -> Unit = {},
    onOpenMovieDetail: (String) -> Unit = {},
    viewModel: MovieTypeViewModel = viewModel(key = "movieType/$type")
) {
    val movies by viewModel.movies.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()
    val appliedFilters by viewModel.appliedFilters.collectAsState()
    val tempFilters by viewModel.tempFilters.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val isFiltered = appliedFilters.isAnyFilterApplied()
    val selectedGenreLabels = remember(type, title) {
        if (type.startsWith("the-loai/")) {
            title.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
        } else {
            emptyList()
        }
    }

    // Load movies on first entry
    LaunchedEffect(type) {
        viewModel.loadMovies(type)
    }

    // Scroll to top when page changes
    LaunchedEffect(currentPage) {
        gridState.animateScrollToItem(0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = navBarHeight + 24.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with back button and filter
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Back button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onBack
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Title
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )

                    // Filter button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFiltered) AccentGold else Color.White.copy(alpha = 0.08f)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    viewModel.resetTempFilters()
                                    showFilterSheet = true
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "Bộ lọc",
                            tint = if (isFiltered) Color.Black else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (selectedGenreLabels.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        selectedGenreLabels.forEach { label ->
                            SelectedGenreChip(label = label)
                        }
                    }
                }
            }

            // Loading skeleton
            if (isLoading && movies.isEmpty()) {
                items(12) { SearchMovieCardSkeleton() }
            } else if (isLoading) {
                // Show loading overlay for page transitions
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        com.example.alphacinema.ui.components.LottieLoadingIndicator(size = 80.dp)
                    }
                }
            } else {
                items(movies, key = { it.slug }) { movie ->
                    MovieTypeCard(
                        movie = movie,
                        onClick = { onOpenMovieDetail(movie.slug) }
                    )
                }
            }

            // Empty state
            if (!isLoading && movies.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        com.example.alphacinema.ui.components.LottieLoadingIndicator(size = 120.dp)
                    }
                }
            }

            // Pagination bar
            if (!isLoading && movies.isNotEmpty() && totalPages > 1) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PaginationBar(
                        currentPage = currentPage,
                        totalPages = totalPages,
                        onPageSelected = { page ->
                            coroutineScope.launch {
                                viewModel.goToPage(page)
                            }
                        },
                        onPrevious = {
                            coroutineScope.launch {
                                viewModel.prevPage()
                            }
                        },
                        onNext = {
                            coroutineScope.launch {
                                viewModel.nextPage()
                            }
                        }
                    )
                }
            }
        }
    }

    // Filter bottom sheet
    if (showFilterSheet) {
        FilterBottomSheet(
            tempFilters = tempFilters,
            onUpdateCountry = { viewModel.updateTempCountry(it) },
            onUpdateCategory = { viewModel.updateTempCategory(it) },
            onUpdateVersion = { viewModel.updateTempVersion(it) },
            onUpdateYear = { viewModel.updateTempYear(it) },
            onApply = {
                viewModel.applyFilters()
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }
}

// ── Pagination Bar ──────────────────────────────────────────────────────────────

@Composable
private fun PaginationBar(
    currentPage: Int,
    totalPages: Int,
    onPageSelected: (Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val pageNumbers = buildPageNumbers(currentPage, totalPages)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous button
        PaginationArrowButton(
            icon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
            enabled = currentPage > 1,
            onClick = onPrevious
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Page number buttons
        pageNumbers.forEach { pageNum ->
            if (pageNum == -1) {
                // Ellipsis
                Text(
                    text = "…",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            } else {
                PageNumberButton(
                    page = pageNum,
                    isSelected = pageNum == currentPage,
                    onClick = { onPageSelected(pageNum) }
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Next button
        PaginationArrowButton(
            icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            enabled = currentPage < totalPages,
            onClick = onNext
        )
    }
}

@Composable
private fun PaginationArrowButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (enabled) Color.White.copy(alpha = 0.08f)
                else Color.White.copy(alpha = 0.03f)
            )
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.2f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun PageNumberButton(
    page: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) AccentGold
                else Color.White.copy(alpha = 0.06f)
            )
            .then(
                if (isSelected) Modifier
                else Modifier.border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = page.toString(),
            color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Build page number list with ellipsis for large page counts.
 * Example: [1, 2, 3, -1, 50] or [1, -1, 5, 6, 7, -1, 50]
 * -1 represents ellipsis
 */
private fun buildPageNumbers(current: Int, total: Int): List<Int> {
    if (total <= 7) return (1..total).toList()

    val pages = mutableListOf<Int>()
    pages.add(1)

    if (current > 3) {
        pages.add(-1) // ellipsis
    }

    val rangeStart = maxOf(2, current - 1)
    val rangeEnd = minOf(total - 1, current + 1)
    for (i in rangeStart..rangeEnd) {
        pages.add(i)
    }

    if (current < total - 2) {
        pages.add(-1) // ellipsis
    }

    pages.add(total)
    return pages
}

// ── Movie Type Card ─────────────────────────────────────────────────────────────

@Composable
fun MovieTypeCard(movie: MovieItem, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF2F2F2F), Color(0xFF1A1A1A)))
                )
        ) {
            val posterUrl = movie.getFullPosterUrl()
            if (posterUrl.isNotEmpty()) {
                com.example.alphacinema.ui.components.AlphaCinemaImage(
                    model = posterUrl,
                    contentDescription = movie.name,
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

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.4f))
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = movie.name,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        movie.origin_name?.takeIf { it.isNotBlank() && it != movie.name }?.let { originName ->
            Text(
                text = originName,
                color = Color.White.copy(alpha = 0.45f),
                style = MaterialTheme.typography.labelMedium,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SelectedGenreChip(
    label: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
