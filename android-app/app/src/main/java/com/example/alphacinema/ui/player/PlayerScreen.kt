package com.example.alphacinema.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
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
import com.example.alphacinema.ui.movie.detail.EpisodeUi
import com.example.alphacinema.ui.movie.detail.MovieDetailUi

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun PlayerScreen(
    movie: MovieDetailUi,
    episode: EpisodeUi?,
    onBack: () -> Unit,
    videoUrl: String = "",
    episodeVideoUrls: Map<String, String> = emptyMap(),
    onSelectEpisode: (EpisodeUi) -> Unit = {}
) {
    val context = LocalContext.current

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
            
            // Tối ưu tốc độ 
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.allowFileAccess = true
            
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            setBackgroundColor(android.graphics.Color.BLACK)
        }
    }

    // Xoay ngang màn hình
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            webView.stopLoading()
            webView.destroy()
        }
    }

    // Load video when URL changes
    DisposableEffect(videoUrl) {
        if (videoUrl.isNotBlank()) {
            if (videoUrl.contains(".m3u8")) {
                val html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                        <style>
                            * { margin: 0; padding: 0; box-sizing: border-box; }
                            body { background: #000; display: flex; align-items: center; justify-content: center; height: 100vh; overflow: hidden; }
                            video { width: 100%; height: 100%; object-fit: contain; background: #000; outline: none; }
                            video::-webkit-media-controls-enclosure { border-radius: 0; }
                        </style>
                    </head>
                    <body>
                        <video id="video" controls autoplay playsinline poster="data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"></video>
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
                                hls.on(Hls.Events.MANIFEST_PARSED, function() { video.play(); });
                            } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
                                video.src = videoSrc;
                                video.addEventListener('loadedmetadata', function() { video.play(); });
                            }
                        </script>
                    </body>
                    </html>
                """.trimIndent()
                webView.loadDataWithBaseURL("https://phimapi.com", html, "text/html", "utf-8", null)
            } else {
                webView.loadUrl(videoUrl)
            }
        }
        onDispose { }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B16))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Video player
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f) // Ratio chuẩn cho player
                        .background(Color.Black)
                ) {
                    if (videoUrl.isNotBlank()) {
                        AndroidView(
                            factory = { webView },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // No video - show placeholder
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E2436)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.PlayArrow,
                                        contentDescription = null,
                                        tint = Color(0xFFF6E29A),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Đang tải nguồn phát...",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Tấm nền Gradient (nằm đè lên trên player) để làm nền cho nút quay lại
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

                    // Nút Back
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
                }
            }

            // Movie info
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp)
                ) {
                    Text(
                        text = movie.title,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = episode?.name ?: "Phát nội dung chính",
                        color = Color(0xFFF6E29A),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (movie.subtitle.isNotBlank()) {
                        Text(
                            text = movie.subtitle,
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Episode list
            if (movie.episodes.size > 1) {
                item {
                    Text(
                        text = "Danh sách tập",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp)
                    )
                }

                items(movie.episodes) { ep ->
                    val isCurrent = ep.id == episode?.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isCurrent) Color(0x22F6E29A) else Color(0xFF131A2F))
                            .border(
                                1.dp,
                                if (isCurrent) Color(0x66F6E29A) else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectEpisode(ep) }
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isCurrent) Color(0xFFF6E29A) else Color(0xFF1A2237)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                tint = if (isCurrent) Color.Black else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ep.name,
                                color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.8f),
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (ep.duration.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = ep.duration,
                                    color = Color.White.copy(alpha = 0.45f),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                        if (isCurrent) {
                            Text(
                                "Đang phát",
                                color = Color(0xFFF6E29A),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // Bottom padding
            item { Spacer(modifier = Modifier.height(48.dp)) }
        }
    }
}
