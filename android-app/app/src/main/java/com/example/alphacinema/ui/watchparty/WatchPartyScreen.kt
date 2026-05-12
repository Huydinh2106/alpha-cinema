package com.example.alphacinema.ui.watchparty

import android.content.Intent
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient

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
import com.example.alphacinema.data.model.WatchPartyChatMessage
import com.example.alphacinema.data.model.WatchPartyMember
import com.example.alphacinema.ui.movie.detail.EpisodeUi


// JS Bridge for host to report playback changes
class WatchPartyJsBridge(
    private val viewModel: WatchPartyViewModel,
    private val getCurrentRoom: () -> com.example.alphacinema.data.model.WatchPartyRoom?
) {
    @JavascriptInterface
    fun onPlay(timeSec: Double) {
        viewModel.updatePlayback("playing", timeSec)
    }

    @JavascriptInterface
    fun onPause(timeSec: Double) {
        viewModel.updatePlayback("paused", timeSec)
    }

    @JavascriptInterface
    fun onSeek(timeSec: Double) {
        val state = getCurrentRoom()?.playbackState ?: "paused"
        viewModel.updatePlayback(state, timeSec)
    }

    @JavascriptInterface
    fun onTimeUpdate(timeSec: Double) {
        // Periodic update from host
        viewModel.updatePlayback("playing", timeSec)
    }
}

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

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val isHost = viewModel.isHost
    val currentUid = viewModel.currentUid

    // Room dismissed → host left
    LaunchedEffect(roomDismissed) {
        if (roomDismissed) {
            android.widget.Toast.makeText(context, "Chủ phòng đã giải tán phòng", android.widget.Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    // WebView with JS bridge
    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            setBackgroundColor(android.graphics.Color.BLACK)
        }
    }

    // JS Bridge for host to report playback changes

    val jsBridge = remember(viewModel) {
        WatchPartyJsBridge(viewModel) { room }
    }

    // Add JS interface
    @SuppressLint("JavascriptInterface")
    LaunchedEffect(jsBridge) {
        webView.addJavascriptInterface(jsBridge, "AndroidBridge")
    }

    // Load video
    DisposableEffect(videoUrl) {
        if (videoUrl.isNotBlank() && videoUrl.contains(".m3u8")) {
            val hostControls = if (isHost) """
                video.addEventListener('play', function() {
                    AndroidBridge.onPlay(video.currentTime);
                });
                video.addEventListener('pause', function() {
                    AndroidBridge.onPause(video.currentTime);
                });
                video.addEventListener('seeked', function() {
                    AndroidBridge.onSeek(video.currentTime);
                });
                // Report time every 3 seconds
                setInterval(function() {
                    if (!video.paused) {
                        AndroidBridge.onTimeUpdate(video.currentTime);
                    }
                }, 3000);
            """ else """
                // Guest: disable controls, only host can control
                video.removeAttribute('controls');
                video.style.pointerEvents = 'none';
            """

            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                    <style>
                        * { margin: 0; padding: 0; box-sizing: border-box; }
                        body { background: #000; display: flex; align-items: center; justify-content: center; height: 100vh; overflow: hidden; }
                        video { width: 100%; height: 100%; object-fit: contain; background: #000; outline: none; }
                    </style>
                </head>
                <body>
                    <video id="video" ${if (isHost) "controls" else ""} autoplay playsinline></video>
                    <script src="https://cdn.jsdelivr.net/npm/hls.js@latest"></script>
                    <script>
                        var video = document.getElementById('video');
                        var videoSrc = '${videoUrl}';
                        if (Hls.isSupported()) {
                            var hls = new Hls({
                                maxBufferLength: 30,
                                maxMaxBufferLength: 600,
                                enableWorker: true,
                                lowLatencyMode: true
                            });
                            hls.loadSource(videoSrc);
                            hls.attachMedia(video);
                            hls.on(Hls.Events.MANIFEST_PARSED, function() {
                                ${if (isHost) "video.play();" else ""}
                            });
                        } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
                            video.src = videoSrc;
                        }

                        // Expose sync functions for guest
                        function seekTo(sec) {
                            video.currentTime = sec;
                        }
                        function playVideo() {
                            video.play();
                        }
                        function pauseVideo() {
                            video.pause();
                        }

                        $hostControls
                    </script>
                </body>
                </html>
            """.trimIndent()
            webView.loadDataWithBaseURL("https://phimapi.com", html, "text/html", "utf-8", null)
        }
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    // Guest sync: react to room state changes
    if (!isHost && room != null) {
        LaunchedEffect(room?.playbackState, room?.currentTimeSec) {
            val r = room ?: return@LaunchedEffect
            when (r.playbackState) {
                "playing" -> {
                    webView.evaluateJavascript("seekTo(${r.currentTimeSec}); playVideo();", null)
                }
                "paused" -> {
                    webView.evaluateJavascript("seekTo(${r.currentTimeSec}); pauseVideo();", null)
                }
            }
        }
    }

    // Layout — Column để chat chiếm hết phần dưới
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B16))
    ) {
        // Video player
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize()
            )

            // Gradient overlay for back button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
                    .align(Alignment.TopCenter)
            )

            // Back button
            IconButton(
                onClick = {
                    viewModel.leaveRoom()
                    onBack()
                },
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 8.dp, start = 12.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Rời phòng",
                    tint = Color.White
                )
            }

            // Room info badge
            if (room != null) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 12.dp, end = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .align(Alignment.TopEnd),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Outlined.Groups,
                        contentDescription = null,
                        tint = Color(0xFFF6E29A),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${members.size}/5",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Guest overlay hint
            if (!isHost) {
                Text(
                    text = "Chủ phòng đang điều khiển",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Room info + share
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
                onShare = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(
                            Intent.EXTRA_TEXT,
                            if (hasMovie) "Xem phim \"${room!!.movieTitle}\" cùng tôi trên AlphaCinema! Mã phòng: ${room!!.roomId}"
                            else "Xem phim cùng tôi trên AlphaCinema! Mã phòng: ${room!!.roomId}"
                        )
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Mời bạn bè xem chung"))
                },
                onChangeMovie = onChangeMovieClick
            )
        }

        // Members
        if (members.isNotEmpty()) {
            MembersList(members = members, hostId = room?.hostId ?: "")
        }

        // Episode selector (host only, khi có nhiều tập)
        if (isHost && episodes.size > 1) {
            EpisodeSelector(
                episodes = episodes,
                currentEpisodeId = currentEpisodeId,
                onSelectEpisode = { ep ->
                    viewModel.changeEpisode(ep.id, ep.name)
                }
            )
        }

        // Chat — chiếm hết phần còn lại ở dưới cùng
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
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = movieTitle,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Mã phòng:",
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = roomId,
                color = Color(0xFFF6E29A),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                letterSpacing = 3.sp
            )
            Icon(
                Icons.Outlined.ContentCopy,
                contentDescription = "Sao chép",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onCopyId() }
            )
        }

        if (isHost) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Bạn là chủ phòng — chỉ bạn điều khiển video",
                color = Color(0xFFF6E29A).copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Share button
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
                    contentDescription = "Chia sẻ",
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
            .padding(top = 12.dp)
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
