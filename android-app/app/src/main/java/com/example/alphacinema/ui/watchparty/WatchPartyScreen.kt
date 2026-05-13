package com.example.alphacinema.ui.watchparty

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.alphacinema.data.model.WatchPartyChatMessage
import com.example.alphacinema.data.model.WatchPartyMember
import com.example.alphacinema.ui.movie.detail.EpisodeUi
import com.example.alphacinema.ui.player.findActivity
import kotlinx.coroutines.delay


@OptIn(UnstableApi::class)
@Composable
fun WatchPartyScreen(
    videoUrl: String,
    episodes: List<EpisodeUi>,
    currentEpisodeId: String?,
    viewModel: WatchPartyViewModel,
    onChangeMovieClick: () -> Unit,
    onBack: () -> Unit
) {
    val room by viewModel.room.collectAsState()
    val members by viewModel.members.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val roomDismissed by viewModel.roomDismissed.collectAsState()
    val serverTimeOffset by viewModel.serverTimeOffset.collectAsState()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val isHost = viewModel.isHost
    val currentUid = viewModel.currentUid

    // Guest time display
    var guestCurrentTimeMs by remember { mutableLongStateOf(0L) }
    var guestDurationMs by remember { mutableLongStateOf(0L) }

    // Fullscreen state
    var isFullscreen by remember { mutableStateOf(false) }
    var showEpisodeDialog by remember { mutableStateOf(false) }
    val activity = context.findActivity()

    // Room dismissed → host left
    LaunchedEffect(roomDismissed) {
        if (roomDismissed) {
            android.widget.Toast.makeText(context, "Chủ phòng đã giải tán phòng", android.widget.Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    // ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()
    }

    // Load video
    LaunchedEffect(videoUrl) {
        if (videoUrl.isNotBlank()) {
            exoPlayer.setMediaItem(MediaItem.fromUri(videoUrl))
            exoPlayer.prepare()
            if (isHost) exoPlayer.playWhenReady = true
        }
    }

    // Host: report play/pause/seek to Firebase
    if (isHost) {
        DisposableEffect(exoPlayer) {
            var updating = false
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (updating) return
                    updating = true
                    val t = exoPlayer.currentPosition / 1000.0
                    if (isPlaying) viewModel.updatePlayback("playing", t)
                    else if (exoPlayer.playbackState == Player.STATE_READY)
                        viewModel.updatePlayback("paused", t)
                    updating = false
                }
                override fun onPositionDiscontinuity(
                    old: Player.PositionInfo, new: Player.PositionInfo, reason: Int
                ) {
                    if (updating || reason != Player.DISCONTINUITY_REASON_SEEK) return
                    updating = true
                    val t = new.positionMs / 1000.0
                    val s = if (exoPlayer.isPlaying) "playing" else "paused"
                    viewModel.updatePlayback(s, t)
                    updating = false
                }
            }
            exoPlayer.addListener(listener)
            onDispose { exoPlayer.removeListener(listener) }
        }
    }

    // Guest: Server Clock sync
    if (!isHost) {
        LaunchedEffect(room?.playbackState, room?.playStartedAt) {
            val r = room ?: return@LaunchedEffect
            when (r.playbackState) {
                "playing" -> {
                    val serverNow = System.currentTimeMillis() + serverTimeOffset
                    val pos = r.currentTimeSec + (serverNow - r.playStartedAt) / 1000.0
                    exoPlayer.seekTo((pos * 1000).toLong())
                    exoPlayer.playWhenReady = true
                    // Self-correction loop
                    while (true) {
                        delay(2000)
                        val cr = room ?: break
                        if (cr.playbackState != "playing") break
                        val now = System.currentTimeMillis() + serverTimeOffset
                        val exp = cr.currentTimeSec + (now - cr.playStartedAt) / 1000.0
                        val act = exoPlayer.currentPosition / 1000.0
                        val diff = exp - act
                        when {
                            kotlin.math.abs(diff) > 5.0 -> {
                                exoPlayer.seekTo((exp * 1000).toLong())
                                exoPlayer.playbackParameters = PlaybackParameters(1.0f)
                            }
                            kotlin.math.abs(diff) < 0.1 -> {
                                exoPlayer.playbackParameters = PlaybackParameters(1.0f)
                            }
                            diff > 0 -> exoPlayer.playbackParameters = PlaybackParameters(
                                (1.0f + diff.toFloat() * 0.02f).coerceAtMost(1.05f)
                            )
                            else -> exoPlayer.playbackParameters = PlaybackParameters(
                                (1.0f + diff.toFloat() * 0.02f).coerceAtLeast(0.95f)
                            )
                        }
                    }
                }
                "paused" -> {
                    exoPlayer.playWhenReady = false
                    exoPlayer.seekTo((r.currentTimeSec * 1000).toLong())
                    exoPlayer.playbackParameters = PlaybackParameters(1.0f)
                }
            }
        }
        // Guest: update time display
        LaunchedEffect(exoPlayer) {
            while (true) {
                guestCurrentTimeMs = exoPlayer.currentPosition
                guestDurationMs = exoPlayer.duration.coerceAtLeast(0)
                delay(500)
            }
        }
    }

    // Release player + restore orientation
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            val window = activity?.window ?: return@onDispose
            WindowCompat.setDecorFitsSystemWindows(window, true)
            WindowInsetsControllerCompat(window, window.decorView)
                .show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Fullscreen toggle
    fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            val window = activity?.window ?: return
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            val window = activity?.window ?: return
            WindowCompat.setDecorFitsSystemWindows(window, true)
            WindowInsetsControllerCompat(window, window.decorView)
                .show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Fullscreen mode
    if (isFullscreen) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { ctx ->
                    (android.view.LayoutInflater.from(ctx).inflate(com.example.alphacinema.R.layout.custom_player_view, null) as androidx.media3.ui.PlayerView).apply {
                        player = exoPlayer
                        useController = isHost
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        setFullscreenButtonClickListener { toggleFullscreen() }

                        // Force custom 10s icons
                        val applyCustomIcons = {
                            findViewById<android.widget.ImageButton>(androidx.media3.ui.R.id.exo_rew)
                                ?.setImageResource(com.example.alphacinema.R.drawable.ic_replay_10)
                            findViewById<android.widget.ImageButton>(androidx.media3.ui.R.id.exo_ffwd)
                                ?.setImageResource(com.example.alphacinema.R.drawable.ic_forward_10)
                        }
                        applyCustomIcons()
                        setControllerVisibilityListener(
                            androidx.media3.ui.PlayerView.ControllerVisibilityListener { applyCustomIcons() }
                        )

                        // Episode selector button
                        val epBtn = findViewById<android.widget.ImageButton>(com.example.alphacinema.R.id.btn_episode_selector)
                        if (isHost && episodes.size > 1) {
                            epBtn?.visibility = android.view.View.VISIBLE
                            epBtn?.setOnClickListener { showEpisodeDialog = true }
                        }
                    }
                },
                update = { it.player = exoPlayer },
                modifier = Modifier.fillMaxSize()
            )
            // Members overlay (top-right)
            if (room != null) {
                Row(
                    modifier = Modifier.padding(16.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 12.dp, vertical = 8.dp).align(Alignment.TopEnd),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Outlined.Groups, null, tint = Color(0xFFF6E29A), modifier = Modifier.size(16.dp))
                    Text("${members.size}/5", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
            if (!isHost) {
                val fmt = { ms: Long -> val ts = ms / 1000; val h = ts / 3600; val m = (ts % 3600) / 60; val s = ts % 60; if (h > 0) "$h:${"%02d".format(m)}:${"%02d".format(s)}" else "$m:${"%02d".format(s)}" }
                Text("${fmt(guestCurrentTimeMs)} / ${fmt(guestDurationMs)}", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 14.dp, vertical = 4.dp))
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
                            "Chọn tập (${episodes.size} tập)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(5),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.heightIn(max = 200.dp)
                        ) {
                            items(episodes.size) { idx ->
                                val ep = episodes[idx]
                                val isSelected = ep.id == currentEpisodeId
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isSelected) Color(0xFFF6E29A) else Color.White.copy(alpha = 0.1f)
                                        )
                                        .clickable {
                                            viewModel.changeEpisode(ep.id, ep.name)
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
        return
    }

    // Portrait mode
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().background(Color(0xFF070B16))
    ) {
        // Video player with built-in fullscreen button
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    (android.view.LayoutInflater.from(ctx).inflate(com.example.alphacinema.R.layout.custom_player_view, null) as androidx.media3.ui.PlayerView).apply {
                        player = exoPlayer
                        useController = isHost
                        setFullscreenButtonClickListener { toggleFullscreen() }

                        // Force custom 10s icons (Media3 overrides them by default)
                        val applyCustomIcons = {
                            findViewById<android.widget.ImageButton>(androidx.media3.ui.R.id.exo_rew)
                                ?.setImageResource(com.example.alphacinema.R.drawable.ic_replay_10)
                            findViewById<android.widget.ImageButton>(androidx.media3.ui.R.id.exo_ffwd)
                                ?.setImageResource(com.example.alphacinema.R.drawable.ic_forward_10)
                        }
                        applyCustomIcons()
                        setControllerVisibilityListener(
                            androidx.media3.ui.PlayerView.ControllerVisibilityListener { applyCustomIcons() }
                        )
                    }
                },
                update = { it.player = exoPlayer },
                modifier = Modifier.fillMaxSize()
            )
            // Guest time
            if (!isHost) {
                val fmt = { ms: Long -> val ts = ms / 1000; val h = ts / 3600; val m = (ts % 3600) / 60; val s = ts % 60; if (h > 0) "$h:${"%02d".format(m)}:${"%02d".format(s)}" else "$m:${"%02d".format(s)}" }
                Text("${fmt(guestCurrentTimeMs)} / ${fmt(guestDurationMs)}", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 14.dp, vertical = 4.dp))
            }
            // Back button
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .align(Alignment.TopStart)
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Quay lại", tint = Color.White)
            }
        }

        // Room info + share
        var showInviteDialog by remember { mutableStateOf(false) }
        if (room != null) {
            val hasMovie = room!!.movieSlug.isNotBlank()
            RoomInfoBar(
                roomId = room!!.roomId,
                movieTitle = if (hasMovie) room!!.movieTitle else "Chưa chọn phim",
                hasMovie = hasMovie,
                isHost = isHost,
                onCopyId = {
                    clipboardManager.setText(AnnotatedString(room!!.roomId))
                    android.widget.Toast.makeText(context, "Đã sao chép mã phòng", android.widget.Toast.LENGTH_SHORT).show()
                },
                onShare = { showInviteDialog = true },
                onChangeMovie = onChangeMovieClick
            )

            // Invite popup
            if (showInviteDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showInviteDialog = false },
                    containerColor = Color(0xFF1A2237),
                    title = {
                        Text("Mời bạn bè", color = Color.White, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Option 1: Copy room code
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(room!!.roomId))
                                        android.widget.Toast.makeText(context, "Đã sao chép mã phòng: ${room!!.roomId}", android.widget.Toast.LENGTH_SHORT).show()
                                        showInviteDialog = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Outlined.ContentCopy, null, tint = Color(0xFFF6E29A), modifier = Modifier.size(22.dp))
                                Column {
                                    Text("Sao chép mã phòng", color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    Text(room!!.roomId, color = Color(0xFFF6E29A), style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp)
                                }
                            }
                            // Option 2: Share deep link
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable {
                                        val deepLink = "alphacinema://watchparty/${room!!.roomId}"
                                        val shareText = if (hasMovie) {
                                            "Xem phim \"${room!!.movieTitle}\" cùng tôi trên AlphaCinema!\n$deepLink"
                                        } else {
                                            "Xem phim cùng tôi trên AlphaCinema!\n$deepLink"
                                        }
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Chia sẻ link phòng"))
                                        showInviteDialog = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Outlined.Share, null, tint = Color(0xFFF6E29A), modifier = Modifier.size(22.dp))
                                Column {
                                    Text("Gửi link mời", color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    Text("Bạn bè bấm link tự vào phòng", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    },
                    confirmButton = {}
                )
            }
        }

        // Episode selector (host only, compact)
        if (isHost && episodes.size > 1) {
            EpisodeSelector(
                episodes = episodes,
                currentEpisodeId = currentEpisodeId,
                onSelectEpisode = { ep -> viewModel.changeEpisode(ep.id, ep.name) }
            )
        }

        // Members
        if (members.isNotEmpty()) {
            MembersList(members = members, hostId = room?.hostId ?: "")
        }

        // Chat
        ChatSection(
            messages = chatMessages,
            currentUid = currentUid ?: "",
            onSend = { viewModel.sendMessage(it) },
            modifier = Modifier.weight(1f)
        )
    }
}


