@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.alphacinema.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.alphacinema.R

// ── UI data class ────────────────────────────────────────────────────────────
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
@Composable
fun SearchScreen(
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    onOpenMovieDetail: (String) -> Unit = {},
    viewModel: SearchViewModel = viewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.initHistory(context)
    }

    val searchResults by viewModel.searchResults.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val isLoading    by viewModel.isLoading.collectAsState()
    val isLoadMore   by viewModel.isLoadMore.collectAsState()
    val appliedFilters by viewModel.appliedFilters.collectAsState()
    val tempFilters by viewModel.tempFilters.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }

    val gridState = rememberLazyGridState()
    val focusManager = LocalFocusManager.current

    val isFiltered     = appliedFilters.isAnyFilterApplied()
    val isSearchActive = searchQuery.trim().isNotEmpty() || isFiltered

    // Logic của pagination
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

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF070B16))) {
        // Top gradient decoration
        Box(modifier = Modifier.fillMaxWidth().height(300.dp).background(
            Brush.verticalGradient(listOf(Color(0xFF1A2240).copy(alpha = 0.6f), Color(0xFF070B16)))
        ))

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
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
                    onSearchSubmit = {
                        viewModel.onPerformSearch(searchQuery)
                        focusManager.clearFocus()
                    },
                    isFiltered = isFiltered,
                    onFilterClick = { 
                        viewModel.resetTempFilters()
                        showFilterSheet = true 
                    },
                    showBackButton = showBackButton,
                    onBackClick = onBackClick,
                    applyStatusBarPadding = true
                )
            }

            if (!isSearchActive) {
                // Hiển thị search history
                if (searchHistory.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                            Icon(Icons.Outlined.History, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                            Text(" Lịch sử tìm kiếm", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                            Spacer(Modifier.weight(1f))
                            Text("Xóa tất cả", color = Color(0xFFF6E29A), modifier = Modifier.clickable { viewModel.clearAllHistory() }, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            searchHistory.forEach { text ->
                                HistoryChip(text = text, 
                                    onClick = { 
                                        searchQuery = text
                                        viewModel.onSearchQueryChanged(text) 
                                    },
                                    onDelete = { viewModel.removeHistoryItem(text) }
                                )
                            }
                        }
                    }
                }
                
                // Luôn hiện khi ko có từ khoá tìm kiếm
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SearchMessageDisplay("Hãy nhập từ khoá tìm kiếm")
                }
                
            } else {
                // Hiển thị search result
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(if (isLoading) "Đang tìm kiếm..." else "Kết quả tìm kiếm", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                }

                if (isLoading) {
                    items(9) { SearchMovieCardSkeleton() }
                } else {
                    items(searchResults) { movie ->
                        SearchMovieCard(movie = movie, onClick = { 
                            viewModel.onPerformSearch(searchQuery)
                            onOpenMovieDetail(movie.movieId) 
                        })
                    }
                }
                if (isLoadMore) items(3) { SearchMovieCardSkeleton() }

                // --- HIỂN THỊ THÔNG BÁO KHI KHÔNG CÓ KẾT QUẢ ---
                if (!isLoading && searchResults.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        val msg = if (searchQuery.trim().isEmpty()) "Hãy nhập từ khoá tìm kiếm" else "Không tìm thấy kết quả"
                        SearchMessageDisplay(msg)
                    }
                }
            }
        }
    }

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
@Composable
fun SearchMessageDisplay(message: String) {
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
            text = message,
            color = Color.White.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FilterBottomSheet(
    tempFilters: SelectedFilterState,
    onUpdateCountry: (FilterOption?) -> Unit,
    onUpdateCategory: (FilterOption?) -> Unit,
    onUpdateVersion: (FilterOption?) -> Unit,
    onUpdateYear: (FilterOption?) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var expandedSection by remember { mutableStateOf<String?>(null) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1B1E2E),
        scrimColor = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) }
    ) {
        Box(modifier = Modifier.fillMaxHeight(0.85f)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Outlined.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Bộ lọc", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                }
                Text("Chọn loại phim hoặc thể loại", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 24.dp))
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    ExpandableFilterRow(
                        title = "Khu vực:",
                        selectedLabel = tempFilters.country?.label ?: "Tất cả",
                        isExpanded = expandedSection == "country",
                        onToggle = { expandedSection = if (expandedSection == "country") null else "country" }
                    ) {
                        FilterOptionChips(COUNTRY_OPTIONS, tempFilters.country, onUpdateCountry)
                    }

                    ExpandableFilterRow(
                        title = "Loại phim:",
                        selectedLabel = tempFilters.sortLang?.label ?: "Tất cả",
                        isExpanded = expandedSection == "type",
                        onToggle = { expandedSection = if (expandedSection == "type") null else "type" }
                    ) {
                        FilterOptionChips(VERSION_OPTIONS, tempFilters.sortLang, onUpdateVersion)
                    }

                    ExpandableFilterRow(
                        title = "Thể loại:",
                        selectedLabel = tempFilters.category?.label ?: "Tất cả",
                        isExpanded = expandedSection == "category",
                        onToggle = { expandedSection = if (expandedSection == "category") null else "category" }
                    ) {
                        FilterOptionChips(CATEGORY_OPTIONS, tempFilters.category, onUpdateCategory)
                    }

                    ExpandableFilterRow(
                        title = "Năm sản xuất:",
                        selectedLabel = tempFilters.year?.label ?: "Tất cả",
                        isExpanded = expandedSection == "year",
                        onToggle = { expandedSection = if (expandedSection == "year") null else "year" }
                    ) {
                        FilterOptionChips(YEAR_OPTIONS, tempFilters.year, onUpdateYear)
                    }
                    
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xFF1B1E2E))
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
            ) {
                Button(
                    onClick = onApply,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Text("Lọc kết quả", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun ExpandableFilterRow(
    title: String,
    selectedLabel: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val arrowRotation by animateFloatAsState(if (isExpanded) 180f else 0f)
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onToggle)
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(selectedLabel, color = if (selectedLabel == "Tất cả") Color.White.copy(alpha = 0.5f) else Color(0xFFF6E29A), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp).rotate(arrowRotation))
        }
        AnimatedVisibility(visible = isExpanded) {
            Box(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)) { content() }
        }
    }
}

