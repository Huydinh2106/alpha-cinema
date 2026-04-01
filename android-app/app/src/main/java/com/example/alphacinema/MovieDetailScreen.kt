@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.alphacinema

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import coil.compose.AsyncImage

@Composable
fun MovieDetailScreen(
    movie: MovieDetailUi,
    onBack: () -> Unit,
    onPlayMovie: (MovieDetailUi, EpisodeUi?) -> Unit,
    onOpenMovie: (String) -> Unit
) {
    var selectedTabName by rememberSaveable(movie.id) {
        mutableStateOf(MovieDetailTab.EPISODES.name)
    }
    var descriptionExpanded by rememberSaveable(movie.id) {
        mutableStateOf(false)
    }

    val selectedTab = MovieDetailTab.valueOf(selectedTabName)

    LazyColumn(
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
                    onPlay = { onPlayMovie(movie, movie.episodes.firstOrNull()) },
                    onEpisodes = { selectedTabName = MovieDetailTab.EPISODES.name }
                )
                Spacer(modifier = Modifier.height(14.dp))
                ActionRow()
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
                MovieDetailTab.entries.forEach { tab ->
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
                MovieDetailTab.EPISODES -> EpisodeTab(
                    episodes = movie.episodes,
                    currentEpisodeText = movie.currentEpisode,
                    onPlayEpisode = { onPlayMovie(movie, it) }
                )

                MovieDetailTab.CAST -> CastTab(cast = movie.cast)
                MovieDetailTab.RECOMMENDATIONS -> RecommendationTab(
                    movies = movie.recommendations,
                    onOpenMovie = onOpenMovie
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
        AsyncImage(
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
                .padding(start = 12.dp, top = 18.dp)
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

        AsyncImage(
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
    onEpisodes: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onPlay,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF6E29A),
                contentColor = Color.Black
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Xem phim", fontWeight = FontWeight.Bold)
        }

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

@Composable
private fun ActionRow() {
    val actions = MovieDetailAction.entries
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        actions.forEach { action ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = actionIcon(action),
                        contentDescription = action.label,
                        tint = Color.White.copy(alpha = 0.85f)
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
    onPlayEpisode: (EpisodeUi) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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

@Composable
private fun CastTab(cast: List<CastUi>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        cast.forEach { castItem ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F2438)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = castItem.name.first().uppercase(),
                        color = Color(0xFFF6E29A),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = castItem.name,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = castItem.role,
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
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
                AsyncImage(
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
    MovieDetailAction.RATE -> Icons.Outlined.StarBorder
    MovieDetailAction.COMMENT -> Icons.Outlined.ChatBubbleOutline
    MovieDetailAction.SHARE -> Icons.Outlined.Share
}
