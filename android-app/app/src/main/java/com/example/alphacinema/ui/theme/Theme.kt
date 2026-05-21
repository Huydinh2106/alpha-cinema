package com.example.alphacinema.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AlphaGold,
    onPrimary = NetflixBlack,
    secondary = AlphaMutedText,
    onSecondary = NetflixBlack,
    tertiary = NetflixSurfaceHighest,
    onTertiary = Color.White,
    background = NetflixBlack,
    onBackground = Color.White,
    surface = NetflixSurface,
    onSurface = Color.White,
    surfaceVariant = NetflixSurfaceHigh,
    onSurfaceVariant = AlphaMutedText,
    outline = NetflixBorder
)

private val LightColorScheme = lightColorScheme(
    primary = AlphaGold,
    onPrimary = NetflixLightOnSurface,
    secondary = NetflixSurfaceHighest,
    onSecondary = Color.White,
    tertiary = NetflixBorder,
    onTertiary = Color.White,
    background = NetflixLightBackground,
    onBackground = NetflixLightOnSurface,
    surface = NetflixLightSurface,
    onSurface = NetflixLightOnSurface,
    surfaceVariant = Color(0xFFE6E6E6),
    onSurfaceVariant = Color(0xFF4D4D4D),
    outline = Color(0xFFB8B8B8)
)

@Composable
fun AlphaCinemaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> if (darkTheme) DarkColorScheme else LightColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
