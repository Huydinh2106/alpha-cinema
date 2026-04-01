package com.example.alphacinema

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private data class SupportMessageUi(
    val id: String,
    val text: String,
    val fromUser: Boolean,
    val time: String
)

@Composable
fun SupportScreen() {
    var input by remember { mutableStateOf("") }
    var messages by remember {
        mutableStateOf(
            listOf(
                SupportMessageUi(
                    id = "1",
                    text = "Xin chào, AlphaCinema có thể hỗ trợ gì cho bạn hôm nay?",
                    fromUser = false,
                    time = "19:20"
                ),
                SupportMessageUi(
                    id = "2",
                    text = "Mình muốn biết gói thành viên tháng có gì.",
                    fromUser = true,
                    time = "19:21"
                ),
                SupportMessageUi(
                    id = "3",
                    text = "Gói tháng hiện có xem không quảng cáo, tải offline và ưu tiên chất lượng cao.",
                    fromUser = false,
                    time = "19:21"
                )
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF111A2E),
                        Color(0xFF0C1324),
                        Color(0xFF070B16)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SupportTopBar()

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Spacer(modifier = Modifier.height(10.dp)) }
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }
                item { Spacer(modifier = Modifier.height(10.dp)) }
            }

            InputBar(
                value = input,
                onValueChange = { input = it },
                onSend = {
                    val content = input.trim()
                    if (content.isNotEmpty()) {
                        messages = messages + SupportMessageUi(
                            id = (messages.size + 1).toString(),
                            text = content,
                            fromUser = true,
                            time = "Bây giờ"
                        )
                        input = ""

                        // TODO: Gọi chatbot API tại đây với nội dung `content`.
                        // TODO: Khi API trả về, append câu trả lời bot vào `messages`.
                    }
                }
            )
        }
    }
}

@Composable
private fun SupportTopBar() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xCC121C33),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Hỗ trợ",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Trợ lý AlphaCinema",
                color = Color(0xFFF6E29A),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun MessageBubble(message: SupportMessageUi) {
    val bubbleColor = if (message.fromUser) Color(0xFFF6E29A) else Color(0xFF1B2742)
    val textColor = if (message.fromUser) Color.Black else Color.White

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Column(horizontalAlignment = if (message.fromUser) Alignment.End else Alignment.Start) {
            Surface(
                color = bubbleColor,
                shadowElevation = 4.dp,
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (message.fromUser) 18.dp else 6.dp,
                    bottomEnd = if (message.fromUser) 6.dp else 18.dp
                ),
                modifier = Modifier.widthIn(max = 290.dp)
            ) {
                Text(
                    text = message.text,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = message.time,
                color = Color.White.copy(alpha = 0.4f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = if (message.fromUser) TextAlign.End else TextAlign.Start
            )
        }
    }
}

@Composable
private fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = Color(0xD4142037),
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Nhập tin nhắn...",
                        color = Color.White.copy(alpha = 0.45f)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF1A2542),
                    unfocusedContainerColor = Color(0xFF16203A),
                    focusedBorderColor = Color(0xFFF6E29A),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.20f),
                    cursorColor = Color(0xFFF6E29A)
                )
            )

            Button(
                onClick = onSend,
                shape = CircleShape,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF6E29A),
                    contentColor = Color.Black
                ),
                modifier = Modifier.size(46.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "Gửi"
                )
            }
        }
    }
}