@Composable
fun FilterOptionChips(options: List<FilterOption>, selected: FilterOption?, onClick: (FilterOption?) -> Unit) {
    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Color(0xFFF6E29A).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                    .border(1.dp, if (isSelected) Color(0xFFF6E29A) else Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .clickable { onClick(option) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(option.label, color = if (isSelected) Color(0xFFF6E29A) else Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.bodySmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
fun SearchHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit = {},
    isFiltered: Boolean = false,
    onFilterClick: () -> Unit = {},
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    applyStatusBarPadding: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().then(if (applyStatusBarPadding) Modifier.statusBarsPadding() else Modifier).height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (showBackButton) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1F2E))
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Trở lại", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            }
        }
        Row(
            modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(26.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF1A1F2E), Color(0xFF1E2438))))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(26.dp)).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Search, null, tint = Color(0xFFF6E29A).copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) Text("Tìm phim...", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.bodyMedium)
                    innerTextField()
                },
                singleLine = true
            )
            if (searchQuery.isNotEmpty()) {
                Icon(Icons.Outlined.Close, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp).clickable { onSearchQueryChange("") })
            }
        }
        Box(
            modifier = Modifier.size(52.dp).clip(CircleShape).background(if (isFiltered) Color(0xFFF6E29A) else Color(0xFF1A1F2E)).clickable { onFilterClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Tune, null, tint = if (isFiltered) Color.Black else Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun HistoryChip(text: String, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.1f))
            .clickable { onClick() }.padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
        Icon(Icons.Outlined.Close, null, tint = Color.White.copy(alpha = 0.4f), 
            modifier = Modifier.size(16.dp).padding(start = 4.dp).clickable { onDelete() })
    }
}

@Composable
fun SearchMovieCard(movie: SearchMovieUi, onClick: () -> Unit = {}) {
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.68f).clip(RoundedCornerShape(18.dp)).background(Color(0xFF131A2F)).clickable(onClick = onClick)) {
        AsyncImage(model = movie.posterUrl, contentDescription = movie.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.logo_app), error = painterResource(id = R.drawable.logo_app))
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.85f)), startY = 100f)))
        Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.25f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text(movie.badge, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 9.sp)
            }
            Text(movie.title, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(movie.subtitle, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun SearchMovieCardSkeleton() {
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.68f).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.05f))) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.05f), Color.Transparent))))
    }
}
