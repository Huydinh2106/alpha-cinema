package com.example.alphacinema.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.alphacinema.R

// Data model
data class NotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val timeAgo: String,
    val iconRes: Int,
    val type: String
)

// ViewModel
class NotificationViewModel : ViewModel() {
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        fetchNotifications()
    }

    private fun fetchNotifications() {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        
        listenerRegistration = db.collection("users").document(user.uid).collection("notifications")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("NotificationScreen", "Listen failed.", error)
                    return@addSnapshotListener
                }
                
                val items = snapshot?.documents?.mapNotNull { doc ->
                    val title = doc.getString("title") ?: ""
                    val body = doc.getString("body") ?: ""
                    val type = doc.getString("type") ?: "system"
                    
                    val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis()
                    val diff = System.currentTimeMillis() - timestamp
                    val timeAgo = when {
                        diff < 60000 -> "Vừa xong"
                        diff < 3600000 -> "${diff / 60000} phút trước"
                        diff < 86400000 -> "${diff / 3600000} giờ trước"
                        else -> "${diff / 86400000} ngày trước"
                    }
                    
                    val iconRes = when (type) {
                        "series_update", "new_movie" -> android.R.drawable.ic_media_play
                        "billing" -> android.R.drawable.ic_secure
                        "security_alert" -> android.R.drawable.ic_dialog_alert
                        else -> android.R.drawable.ic_dialog_info
                    }

                    NotificationItem(
                        id = doc.id,
                        title = title,
                        body = body,
                        timeAgo = timeAgo,
                        iconRes = iconRes,
                        type = type
                    )
                } ?: emptyList()
                
                // Sắp xếp giảm dần theo timestamp (mới nhất lên trên) để tránh lỗi Index của Firestore
                val sortedItems = items.sortedByDescending { item ->
                    val doc = snapshot?.documents?.find { it.id == item.id }
                    doc?.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis()
                }
                
                _notifications.value = sortedItems
            }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}

// UI
@Composable
fun NotificationScreen(
    modifier: Modifier = Modifier,
    viewModel: NotificationViewModel = viewModel(),
    onClose: () -> Unit = {}
) {
    val notifications by viewModel.notifications.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B16)) // Đồng bộ màu nền với HomeScreen
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1A1D2B), Color(0xFF070B16))
                        )
                    )
                    .padding(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Thông báo",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_menu_close_clear_cancel),
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications) { notif ->
                    NotificationCard(notif)
                }
            }
        }
    }
}


@Composable
fun NotificationCard(item: NotificationItem) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D2B).copy(alpha = 0.7f)), // Glassmorphism navy style
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .clickable { /* Handle navigation based on item.type */ }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .background(Color.Transparent)
        ) {
            Icon(
                painter = painterResource(id = item.iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.body,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.timeAgo,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}