@Composable
private fun RoomInfoBar(
    roomId: String,
    movieTitle: String,
    hasMovie: Boolean = true,
    isHost: Boolean,
    onCopyId: () -> Unit,
    onShare: () -> Unit,
    onChangeMovie: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp)
    ) {
        Text(
            text = movieTitle,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Invite button
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF6E29A).copy(alpha = 0.12f))
                    .clickable { onShare() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Outlined.Share,
                    contentDescription = "Mời bạn bè",
                    tint = Color(0xFFF6E29A),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mời bạn bè",
                    color = Color(0xFFF6E29A),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Change / Choose Movie button
            if (isHost) {
                val btnLabel = if (hasMovie) "Đổi phim" else "Chọn phim"
                val btnTint = if (hasMovie) Color.White else Color(0xFFF6E29A)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (hasMovie) Color.White.copy(alpha = 0.08f)
                            else Color(0xFFF6E29A).copy(alpha = 0.15f)
                        )
                        .clickable { onChangeMovie() }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Outlined.Movie,
                        contentDescription = btnLabel,
                        tint = btnTint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = btnLabel,
                        color = btnTint,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun MembersList(members: List<WatchPartyMember>, hostId: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Thành viên (${members.size}/5)",
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            members.forEach { member ->
                val memberIsHost = member.uid == hostId
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.widthIn(max = 60.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (memberIsHost) Brush.linearGradient(
                                    listOf(Color(0xFFF6E29A), Color(0xFFD4A843))
                                ) else Brush.linearGradient(
                                    listOf(Color(0xFF1A2237), Color(0xFF1A2237))
                                )
                            )
                            .then(
                                if (memberIsHost) Modifier.border(2.dp, Color(0xFFF6E29A), CircleShape)
                                else Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.displayName.firstOrNull()?.uppercase() ?: "?",
                            color = if (memberIsHost) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (memberIsHost) "Host" else member.displayName.split(" ").lastOrNull() ?: "",
                        color = if (memberIsHost) Color(0xFFF6E29A) else Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeSelector(
    episodes: List<EpisodeUi>,
    currentEpisodeId: String?,
    onSelectEpisode: (EpisodeUi) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Chọn tập (${episodes.size} tập)",
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(episodes, key = { it.id }) { ep ->
                val isActive = ep.id == currentEpisodeId
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isActive) Color(0xFFF6E29A) else Color.White.copy(alpha = 0.08f)
                        )
                        .border(
                            1.dp,
                            if (isActive) Color(0xFFF6E29A) else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { if (!isActive) onSelectEpisode(ep) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ep.name,
                        color = if (isActive) Color.Black else Color.White.copy(alpha = 0.7f),
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatSection(
    messages: List<WatchPartyChatMessage>,
    currentUid: String,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Trò chuyện",
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Messages — chiếm hết không gian còn lại
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = Color.White.copy(alpha = 0.04f),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có tin nhắn nào",
                        color = Color.White.copy(alpha = 0.3f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isMe = msg.uid == currentUid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                        ) {
                            Column(
                                modifier = Modifier.widthIn(max = 250.dp)
                            ) {
                                if (!isMe) {
                                    Text(
                                        text = msg.displayName,
                                        color = Color(0xFFF6E29A).copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 14.dp,
                                                topEnd = 14.dp,
                                                bottomStart = if (isMe) 14.dp else 4.dp,
                                                bottomEnd = if (isMe) 4.dp else 14.dp
                                            )
                                        )
                                        .background(
                                            if (isMe) Color(0xFFF6E29A) else Color(0xFF17233A)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = msg.text,
                                        color = if (isMe) Color.Black else Color.White,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Nhắn gì đi...", color = Color.White.copy(alpha = 0.3f))
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF16233B),
                    unfocusedContainerColor = Color(0xFF10192D),
                    focusedBorderColor = Color(0xFFF6E29A),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    cursorColor = Color(0xFFF6E29A)
                )
            )

            Button(
                onClick = {
                    if (input.isNotBlank()) {
                        onSend(input)
                        input = ""
                    }
                },
                enabled = input.isNotBlank(),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF6E29A),
                    contentColor = Color.Black,
                    disabledContainerColor = Color.White.copy(alpha = 0.08f),
                    disabledContentColor = Color.White.copy(alpha = 0.25f)
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Gửi")
            }
        }
    }
}
