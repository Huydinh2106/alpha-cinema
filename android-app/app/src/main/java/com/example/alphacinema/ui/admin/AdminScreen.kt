package com.example.alphacinema.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alphacinema.data.model.HomeCategory
import com.example.alphacinema.data.model.MovieItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit,
    viewModel: AdminViewModel
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val addStatus by viewModel.addMovieStatus.collectAsState()
    val catStatus by viewModel.categoryStatus.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(addStatus) {
        addStatus?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatus()
        }
    }
    
    LaunchedEffect(catStatus) {
        catStatus?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearCategoryStatus()
        }
    }

    // Load categories when entering the category tab
    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 1) {
            viewModel.fetchCategories()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản trị hệ thống", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = Color(0xFFF6E29A),
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color(0xFFF6E29A)
                        )
                    }
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Thêm Phim") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Quản lý Danh mục") }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (selectedTabIndex == 0) {
                    AddMovieTab(viewModel)
                } else {
                    ManageCategoriesTab(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMovieTab(viewModel: AdminViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Tìm kiếm phim từ PhimAPI...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.Gray) },
            trailingIcon = {
                Button(
                    onClick = { viewModel.searchMovies(searchQuery) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF6E29A))
                ) {
                    Text("Tìm", color = Color.Black)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFF6E29A),
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color(0xFFF6E29A)
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        when (uiState) {
            is AdminUiState.Idle -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nhập tên phim để tìm kiếm", color = Color.Gray)
                }
            }
            is AdminUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    com.example.alphacinema.ui.components.LottieLoadingIndicator(size = 120.dp)
                }
            }
            is AdminUiState.Success -> {
                val movies = (uiState as AdminUiState.Success).movies
                if (movies.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Không tìm thấy kết quả nào", color = Color.Gray)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(movies) { movie ->
                            AdminMovieItemCard(movie = movie, onAdd = { viewModel.addMovieToDatabase(movie) })
                        }
                    }
                }
            }
            is AdminUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Lỗi: ${(uiState as AdminUiState.Error).message}", color = Color.Red)
                }
            }
        }
    }
}

@Composable
fun ManageCategoriesTab(viewModel: AdminViewModel) {
    val categories by viewModel.categories.collectAsState()
    var editingCategory by remember { mutableStateOf<HomeCategory?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(categories) { cat ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            editingCategory = cat
                            showDialog = true
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(cat.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("ID: ${cat.id}", color = Color(0xFFF6E29A), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (cat.movieSlugs.isEmpty()) "Chưa có phim nào" else "${cat.movieSlugs.size} phim: ${cat.movieSlugs.joinToString(", ")}",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingCategory = HomeCategory() // New empty category
                showDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = Color(0xFFF6E29A)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Thêm Danh Mục", tint = Color.Black)
        }
    }

    if (showDialog) {
        CategoryEditorDialog(
            viewModel = viewModel,
            category = editingCategory,
            onDismiss = { showDialog = false },
            onSave = { updatedCategory ->
                viewModel.saveCategory(updatedCategory)
                showDialog = false
            },
            onDelete = { categoryId ->
                viewModel.deleteCategory(categoryId)
                showDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditorDialog(
    viewModel: AdminViewModel,
    category: HomeCategory?,
    onDismiss: () -> Unit,
    onSave: (HomeCategory) -> Unit,
    onDelete: (String) -> Unit
) {
    if (category == null) return

    val isNew = category.id.isEmpty()
    var id by remember { mutableStateOf(category.id) }
    var title by remember { mutableStateOf(category.title) }
    var slugsText by remember { mutableStateOf(category.movieSlugs.joinToString(", ")) }
    var searchQuery by remember { mutableStateOf("") }
    val searchState by viewModel.categorySearchState.collectAsState()

    LaunchedEffect(searchQuery) {
        kotlinx.coroutines.delay(500)
        viewModel.searchMoviesForCategory(searchQuery)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141414),
        titleContentColor = Color.White,
        title = { Text(if (isNew) "Thêm Danh Mục" else "Sửa Danh Mục") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = id,
                    onValueChange = { id = it },
                    label = { Text("ID (VD: phim-bo-moi)", color = Color.Gray) },
                    enabled = isNew, // Only allow changing ID if creating new
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF6E29A), unfocusedBorderColor = Color.Gray,
                        disabledTextColor = Color.Gray, disabledBorderColor = Color.DarkGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tên hiển thị", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF6E29A), unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Tìm kiếm thêm phim từ PhimAPI...", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF6E29A), unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                when (val state = searchState) {
                    is AdminUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = Color(0xFFF6E29A))
                    }
                    is AdminUiState.Success -> {
                        val items = state.movies
                        if (items.isNotEmpty()) {
                            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                                items(items) { movie ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.addMovieToCategory(movie) { slug ->
                                                    val currentSlugs = slugsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                                                    if (!currentSlugs.contains(slug)) {
                                                        currentSlugs.add(slug)
                                                        slugsText = currentSlugs.joinToString(", ")
                                                    }
                                                    searchQuery = ""
                                                    viewModel.clearCategorySearch()
                                                }
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        com.example.alphacinema.ui.components.AlphaCinemaImage(
                                            model = movie.getFullPosterUrl(),
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(movie.name, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                    is AdminUiState.Error -> {
                        Text(state.message, color = Color.Red, fontSize = 12.sp)
                    }
                    else -> {}
                }

                OutlinedTextField(
                    value = slugsText,
                    onValueChange = { slugsText = it },
                    label = { Text("Danh sách Slug phim", color = Color.Gray) },
                    placeholder = { Text("slug1, slug2, slug3", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF6E29A), unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5
                )
                Text(
                    text = "Nhập các slug phim cách nhau bởi dấu phẩy.",
                    color = Color.Gray, fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val slugsList = slugsText.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    onSave(HomeCategory(id = id, title = title, movieSlugs = slugsList))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF6E29A))
            ) {
                Text("Lưu", color = Color.Black)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isNew) {
                    IconButton(onClick = { onDelete(id) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Xóa", tint = Color.Red)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Hủy", color = Color.Gray)
                }
            }
        }
    )
}

@Composable
fun AdminMovieItemCard(movie: MovieItem, onAdd: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            com.example.alphacinema.ui.components.AlphaCinemaImage(
                model = movie.getFullPosterUrl(),
                contentDescription = null,
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = movie.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = movie.origin_name ?: "",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${movie.year} • ${movie.type ?: ""}",
                    color = Color(0xFFF6E29A),
                    fontSize = 12.sp
                )
                Text(
                    text = "Slug: ${movie.slug}",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onAdd,
                    modifier = Modifier.background(Color(0xFFF6E29A).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add", tint = Color(0xFFF6E29A))
                }
            }
        }
    }
}
