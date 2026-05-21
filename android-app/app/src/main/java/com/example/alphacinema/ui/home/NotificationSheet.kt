@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.alphacinema.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

// ── Color Palette ────────────────────────────────────────────────────────────
private val DarkBg = Color(0xFF070B16)
private val CardBg = Color(0xFF141414)
private val CardBorder = Color(0xFF333333)
private val GoldAccent = Color(0xFFF6E29A)
private val TextPrimary = Color.White
private val TextSecondary = Color.White.copy(alpha = 0.7f)
private val TextMuted = Color.White.copy(alpha = 0.38f)
private val DividerColor = Color.White.copy(alpha = 0.08f)

// ── Notification Types ───────────────────────────────────────────────────────
enum class NotificationType(
    val label: String,
    val icon: ImageVector,
    val color: Color
) {
    MARKETING(
        label = "Khuyến mãi",
        icon = Icons.Outlined.Movie,
        color = Color(0xFFF6E29A)
    ),
    TRANSACTION(
        label = "Giao dịch",
        icon = Icons.Outlined.Payment,
        color = Color(0xFF4ADE80)
    ),
    SOCIAL(
        label = "Xã hội",
        icon = Icons.Outlined.Movie,
        color = Color(0xFF60A5FA)
    ),
    SYSTEM(
        label = "Hệ thống",
        icon = Icons.Outlined.SystemUpdate,
        color = Color(0xFFA78BFA)
    ),
    NEW_MOVIE(
        label = "Phim mới",
        icon = Icons.Outlined.Movie,
        color = Color(0xFFFB7185)
    )
}

// ── Data Model ───────────────────────────────────────────────────────────────
data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val rawType: String = "",
    val title: String,
    val message: String,
    val timeAgo: String,
    val isRead: Boolean = false,
    val movieId: String? = null,
    val plan: String? = null,
    val imageUrl: String? = null
)

