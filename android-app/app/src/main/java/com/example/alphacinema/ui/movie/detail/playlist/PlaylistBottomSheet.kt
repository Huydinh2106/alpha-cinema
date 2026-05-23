package com.example.alphacinema.ui.movie.detail.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alphacinema.data.local.Playlist
import com.example.alphacinema.data.local.PlaylistMovie
import com.example.alphacinema.ui.components.AlphaCinemaImage
import com.example.alphacinema.ui.movie.detail.MovieDetailUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistBottomSheet(
    movie: MovieDetailUi,
    onDismissRequest: () -> Unit,
    onOpenMovie: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: PlaylistViewModel = viewModel()
) {
    val playlists by viewModel.playlists.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var selectedPlaylistForDetail by remember { mutableStateOf<Playlist?>(null) }
    
    val playlistListState = rememberLazyListState()
    //Reset scroll state khi thay đổi playlist
    val movieInPlaylistState = remember(selectedPlaylistForDetail?.id) { LazyListState() }
    
    var showCreateDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF121212),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding() 
                .navigationBarsPadding()
        ) {
            if (selectedPlaylistForDetail == null) {
                //List of Playlists
                PlaylistListHeader(onClose = onDismissRequest)
                
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        state = playlistListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScrollbar(playlistListState),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(playlists) { playlist ->
                            val isMovieInPlaylist = playlist.movies.any { it.id == movie.id }
                            PlaylistItem(
                                playlist = playlist,
                                isMovieInPlaylist = isMovieInPlaylist,
                                onToggle = { viewModel.toggleMovieInPlaylist(playlist.id, movie) },
                                onClick = { selectedPlaylistForDetail = playlist }
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF121212),
                    shadowElevation = 24.dp,
                    tonalElevation = 8.dp
                ) {
                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF6E29A),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(24.dp))
                        Text("Danh sách phát mới", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    }
                }
            } else {
                //Playlist Details
                val currentPlaylist = playlists.find { it.id == selectedPlaylistForDetail?.id }
                if (currentPlaylist == null) {
                    selectedPlaylistForDetail = null
                } else {
                    PlaylistDetailHeader(
                        title = currentPlaylist.name,
                        onBack = { selectedPlaylistForDetail = null },
                        onDeletePlaylist = { viewModel.deletePlaylist(currentPlaylist.id) }
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        LazyColumn(
                            state = movieInPlaylistState,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScrollbar(movieInPlaylistState),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(currentPlaylist.movies) { playlistMovie ->
                                MovieItemInPlaylist(
                                    movie = playlistMovie,
                                    onRemove = { viewModel.removeMovieFromPlaylist(currentPlaylist.id, playlistMovie.id) },
                                    onClick = {
                                        onBack() // Call back before opening new movie
                                        onOpenMovie(playlistMovie.id)
                                        onDismissRequest()
                                    }
                                )
                            }
                            if (currentPlaylist.movies.isEmpty()) {
                                item {
                                    Box(Modifier.fillMaxSize().padding(top = 150.dp), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Rounded.VideoLibrary, contentDescription = null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(64.dp))
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text("Chưa có phim nào trong danh sách", color = Color.White.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                viewModel.createPlaylistWithMovie(name, movie)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun Modifier.verticalScrollbar(
    state: LazyListState,
    width: Dp = 8.dp
): Modifier {
    return drawWithContent {
        drawContent()

        val layoutInfo = state.layoutInfo
        val totalItemsCount = layoutInfo.totalItemsCount
        val visibleItemsInfo = layoutInfo.visibleItemsInfo

        if (totalItemsCount <= 0 || visibleItemsInfo.isEmpty()) return@drawWithContent

        val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
        if (viewportHeight <= 0) return@drawWithContent

        val firstItem = visibleItemsInfo.first()
        val lastItem = visibleItemsInfo.last()

        val isScrollable = totalItemsCount > visibleItemsInfo.size ||
                firstItem.offset < 0 ||
                lastItem.offset + lastItem.size > layoutInfo.viewportEndOffset

        if (isScrollable) {
            //Tối ưu thanh trược

            val averageItemHeight = visibleItemsInfo.map { it.size }.average().toFloat()
            val estimatedTotalHeight = averageItemHeight * totalItemsCount
            
            val visibleFraction = (viewportHeight / estimatedTotalHeight).coerceIn(0.1f, 0.9f)
            val scrollbarHeight = (visibleFraction * size.height).coerceAtLeast(40.dp.toPx())
            
            val firstItemOffsetFraction = if (firstItem.size > 0) -firstItem.offset.toFloat() / firstItem.size.toFloat() else 0f
            val scrollPosition = firstItem.index.toFloat() + firstItemOffsetFraction
            
            val itemsVisibleOnScreen = viewportHeight / averageItemHeight
            val maxScrollPosition = (totalItemsCount.toFloat() - itemsVisibleOnScreen).coerceAtLeast(0.1f)
            
            val progress = (scrollPosition / maxScrollPosition).coerceIn(0f, 1f)
            val scrollOffset = progress * (size.height - scrollbarHeight)

            val xOffset = size.width - width.toPx() - 4.dp.toPx()

            drawRoundRect(
                color = Color.White.copy(alpha = 0.05f),
                topLeft = Offset(xOffset, 0f),
                size = Size(width.toPx(), size.height),
                cornerRadius = CornerRadius(width.toPx() / 2, width.toPx() / 2)
            )

            drawRoundRect(
                color = Color(0xFFF6E29A),
                topLeft = Offset(xOffset, scrollOffset.coerceIn(0f, (size.height - scrollbarHeight).coerceAtLeast(0f))),
                size = Size(width.toPx(), scrollbarHeight),
                cornerRadius = CornerRadius(width.toPx() / 2, width.toPx() / 2)
            )
        }
    }
}

@Composable
private fun PlaylistListHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Lưu vào...",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PlaylistDetailHeader(
    title: String,
    onBack: () -> Unit,
    onDeletePlaylist: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại", tint = Color.White)
        }
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier
                .padding(end = 8.dp)
                .background(Color.Red.copy(alpha = 0.15f), CircleShape)
        ) {
            Icon(Icons.Rounded.Delete, contentDescription = "Xóa danh sách", tint = Color.Red, modifier = Modifier.size(22.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Color(0xFF1E1E1E),
            title = { Text("Xóa danh sách phát?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn xóa danh sách \"$title\" không?", color = Color.White.copy(alpha = 0.8f)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeletePlaylist()
                    showDeleteConfirm = false
                }) {
                    Text("Xóa", color = Color.Red, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Hủy", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun PlaylistItem(
    playlist: Playlist,
    isMovieInPlaylist: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (playlist.movies.isNotEmpty()) {
                AlphaCinemaImage(
                    model = playlist.movies.first().posterUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Rounded.VideoLibrary, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(32.dp))
            }
        }
        
        Spacer(modifier = Modifier.width(18.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${playlist.movies.size} phim",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        IconButton(
            onClick = onToggle,
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (isMovieInPlaylist) Color(0xFFF6E29A).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = if (isMovieInPlaylist) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                contentDescription = "Toggle",
                tint = if (isMovieInPlaylist) Color(0xFFF6E29A) else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun MovieItemInPlaylist(
    movie: PlaylistMovie,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    var showRemoveConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlphaCinemaImage(
            model = movie.posterUrl,
            contentDescription = movie.title,
            modifier = Modifier
                .width(72.dp)
                .height(100.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = movie.title,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        IconButton(
            onClick = { showRemoveConfirm = true },
            modifier = Modifier.background(Color.Red.copy(alpha = 0.15f), CircleShape)
        ) {
            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Gỡ khỏi danh sách", tint = Color.Red, modifier = Modifier.size(24.dp))
        }
    }

    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            containerColor = Color(0xFF1E1E1E),
            title = { Text("Gỡ khỏi danh sách?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có muốn gỡ phim \"${movie.title}\" khỏi danh sách này không?", color = Color.White.copy(alpha = 0.8f)) },
            confirmButton = {
                TextButton(onClick = {
                    onRemove()
                    showRemoveConfirm = false
                }) {
                    Text("Gỡ", color = Color.Red, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) {
                    Text("Hủy", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = { Text("Danh sách phát mới", color = Color.White, fontWeight = FontWeight.ExtraBold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tên danh sách", color = Color.White.copy(alpha = 0.6f)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFF6E29A),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedLabelColor = Color(0xFFF6E29A),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Tạo", color = Color(0xFFF6E29A), fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = Color.White)
            }
        }
    )
}
