@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.alphacinema.ui.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alphacinema.ui.components.clearFocusOnTapOutside
import kotlinx.coroutines.delay

// ── Color constants (consistent with AccountScreen) ──────────────────────────
private val GoldAccent = Color(0xFFF6E29A)
private val DarkBackground = Color.Black
private val CardBackground = Color(0xFF141414)
private val CardBorder = Color(0xFF333333)
private val TextPrimary = Color.White
private val TextSecondary = Color.White.copy(alpha = 0.68f)
private val TextMuted = Color.White.copy(alpha = 0.45f)
private val ErrorRed = Color(0xFFFF6B6B)
private val SuccessGreen = Color(0xFF4ADE80)

/**
 * Màn hình xác thực email bằng mã OTP 6 số.
 *
 * @param email Email mà mã OTP đã được gửi tới.
 * @param onVerifyCode Gọi khi người dùng nhấn xác nhận, truyền mã 6 số.
 * @param onResendCode Gọi khi người dùng yêu cầu gửi lại mã.
 * @param onBackToLogin Quay lại màn hình đăng nhập.
 * @param isLoading Trạng thái loading khi đang xác thực.
 * @param isSuccess Xác thực thành công.
 * @param errorMessage Thông báo lỗi (mã sai, hết hạn, v.v.).
 */
