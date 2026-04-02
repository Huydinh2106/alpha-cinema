package com.example.alphacinema

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable

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
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            setBackgroundColor(android.graphics.Color.BLACK)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    // Load video when URL changes
    DisposableEffect(videoUrl) {
        if (videoUrl.isNotBlank()) {
            if (videoUrl.contains(".m3u8")) {
                // HLS stream - use hls.js player
                val html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
                        <style>
                            * { margin: 0; padding: 0; box-sizing: border-box; }
                            body { background: #000; display: flex; align-items: center; justify-content: center; height: 100vh; }
                            video { width: 100%; max-height: 100vh; background: #000; }
                        </style>
                    </head>
                    <body>
                        <video id="video" controls autoplay playsinline></video>
                        <script src="https://cdn.jsdelivr.net/npm/hls.js@latest"></script>
                        <script>
                            var video = document.getElementById('video');
                            var videoSrc = '${videoUrl}';
                            if (Hls.isSupported()) {
                                var hls = new Hls();
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
                // Embed URL
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
                        .height(240.dp)
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
                                Icon(
                                    Icons.Outlined.PlayArrow,
                                    contentDescription = null,
                                    tint = Color(0xFFF6E29A),
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    "Không tìm thấy nguồn phát",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // Movie info
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = movie.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = episode?.name ?: "Phát nội dung chính",
                        color = Color(0xFFF6E29A),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (movie.subtitle.isNotBlank()) {
                        Text(
                            text = movie.subtitle,
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodySmall
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
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                items(movie.episodes) { ep ->
                    val isCurrent = ep.id == episode?.id
                    val epVideoUrl = episodeVideoUrls[ep.id]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCurrent) Color(0x22F6E29A) else Color.White.copy(alpha = 0.06f))
                            .border(
                                1.dp,
                                if (isCurrent) Color(0x66F6E29A) else Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectEpisode(ep) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isCurrent) Color(0xFFF6E29A) else Color.White.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                tint = if (isCurrent) Color.Black else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ep.name,
                                color = Color.White,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (ep.duration.isNotBlank()) {
                                Text(
                                    text = ep.duration,
                                    color = Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        if (isCurrent) {
                            Text(
                                "Đang xem",
                                color = Color(0xFFF6E29A),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Bottom padding
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }

        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(start = 8.dp, top = 8.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
    }
}
