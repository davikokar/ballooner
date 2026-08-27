package com.ballooner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BalloonerBlue = Color(0xFF004AC5)

// White page background with blue top bars (see balloonerTopAppBarColors).
private val BalloonerColors = lightColorScheme(
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
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

/** Blue top bar with white title and icons, used across the app's screens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun balloonerTopAppBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = BalloonerBlue,
    titleContentColor = Color.White,
    navigationIconContentColor = Color.White,
    actionIconContentColor = Color.White,
)
