@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.alphacinema.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ── Color Palette ────────────────────────────────────────────────────────────
private val DarkBg = Color.Black
private val CardBg = Color(0xFF141414)
private val CardBorder = Color(0xFF333333)
private val GoldAccent = Color(0xFFF6E29A)
private val TextPrimary = Color.White
private val TextSecondary = Color.White.copy(alpha = 0.68f)
private val TextMuted = Color.White.copy(alpha = 0.4f)

// ── Notification Types ───────────────────────────────────────────────────────
enum class NotificationType(
    val label: String,
    val icon: ImageVector,
    val color: Color
) {
    MARKETING(
        label = "Khuyến mãi",
        icon = Icons.Outlined.LocalOffer,
        color = Color(0xFFF6E29A) // Gold
    ),
    TRANSACTION(
        label = "Giao dịch",
        icon = Icons.Outlined.Payment,
        color = Color(0xFF4ADE80) // Green
    ),
    SOCIAL(
        label = "Xã hội",
        icon = Icons.Outlined.Groups,
        color = Color(0xFF60A5FA) // Blue
    ),
    SYSTEM(
        label = "Hệ thống",
        icon = Icons.Outlined.SystemUpdate,
        color = Color(0xFFA78BFA) // Purple
    ),
    NEW_MOVIE(
        label = "Phim mới",
        icon = Icons.Outlined.Movie,
        color = Color(0xFFFB7185) // Pink
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
    val plan: String? = null
)

// ── Filter Chip Labels ───────────────────────────────────────────────────────
private val FILTER_ALL = "Tất cả"

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
    var selectedFilter by remember { mutableStateOf(FILTER_ALL) }

    // Firestore listener
    androidx.compose.runtime.DisposableEffect(Unit) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        
        if (user != null) {
            listener = db.collection("users").document(user.uid).collection("notifications")
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

                            NotificationItem(
                                id = doc.id,
                                type = type,
                                rawType = typeString,
                                title = title,
                                message = message,
                                timeAgo = timeAgo,
                                isRead = isRead,
                                movieId = movieId,
                                plan = plan
                            )
                        }
                        // Sắp xếp giảm dần theo thời gian (mới nhất lên trên)
                        val sortedItems = items.sortedByDescending { item ->
                            val doc = snapshot.documents.find { it.id == item.id }
                            doc?.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis()
                        }
                        notifications.clear()
                        notifications.addAll(sortedItems)
                    }
                }
        }
        onDispose { listener?.remove() }
    }

    val filterOptions = remember {
        listOf(FILTER_ALL) + NotificationType.entries.map { it.label }
    }

    val filteredNotifications = remember(selectedFilter, notifications.toList()) {
        if (selectedFilter == FILTER_ALL) {
            notifications.toList()
        } else {
            notifications.filter { it.type.label == selectedFilter }
        }
    }

    val unreadCount = remember(notifications.toList()) {
        notifications.count { !it.isRead }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Track which notification is in "delete mode" (long-pressed)
    var deleteTargetId by remember { mutableStateOf<String?>(null) }

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Tap anywhere outside to cancel delete mode
                deleteTargetId = null
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── Header ───────────────────────────────────────────────────────
            NotificationHeader(
                unreadCount = unreadCount,
                onClose = onClose,
                onMarkAllRead = {
                    val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    if (user != null) {
                        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        notifications.filter { !it.isRead }.forEach { notif ->
                            db.collection("users").document(user.uid)
                                .collection("notifications").document(notif.id)
                                .update("isRead", true)
                        }
                    }
                }
            )

            // ── Filter Chips ─────────────────────────────────────────────────
            NotificationFilterChips(
                options = filterOptions,
                selected = selectedFilter,
                onSelect = { selectedFilter = it },
                notifications = notifications
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Notification List / Empty State ──────────────────────────────
            if (filteredNotifications.isEmpty()) {
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
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 120.dp // Thêm padding dưới để không bị che bởi Bottom Navigation
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = filteredNotifications,
                        key = { it.id }
                    ) { notification ->
                        NotificationCard(
                            notification = notification,
                            isDeleteMode = deleteTargetId == notification.id,
                            onTap = {
                                if (deleteTargetId != null) {
                                    deleteTargetId = null
                                } else {
                                    if (!notification.isRead) {
                                        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                        if (user != null) {
                                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                                .collection("users").document(user.uid)
                                                .collection("notifications").document(notification.id)
                                                .update("isRead", true)
                                        }
                                    }
                                    // Handle Deep Linking / Navigation
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
                                    snackbarHostState.showSnackbar("Đã xóa thông báo thành công")
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
// Header
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun NotificationHeader(
    unreadCount: Int,
    onClose: () -> Unit,
    onMarkAllRead: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Quay lại",
                tint = TextPrimary
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Thông báo",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            if (unreadCount > 0) {
                Text(
                    text = "$unreadCount thông báo chưa đọc",
                    color = GoldAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (unreadCount > 0) {
            IconButton(onClick = onMarkAllRead) {
                Icon(
                    imageVector = Icons.Outlined.DoneAll,
                    contentDescription = "Đánh dấu đã đọc tất cả",
                    tint = GoldAccent
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Filter Chips Row
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun NotificationFilterChips(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    notifications: List<NotificationItem>
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
    ) {
        items(options.size) { index ->
            val option = options[index]
            val isSelected = option == selected

            // Count for each filter
            val count = if (option == FILTER_ALL) {
                notifications.size
            } else {
                notifications.count { it.type.label == option }
            }

            val bgColor by animateColorAsState(
                targetValue = if (isSelected) GoldAccent else CardBg,
                animationSpec = tween(200),
                label = "chipBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.Black else TextSecondary,
                animationSpec = tween(200),
                label = "chipText"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) GoldAccent else CardBorder,
                animationSpec = tween(200),
                label = "chipBorder"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                    .clickable { onSelect(option) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = option,
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    if (count > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color.Black.copy(alpha = 0.2f)
                                    else TextMuted.copy(alpha = 0.3f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = count.toString(),
                                color = if (isSelected) Color.Black else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Single Notification Card (with long-press delete)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun NotificationCard(
    notification: NotificationItem,
    isDeleteMode: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onDelete: () -> Unit
) {
    val cardBgColor = if (notification.isRead) {
        CardBg.copy(alpha = 0.6f)
    } else {
        CardBg
    }

    val borderCol = if (isDeleteMode) {
        Color(0xFFEF4444).copy(alpha = 0.5f)
    } else if (notification.isRead) {
        CardBorder.copy(alpha = 0.4f)
    } else {
        notification.type.color.copy(alpha = 0.25f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBgColor)
            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress
            )
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        // ── Type Icon ────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(notification.type.color.copy(alpha = 0.12f))
                .border(
                    1.dp,
                    notification.type.color.copy(alpha = 0.2f),
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = notification.type.icon,
                contentDescription = null,
                tint = notification.type.color,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // ── Content ──────────────────────────────────────────────────────────
        Column(modifier = Modifier.weight(1f)) {
            // Type label + time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notification.type.label,
                    color = notification.type.color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = notification.timeAgo,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Title
            Text(
                text = notification.title,
                color = if (notification.isRead) TextSecondary else TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Message
            Text(
                text = notification.message,
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // ── Delete Button or Unread Dot ───────────────────────────────────────
        AnimatedVisibility(
            visible = isDeleteMode,
            enter = scaleIn(tween(200)) + fadeIn(tween(200)),
            exit = scaleOut(tween(150)) + fadeOut(tween(150))
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Xóa thông báo",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (!isDeleteMode && !notification.isRead) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                notification.type.color,
                                notification.type.color.copy(alpha = 0.5f)
                            )
                        )
                    )
            )
        }
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
            // Big muted icon
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(CardBg)
                    .border(1.dp, CardBorder, RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsNone,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

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
