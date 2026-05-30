package com.example.alphacinema.ui.watchparty

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Share
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.mutableFloatStateOf
import com.example.alphacinema.data.model.WatchPartyChatMessage
import com.example.alphacinema.data.model.WatchPartyMember
import com.example.alphacinema.ui.components.clearFocusOnTapOutside
import com.example.alphacinema.ui.movie.detail.EpisodeUi
import com.example.alphacinema.ui.player.findActivity
import com.example.alphacinema.ui.player.installBrightnessGesture
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
    val serverTimeOffset by viewModel.serverTimeOffset.collectAsState()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    val isHost = viewModel.isHost
    val currentUid = viewModel.currentUid
    
    val isMicMuted by viewModel.isMicMuted.collectAsState()
    val speakingUsers by viewModel.speakingUsers.collectAsState()
    val memberVolumes by viewModel.memberVolumes.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            room?.roomId?.let { viewModel.initAgora(context, it) }
        }
    }

    LaunchedEffect(room?.roomId) {
        if (room != null) {
            val hasPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                viewModel.initAgora(context, room!!.roomId)
            } else {
                permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // Guest time display
    var guestCurrentTimeMs by remember { mutableLongStateOf(0L) }
    var guestDurationMs by remember { mutableLongStateOf(0L) }

    // Fullscreen state
    var isFullscreen by remember { mutableStateOf(false) }
    var showEpisodeDialog by remember { mutableStateOf(false) }
    val activity = context.findActivity()

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

        // Force sync when a new member joins
        var previousMembersSize by remember { mutableStateOf(members.size) }
        LaunchedEffect(members.size) {
            if (members.size > previousMembersSize && previousMembersSize > 0) {
                // Wait 5 seconds to let the new guest load the video, then force a sync update
                delay(5000)
                val state = if (exoPlayer.isPlaying) "playing" else if (exoPlayer.playbackState == Player.STATE_READY) "paused" else return@LaunchedEffect
                viewModel.updatePlayback(state, exoPlayer.currentPosition / 1000.0)
            }
            previousMembersSize = members.size
        }
    }

    // Guest: Server Clock sync
    if (!isHost) {
        LaunchedEffect(room?.playbackState, room?.playStartedAt) {
            val r = room ?: return@LaunchedEffect
            when (r.playbackState) {
                "playing" -> {
                    val serverNow = System.currentTimeMillis() + serverTimeOffset
                    // Trick: Add 150ms (0.15s) offset because guests usually lag slightly behind
                    val pos = r.currentTimeSec + (serverNow - r.playStartedAt) / 1000.0 + 0.15
                    exoPlayer.seekTo((pos * 1000).toLong())
                    exoPlayer.playWhenReady = true
                    // Self-correction loop
                    while (true) {
                        delay(2000)
                        val cr = room ?: break
                        if (cr.playbackState != "playing") break
                        val now = System.currentTimeMillis() + serverTimeOffset
                        // Trick: Add 150ms (0.15s) offset because guests usually lag slightly behind
                        val exp = cr.currentTimeSec + (now - cr.playStartedAt) / 1000.0 + 0.15
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
            WindowInsetsControllerCompat(window, window.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            val window = activity?.window ?: return
            WindowInsetsControllerCompat(window, window.decorView)
                .show(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun toggleMicWithPermission() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            viewModel.toggleMic()
        } else {
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    BackHandler {
        when {
            isImeVisible -> focusManager.clearFocus()
            isFullscreen -> toggleFullscreen()
            else -> onBack()
        }
    }

    // Fullscreen mode
    if (isFullscreen) {
        var brightness by remember { mutableFloatStateOf(1f) }
        var showBrightnessIndicator by remember { mutableStateOf(false) }
        var visibleChats by remember { mutableStateOf<List<WatchPartyChatMessage>>(emptyList()) }
        val prevMsgCount = remember { mutableStateOf(chatMessages.size) }
        var showGuestControls by remember { mutableStateOf(true) }
        var controlsTrigger by remember { mutableStateOf(0) }

        LaunchedEffect(showBrightnessIndicator) {
            if (showBrightnessIndicator) { delay(1500); showBrightnessIndicator = false }
        }
        LaunchedEffect(chatMessages.size) {
            if (chatMessages.size > prevMsgCount.value) {
                val newMsgs = chatMessages.takeLast(chatMessages.size - prevMsgCount.value)
                visibleChats = (visibleChats + newMsgs).takeLast(4)
                prevMsgCount.value = chatMessages.size
                delay(4000)
                visibleChats = visibleChats.filterNot { it in newMsgs }
            } else { prevMsgCount.value = chatMessages.size }
        }
        LaunchedEffect(controlsTrigger, showGuestControls) {
            if (showGuestControls) {
                delay(3500)
                showGuestControls = false
            }
        }
        val overlayAlpha = (1f - brightness).coerceIn(0f, 0.85f)
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { ctx ->
                    (android.view.LayoutInflater.from(ctx).inflate(com.example.alphacinema.R.layout.custom_player_view, null) as androidx.media3.ui.PlayerView).apply {
                        player = exoPlayer
                        useController = isHost
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        setFullscreenButtonClickListener { toggleFullscreen() }
                        installBrightnessGesture { delta ->
                            brightness = (brightness + delta).coerceIn(0.05f, 1f)
                            showBrightnessIndicator = true
                        }

                        // Force custom 10s icons
                        val applyCustomIcons = {
                            val rewId = resources.getIdentifier("exo_rew", "id", context.packageName)
                            if (rewId != 0) findViewById<android.widget.ImageButton>(rewId)?.setImageResource(com.example.alphacinema.R.drawable.ic_replay_10)
                            val ffwdId = resources.getIdentifier("exo_ffwd", "id", context.packageName)
                            if (ffwdId != 0) findViewById<android.widget.ImageButton>(ffwdId)?.setImageResource(com.example.alphacinema.R.drawable.ic_forward_10)
                        }
                        applyCustomIcons()
                        setControllerVisibilityListener(
                            androidx.media3.ui.PlayerView.ControllerVisibilityListener { applyCustomIcons() }
                        )

                        // Episode selector button
                        val micBtn = findViewById<android.widget.ImageButton>(com.example.alphacinema.R.id.btn_mic_toggle)
                        micBtn?.visibility = android.view.View.VISIBLE
                        micBtn?.setImageResource(
                            if (isMicMuted) com.example.alphacinema.R.drawable.ic_mic_off
                            else com.example.alphacinema.R.drawable.ic_mic
                        )
                        micBtn?.setOnClickListener { toggleMicWithPermission() }

                        val epBtn = findViewById<android.widget.ImageButton>(com.example.alphacinema.R.id.btn_episode_selector)
                        if (isHost && episodes.size > 1) {
                            epBtn?.visibility = android.view.View.VISIBLE
                            epBtn?.setOnClickListener { showEpisodeDialog = true }
                        }

                        if (!isHost) {
                            isClickable = true
                            setOnClickListener {
                                showGuestControls = !showGuestControls
                                if (showGuestControls) {
                                    controlsTrigger++
                                }
                            }
                        }
                    }
                },
                update = { playerView ->
                    playerView.player = exoPlayer
                    playerView.findViewById<android.widget.ImageButton>(com.example.alphacinema.R.id.btn_mic_toggle)
                        ?.setImageResource(
                            if (isMicMuted) com.example.alphacinema.R.drawable.ic_mic_off
                            else com.example.alphacinema.R.drawable.ic_mic
                        )
                },
                modifier = Modifier.fillMaxSize()
            )
            // Brightness dim overlay (pass-through touches)
            if (overlayAlpha > 0.01f) {
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 1f }.background(Color.Black.copy(alpha = overlayAlpha)))
            }

            // CENTER: Brightness indicator (Netflix-style)
            AnimatedVisibility(
                visible = showBrightnessIndicator,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 14.dp, vertical = 16.dp)
                        .width(44.dp)
                ) {
                    Icon(
                        if (brightness > 0.5f) Icons.Rounded.BrightnessHigh else Icons.Rounded.BrightnessLow,
                        null, tint = Color.White, modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    // Vertical progress bar
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(100.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(brightness)
                                .align(Alignment.BottomCenter)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${(brightness * 100).toInt()}%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Members overlay (top-right)
            if (room != null) {
                Row(
                    modifier = Modifier.padding(16.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 12.dp, vertical = 8.dp).align(Alignment.TopEnd),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.Groups, null, tint = Color(0xFFF6E29A), modifier = Modifier.size(16.dp))
                    Text("${members.size}/5", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
            // Guest overlays
            AnimatedVisibility(
                visible = !isHost && showGuestControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val fmt = { ms: Long -> val ts = ms / 1000; val h = ts / 3600; val m = (ts % 3600) / 60; val s = ts % 60; if (h > 0) "$h:${"%02d".format(m)}:${"%02d".format(s)}" else "$m:${"%02d".format(s)}" }
                    Text(
                        text = "${fmt(guestCurrentTimeMs)} / ${fmt(guestDurationMs)}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { toggleMicWithPermission() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isMicMuted) Color.Red.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = if (isMicMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                                contentDescription = "Mic",
                                tint = if (isMicMuted) Color.White else Color(0xFFF6E29A)
                            )
                        }

                        IconButton(
                            onClick = { toggleFullscreen() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.Rounded.FullscreenExit, "Thu nhỏ", tint = Color.White)
                        }
                    }
                }
            }

            // RIGHT: Floating chat toasts
            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 56.dp).widthIn(max = 220.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.End
            ) {
                visibleChats.forEach { msg ->
                    androidx.compose.runtime.key(msg.id) {
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.65f)).padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(msg.displayName.split(" ").lastOrNull() ?: "", color = Color(0xFFF6E29A), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(msg.text, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
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
                            .background(Color(0xFF1F1F1F).copy(alpha = 0.75f))
                            .padding(16.dp)
                            .clickable(enabled = false) {}
                    ) {
                        Text(
                            "Chọn tập",
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
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color.Black)
            .clearFocusOnTapOutside()
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
                            val rewId = resources.getIdentifier("exo_rew", "id", context.packageName)
                            if (rewId != 0) findViewById<android.widget.ImageButton>(rewId)?.setImageResource(com.example.alphacinema.R.drawable.ic_replay_10)
                            val ffwdId = resources.getIdentifier("exo_ffwd", "id", context.packageName)
                            if (ffwdId != 0) findViewById<android.widget.ImageButton>(ffwdId)?.setImageResource(com.example.alphacinema.R.drawable.ic_forward_10)
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
                
                IconButton(
                    onClick = { toggleFullscreen() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Rounded.Fullscreen, "Toàn màn hình", tint = Color.White)
                }
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
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Quay lại", tint = Color.White)
            }
        }

        // Room info + share
        var showInviteDialog by remember { mutableStateOf(false) }

        if (!isImeVisible) {
            Column {
                if (room != null) {
                    val hasMovie = room!!.movieSlug.isNotBlank()
                    RoomInfoBar(
                        movieTitle = if (hasMovie) room!!.movieTitle else "Chưa chọn phim"
                    )

                    // Invite popup
                    if (showInviteDialog) {
                        androidx.compose.ui.window.Dialog(
                            onDismissRequest = { showInviteDialog = false },
                            properties = androidx.compose.ui.window.DialogProperties(
                                usePlatformDefaultWidth = false
                            )
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(0.9f),
                                shape = RoundedCornerShape(30.dp),
                                color = Color(0xFF1F1F1F),
                                shadowElevation = 0.dp,
                                tonalElevation = 0.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        "Mời bạn bè",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp,
                                        lineHeight = 28.sp
                                    )

                                    // Option 1: Copy room code
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                clipboardManager.setText(AnnotatedString(room!!.roomId))
                                                android.widget.Toast.makeText(context, "Đã sao chép mã phòng", android.widget.Toast.LENGTH_SHORT).show()
                                                showInviteDialog = false
                                            }
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.08f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = Color(0xFFF6E29A))
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text("Sao chép mã phòng", color = Color.White, fontWeight = FontWeight.SemiBold)
                                            Text(room!!.roomId, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                        }
                                    }

                                    // Option 2: Share link (if you have deep links)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val shareLink = com.example.alphacinema.ui.app.watchPartyShareLink(room!!.roomId)
                                                val movieTitle = room!!.movieTitle.ifBlank { "Xem chung" }
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, shareLink)
                                                    putExtra(Intent.EXTRA_TITLE, movieTitle)
                                                    putExtra(Intent.EXTRA_SUBJECT, movieTitle)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Chia sẻ qua"))
                                                showInviteDialog = false
                                            }
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.08f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Rounded.Share, contentDescription = null, tint = Color(0xFFF6E29A))
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text("Chia sẻ qua ứng dụng", color = Color.White, fontWeight = FontWeight.SemiBold)
                                            Text("Mở thẳng phòng xem chung", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val showEpisodeSelector = isHost && episodes.size > 1
                if (showEpisodeSelector) {
                    EpisodeSelector(
                        episodes = episodes,
                        currentEpisodeId = currentEpisodeId,
                        onSelectEpisode = { ep -> viewModel.changeEpisode(ep.id, ep.name) }
                    )
                }

                if (room != null) {
                    val hasMovie = room!!.movieSlug.isNotBlank()
                    WatchPartyActionRow(
                        hasMovie = hasMovie,
                        isHost = isHost,
                        isMicMuted = isMicMuted,
                        onToggleMic = ::toggleMicWithPermission,
                        onShare = { showInviteDialog = true },
                        onChangeMovie = onChangeMovieClick
                    )
                }

                // Members
                if (members.isNotEmpty()) {
                    MembersList(
                        members = members,
                        hostId = room?.hostId ?: "",
                        currentUid = currentUid,
                        speakingUsers = speakingUsers,
                        memberVolumes = memberVolumes,
                        onAdjustVolume = { uid, vol -> viewModel.adjustUserVolume(uid, vol) }
                    )
                }
            }
        }

        // Chat
        ChatSection(
            messages = chatMessages,
            members = members,
            currentUid = currentUid ?: "",
            onSend = { viewModel.sendMessage(it) },
            modifier = Modifier.weight(1f)
        )
    }
}


@Composable
private fun RoomInfoBar(
    movieTitle: String
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
    }
}

@Composable
private fun WatchPartyActionRow(
    hasMovie: Boolean,
    isHost: Boolean,
    isMicMuted: Boolean,
    onToggleMic: () -> Unit,
    onShare: () -> Unit,
    onChangeMovie: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            WatchPartyActionButton(
                icon = if (isMicMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                label = if (isMicMuted) "Tắt mic" else "Mic",
                tint = if (isMicMuted) Color.Red else Color(0xFFF6E29A),
                onClick = onToggleMic
            )

            WatchPartyActionButton(
                icon = Icons.Rounded.Share,
                label = "Mời bạn bè",
                tint = Color(0xFFF6E29A),
                onClick = onShare
            )

            // Change / Choose Movie button
            if (isHost) {
                val btnLabel = if (hasMovie) "Đổi phim" else "Chọn phim"
                val btnTint = if (hasMovie) Color.White else Color(0xFFF6E29A)
                WatchPartyActionButton(
                    icon = Icons.Rounded.Movie,
                    label = btnLabel,
                    tint = btnTint,
                    onClick = onChangeMovie
                )
            }
        }
    }
}

