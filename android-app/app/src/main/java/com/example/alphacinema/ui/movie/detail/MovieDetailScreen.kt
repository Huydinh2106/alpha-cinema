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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import com.example.alphacinema.data.model.Comment
import com.example.alphacinema.data.model.MovieStats
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

@Composable
fun MovieDetailScreen(
    movie: MovieDetailUi,
    isFavorite: Boolean,
    movieStats: MovieStats?,
    userRating: Int?,
    comments: List<Comment>,
    onToggleFavorite: (MovieDetailUi) -> Unit,
    onPostComment: (String) -> Unit,
    onSubmitRating: (Int) -> Unit,
    onBack: () -> Unit,
    onPlayMovie: (MovieDetailUi, EpisodeUi?) -> Unit,
    onOpenMovie: (String) -> Unit,
    onWatchTogether: () -> Unit = {}
) {
    var selectedTabName by rememberSaveable(movie.id) {
        mutableStateOf(if (movie.episodes.size > 1 || movie.relatedSeasons.isNotEmpty()) MovieDetailTab.EPISODES.name else MovieDetailTab.CAST.name)
    }
    var descriptionExpanded by rememberSaveable(movie.id) {
        mutableStateOf(false)
    }

    val selectedTab = MovieDetailTab.valueOf(selectedTabName)
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var activeEpisodeId by rememberSaveable(movie.id) {
        val defaultEp = movie.episodes.firstOrNull { ep ->
            movie.currentEpisode.contains(ep.name.substringBefore(":"), ignoreCase = true)
        } ?: movie.episodes.firstOrNull()
        mutableStateOf(defaultEp?.id)
    }
    val activeEpisode = movie.episodes.find { it.id == activeEpisodeId }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B16)),
        contentPadding = PaddingValues(bottom = 28.dp)
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
                    onEpisodes = if (movie.episodes.size > 1 || movie.relatedSeasons.isNotEmpty()) {
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
                    onActionClick = { action ->
                        when(action) {
                            MovieDetailAction.FAVORITE -> onToggleFavorite(movie)
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
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                indicator = {}
            ) {
                MovieDetailTab.entries.filter { tab -> tab != MovieDetailTab.EPISODES || movie.episodes.size > 1 || movie.relatedSeasons.isNotEmpty() }.forEach { tab ->
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
                MovieDetailTab.EPISODES -> if (movie.episodes.size > 1 || movie.relatedSeasons.isNotEmpty()) {
                    EpisodeTabModern(
                        episodes = movie.episodes,
                        currentEpisode = activeEpisode,
                        relatedSeasons = movie.relatedSeasons,
                        currentMovieName = movie.title,
                        onSelectEpisode = { activeEpisodeId = it.id },
                        onOpenSeason = onOpenMovie
                    )
                }

                MovieDetailTab.CAST -> CastTab(cast = movie.cast)
                MovieDetailTab.RECOMMENDATIONS -> RecommendationTab(
                    movies = movie.recommendations,
                    onOpenMovie = onOpenMovie
                )
                MovieDetailTab.COMMENTS -> CommentsTab(
                    comments = comments,
                    onPostComment = onPostComment
                )
                MovieDetailTab.RATINGS -> RatingsTab(
                    stats = movieStats,
                    userRating = userRating,
                    onSubmitRating = onSubmitRating
                )
            }
        }
    }
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
                            Color(0xFF070B16)
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
                imageVector = Icons.Outlined.ArrowBack,
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
                        .border(1.dp, if (isFavActive) Color(0xFFF6E29A) else Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFavActive) Icons.Filled.Favorite else actionIcon(action),
                        contentDescription = action.label,
                        tint = if (isFavActive) Color(0xFFF6E29A) else Color.White.copy(alpha = 0.85f)
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
                    Icons.Outlined.KeyboardArrowUp
                } else {
                    Icons.Outlined.KeyboardArrowDown
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
                            imageVector = Icons.Outlined.PlayArrow,
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
        val activeGroup = if (currentEpisode != null) {
            val match = Regex("(?i)(Phần|Mùa)\\s*\\d+").find(currentEpisode.name)
            match?.value ?: "Chung"
        } else {
            groupedEpisodes.keys.first()
        }
        mutableStateOf(activeGroup)
    }

    if (!groupedEpisodes.containsKey(selectedGroup)) {
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
                    imageVector = if (seasonsExpanded) androidx.compose.material.icons.Icons.Outlined.KeyboardArrowUp else androidx.compose.material.icons.Icons.Outlined.KeyboardArrowDown,
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
                            color = if (isSelected) Color(0xFF0A0F1E) else Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

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
                                color = if (isCurrent) Color(0xFF0A0F1E) else Color.White,
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
                                            listOf(Color(0xFF1A1F35), Color(0xFF0E1225))
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
                modifier = Modifier.height(14.dp)
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
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFFF6E29A)
                )
            }
        }
    }
}

private fun actionIcon(action: MovieDetailAction) = when (action) {
    MovieDetailAction.FAVORITE -> Icons.Outlined.FavoriteBorder
    MovieDetailAction.ADD_TO_LIST -> Icons.Outlined.Add
    MovieDetailAction.SHARE -> Icons.Outlined.Share
    MovieDetailAction.WATCH_TOGETHER -> Icons.Outlined.Groups
}

@Composable
private fun CommentsTab(
    comments: List<Comment>,
    onPostComment: (String) -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Viết bình luận...", color = Color.White.copy(alpha = 0.5f)) },
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedBorderColor = Color(0xFFF6E29A)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    onPostComment(text)
                    text = ""
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF6E29A), contentColor = Color.Black)
            ) {
                Text("Gửi", fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        if (comments.isEmpty()) {
            Text("Chưa có bình luận nào.", color = Color.White.copy(alpha = 0.5f))
        } else {
            comments.forEach { comment ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1F2438)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = comment.userName.firstOrNull()?.uppercase() ?: "?",
                            color = Color(0xFFF6E29A),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
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
                            text = comment.content,
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
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
                    imageVector = if (i <= selectedScore) Icons.Filled.Star else Icons.Outlined.StarBorder,
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
