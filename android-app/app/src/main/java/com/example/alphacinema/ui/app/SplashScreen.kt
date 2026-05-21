package com.example.alphacinema.ui.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.alphacinema.R

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    // Trạng thái fade-in: bắt đầu từ tối (0f) → hiện ảnh (1f)
    var visible by remember { mutableStateOf(false) }

    // Kích hoạt fade-in ngay khi composable xuất hiện lần đầu
    LaunchedEffect(Unit) {
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "splash_fade_in"
    )

    // Spinner xoay
    val infiniteTransition = rememberInfiniteTransition(label = "splash_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinner_rotation"
    )

    // Lottie logo animation
    val lottieComposition by rememberLottieComposition(
        LottieCompositionSpec.Asset("logo_animation.json")
    )
    val lottieProgress by animateLottieCompositionAsState(
        composition = lottieComposition,
        iterations = 1,
        isPlaying = true
    )

    // Khi animation chạy xong (progress == 1f) thì chuyển sang màn hình chính
    LaunchedEffect(lottieProgress) {
        if (lottieProgress == 1f) {
            onFinished()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Nền tối giống System Splash — hiển thị ngay, không cần đợi
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )

        // Ảnh nền fade in mượt mà từ nền tối
        Image(
            painter = painterResource(id = R.drawable.splash_background),
            contentDescription = "Splash Screen",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { this.alpha = alpha }
        )

        // ── Logo Lottie animation ở giữa màn hình ──
        LottieAnimation(
            composition = lottieComposition,
            progress = { lottieProgress },
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.Center)
                .size(400.dp)
                .graphicsLayer { this.alpha = alpha }
        )

        // Spinner + text "Đang tải" ở phía dưới, cũng fade in cùng lúc
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .graphicsLayer { this.alpha = alpha },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            com.example.alphacinema.ui.components.LottieLoadingIndicator(size = 140.dp)

            Text(
                text = "Đang tải",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
                modifier = Modifier.offset(y = (-24).dp)
            )
        }
    }
}