@Composable
private fun WatchPartyActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .widthIn(min = 54.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
                .border(1.dp, tint.copy(alpha = 0.28f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.75f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MembersList(
    members: List<WatchPartyMember>, 
    hostId: String,
    currentUid: String?,
    speakingUsers: Map<Int, Int>,
    memberVolumes: Map<String, Int>,
    onAdjustVolume: (String, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Thành viên",
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            members.forEach { member ->
                androidx.compose.runtime.key(member.uid) {
                    val memberIsHost = member.uid == hostId
                    val agoraUid = member.uid.hashCode() and 0x7FFFFFFF
                    val isSpeaking = speakingUsers.containsKey(agoraUid)
                    val canAdjustVolume = member.uid != currentUid
                    var showVolumePopup by remember { mutableStateOf(false) }
                    val volume = memberVolumes[member.uid] ?: 100

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
                                        listOf(Color(0xFF1F1F1F), Color(0xFF1F1F1F))
                                    )
                                )
                                .then(
                                    if (isSpeaking) Modifier.border(2.dp, Color(0xFF4CAF50), CircleShape)
                                    else if (memberIsHost) Modifier.border(2.dp, Color(0xFFF6E29A), CircleShape)
                                    else Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                                )
                                .clickable(enabled = canAdjustVolume) { showVolumePopup = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (member.photoUrl.isNotBlank()) {
                                coil.compose.AsyncImage(
                                    model = member.photoUrl,
                                    contentDescription = member.displayName,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = member.displayName.firstOrNull()?.uppercase() ?: "?",
                                    color = if (memberIsHost) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
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

                    if (showVolumePopup && canAdjustVolume) {
                        MemberVolumeDialog(
                            member = member,
                            volume = volume,
                            onVolumeChange = { onAdjustVolume(member.uid, it) },
                            onDismiss = { showVolumePopup = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomVolumeSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val icon = when {
            value <= 0 -> Icons.Rounded.VolumeOff
            value < 33 -> Icons.Rounded.VolumeDown
            else -> Icons.Rounded.VolumeUp
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(22.dp)
        )

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(32.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val progress = (offset.x / size.width).coerceIn(0f, 1f)
                        onValueChange((progress * 100).toInt())
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val width = size.width.toFloat()
                        if (width > 0f) {
                            val newProgress = ((value / 100f) + (dragAmount.x / width)).coerceIn(0f, 1f)
                            onValueChange((newProgress * 100).toInt())
                        }
                    }
                }
        ) {
            val width = maxWidth
            val progressWidth = width * (value / 100f)

            // Track background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                // Active track
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(progressWidth)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFF6E29A), Color(0xFFD4A843))
                            )
                        )
                )
            }

            // Thumb
            val thumbSize = 14.dp
            val offset = (progressWidth - (thumbSize / 2)).coerceAtLeast(0.dp).coerceAtMost(width - thumbSize)

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = offset)
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(Color(0xFFF6E29A))
                    .border(2.dp, Color(0xFF181818), CircleShape)
            )
        }
    }
}

