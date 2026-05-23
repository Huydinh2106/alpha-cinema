@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.alphacinema.ui.movie.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.alphacinema.data.model.Comment
import com.example.alphacinema.data.model.MovieStats
import com.example.alphacinema.data.model.UserPlaylist
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.example.alphacinema.ui.components.GradientPlayButton

private const val PLAYLIST_NAME_MAX_LENGTH = 60

@Composable
fun MovieDetailScreen(
    movie: MovieDetailUi,
    isFavorite: Boolean,
    movieStats: MovieStats?,
    userRating: Int?,
    comments: List<Comment>,
    playlists: List<UserPlaylist> = emptyList(),
    playlistIdsForMovie: Set<String> = emptySet(),
    playlistActionInProgress: Boolean = false,
    currentUserId: String? = null,
    currentUserAvatarUrl: String = "",
    onToggleFavorite: (MovieDetailUi) -> Unit,
    onCreatePlaylist: (String, MovieDetailUi) -> Unit = { _, _ -> },
    onTogglePlaylistMovie: (String, Boolean, MovieDetailUi) -> Unit = { _, _, _ -> },
    onRenamePlaylist: (String, String) -> Unit = { _, _ -> },
    onDeletePlaylist: (String) -> Unit = {},
    onAddToListLoginRequired: () -> Unit = {},
    onPostComment: (String) -> Unit,
    onReplyComment: (Comment, String) -> Unit = { _, _ -> },
    onToggleCommentLike: (Comment) -> Unit = {},
    onSubmitRating: (Int) -> Unit,
    onBack: () -> Unit,
    onPlayMovie: (MovieDetailUi, EpisodeUi?) -> Unit,
    onOpenMovie: (String) -> Unit,
    onWatchTogether: () -> Unit = {}
) {
    val hasMultipleEpisodes = remember(movie.episodes) {
        movie.episodes.distinctBy { it.normalizedEpisodeKey() }.size > 1
    }
    val episodesForPicker = remember(movie.episodes, hasMultipleEpisodes) {
        if (hasMultipleEpisodes) {
            movie.episodes.distinctBy { it.normalizedEpisodeKey() }
        } else {
            emptyList()
        }
    }
    val showEpisodesTab = hasMultipleEpisodes || movie.relatedSeasons.isNotEmpty()
    val visibleTabs = remember(showEpisodesTab) {
        MovieDetailTab.entries.filter { tab ->
            tab != MovieDetailTab.EPISODES || showEpisodesTab
        }
    }
    var selectedTabName by rememberSaveable(movie.id) {
        mutableStateOf(if (showEpisodesTab) MovieDetailTab.EPISODES.name else MovieDetailTab.CAST.name)
    }
    var descriptionExpanded by rememberSaveable(movie.id) {
        mutableStateOf(false)
    }
    var showSaveToPlaylistSheet by rememberSaveable(movie.id) {
        mutableStateOf(false)
    }

    val selectedTab = visibleTabs.firstOrNull { it.name == selectedTabName }
        ?: visibleTabs.first()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var activeEpisodeId by rememberSaveable(movie.id) {
        val defaultEp = movie.episodes.firstOrNull { ep ->
            movie.currentEpisode.contains(ep.name.substringBefore(":"), ignoreCase = true)
        } ?: movie.episodes.firstOrNull()
        mutableStateOf(defaultEp?.id)
    }
    val activeEpisode = movie.episodes.find { it.id == activeEpisodeId }
    var commentDraft by rememberSaveable(movie.id) { mutableStateOf("") }
    var replyTarget by remember(movie.id) { mutableStateOf<Comment?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (selectedTab == MovieDetailTab.COMMENTS) 172.dp else 28.dp)
        ) {
            item {
                DetailTopBanner(
                    movie = movie,
                    onBack = onBack
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = movie.title,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = movie.subtitle,
                        color = Color.White.copy(alpha = 0.65f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    DetailMetaRow(movie = movie)
                    Spacer(modifier = Modifier.height(10.dp))
                    GenreRow(genres = movie.genres)
                    Spacer(modifier = Modifier.height(16.dp))
                    DescriptionBlock(
                        description = movie.description,
                        expanded = descriptionExpanded,
                        onToggle = { descriptionExpanded = !descriptionExpanded }
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    ButtonRow(
                        onPlay = { activeEpisode?.let { onPlayMovie(movie, it) } },
                        onEpisodes = if (showEpisodesTab) {
                            {
                                selectedTabName = MovieDetailTab.EPISODES.name
                                coroutineScope.launch {
                                    listState.animateScrollToItem(2)
                                }
                            }
                        } else null
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    ActionRow(
                        isFavorite = isFavorite,
                        isInPlaylist = playlistIdsForMovie.isNotEmpty(),
                        onActionClick = { action ->
                            when(action) {
                                MovieDetailAction.FAVORITE -> onToggleFavorite(movie)
                                MovieDetailAction.ADD_TO_LIST -> {
                                    if (currentUserId == null) {
                                        onAddToListLoginRequired()
                                    } else {
                                        showSaveToPlaylistSheet = true
                                    }
                                }
                                MovieDetailAction.WATCH_TOGETHER -> onWatchTogether()
                                else -> {}
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                }
            }

            item {
                TabRow(
                    selectedTabIndex = visibleTabs.indexOf(selectedTab).coerceAtLeast(0),
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    indicator = {}
                ) {
                    visibleTabs.forEach { tab ->
                        val selected = tab == selectedTab
                        Tab(
                            selected = selected,
                            onClick = { selectedTabName = tab.name },
                            text = {
                                Text(
                                    text = tab.title,
                                    color = if (selected) Color(0xFFF6E29A) else Color.White.copy(alpha = 0.65f),
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                when (selectedTab) {
                    MovieDetailTab.EPISODES -> if (showEpisodesTab) {
                        EpisodeTabModern(
                            episodes = episodesForPicker,
                            currentEpisode = activeEpisode,
                            relatedSeasons = movie.relatedSeasons,
                            currentMovieName = movie.title,
                            onSelectEpisode = { activeEpisodeId = it.id },
                            onOpenSeason = onOpenMovie
                        )
                    }

                    MovieDetailTab.CAST -> CastTab(cast = movie.cast)
                    MovieDetailTab.COMMENTS -> CommentsTab(
                        comments = comments,
                        currentUserId = currentUserId,
                        currentUserAvatarUrl = currentUserAvatarUrl,
                        onSelectReplyTarget = { replyTarget = it },
                        onToggleCommentLike = onToggleCommentLike
                    )
                    MovieDetailTab.RATINGS -> RatingsTab(
                        stats = movieStats,
                        userRating = userRating,
                        onSubmitRating = onSubmitRating
                    )
                }
            }
        }

        if (showSaveToPlaylistSheet) {
            SaveToPlaylistSheet(
                movie = movie,
                playlists = playlists,
                selectedPlaylistIds = playlistIdsForMovie,
                actionInProgress = playlistActionInProgress,
                onDismiss = { showSaveToPlaylistSheet = false },
                onTogglePlaylist = { playlist ->
                    onTogglePlaylistMovie(
                        playlist.id,
                        playlist.id in playlistIdsForMovie,
                        movie
                    )
                },
                onCreatePlaylist = { playlistName ->
                    onCreatePlaylist(playlistName, movie)
                },
                onRenamePlaylist = onRenamePlaylist,
                onDeletePlaylist = onDeletePlaylist
            )
        }

        if (selectedTab == MovieDetailTab.COMMENTS) {
            CommentInputBar(
                text = commentDraft,
                replyTarget = replyTarget,
                onTextChange = { commentDraft = it },
                onCancelReply = { replyTarget = null },
                onSend = {
                    val content = commentDraft.trim()
                    if (content.isBlank()) return@CommentInputBar
                    val target = replyTarget
                    if (target == null) {
                        onPostComment(content)
                    } else {
                        onReplyComment(target, content)
                    }
                    commentDraft = ""
                    replyTarget = null
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

private fun EpisodeUi.normalizedEpisodeKey(): String {
    return name
        .lowercase()
        .replace(Regex("(phần|mùa)\\s*\\d+\\s*(:|-)"), "")
        .replace(Regex("tập\\s*"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
        .ifBlank { id }
}

@Composable
private fun DetailTopBanner(
    movie: MovieDetailUi,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp)
    ) {
        com.example.alphacinema.ui.components.AlphaCinemaImage(
            model = movie.bannerUrl,
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black
                        )
                    )
                )
        )

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(start = 12.dp, top = 40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        com.example.alphacinema.ui.components.AlphaCinemaImage(
            model = movie.posterUrl,
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(126.dp)
                .height(176.dp)
                .padding(start = 20.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .align(Alignment.BottomStart)
        )
    }
}

@Composable
private fun DetailMetaRow(movie: MovieDetailUi) {
    val tags = listOf(movie.year, movie.ageRating, movie.currentEpisode)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.forEach { text ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = text,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun GenreRow(genres: List<String>) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        genres.forEach { genre ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x22F6E29A))
                    .border(1.dp, Color(0x66F6E29A), RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = genre,
                    color = Color(0xFFF6E29A),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun DescriptionBlock(
    description: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Text(
        text = description,
        color = Color.White.copy(alpha = 0.88f),
        style = MaterialTheme.typography.bodyLarge,
        lineHeight = 22.sp,
        maxLines = if (expanded) Int.MAX_VALUE else 3,
        overflow = TextOverflow.Ellipsis
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = if (expanded) "Thu gọn" else "Xem thêm",
        color = Color(0xFFF6E29A),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clickable(onClick = onToggle)
    )
}

@Composable
private fun ButtonRow(
    onPlay: () -> Unit,
    onEpisodes: (() -> Unit)?
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        GradientPlayButton(
            onClick = onPlay,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            label = "Xem phim",
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 14.dp)
        )

        if (onEpisodes != null) {
            OutlinedButton(
                onClick = onEpisodes,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Tập phim", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ActionRow(
    isFavorite: Boolean,
    isInPlaylist: Boolean,
    onActionClick: (MovieDetailAction) -> Unit
) {
    val actions = MovieDetailAction.entries
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        actions.forEach { action ->
            val isFavActive = action == MovieDetailAction.FAVORITE && isFavorite
            val isPlaylistActive = action == MovieDetailAction.ADD_TO_LIST && isInPlaylist
            val isActionActive = isFavActive || isPlaylistActive
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable { onActionClick(action) }
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                        .border(
                            1.dp,
                            if (isActionActive) Color(0xFFF6E29A) else Color.White.copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isFavActive -> Icons.Rounded.Favorite
                            isPlaylistActive -> Icons.Rounded.Bookmark
                            else -> actionIcon(action)
                        },
                        contentDescription = action.label,
                        tint = if (isActionActive) Color(0xFFF6E29A) else Color.White.copy(alpha = 0.85f)
                    )
                }
                Text(
                    text = action.label,
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun SaveToPlaylistSheet(
    movie: MovieDetailUi,
    playlists: List<UserPlaylist>,
    selectedPlaylistIds: Set<String>,
    actionInProgress: Boolean,
    onDismiss: () -> Unit,
    onTogglePlaylist: (UserPlaylist) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit
) {
    var showCreateInput by remember { mutableStateOf(playlists.isEmpty()) }
    var newPlaylistName by remember { mutableStateOf("") }
    var playlistToRename by remember { mutableStateOf<UserPlaylist?>(null) }
    var renamePlaylistName by remember { mutableStateOf("") }
    var playlistToDelete by remember { mutableStateOf<UserPlaylist?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF1E1E1E),
        scrimColor = Color.Black.copy(alpha = 0.62f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lưu vào...",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                if (actionInProgress) {
                    CircularProgressIndicator(
                        color = Color(0xFFF6E29A),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (playlists.isEmpty()) {
                Text(
                    text = "Bạn chưa có danh sách phát nào.",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                playlists.forEach { playlist ->
                    PlaylistSaveRow(
                        playlist = playlist,
                        isSelected = playlist.id in selectedPlaylistIds,
                        actionInProgress = actionInProgress,
                        onToggle = { onTogglePlaylist(playlist) },
                        onRename = {
                            playlistToRename = playlist
                            renamePlaylistName = playlist.name
                        },
                        onDelete = { playlistToDelete = playlist }
                    )
                }
            }

            if (showCreateInput) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it.take(PLAYLIST_NAME_MAX_LENGTH) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Tên danh sách phát") },
                        colors = playlistTextFieldColors()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showCreateInput = false
                                newPlaylistName = ""
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !actionInProgress
                        ) {
                            Text("Hủy")
                        }
                        Button(
                            onClick = {
                                onCreatePlaylist(newPlaylistName)
                                newPlaylistName = ""
                                showCreateInput = false
                            },
                            modifier = Modifier.weight(1f),
                            enabled = newPlaylistName.trim().isNotBlank() && !actionInProgress,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF6E29A),
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Tạo", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Button(
                    onClick = { showCreateInput = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !actionInProgress,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.12f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Danh sách phát mới", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    playlistToRename?.let { playlist ->
        AlertDialog(
            onDismissRequest = { playlistToRename = null },
            containerColor = Color(0xFF1E1E1E),
            title = { Text("Đổi tên danh sách", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = renamePlaylistName,
                    onValueChange = { renamePlaylistName = it.take(PLAYLIST_NAME_MAX_LENGTH) },
                    singleLine = true,
                    label = { Text("Tên danh sách phát") },
                    colors = playlistTextFieldColors()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRenamePlaylist(playlist.id, renamePlaylistName)
                        playlistToRename = null
                    },
                    enabled = renamePlaylistName.trim().isNotBlank() && !actionInProgress
                ) {
                    Text("Lưu", color = Color(0xFFF6E29A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToRename = null }) {
                    Text("Hủy", color = Color.White.copy(alpha = 0.72f))
                }
            }
        )
    }

    playlistToDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            containerColor = Color(0xFF1E1E1E),
            title = { Text("Xóa danh sách phát?", color = Color.White) },
            text = {
                Text(
                    text = "Danh sách \"${playlist.name}\" và toàn bộ phim trong đó sẽ bị xóa vĩnh viễn.",
                    color = Color.White.copy(alpha = 0.76f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePlaylist(playlist.id)
                        playlistToDelete = null
                    },
                    enabled = !actionInProgress
                ) {
                    Text("Xóa", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("Hủy", color = Color.White.copy(alpha = 0.72f))
                }
            }
        )
    }
}

@Composable
private fun PlaylistSaveRow(
    playlist: UserPlaylist,
    isSelected: Boolean,
    actionInProgress: Boolean,
    onToggle: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = !actionInProgress, onClick = onToggle)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (playlist.coverPosterUrl.isNotBlank()) {
            com.example.alphacinema.ui.components.AlphaCinemaImage(
                model = playlist.coverPosterUrl,
                contentDescription = playlist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 88.dp, height = 52.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(width = 88.dp, height = 52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bookmark,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f)
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        ) {
            Text(
                text = playlist.name,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Riêng tư · ${playlist.itemCount} phim",
                color = Color.White.copy(alpha = 0.58f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        IconButton(
            onClick = onToggle,
            enabled = !actionInProgress
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Rounded.Check else Icons.Rounded.Bookmark,
                contentDescription = null,
                tint = if (isSelected) Color(0xFFF6E29A) else Color.White.copy(alpha = 0.86f)
            )
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Quản lý danh sách phát",
                    tint = Color.White.copy(alpha = 0.72f)
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                containerColor = Color(0xFF2A2A2A)
            ) {
                DropdownMenuItem(
                    text = { Text("Đổi tên", color = Color.White) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Edit, null, tint = Color.White.copy(alpha = 0.76f))
                    },
                    onClick = {
                        menuExpanded = false
                        onRename()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Xóa", color = Color(0xFFFF6B6B)) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Delete, null, tint = Color(0xFFFF6B6B))
                    },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
private fun playlistTextFieldColors() =
    androidx.compose.material3.OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Color(0xFFF6E29A),
        unfocusedBorderColor = Color.White.copy(alpha = 0.24f),
        focusedLabelColor = Color(0xFFF6E29A),
        unfocusedLabelColor = Color.White.copy(alpha = 0.64f),
        cursorColor = Color(0xFFF6E29A)
    )

@Composable
private fun EpisodeTab(
    episodes: List<EpisodeUi>,
    currentEpisodeText: String,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onPlayEpisode: (EpisodeUi) -> Unit
) {
    val currentEpisode = episodes.firstOrNull { episode ->
        currentEpisodeText.contains(
            episode.name.substringBefore(":"),
            ignoreCase = true
        )
    } ?: episodes.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                .clickable(onClick = onToggleExpanded)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Danh sách tập phim",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentEpisode?.name ?: currentEpisodeText,
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${episodes.size} tập${if (expanded) " • Nhấn để thu gọn" else " • Nhấn để chọn tập"}",
                    color = Color(0xFFF6E29A),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Icon(
                imageVector = if (expanded) {
                    Icons.Rounded.KeyboardArrowUp
                } else {
                    Icons.Rounded.KeyboardArrowDown
                },
                contentDescription = null,
                tint = Color(0xFFF6E29A)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                episodes.forEach { episode ->
                    val isCurrent = currentEpisodeText.contains(
                        episode.name.substringBefore(":"),
                        ignoreCase = true
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isCurrent) Color(0x22F6E29A) else Color.White.copy(alpha = 0.06f))
                            .border(
                                1.dp,
                                if (isCurrent) Color(0x66F6E29A) else Color.White.copy(alpha = 0.12f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onPlayEpisode(episode) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = if (isCurrent) Color(0xFFF6E29A) else Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = episode.name,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = episode.duration,
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        if (isCurrent) {
                            Text(
                                text = "Đang xem",
                                color = Color(0xFFF6E29A),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeTabModern(
    episodes: List<EpisodeUi>,
    currentEpisode: EpisodeUi?,
    relatedSeasons: List<RecommendedMovieUi> = emptyList(),
    currentMovieName: String = "",
    onSelectEpisode: (EpisodeUi) -> Unit,
    onOpenSeason: (String) -> Unit = {}
) {
    if (episodes.isEmpty() && relatedSeasons.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                .padding(18.dp)
        ) {
            Text(
                text = "Chưa có danh sách tập phim.",
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    val groupedEpisodes = remember(episodes) {
        episodes.groupBy { ep ->
            val match = Regex("(?i)(Phần|Mùa)\\s*\\d+").find(ep.name)
            match?.value ?: "Chung"
        }
    }

    var selectedGroup by remember(episodes) {
        val activeGroup = if (groupedEpisodes.isEmpty()) {
            ""
        } else if (currentEpisode != null) {
            val match = Regex("(?i)(Phần|Mùa)\\s*\\d+").find(currentEpisode.name)
            match?.value ?: "Chung"
        } else {
            groupedEpisodes.keys.first()
        }
        mutableStateOf(
            activeGroup.takeIf { groupedEpisodes.containsKey(it) }
                ?: groupedEpisodes.keys.firstOrNull()
                ?: ""
        )
    }

    if (groupedEpisodes.isNotEmpty() && !groupedEpisodes.containsKey(selectedGroup)) {
        selectedGroup = groupedEpisodes.keys.first()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (relatedSeasons.isNotEmpty()) {
            var seasonsExpanded by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { seasonsExpanded = !seasonsExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Phần phim khác",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Đang xem: $currentMovieName",
                        color = Color(0xFFF6E29A),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = if (seasonsExpanded) androidx.compose.material.icons.Icons.Rounded.KeyboardArrowUp else androidx.compose.material.icons.Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White
                )
            }

            if (seasonsExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val allSeasons = mutableListOf<RecommendedMovieUi>().apply {
                        add(RecommendedMovieUi("current", currentMovieName, "", ""))
                        addAll(relatedSeasons)
                    }
                    allSeasons.forEach { season ->
                        val isCurrent = season.id == "current"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCurrent) Color(0xFFF6E29A).copy(alpha = 0.15f) else Color.Transparent)
                                .clickable {
                                    if (!isCurrent) {
                                        seasonsExpanded = false
                                        onOpenSeason(season.id)
                                    }
                                }
                                .padding(vertical = 12.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = season.title,
                                color = if (isCurrent) Color(0xFFF6E29A) else Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (episodes.isNotEmpty()) {
            Text(
                text = "Danh sách tập phim",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        if (groupedEpisodes.keys.size > 1) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groupedEpisodes.keys.forEach { groupName ->
                    val isSelected = groupName == selectedGroup
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Color(0xFFF6E29A) else Color.White.copy(alpha = 0.08f))
                            .border(1.dp, if (isSelected) Color(0xFFF6E29A) else Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            .clickable { selectedGroup = groupName }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = groupName,
                            color = if (isSelected) Color.Black else Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (groupedEpisodes.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val columns = 3
                val episodesToShow = groupedEpisodes[selectedGroup] ?: emptyList()
                episodesToShow.chunked(columns).forEach { rowEpisodes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowEpisodes.forEach { episode ->
                            val isCurrent = currentEpisode?.id == episode.id
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isCurrent) Color(0xFFF6E29A) else Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, if (isCurrent) Color(0xFFF6E29A) else Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .clickable { onSelectEpisode(episode) }
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                val badgeText = episode.name
                                    .replace(Regex("(?i)(Phần|Mùa)\\s*\\d+\\s*(:|-)"), "")
                                    .replace(Regex("(?i)tập\\s*"), "")
                                    .trim()
                                Text(
                                    text = badgeText,
                                    color = if (isCurrent) Color.Black else Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        repeat(columns - rowEpisodes.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CastTab(cast: List<CastUi>) {
    val columns = 3
    val spacing = 12.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        cast.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                rowItems.forEach { castItem ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp)),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (castItem.profileUrl != null) {
                            com.example.alphacinema.ui.components.AlphaCinemaImage(
                                model = castItem.profileUrl,
                                contentDescription = castItem.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF1F1F1F), Color(0xFF101010))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = castItem.name.firstOrNull()?.uppercase() ?: "?",
                                    color = Color(0xFFF6E29A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp
                                )
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = castItem.name,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = castItem.role,
                                color = Color.White.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        // TMDB Attribution
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val context = LocalContext.current
            com.example.alphacinema.ui.components.AlphaCinemaImage(
                model = ImageRequest.Builder(context)
                    .data("file:///android_asset/tmdb_logo.svg")
                    .decoderFactory(SvgDecoder.Factory())
                    .build(),
                contentDescription = "TMDB Logo",
                modifier = Modifier.height(14.dp).width(108.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Powered by TMDB",
                color = Color.White.copy(alpha = 0.4f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun RecommendationTab(
    movies: List<RecommendedMovieUi>,
    onOpenMovie: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        movies.forEach { recommended ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                    .clickable { onOpenMovie(recommended.id) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.example.alphacinema.ui.components.AlphaCinemaImage(
                    model = recommended.posterUrl,
                    contentDescription = recommended.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(70.dp)
                        .height(96.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recommended.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = recommended.year,
                        color = Color.White.copy(alpha = 0.65f),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFFF6E29A)
                )
            }
        }
    }
}

private fun actionIcon(action: MovieDetailAction) = when (action) {
    MovieDetailAction.FAVORITE -> Icons.Rounded.FavoriteBorder
    MovieDetailAction.ADD_TO_LIST -> Icons.Rounded.Add
    MovieDetailAction.SHARE -> Icons.Rounded.Share
    MovieDetailAction.WATCH_TOGETHER -> Icons.Rounded.Groups
}

@Composable
private fun CommentsTab(
    comments: List<Comment>,
    currentUserId: String?,
    currentUserAvatarUrl: String,
    onSelectReplyTarget: (Comment) -> Unit,
    onToggleCommentLike: (Comment) -> Unit
) {
    val topLevelComments = remember(comments) {
        comments
            .filter { it.parentCommentId.isBlank() }
            .sortedByDescending { it.createdAtMillis() }
    }
    val repliesByParent = remember(comments) {
        comments
            .filter { it.parentCommentId.isNotBlank() }
            .groupBy { it.parentCommentId }
            .mapValues { (_, replies) -> replies.sortedBy { it.createdAtMillis() } }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (topLevelComments.isEmpty()) {
            Text("Chưa có bình luận nào.", color = Color.White.copy(alpha = 0.5f))
        } else {
            topLevelComments.forEach { comment ->
                CommentRow(
                    comment = comment,
                    currentUserId = currentUserId,
                    currentUserAvatarUrl = currentUserAvatarUrl,
                    showReplyAction = true,
                    onReply = { onSelectReplyTarget(comment) },
                    onToggleLike = { onToggleCommentLike(comment) }
                )

                repliesByParent[comment.id].orEmpty().forEach { reply ->
                    CommentRow(
                        comment = reply,
                        currentUserId = currentUserId,
                        currentUserAvatarUrl = currentUserAvatarUrl,
                        showReplyAction = false,
                        modifier = Modifier.padding(start = 48.dp),
                        onReply = {},
                        onToggleLike = { onToggleCommentLike(reply) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentInputBar(
    text: String,
    replyTarget: Comment?,
    onTextChange: (String) -> Unit,
    onCancelReply: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.96f))
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .navigationBarsPadding()
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        replyTarget?.let { target ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(start = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Reply,
                    contentDescription = null,
                    tint = Color(0xFFF6E29A),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Đang trả lời ${target.userName.ifBlank { "Người dùng" }}",
                    color = Color.White.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(
                    onClick = onCancelReply,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Hủy trả lời",
                        tint = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        if (replyTarget == null) "Viết bình luận..." else "Viết câu trả lời...",
                        color = Color.White.copy(alpha = 0.5f)
                    )
                },
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.White.copy(alpha = 0.08f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedBorderColor = Color(0xFFF6E29A)
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onSend,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF6E29A),
                    contentColor = Color.Black
                )
            ) {
                Text("Gửi", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CommentRow(
    comment: Comment,
    currentUserId: String?,
    currentUserAvatarUrl: String,
    showReplyAction: Boolean,
    modifier: Modifier = Modifier,
    onReply: () -> Unit,
    onToggleLike: () -> Unit
) {
    val avatarUrl = comment.resolvedAvatarUrl(currentUserId, currentUserAvatarUrl)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFF262626)),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl.isNotBlank()) {
                com.example.alphacinema.ui.components.AlphaCinemaImage(
                    model = avatarUrl,
                    contentDescription = comment.userName.ifBlank { "Người dùng" },
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = comment.userName.firstOrNull()?.uppercase() ?: "?",
                    color = Color(0xFFF6E29A),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(12.dp)
            ) {
                Text(
                    text = comment.userName.ifBlank { "Người dùng" },
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = comment.replyContentLabel(),
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                IconButton(
                    onClick = onToggleLike,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (comment.isLikedBy(currentUserId)) {
                            Icons.Rounded.Favorite
                        } else {
                            Icons.Rounded.FavoriteBorder
                        },
                        contentDescription = "Thả tim",
                        tint = if (comment.isLikedBy(currentUserId)) {
                            Color(0xFFFF5C7A)
                        } else {
                            Color.White.copy(alpha = 0.58f)
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }
                val likeCount = comment.visibleLikeCount()
                if (likeCount > 0L) {
                    Text(
                        text = likeCount.toString(),
                        color = Color.White.copy(alpha = 0.62f),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                if (showReplyAction) {
                    TextButton(
                        onClick = onReply,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Reply,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.62f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Trả lời",
                            color = Color.White.copy(alpha = 0.62f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private fun Comment.createdAtMillis(): Long {
    return createdAt?.toDate()?.time ?: 0L
}

private fun Comment.visibleLikeCount(): Long {
    return if (likedBy.isNotEmpty()) likedBy.size.toLong() else likes.coerceAtLeast(0L)
}

private fun Comment.isLikedBy(userId: String?): Boolean {
    return !userId.isNullOrBlank() && userId in likedBy
}

private fun Comment.resolvedAvatarUrl(currentUserId: String?, currentUserAvatarUrl: String): String {
    return if (userId == currentUserId && currentUserAvatarUrl.isNotBlank()) {
        currentUserAvatarUrl
    } else {
        userAvatar
    }
}

private fun Comment.replyContentLabel(): String {
    return if (parentCommentId.isNotBlank() && replyToUserName.isNotBlank()) {
        "@$replyToUserName $content"
    } else {
        content
    }
}

@Composable
private fun RatingsTab(
    stats: MovieStats?,
    userRating: Int?,
    onSubmitRating: (Int) -> Unit
) {
    var selectedScore by rememberSaveable(userRating) { mutableStateOf(userRating ?: 0) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (stats != null) {
            Text(
                text = "${stats.averageRating} / 10",
                color = Color(0xFFF6E29A),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Dựa trên ${stats.totalRatings} lượt đánh giá",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelMedium
            )
        } else {
            Text(
                text = "Chưa có đánh giá nào",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelMedium
            )
        }
        
        Spacer(modifier = Modifier.height(30.dp))
        
        Text(
            text = "Đánh giá của bạn:",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Display 5 pair blocks for 10 scores? Actually let's just make 5 stars that act as 10 scores (half stars) or simply 10 stars in a scrollable or wrapped row. 
            // 10 stars in a row might be too dense, but we can fit it.
            for (i in 1..10) {
                Icon(
                    imageVector = if (i <= selectedScore) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = "$i sao",
                    tint = if (i <= selectedScore) Color(0xFFF6E29A) else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { selectedScore = i }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { onSubmitRating(selectedScore) },
            enabled = selectedScore > 0,
            modifier = Modifier
                .width(200.dp)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF6E29A), 
                contentColor = Color.Black,
                disabledContainerColor = Color.White.copy(alpha = 0.1f),
                disabledContentColor = Color.White.copy(alpha = 0.3f)
            )
        ) {
            Text("Lưu Đánh Giá", fontWeight = FontWeight.Bold)
        }
    }
}
