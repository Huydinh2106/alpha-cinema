package com.example.alphacinema.ui.support

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.example.alphacinema.ui.components.LottieLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alphacinema.R
import com.example.alphacinema.data.model.SupportChatAction
import com.example.alphacinema.data.model.SupportChatMessage
import com.example.alphacinema.data.model.SupportChatMovieItem
import com.example.alphacinema.data.model.SupportMessageSender
import com.example.alphacinema.data.model.primaryAction
import com.example.alphacinema.data.model.resolveRoute
import com.example.alphacinema.ui.components.GradientPlayButton
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars

private val UserBubbleShape = RoundedCornerShape(
    topStart = 22.dp,
    topEnd = 22.dp,
    bottomStart = 22.dp,
    bottomEnd = 8.dp
)
private val BotBubbleShape = RoundedCornerShape(
    topStart = 22.dp,
    topEnd = 22.dp,
    bottomStart = 8.dp,
    bottomEnd = 22.dp
)

@Composable
fun SupportScreen(
    supportViewModel: SupportViewModel = viewModel(),
    onMovieAction: (SupportChatAction) -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val messages by supportViewModel.messages.collectAsState()
    val listState = rememberLazyListState()
    val hasStartedChat = messages.isNotEmpty()
    val isLoading = messages.any { it.sender == SupportMessageSender.LOADING }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF10182B),
                        Color(0xFF0A1120),
                        Color(0xFF060913)
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x44F6E29A),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                // Reserve room above the floating GlassBottomBar from AppScreen.
                // Use dynamic WindowInsets so it works on all screen ratios.
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 84.dp)
        ) {
            ChatHeader(
                hasStartedChat = hasStartedChat,
                onNewConversation = {
                    inputText = ""
                    error = null
                    supportViewModel.startNewConversation()
                }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (hasStartedChat) {
                    ChatMessageList(
                        messages = messages,
                        listState = listState,
                        onMovieAction = onMovieAction
                    )
                } else {
                    ChatWelcome(
                        onSuggestionClick = { suggestion ->
                            inputText = ""
                            error = null
                            supportViewModel.sendMessage(suggestion)
                        }
                    )
                }
            }

            ChatInputBar(
                value = inputText,
                onValueChange = { inputText = it },
                isLoading = isLoading,
                error = error,
                onSend = {
                    val content = inputText.trim()
                    if (content.isNotEmpty()) {
                        inputText = ""
                        error = null
                        supportViewModel.sendMessage(content)
                    }
                }
            )
        }
    }
}

@Composable
private fun ChatHeader(
    hasStartedChat: Boolean,
    onNewConversation: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderIconButton(
            icon = Icons.Outlined.Menu,
            contentDescription = "Mở menu",
            onClick = {}
        )

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFF6E29A),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Alpha AI",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (hasStartedChat) {
                TextButton(
                    onClick = onNewConversation,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = Color(0xFFF6E29A),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Cuộc trò chuyện mới",
                            color = Color(0xFFF6E29A),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Surface(
                    color = Color(0x18F6E29A),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "AI Chat",
                        color = Color(0xFFF6E29A),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
        }

        UserAvatar(modifier = Modifier.size(38.dp))
    }
}

@Composable
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White.copy(alpha = 0.84f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ChatWelcome(
    onSuggestionClick: (String) -> Unit
) {
    val suggestions = listOf(
        ChatSuggestionUi("Gợi ý phim cho tôi", Icons.Outlined.Movie),
        ChatSuggestionUi("Tìm phim theo tâm trạng", Icons.Outlined.AutoAwesome),
        ChatSuggestionUi("Gói Premium có gì?", Icons.Outlined.WorkspacePremium),
        ChatSuggestionUi("Cách tạo phòng xem chung?", Icons.Outlined.Groups),
        ChatSuggestionUi("Tôi cần hỗ trợ tài khoản", Icons.Outlined.PersonOutline),
        ChatSuggestionUi("Phim đang hot hôm nay", Icons.Outlined.Movie)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Xin chào Khoa!",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 36.sp
            )
            Text(
                text = "Bạn muốn xem gì hôm nay?",
                color = Color.White.copy(alpha = 0.74f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 30.sp
            )
            Text(
                text = "Hỏi Alpha AI về phim, tài khoản, xem chung hoặc gói thành viên.",
                color = Color.White.copy(alpha = 0.54f),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 21.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        SuggestionList(
            suggestions = suggestions,
            onSuggestionClick = { onSuggestionClick(it.text) }
        )
    }
}

@Composable
private fun SuggestionList(
    suggestions: List<ChatSuggestionUi>,
    onSuggestionClick: (ChatSuggestionUi) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        suggestions.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { suggestion ->
                    SuggestionChip(
                        suggestion = suggestion,
                        onClick = { onSuggestionClick(suggestion) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SuggestionChip(
    suggestion: ChatSuggestionUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.055f),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = suggestion.icon,
                contentDescription = null,
                tint = Color(0xFFF6E29A),
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = suggestion.text,
                color = Color.White.copy(alpha = 0.86f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 17.sp
            )
        }
    }
}

private data class ChatSuggestionUi(
    val text: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
private fun BotAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFFF6E29A).copy(alpha = 0.14f))
            .border(1.dp, Color(0x33F6E29A), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = Color(0xFFF6E29A),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun UserAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFF6E29A),
                        Color(0xFF9EE8FF)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "K",
            color = Color(0xFF070B16),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun ChatMessageList(
    messages: List<SupportChatMessage>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onMovieAction: (SupportChatAction) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 18.dp,
            end = 16.dp,
            bottom = 22.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            ChatBubble(
                message = message,
                onMovieAction = onMovieAction
            )
        }
    }
}