@Composable
private fun MemberVolumeDialog(
    member: WatchPartyMember,
    volume: Int,
    onVolumeChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF181818))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(24.dp))
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Âm lượng",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            CustomVolumeSlider(
                value = volume,
                onValueChange = onVolumeChange,
                modifier = Modifier.fillMaxWidth()
            )
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
            text = "Chọn tập",
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
    members: List<WatchPartyMember>,
    currentUid: String,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val memberMap = remember(members) { members.associateBy { it.uid } }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Rounded.Groups,
                contentDescription = null,
                tint = Color(0xFFF6E29A),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Trò chuyện",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (messages.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF6E29A).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        "${messages.size}",
                        color = Color(0xFFF6E29A),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Messages area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .background(Color.Black)
        ) {
            if (messages.isEmpty()) {
                Spacer(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(messages.size) { index ->
                        val msg = messages[index]
                        val isMe = msg.uid == currentUid
                        val prevMsg = if (index > 0) messages[index - 1] else null
                        val nextMsg = if (index < messages.size - 1) messages[index + 1] else null
                        val isFirstInGroup = prevMsg == null || prevMsg.uid != msg.uid ||
                                (msg.timestamp - prevMsg.timestamp > 60_000)
                        val isLastInGroup = nextMsg == null || nextMsg.uid != msg.uid ||
                                (nextMsg.timestamp - msg.timestamp > 60_000)
                        val member = memberMap[msg.uid]

                        // Add spacing between groups
                        if (isFirstInGroup && index > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        ChatBubble(
                            message = msg,
                            isMe = isMe,
                            isFirstInGroup = isFirstInGroup,
                            isLastInGroup = isLastInGroup,
                            avatarUrl = member?.photoUrl ?: ""
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input bar
        val hasText = input.isNotBlank()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .imePadding()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF1F1F1F), Color(0xFF242424))
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 42.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    lineHeight = 20.sp
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (hasText) {
                            onSend(input)
                            input = ""
                        }
                    }
                ),
                cursorBrush = SolidColor(Color(0xFFF6E29A)),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (input.isBlank()) {
                            Text(
                                "Nhắn gì đi...",
                                color = Color.White.copy(alpha = 0.42f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // Send button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (hasText) Color(0xFFF6E29A) else Color.White.copy(alpha = 0.06f)
                    )
                    .clickable(enabled = hasText) {
                        onSend(input)
                        input = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Gửi",
                    tint = if (hasText) Color.Black else Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: WatchPartyChatMessage,
    isMe: Boolean,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    avatarUrl: String
) {
    val timeText = remember(message.timestamp) {
        if (message.timestamp > 0) {
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            sdf.format(java.util.Date(message.timestamp))
        } else ""
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Avatar for others (only show on last message in group)
        if (!isMe) {
            if (isLastInGroup) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F1F1F)),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl.isNotBlank()) {
                        coil.compose.AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            message.displayName.firstOrNull()?.uppercase() ?: "?",
                            color = Color(0xFFF6E29A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 240.dp),
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            // Name (only first in group, only for others)
            if (!isMe && isFirstInGroup) {
                Text(
                    text = message.displayName,
                    color = Color(0xFFF6E29A).copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 3.dp)
                )
            }

            // Bubble
            val bubbleShape = RoundedCornerShape(
                topStart = if (!isMe && isFirstInGroup) 18.dp else if (!isMe) 6.dp else 18.dp,
                topEnd = if (isMe && isFirstInGroup) 18.dp else if (isMe) 6.dp else 18.dp,
                bottomStart = if (!isMe && isLastInGroup) 4.dp else if (!isMe) 6.dp else 18.dp,
                bottomEnd = if (isMe && isLastInGroup) 4.dp else if (isMe) 6.dp else 18.dp
            )

            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .background(
                        if (isMe) Color(0xFFF6E29A) else Color(0xFF242424)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column {
                    Text(
                        text = message.text,
                        color = if (isMe) Color.Black else Color.White,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    // Timestamp on last message of group
                    if (isLastInGroup && timeText.isNotEmpty()) {
                        Text(
                            text = timeText,
                            color = if (isMe) Color.Black.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.35f),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        // Right spacer for others to balance layout
        if (!isMe) {
            Spacer(modifier = Modifier.weight(1f, fill = false))
        }
    }
}
