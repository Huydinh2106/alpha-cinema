package com.example.alphacinema.ui.support

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

private val SupportBackground = Color(0xFF070B16)
private val SupportPanel = Color(0xFF10192D)
private val SupportPanelSoft = Color(0xFF17233A)
private val SupportGold = Color(0xFFF6E29A)
private val SupportGoldDeep = Color(0xFFC89831)
private val SupportRose = Color(0xFFE35D8F)
private val SupportTeal = Color(0xFF4DD6B1)
private val SupportBlue = Color(0xFF5B7CFA)

private val SupportScreenShape = RoundedCornerShape(26.dp)
private val SupportInputShape = RoundedCornerShape(24.dp)
private val UserBubbleShape = RoundedCornerShape(
    topStart = 24.dp,
    topEnd = 24.dp,
    bottomStart = 24.dp,
    bottomEnd = 8.dp
)
private val BotBubbleShape = RoundedCornerShape(
    topStart = 24.dp,
    topEnd = 24.dp,
    bottomStart = 8.dp,
    bottomEnd = 24.dp
)

private data class SupportQuickPrompt(
    val label: String,
    val prompt: String
)

private val QuickPrompts = listOf(
    SupportQuickPrompt(
        label = "G\u1ee3i \u00fd phim",
        prompt = "G\u1ee3i \u00fd v\u00e0i phim \u0111\u00e1ng xem t\u1ed1i nay"
    ),
    SupportQuickPrompt(
        label = "L\u1ed7i xem phim",
        prompt = "T\u00f4i kh\u00f4ng xem \u0111\u01b0\u1ee3c phim, h\u00e3y h\u01b0\u1edbng d\u1eabn c\u00e1ch x\u1eed l\u00fd"
    ),
    SupportQuickPrompt(
        label = "T\u00e0i kho\u1ea3n",
        prompt = "T\u00f4i c\u1ea7n h\u1ed7 tr\u1ee3 v\u1ec1 t\u00e0i kho\u1ea3n"
    ),
    SupportQuickPrompt(
        label = "Phim gia \u0111\u00ecnh",
        prompt = "G\u1ee3i \u00fd phim ph\u00f9 h\u1ee3p \u0111\u1ec3 xem c\u00f9ng gia \u0111\u00ecnh"
    )
)

@Composable
fun SupportScreen(
    supportViewModel: SupportViewModel = viewModel(),
    onMovieAction: (SupportChatAction) -> Unit = {}
) {
    var input by remember { mutableStateOf("") }
    val messages by supportViewModel.messages.collectAsState()
    val listState = rememberLazyListState()

    fun sendMessage(content: String) {
        val trimmed = content.trim()
        if (trimmed.isNotEmpty()) {
            input = ""
            supportViewModel.sendMessage(trimmed)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SupportBackground)
    ) {
        SupportBackgroundLayer()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 82.dp)
        ) {
            SupportTopBar(
                onStartNewConversation = {
                    input = ""
                    supportViewModel.startNewConversation()
                }
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 12.dp,
                    end = 16.dp,
                    bottom = 22.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        SupportIntroCard(onQuickPrompt = ::sendMessage)
                    }
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
                onSend = { sendMessage(input) }
            )
        }
    }
}

@Composable
private fun SupportBackgroundLayer() {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_home),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.22f,
            modifier = Modifier
                .fillMaxWidth()
                .height(430.dp)
                .align(Alignment.TopCenter)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(460.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x66111B2E),
                            SupportBackground.copy(alpha = 0.78f),
                            SupportBackground
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            SupportBackground.copy(alpha = 0.86f),
                            SupportBackground
                        )
                    )
                )
        )
    }
}

