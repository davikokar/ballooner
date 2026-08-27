package com.ballooner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BalloonerBlue = Color(0xFF004AC5)

// Blue app background with white titles and icons on top of it.
private val BalloonerColors = lightColorScheme(
    background = BalloonerBlue,
    onBackground = Color.White,
    surface = BalloonerBlue,
    onSurface = Color.White,
    onSurfaceVariant = Color.White,
)

@Composable
fun BalloonerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = BalloonerColors,
        typography = Typography(),
        content = content,
    )
}
