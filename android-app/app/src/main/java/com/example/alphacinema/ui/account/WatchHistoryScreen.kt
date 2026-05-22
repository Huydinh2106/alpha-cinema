package com.example.alphacinema.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.alphacinema.data.model.WatchHistoryItem
import com.example.alphacinema.data.repository.FirestoreRepository
import com.example.alphacinema.ui.components.AlphaCinemaImage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun WatchHistoryScreen(
    onBack: () -> Unit,
    onOpenWatchHistoryItem: (movieSlug: String, episodeId: String?, startPositionMs: Long) -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val firestoreRepository = remember { FirestoreRepository() }
    val userId = auth.currentUser?.uid
    val watchHistoryFlow = remember(userId) {
        userId?.let(firestoreRepository::getWatchHistory) ?: flowOf(emptyList())
    }
    val watchHistory by watchHistoryFlow.collectAsState(initial = emptyList())
    val dateGroups = remember(watchHistory) { watchHistory.groupByWatchDate() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                }
                Text(
                    text = "Lịch sử xem",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp)
                )
            }

            if (dateGroups.isEmpty()) {
                WatchHistoryEmptyState()
            } else {
                dateGroups.forEach { group ->
                    WatchHistoryDateSection(
                        title = group.label,
                        items = group.items,
                        onOpenItem = { item ->
                            onOpenWatchHistoryItem(
                                item.movieId,
                                item.episodeId.takeIf { it.isNotBlank() },
                                item.playbackStartPositionMs()
                            )
                        }
                    )
                }
            }
        }
    }
}

private data class WatchHistoryDateGroup(
    val label: String,
    val items: List<WatchHistoryItem>
)

private const val WATCH_COMPLETE_THRESHOLD_MS = 30_000L
private const val UNKNOWN_WATCH_DAY_KEY = Long.MIN_VALUE
private val VIETNAMESE_LOCALE: Locale = Locale.forLanguageTag("vi-VN")

private fun List<WatchHistoryItem>.groupByWatchDate(): List<WatchHistoryDateGroup> {
    return filter { it.movieId.isNotBlank() && it.movieName.isNotBlank() }
        .groupBy { item -> item.watchDayKey() }
        .map { (dayKey, items) ->
            WatchHistoryDateGroup(
                label = formatWatchHistoryDay(dayKey),
                items = items
            )
        }
}

private fun WatchHistoryItem.watchDayKey(): Long {
    val watchedAt = lastWatchedAt?.toDate() ?: return UNKNOWN_WATCH_DAY_KEY
    return startOfDayMillis(watchedAt.time)
}

private fun WatchHistoryItem.isCompleted(): Boolean {
    val safeDuration = duration.coerceAtLeast(0L)
    if (safeDuration == 0L) return false

    val safeProgress = progress.coerceAtLeast(0L)
    val completionCutoff = (safeDuration - WATCH_COMPLETE_THRESHOLD_MS)
        .coerceAtMost((safeDuration * 9L) / 10L)
        .coerceAtLeast(0L)
    return safeProgress >= completionCutoff
}

private fun WatchHistoryItem.progressFraction(): Float? {
    val safeDuration = duration.coerceAtLeast(0L)
    if (safeDuration == 0L) return null
    return (progress.coerceAtLeast(0L).toFloat() / safeDuration.toFloat())
        .coerceIn(0f, 1f)
}

private fun WatchHistoryItem.playbackStartPositionMs(): Long {
    if (isCompleted()) return 0L
    return (progress.coerceAtLeast(0L) - 3_000L).coerceAtLeast(0L)
}

private fun WatchHistoryItem.historySubtitle(): String {
    val episodeLabel = episodeName.ifBlank { "Nội dung đã xem" }
    return if (isCompleted()) "$episodeLabel - Đã xem xong" else episodeLabel
}

private fun WatchHistoryItem.historyMetaLabel(): String {
    if (isCompleted()) return "Phát lại từ đầu"

    val safeDuration = duration.coerceAtLeast(0L)
    if (safeDuration == 0L) return "Đang xem"

    val percent = ((progress.coerceAtLeast(0L) * 100L) / safeDuration)
        .coerceIn(0L, 100L)
    val timeLabel = formatWatchPosition(playbackStartPositionMs())
    return "$timeLabel - $percent% đã xem"
}

private fun formatWatchHistoryDay(dayKey: Long): String {
    if (dayKey == UNKNOWN_WATCH_DAY_KEY) return "Chưa rõ ngày"

    val today = startOfDayMillis(System.currentTimeMillis())
    val yesterday = Calendar.getInstance().apply {
        timeInMillis = today
        add(Calendar.DATE, -1)
    }.timeInMillis

    return when (dayKey) {
        today -> "Hôm nay"
        yesterday -> "Hôm qua"
        else -> SimpleDateFormat("EEEE, dd/MM/yyyy", VIETNAMESE_LOCALE)
            .format(Date(dayKey))
            .replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(VIETNAMESE_LOCALE) else char.toString()
            }
    }
}

private fun startOfDayMillis(timeMillis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun formatWatchPosition(positionMs: Long): String {
    val totalSeconds = (positionMs.coerceAtLeast(0L) / 1000L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    val secondsText = seconds.toString().padStart(2, '0')
    return if (hours > 0L) {
        val minutesText = minutes.toString().padStart(2, '0')
        "$hours:$minutesText:$secondsText"
    } else {
        "$minutes:$secondsText"
    }
}

@Composable
private fun WatchHistoryDateSection(
    title: String,
    items: List<WatchHistoryItem>,
    onOpenItem: (WatchHistoryItem) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.5f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        items.forEachIndexed { index, item ->
            WatchHistoryMediaRow(
                title = item.movieName,
                subtitle = item.historySubtitle(),
                meta = item.historyMetaLabel(),
                posterUrl = item.posterUrl,
                progressFraction = item.progressFraction(),
                onClick = { onOpenItem(item) }
            )
            if (index != items.lastIndex) {
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.08f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(start = 72.dp)
                )
            }
        }
    }
}

@Composable
private fun WatchHistoryMediaRow(
    title: String,
    subtitle: String,
    meta: String,
    posterUrl: String,
    progressFraction: Float?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlphaCinemaImage(
            model = posterUrl,
            contentDescription = title,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = meta,
                color = Color(0xFFF6E29A),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (progressFraction != null) {
                Spacer(modifier = Modifier.height(7.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                            .height(3.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFF6E29A))
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.35f),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun WatchHistoryEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Bạn chưa có lịch sử xem nào.",
            color = Color.White.copy(alpha = 0.58f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}