// ═════════════════════════════════════════════════════════════════════════════
// MAIN COMPOSABLE: NotificationScreen (Full-screen overlay)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun NotificationScreen(
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onNavigateToMovie: (String) -> Unit = {},
    onNavigateToPlan: () -> Unit = {}
) {
    // ── State ────────────────────────────────────────────────────────────────
    val notifications = remember { mutableStateListOf<NotificationItem>() }

    val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    var currentUid by remember { mutableStateOf(auth.currentUser?.uid) }

    androidx.compose.runtime.DisposableEffect(Unit) {
        val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUid = firebaseAuth.currentUser?.uid
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    androidx.compose.runtime.DisposableEffect(currentUid) {
        val uid = currentUid
        if (uid == null) {
            notifications.clear()
            return@DisposableEffect onDispose {}
        }
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val listener = db.collection("users").document(uid).collection("notifications")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("NotificationSheet", "Listen failed.", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        val title = doc.getString("title") ?: ""
                        val message = doc.getString("body") ?: ""
                        val typeString = doc.getString("type") ?: "system"
                        val isRead = doc.getBoolean("isRead") ?: false
                        
                        val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis()
                        val diff = System.currentTimeMillis() - timestamp
                        val timeAgo = when {
                            diff < 60000 -> "Vừa xong"
                            diff < 3600000 -> "${diff / 60000} phút trước"
                            diff < 86400000 -> "${diff / 3600000} giờ trước"
                            else -> "${diff / 86400000} ngày trước"
                        }
                        
                        val type = when (typeString) {
                            "series_update", "new_movie" -> NotificationType.NEW_MOVIE
                            "billing", "transaction" -> NotificationType.TRANSACTION
                            "security_alert", "system" -> NotificationType.SYSTEM
                            "social" -> NotificationType.SOCIAL
                            "marketing" -> NotificationType.MARKETING
                            else -> NotificationType.SYSTEM
                        }

                        val movieId = doc.getString("movieId")
                        val plan = doc.getString("plan")
                        val imageUrl = doc.getString("imageUrl")

                        NotificationItem(
                            id = doc.id,
                            type = type,
                            rawType = typeString,
                            title = title,
                            message = message,
                            timeAgo = timeAgo,
                            isRead = isRead,
                            movieId = movieId,
                            plan = plan,
                            imageUrl = imageUrl
                        )
                    }
                    // Sắp xếp giảm dần theo thời gian (mới nhất lên trên)
                    val sortedItems = items.sortedByDescending { item ->
                        val doc = snapshot.documents.find { it.id == item.id }
                        doc?.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis()
                    }
                    
                    // Lọc trùng lặp để xử lý các thông báo cũ bị lỗi tạo 2 lần
                    val uniqueItems = sortedItems.distinctBy { 
                        // Ưu tiên gom nhóm theo movieId, nếu không có thì gom nhóm theo tiêu đề
                        it.movieId ?: it.title.trim()
                    }
                    
                    notifications.clear()
                    notifications.addAll(uniqueItems)
                }
            }
        onDispose { listener.remove() }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var deleteTargetId by remember { mutableStateOf<String?>(null) }

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── Header ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = TextPrimary
                    )
                }

                Text(
                    text = "Thông báo",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Notification List / Empty State ──────────────────────────────
            if (notifications.isEmpty()) {
                NotificationEmptyState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        bottom = 120.dp
                    )
                ) {
                    items(
                        items = notifications.toList(),
                        key = { it.id }
                    ) { notification ->
                        NotificationRow(
                            notification = notification,
                            isDeleteMode = deleteTargetId == notification.id,
                            onTap = {
                                if (deleteTargetId != null) {
                                    deleteTargetId = null
                                } else {
                                    // Đánh dấu đã đọc
                                    if (!notification.isRead) {
                                        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                        if (user != null) {
                                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                                .collection("users").document(user.uid)
                                                .collection("notifications").document(notification.id)
                                                .update("isRead", true)
                                        }
                                    }
                                    // Điều hướng
                                    if (notification.rawType == "new_movie" && notification.movieId != null) {
                                        onNavigateToMovie(notification.movieId)
                                        onClose()
                                    } else if (notification.rawType == "billing") {
                                        onNavigateToPlan()
                                        onClose()
                                    }
                                }
                            },
                            onLongPress = {
                                deleteTargetId = notification.id
                            },
                            onDelete = {
                                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                if (user != null) {
                                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                        .collection("users").document(user.uid)
                                        .collection("notifications").document(notification.id)
                                        .delete()
                                }
                                deleteTargetId = null
                                scope.launch {
                                    snackbarHostState.showSnackbar("Đã xóa thông báo")
                                }
                            }
                        )
                    }
                }
            }
        }

        // ── Snackbar ─────────────────────────────────────────────────────────
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = Color(0xFF333333),
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Single Notification Row (Netflix-style)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun NotificationRow(
    notification: NotificationItem,
    isDeleteMode: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onDelete: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onTap,
                    onLongClick = onLongPress
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // ── Thumbnail (poster hoặc icon) ─────────────────────────────────
            if (!notification.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = notification.imageUrl,
                    contentDescription = notification.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 100.dp, height = 56.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            } else {
                // Placeholder icon cho thông báo không có poster
                Box(
                    modifier = Modifier
                        .size(width = 100.dp, height = 56.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    notification.type.color.copy(alpha = 0.25f),
                                    notification.type.color.copy(alpha = 0.08f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = notification.type.icon,
                        contentDescription = null,
                        tint = notification.type.color.copy(alpha = 0.7f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // ── Text Content ─────────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    color = if (notification.isRead) TextSecondary else TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = notification.message,
                    color = TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.timeAgo,
                    color = TextMuted.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }

            // ── Delete Button ────────────────────────────────────────────────
            AnimatedVisibility(
                visible = isDeleteMode,
                enter = scaleIn(tween(200)) + fadeIn(tween(200)),
                exit = scaleOut(tween(150)) + fadeOut(tween(150))
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Xóa thông báo",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Divider mỏng giữa các thông báo
        HorizontalDivider(
            color = DividerColor,
            thickness = 0.5.dp,
            modifier = Modifier.padding(start = 128.dp, end = 16.dp)
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Empty State
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun NotificationEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsNone,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Chưa có thông báo nào",
                color = TextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Các thông báo mới sẽ xuất hiện tại đây",
                color = TextMuted,
                fontSize = 13.sp
            )
        }
    }
}
