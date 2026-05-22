package com.example.alphacinema.ui.watchparty

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alphacinema.ui.components.LottieLoadingIndicator

private enum class LobbyTab { CREATE, JOIN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchPartyLobbySheet(
    movieTitle: String,
    isCreating: Boolean,
    isJoining: Boolean,
    error: String?,
    joinOnly: Boolean = false,
    onDismiss: () -> Unit,
    onCreateRoom: () -> Unit,
    onJoinRoom: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeTab by remember { mutableStateOf(if (joinOnly) LobbyTab.JOIN else LobbyTab.CREATE) }
    var roomIdInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141414),
        scrimColor = Color.Black.copy(alpha = 0.62f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Icon(
                imageVector = Icons.Rounded.Groups,
                contentDescription = null,
                tint = Color(0xFFF6E29A),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Xem chung",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Xem phim cùng bạn bè theo thời gian thực",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Tab Selector — ẩn khi chỉ có Join
            if (!joinOnly) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LobbyTab.entries.forEach { tab ->
                        val selected = tab == activeTab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) Brush.horizontalGradient(
                                        listOf(Color(0xFFF6E29A), Color(0xFFD4A843))
                                    ) else Brush.horizontalGradient(
                                        listOf(Color.Transparent, Color.Transparent)
                                    )
                                )
                                .clickable { activeTab = tab }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (tab == LobbyTab.CREATE) "Tạo phòng" else "Tham gia",
                                color = if (selected) Color.Black else Color.White.copy(alpha = 0.7f),
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Nhập mã phòng để tham gia xem chung",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Error
            AnimatedVisibility(visible = error != null) {
                Text(
                    text = error ?: "",
                    color = Color(0xFFFF6B6B),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            }

            when (activeTab) {
                LobbyTab.CREATE -> {
                    val hasMovie = movieTitle.isNotBlank() &&
                        movieTitle != "Phòng xem chung (chưa chọn phim)" &&
                        movieTitle != "Xem chung"

                    // Movie info card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1F1F1F)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.MeetingRoom,
                                contentDescription = null,
                                tint = Color(0xFFF6E29A),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            if (hasMovie) {
                                Text(
                                    text = movieTitle,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 2
                                )
                                Text(
                                    text = "Tối đa 5 người",
                                    color = Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            } else {
                                Text(
                                    text = "Tạo phòng trống",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Chọn phim sau khi vào phòng",
                                    color = Color(0xFFF6E29A).copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onCreateRoom,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = !isCreating,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF6E29A),
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFFF6E29A).copy(alpha = 0.5f)
                        )
                    ) {
                        if (isCreating) {
                            LottieLoadingIndicator(size = 32.dp)
                        } else {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tạo phòng xem chung", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                LobbyTab.JOIN -> {
                    OutlinedTextField(
                        value = roomIdInput,
                        onValueChange = { roomIdInput = it.uppercase().take(6) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Mã phòng (6 ký tự)") },
                        placeholder = { Text("VD: ABC123", color = Color.White.copy(alpha = 0.3f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF1F1F1F),
                            unfocusedContainerColor = Color(0xFF141414),
                            focusedBorderColor = Color(0xFFF6E29A),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            cursorColor = Color(0xFFF6E29A),
                            focusedLabelColor = Color(0xFFF6E29A),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                        ),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            letterSpacing = 6.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { onJoinRoom(roomIdInput) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = roomIdInput.length == 6 && !isJoining,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF6E29A),
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFFF6E29A).copy(alpha = 0.3f)
                        )
                    ) {
                        if (isJoining) {
                            LottieLoadingIndicator(size = 32.dp)
                        } else {
                            Text("Tham gia phòng", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