@Composable
private fun SupportTopBar(
    onStartNewConversation: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 18.dp, top = 12.dp, end = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(SupportGold, SupportRose, SupportBlue)
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.26f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_app),
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Alpha Support",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(SupportTeal)
                )
                Text(
                    text = "Tr\u1ee3 l\u00fd r\u1ea1p phim c\u1ee7a b\u1ea1n",
                    color = Color.White.copy(alpha = 0.68f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(
            onClick = onStartNewConversation,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = "B\u1eaft \u0111\u1ea7u l\u1ea1i",
                tint = Color.White.copy(alpha = 0.86f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SupportIntroCard(
    onQuickPrompt: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SupportScreenShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1D2942).copy(alpha = 0.96f),
                        Color(0xFF10192D).copy(alpha = 0.94f),
                        Color(0xFF221C34).copy(alpha = 0.92f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.10f),
                shape = SupportScreenShape
            )
            .padding(vertical = 18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(SupportGold, Color(0xFFFFF6DA))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HeadsetMic,
                        contentDescription = null,
                        tint = Color(0xFF101010),
                        modifier = Modifier.size(25.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Xin ch\u00e0o Huy, h\u00f4m nay b\u1ea1n mu\u1ed1n xem g\u00ec?",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ch\u1ecdn g\u1ee3i \u00fd ho\u1eb7c nh\u1eadp c\u00e2u h\u1ecfi v\u1ec1 phim, t\u00e0i kho\u1ea3n v\u00e0 c\u00e1ch s\u1eed d\u1ee5ng AlphaCinema.",
                        color = Color.White.copy(alpha = 0.70f),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 18.dp)
            ) {
                items(QuickPrompts) { prompt ->
                    QuickPromptChip(
                        label = prompt.label,
                        onClick = { onQuickPrompt(prompt.prompt) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickPromptChip(
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = SupportGold,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
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
    val textColor = if (fromUser) Color(0xFF111111) else Color.White
    val bubbleShape = if (fromUser) UserBubbleShape else BotBubbleShape
    val bubbleBrush = if (fromUser) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFF3B8),
                SupportGold,
                SupportGoldDeep
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                SupportPanelSoft,
                Color(0xFF111B2E)
            )
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!fromUser) {
            SupportAvatar()
            Spacer(modifier = Modifier.width(9.dp))
        }

        Column(
            horizontalAlignment = if (fromUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 292.dp)
        ) {
            AnimatedVisibility(visible = !fromUser) {
                Text(
                    text = if (isLoading) "AlphaCinema \u0111ang so\u1ea1n" else "Tr\u1ee3 l\u00fd AlphaCinema",
                    color = Color.White.copy(alpha = 0.48f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 4.dp, bottom = 5.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .background(bubbleBrush)
                    .border(
                        width = 1.dp,
                        color = if (fromUser) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.09f),
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
                            color = SupportGold
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

        if (fromUser) {
            Spacer(modifier = Modifier.width(9.dp))
            UserAvatar()
        }
    }
}

@Composable
private fun SupportAvatar() {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_app),
            contentDescription = null,
            modifier = Modifier
                .size(29.dp)
                .clip(CircleShape)
        )
    }
}

@Composable
private fun UserAvatar() {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "B",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("support_movie_card_${movie.id}")
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MoviePoster(movie = movie)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = movie.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (movie.subtitle.isNotBlank()) {
                        Text(
                            text = movie.subtitle,
                            color = Color.White.copy(alpha = 0.66f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (movie.year.isNotBlank()) {
                        Surface(
                            color = SupportGold.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = movie.year,
                                color = SupportGold,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
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
private fun MoviePoster(
    movie: SupportChatMovieItem
) {
    Box(
        modifier = Modifier
            .width(70.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF303A5C),
                        Color(0xFF111B2E)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (!movie.posterUrl.isNullOrBlank()) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.logo_app),
                error = painterResource(id = R.drawable.logo_app),
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.logo_app),
                contentDescription = null,
                modifier = Modifier.size(42.dp)
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 8.dp, end = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp),
                placeholder = {
                    Text(
                        text = "Nh\u1eadp c\u00e2u h\u1ecfi cho Alpha Support...",
                        color = Color.White.copy(alpha = 0.42f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = SupportGold.copy(alpha = 0.86f)
                    )
                },
                minLines = 1,
                maxLines = 4,
                shape = SupportInputShape,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF16233B),
                    unfocusedContainerColor = SupportPanel,
                    focusedBorderColor = SupportGold,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    cursorColor = SupportGold,
                    focusedLeadingIconColor = SupportGold,
                    unfocusedLeadingIconColor = SupportGold.copy(alpha = 0.70f)
                )
            )

            Button(
                onClick = onSend,
                enabled = value.isNotBlank(),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SupportGold,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.White.copy(alpha = 0.10f),
                    disabledContentColor = Color.White.copy(alpha = 0.28f)
                ),
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "G\u1eedi",
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
