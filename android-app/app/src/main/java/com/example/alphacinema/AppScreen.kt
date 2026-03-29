@file:OptIn(ExperimentalFoundationApi::class)
package com.example.alphacinema

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

enum class ScreenType {
    HOME,
    SEARCH,
    SCHEDULE,
    ACCOUNT
}

@Composable
fun AppScreen(modifier: Modifier = Modifier) {
    var currentScreen by remember { mutableStateOf(ScreenType.HOME) }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF070B16))) {
        Crossfade(
            targetState = currentScreen,
            animationSpec = tween(200),
            modifier = Modifier.fillMaxSize(),
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                ScreenType.HOME -> HomeScreen()
                ScreenType.SEARCH -> SearchScreen()
                ScreenType.SCHEDULE -> HomeScreen() // TODO
                ScreenType.ACCOUNT -> HomeScreen() // TODO
            }
        }

        GlassBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            currentScreen = currentScreen,
            onNavigate = { currentScreen = it }
        )
    }
}

