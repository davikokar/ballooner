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

val BalloonerBlue = Color(0xFF2563EB)

// "Action Narrative" comic palette: hard-edged inking on vibrant primaries.
val InkBlack = Color(0xFF000000)
val PaperWhite = Color(0xFFF9F9F9)
val VibrantBlue = BalloonerBlue
val EnergeticRed = Color(0xFFDC2626)
val ComicYellow = Color(0xFFFACC15)

// White page background with blue top bars (see balloonerTopAppBarColors).
private val BalloonerColors = lightColorScheme(
    primary = VibrantBlue,
    onPrimary = Color.White,
    secondary = EnergeticRed,
    onSecondary = Color.White,
    tertiary = ComicYellow,
    onTertiary = InkBlack,
    background = PaperWhite,
    onBackground = InkBlack,
    surface = Color.White,
    onSurface = InkBlack,
    surfaceVariant = Color(0xFFE2E2E2),
    onSurfaceVariant = Color(0xFF434655),
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
