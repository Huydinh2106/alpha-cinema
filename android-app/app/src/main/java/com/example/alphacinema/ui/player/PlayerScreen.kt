package com.example.alphacinema.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.alphacinema.ui.movie.detail.EpisodeUi
import com.example.alphacinema.ui.movie.detail.MovieDetailUi
import kotlinx.coroutines.delay

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    movie: MovieDetailUi,
    episode: EpisodeUi?,
    onBack: () -> Unit,
    videoUrl: String = "",
    episodeVideoUrls: Map<String, String> = emptyMap(),
    onSelectEpisode: (EpisodeUi) -> Unit = {},
    viewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    val currentEpisode by rememberUpdatedState(episode)
    val currentMovie by rememberUpdatedState(movie)
    val currentVideoUrl by rememberUpdatedState(videoUrl)

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build().apply {
            playWhenReady = true
        }
    }

    var playerView: PlayerView? by remember {
        mutableStateOf(null)
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int
            ) {
                if (
                    reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
                    reason != Player.MEDIA_ITEM_TRANSITION_REASON_SEEK
                ) {
                    return
                }

                val episodeId = mediaItem?.mediaId ?: return
                val targetEpisode =
                    viewModel.getEpisodeById(currentMovie, episodeId)

                if (
                    targetEpisode != null &&
                    targetEpisode.id != currentEpisode?.id
                ) {
                    onSelectEpisode(targetEpisode)
                }

                // Chạy sau khi PlayerView xử lý xong focus/UI
                playerView?.post {
                    hideSystemBars(activity)
                }
            }
        }

        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    LaunchedEffect(movie.episodes, episodeVideoUrls) {
        val mediaItems = viewModel.buildEpisodeMediaItems(
            movie = movie,
            episodeVideoUrls = episodeVideoUrls
        )

        exoPlayer.setMediaItems(mediaItems)
        exoPlayer.prepare()

        val targetIndex = viewModel.getEpisodeIndex(
            movie = movie,
            episode = episode
        )

        exoPlayer.seekTo(targetIndex, 0L)
        exoPlayer.playWhenReady = true
    }

    LaunchedEffect(episode) {
        val targetIndex = viewModel.getEpisodeIndex(
            movie = movie,
            episode = episode
        )

        if (
            targetIndex != -1 &&
            targetIndex != exoPlayer.currentMediaItemIndex
        ) {
            exoPlayer.seekTo(targetIndex, 0L)
        }
    }

    LaunchedEffect(exoPlayer, movie.id, episode?.id, videoUrl) {
        if (videoUrl.isBlank()) return@LaunchedEffect

        viewModel.saveWatchProgress(
            movie = currentMovie,
            episode = currentEpisode,
            progress = exoPlayer.currentPosition,
            duration = normalizedDurationMs(exoPlayer.duration)
        )

        while (true) {
            delay(10_000)
            viewModel.saveWatchProgress(
                movie = currentMovie,
                episode = currentEpisode,
                progress = exoPlayer.currentPosition,
                duration = normalizedDurationMs(exoPlayer.duration)
            )
        }
    }

    LaunchedEffect(exoPlayer.currentMediaItemIndex) {
        delay(5000)
        hideSystemBars(context.findActivity())
    }

    DisposableEffect(Unit) {
        activity?.requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        hideSystemBars(activity)

        onDispose {
            activity?.requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

            showSystemBars(activity)

            if (currentVideoUrl.isNotBlank()) {
                viewModel.saveWatchProgress(
                    movie = currentMovie,
                    episode = currentEpisode,
                    progress = exoPlayer.currentPosition,
                    duration = normalizedDurationMs(exoPlayer.duration)
                )
            }

            exoPlayer.release()
        }
    }

    var showEpisodeDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                val inflatedView = android.view.LayoutInflater.from(ctx).inflate(com.example.alphacinema.R.layout.custom_player_view, null) as androidx.media3.ui.PlayerView
                inflatedView.apply {
                    player = exoPlayer
                    useController = true
                    showController()

                    playerView = this

                    // Force custom 10s icons (Media3 overrides them by default)
                    val applyCustomIcons = {
                        findViewById<android.widget.ImageButton>(androidx.media3.ui.R.id.exo_rew)
                            ?.setImageResource(com.example.alphacinema.R.drawable.ic_replay_10)
                        findViewById<android.widget.ImageButton>(androidx.media3.ui.R.id.exo_ffwd)
                            ?.setImageResource(com.example.alphacinema.R.drawable.ic_forward_10)
                    }
                    applyCustomIcons()

                    setControllerVisibilityListener(
                        androidx.media3.ui.PlayerView.ControllerVisibilityListener { visibility ->
                            applyCustomIcons()
                            if (visibility == android.view.View.GONE) {
                                post {
                                    post {
                                        hideSystemBars(activity)
                                    }
                                }
                            }
                        }
                    )

                    // Episode selector button
                    val epBtn = findViewById<android.widget.ImageButton>(com.example.alphacinema.R.id.btn_episode_selector)
                    if (movie.episodes.size > 1) {
                        epBtn?.visibility = android.view.View.VISIBLE
                        epBtn?.setOnClickListener { showEpisodeDialog = true }
                    }
                }
                inflatedView
            },
            update = { view ->
                view.player = exoPlayer
                playerView = view
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.TopCenter)
        )

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 8.dp, start = 16.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Quay lại",
                tint = Color.White
            )
        }

        // Episode picker dialog overlay
        if (showEpisodeDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.15f))
                    .clickable { showEpisodeDialog = false },
                contentAlignment = Alignment.BottomEnd
            ) {
                Column(
                    modifier = Modifier
                        .padding(end = 48.dp, bottom = 56.dp)
                        .widthIn(max = 280.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A2237).copy(alpha = 0.75f))
                        .padding(16.dp)
                        .clickable(enabled = false) {}
                ) {
                    Text(
                        "Chọn tập (${movie.episodes.size} tập)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        items(movie.episodes.size) { idx ->
                            val ep = movie.episodes[idx]
                            val isSelected = ep.id == episode?.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelected) Color(0xFFF6E29A) else Color.White.copy(alpha = 0.1f)
                                    )
                                    .clickable {
                                        onSelectEpisode(ep)
                                        showEpisodeDialog = false
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    ep.name.replace("Tập ", "").replace("tập ", "").ifBlank { "${idx + 1}" },
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun hideSystemBars(activity: Activity?) {
    val window = activity?.window ?: return

    WindowCompat.setDecorFitsSystemWindows(window, false)

    WindowInsetsControllerCompat(window, window.decorView).apply {
        hide(WindowInsetsCompat.Type.systemBars())
        systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

private fun showSystemBars(activity: Activity?) {
    val window = activity?.window ?: return

    WindowCompat.setDecorFitsSystemWindows(window, true)

    WindowInsetsControllerCompat(window, window.decorView)
        .show(WindowInsetsCompat.Type.systemBars())
}

private fun normalizedDurationMs(duration: Long): Long {
    return if (duration > 0L && duration != C.TIME_UNSET) duration else 0L
}