@Composable
fun EmailVerificationScreen(
    email: String,
    onVerifyCode: (String) -> Unit,
    onResendCode: () -> Unit,
    onBackToLogin: () -> Unit,
    isLoading: Boolean = false,
    isSuccess: Boolean = false,
    errorMessage: String? = null,
    clearTrigger: Int = 0
) {
    var otpValue by remember { mutableStateOf(TextFieldValue("")) }
    var resendCountdown by remember { mutableIntStateOf(30) }
    var canResend by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // ── Xóa mã OTP khi trigger thay đổi (thường là khi nhập sai) ────────────
    LaunchedEffect(clearTrigger) {
        if (clearTrigger > 0) {
            otpValue = TextFieldValue("")
        }
    }

    // ── Countdown timer cho nút "Gửi lại mã" ────────────────────────────────
    LaunchedEffect(resendCountdown) {
        if (resendCountdown > 0) {
            canResend = false
            delay(1000L)
            resendCountdown--
        } else {
            canResend = true
        }
    }

    // ── Auto-focus ô nhập mã khi màn hình hiện ra ───────────────────────────
    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
    }

    // ── Hiệu ứng pulse cho icon email ────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // ── Auto-redirect sau khi xác thực thành công ────────────────────────────
    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            delay(2000)
            onBackToLogin()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .clearFocusOnTapOutside()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Nút quay lại ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToLogin) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Icon email với hiệu ứng ─────────────────────────────────────
            AnimatedVisibility(
                visible = !isSuccess,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    GoldAccent.copy(alpha = 0.15f),
                                    Color(0xFF1F1F1F).copy(alpha = 0.8f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            GoldAccent.copy(alpha = pulseAlpha * 0.4f),
                            RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Email,
                        contentDescription = null,
                        tint = GoldAccent.copy(alpha = pulseAlpha),
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            // ── Icon xác thực thành công ─────────────────────────────────────
            AnimatedVisibility(
                visible = isSuccess,
                enter = fadeIn(tween(400)) + scaleIn(tween(400)),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    SuccessGreen.copy(alpha = 0.15f),
                                    Color(0xFF1F1F1F).copy(alpha = 0.8f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            SuccessGreen.copy(alpha = 0.5f),
                            RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Tiêu đề ─────────────────────────────────────────────────────
            Text(
                text = if (isSuccess) "Xác thực thành công!" else "Xác thực email",
                color = if (isSuccess) SuccessGreen else TextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ── Mô tả ───────────────────────────────────────────────────────
            Text(
                text = if (isSuccess) {
                    "Tài khoản của bạn đã được tạo thành công.\nĐang chuyển về trang đăng nhập..."
                } else {
                    "Chúng tôi đã gửi mã xác thực 6 số đến"
                },
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            if (!isSuccess) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = email,
                    color = GoldAccent,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // ── OTP Input (6 ô số) ──────────────────────────────────────────
            if (!isSuccess) {
                OtpInputRow(
                    otpValue = otpValue,
                    onOtpChanged = { newValue ->
                        // Chỉ cho phép nhập số, tối đa 6 ký tự
                        val filtered = newValue.text.filter { it.isDigit() }.take(6)
                        otpValue = TextFieldValue(
                            text = filtered,
                            selection = TextRange(filtered.length)
                        )
                    },
                    hasError = errorMessage != null,
                    focusRequester = focusRequester
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Thông báo lỗi ────────────────────────────────────────────
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── Nút Xác nhận ─────────────────────────────────────────────
                Button(
                    onClick = {
                        if (otpValue.text.length == 6) {
                            onVerifyCode(otpValue.text)
                        }
                    },
                    enabled = otpValue.text.length == 6 && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = Color.Black,
                        disabledContainerColor = GoldAccent.copy(alpha = 0.35f),
                        disabledContentColor = Color.Black.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (isLoading) {
                        Text(
                            text = "Đang xác thực...",
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Xác nhận",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Gửi lại mã ───────────────────────────────────────────────
                ResendCodeSection(
                    canResend = canResend,
                    countdown = resendCountdown,
                    onResend = {
                        onResendCode()
                        resendCountdown = 30
                        canResend = false
                    }
                )
            }
        }
    }
}

// ── Composable: Hàng nhập OTP gồm 6 ô ──────────────────────────────────────
@Composable
private fun OtpInputRow(
    otpValue: TextFieldValue,
    onOtpChanged: (TextFieldValue) -> Unit,
    hasError: Boolean,
    focusRequester: FocusRequester
) {
    // ── Hiệu ứng shimmer cho viền ô đang chờ nhập ───────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Box(contentAlignment = Alignment.Center) {
        // ── BasicTextField ẩn, xử lý toàn bộ logic nhập ─────────────────────
        BasicTextField(
            value = otpValue,
            onValueChange = onOtpChanged,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            cursorBrush = SolidColor(Color.Transparent),
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                color = Color.Transparent
            ),
            modifier = Modifier
                .focusRequester(focusRequester)
                .size(1.dp) // ẩn text field thật
        )

        // ── 6 ô hiển thị ────────────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { focusRequester.requestFocus() }
        ) {
            repeat(6) { index ->
                val char = otpValue.text.getOrNull(index)
                val isCurrent = index == otpValue.text.length && otpValue.text.length < 6
                val isFilled = char != null

                OtpDigitBox(
                    digit = char,
                    isCurrent = isCurrent,
                    isFilled = isFilled,
                    hasError = hasError,
                    shimmerAlpha = shimmerAlpha,
                    modifier = Modifier.weight(1f).aspectRatio(1f)
                )
            }
        }
    }
}

// ── Composable: Một ô số trong OTP ──────────────────────────────────────────
@Composable
private fun OtpDigitBox(
    digit: Char?,
    isCurrent: Boolean,
    isFilled: Boolean,
    hasError: Boolean,
    shimmerAlpha: Float,
    modifier: Modifier = Modifier
) {
    // ── Hiệu ứng scale khi số xuất hiện ─────────────────────────────────────
    val scale = remember { Animatable(1f) }
    LaunchedEffect(digit) {
        if (digit != null) {
            scale.snapTo(0.8f)
            scale.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
        }
    }

    val borderColor = when {
        hasError -> ErrorRed.copy(alpha = 0.7f)
        isFilled -> GoldAccent.copy(alpha = 0.7f)
        isCurrent -> GoldAccent.copy(alpha = shimmerAlpha)
        else -> CardBorder
    }

    val bgColor = when {
        isFilled -> Color(0xFF1F1F1F)
        isCurrent -> Color(0xFF1A1A1A)
        else -> CardBackground.copy(alpha = 0.7f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (digit != null) {
            Text(
                text = digit.toString(),
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        } else if (isCurrent) {
            // Dấu gạch ngang nhấp nháy cho ô hiện tại
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(2.dp)
                    .background(
                        GoldAccent.copy(alpha = shimmerAlpha),
                        RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

// ── Composable: Phần gửi lại mã với countdown ──────────────────────────────
@Composable
private fun ResendCodeSection(
    canResend: Boolean,
    countdown: Int,
    onResend: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Không nhận được mã?",
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (canResend) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onResend)
                    .background(GoldAccent.copy(alpha = 0.1f))
                    .border(1.dp, GoldAccent.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gửi lại mã",
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gửi lại mã sau ${countdown}s",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
