package com.example.alphacinema.ui.support

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
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

private val SupportScreenShape = RoundedCornerShape(28.dp)
private val SupportInputShape = RoundedCornerShape(24.dp)
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
    var input by remember { mutableStateOf("") }
    val messages by supportViewModel.messages.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
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
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 104.dp)
        ) {
            SupportTopBar()

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 18.dp,
                    end = 16.dp,
                    bottom = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SupportIntroCard()
                }

                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        onMovieAction = onMovieAction
                    )
                }
            }

            SupportInputBar(
                value = input,
                onValueChange = { input = it },
                onSend = {
                    val content = input.trim()
                    if (content.isNotEmpty()) {
                        input = ""
                        supportViewModel.sendMessage(content)
                    }
                }
            )
        }
    }
}

@Composable
private fun SupportTopBar() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(
            text = "H\u1ed7 tr\u1ee3",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tr\u1ee3 l\u00fd AlphaCinema lu\u00f4n s\u1eb5n s\u00e0ng gi\u1ea3i \u0111\u00e1p",
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                color = Color(0x22F6E29A),
                shape = RoundedCornerShape(999.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFF6E29A),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "AI Chat",
                        color = Color(0xFFF6E29A),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportIntroCard() {
    Surface(
        color = Color(0xFF101C31).copy(alpha = 0.88f),
        shape = SupportScreenShape,
        tonalElevation = 2.dp,
        shadowElevation = 10.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = SupportScreenShape
            )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "H\u1ecfi nhanh, tr\u1ea3 l\u1eddi r\u00f5",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "B\u1ea1n c\u00f3 th\u1ec3 h\u1ecfi v\u1ec1 phim, th\u00f4ng tin \u1ee9ng d\u1ee5ng ho\u1eb7c c\u00e1ch s\u1eed d\u1ee5ng AlphaCinema. C\u00e1c c\u00e2u tr\u1ea3 l\u1eddi s\u1ebd \u0111\u01b0\u1ee3c l\u1ea5y t\u1eeb chatbot API th\u1eadt.",
                color = Color.White.copy(alpha = 0.74f),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 21.sp
            )
        }
    }
}

@Composable
internal fun MessageBubble(
    message: SupportChatMessage,
    onMovieAction: (SupportChatAction) -> Unit = {}
) {
    val fromUser = message.sender == SupportMessageSender.USER
    val isLoading = message.sender == SupportMessageSender.LOADING
    val movieItems = message.metadata?.movieItems.orEmpty()
    val bubbleColor = when (message.sender) {
        SupportMessageSender.USER -> Color(0xFFF6E29A)
        SupportMessageSender.BOT -> Color(0xFF17233A)
        SupportMessageSender.LOADING -> Color(0xFF111B2E)
    }
    val textColor = if (fromUser) Color(0xFF111111) else Color.White
    val bubbleShape = if (fromUser) UserBubbleShape else BotBubbleShape

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (fromUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            AnimatedVisibility(visible = !fromUser) {
                Text(
                    text = if (isLoading) "AlphaCinema \u0111ang so\u1ea1n" else "Tr\u1ee3 l\u00fd AlphaCinema",
                    color = Color.White.copy(alpha = 0.42f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 4.dp, bottom = 5.dp)
                )
            }

            Surface(
                color = bubbleColor,
                shadowElevation = if (fromUser) 4.dp else 8.dp,
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
                    Row(
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFF6E29A)
                        )
                        Text(
                            text = message.text,
                            color = Color.White.copy(alpha = 0.84f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
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
                    AsyncImage(
                        model = movie.posterUrl,
                        contentDescription = movie.title,
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.logo_app),
                        error = painterResource(id = R.drawable.logo_app),
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
private fun SupportInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = Color(0xE0131D31),
        shadowElevation = 18.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "H\u1ecfi v\u1ec1 phim, t\u00e0i kho\u1ea3n, g\u00f3i th\u00e0nh vi\u00ean...",
                        color = Color.White.copy(alpha = 0.42f)
                    )
                },
                minLines = 1,
                maxLines = 4,
                shape = SupportInputShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF16233B),
                    unfocusedContainerColor = Color(0xFF10192D),
                    focusedBorderColor = Color(0xFFF6E29A),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    cursorColor = Color(0xFFF6E29A)
                )
            )

            Button(
                onClick = onSend,
                enabled = value.isNotBlank(),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF6E29A),
                    contentColor = Color.Black,
                    disabledContainerColor = Color.White.copy(alpha = 0.10f),
                    disabledContentColor = Color.White.copy(alpha = 0.28f)
                ),
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "G\u1eedi"
                )
            }
        }
    }
}