@Composable
internal fun MessageBubble(
    message: SupportChatMessage,
    onMovieAction: (SupportChatAction) -> Unit = {}
) {
    ChatBubble(
        message = message,
        onMovieAction = onMovieAction
    )
}

@Composable
private fun ChatBubble(
    message: SupportChatMessage,
    onMovieAction: (SupportChatAction) -> Unit = {}
) {
    val fromUser = message.sender == SupportMessageSender.USER
    val isLoading = message.sender == SupportMessageSender.LOADING
    val movieItems = message.metadata?.movieItems.orEmpty()
    val bubbleColor = when (message.sender) {
        SupportMessageSender.USER -> Color(0xFFF6E29A)
        SupportMessageSender.BOT -> Color(0xFF151F35)
        SupportMessageSender.LOADING -> Color(0xFF111B2E)
    }
    val textColor = if (fromUser) Color(0xFF111111) else Color.White
    val bubbleShape = if (fromUser) UserBubbleShape else BotBubbleShape

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!fromUser) {
            BotAvatar(modifier = Modifier.size(30.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (fromUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Surface(
                color = bubbleColor,
                shadowElevation = if (fromUser) 3.dp else 5.dp,
                shape = bubbleShape,
                modifier = Modifier
                    .clip(bubbleShape)
                    .border(
                        width = 1.dp,
                        color = if (fromUser) Color.Transparent else Color.White.copy(alpha = 0.08f),
                        shape = bubbleShape
                    )
            ) {
                if (isLoading) {
                    TypingIndicator()
                } else {
                    Column(
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (message.text.isNotBlank()) {
                            Text(
                                text = message.text,
                                color = textColor,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                            )
                        }

                        if (!fromUser && movieItems.isNotEmpty()) {
                            movieItems.forEach { movieItem ->
                                SupportMovieSuggestionCard(
                                    movie = movieItem,
                                    onMovieAction = onMovieAction
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = message.timestamp,
                color = Color.White.copy(alpha = 0.34f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = if (fromUser) TextAlign.End else TextAlign.Start,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        if (fromUser) {
            Spacer(modifier = Modifier.width(8.dp))
            UserAvatar(modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LottieLoadingIndicator(size = 30.dp)
        Text(
            text = "Alpha AI đang trả lời...",
            color = Color.White.copy(alpha = 0.84f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SupportMovieSuggestionCard(
    movie: SupportChatMovieItem,
    onMovieAction: (SupportChatAction) -> Unit
) {
    val action = movie.primaryAction()
    val isActionEnabled = action?.enabled == true && action.resolveRoute() != null

    Surface(
        color = Color.White.copy(alpha = 0.06f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("support_movie_card_${movie.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!movie.posterUrl.isNullOrBlank()) {
                    com.example.alphacinema.ui.components.AlphaCinemaImage(
                        model = movie.posterUrl,
                        contentDescription = movie.title,
                        contentScale = ContentScale.Crop,
                        
                        
                        modifier = Modifier
                            .width(68.dp)
                            .height(96.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = movie.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (movie.subtitle.isNotBlank()) {
                        Text(
                            text = movie.subtitle,
                            color = Color.White.copy(alpha = 0.68f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2
                        )
                    }

                    if (movie.year.isNotBlank()) {
                        Text(
                            text = movie.year,
                            color = Color(0xFFF6E29A),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            SupportMovieActionButton(
                label = action?.label ?: "Xem phim",
                enabled = isActionEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("support_movie_action_${movie.id}"),
                onClick = {
                    if (action != null) {
                        onMovieAction(action)
                    }
                }
            )
        }
    }
}

@Composable
private fun SupportMovieActionButton(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GradientPlayButton(
        onClick = onClick,
        enabled = enabled,
        label = label,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp)
    )
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    onSend: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (error != null) {
            Text(
                text = error,
                color = Color(0xFFFFB4AB),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        Surface(
            color = Color(0xF0141E32),
            shadowElevation = 12.dp,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.09f),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                InputActionButton(
                    icon = Icons.Outlined.Add,
                    contentDescription = "Thêm lựa chọn",
                    enabled = !isLoading,
                    onClick = {}
                )

                BasicTextField(
                    value = value,
                    onValueChange = {
                        if (!isLoading) {
                            onValueChange(it)
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        lineHeight = 20.sp
                    ),
                    cursorBrush = SolidColor(Color(0xFFF6E29A)),
                    maxLines = 4,
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp, vertical = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (value.isBlank()) {
                                Text(
                                    text = "Hỏi Alpha AI...",
                                    color = Color.White.copy(alpha = 0.42f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                InputActionButton(
                    icon = Icons.Outlined.HeadsetMic,
                    contentDescription = "Nhập bằng giọng nói",
                    enabled = !isLoading,
                    onClick = {}
                )

                Button(
                    onClick = onSend,
                    enabled = value.isNotBlank() && !isLoading,
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF6E29A),
                        contentColor = Color.Black,
                        disabledContainerColor = Color.White.copy(alpha = 0.10f),
                        disabledContentColor = Color.White.copy(alpha = 0.30f)
                    ),
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Gửi",
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InputActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 0.07f else 0.035f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White.copy(alpha = if (enabled) 0.72f else 0.28f),
            modifier = Modifier.size(19.dp)
        )
    }
}
