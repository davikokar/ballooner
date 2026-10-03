package com.ballooner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Ink used to draw the comic itself. These belong to the artwork rather than to the app's chrome,
 * so they stay the same whatever the interface is themed as.
 */
val InkBlack = Color(0xFF000000)
val PaperWhite = Color(0xFFF9F9F9)

/**
 * "Graphic Novel Neo-Brutalist Studio": four-colour process printing translated into
 * high-luminance digital primaries, on high-albedo paper neutrals.
 */
private val BalloonerColors = lightColorScheme(
    primary = Color(0xFF003FCD),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1254FF),
    onPrimaryContainer = Color(0xFFE3E6FF),
    inversePrimary = Color(0xFFB7C4FF),
    secondary = Color(0xFFBB0026),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE71034),
    onSecondaryContainer = Color(0xFFFFFBFF),
    tertiary = Color(0xFF735C00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCFA600),
    onTertiaryContainer = Color(0xFF4E3D00),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF9F9FF),
    onBackground = Color(0xFF191C22),
    surface = Color(0xFFF9F9FF),
    onSurface = Color(0xFF191C22),
    surfaceVariant = Color(0xFFE1E2EB),
    onSurfaceVariant = Color(0xFF434656),
    surfaceTint = Color(0xFF004BF0),
    inverseSurface = Color(0xFF2D3037),
    inverseOnSurface = Color(0xFFEFF0F9),
    outline = Color(0xFF737688),
    outlineVariant = Color(0xFFC3C5D9),
    surfaceBright = Color(0xFFF9F9FF),
    surfaceDim = Color(0xFFD8DAE2),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F3FC),
    surfaceContainer = Color(0xFFECEDF6),
    surfaceContainerHigh = Color(0xFFE6E8F0),
    surfaceContainerHighest = Color(0xFFE1E2EB),
)

@Composable
fun BalloonerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = BalloonerColors,
        typography = BalloonerTypography,
        content = content,
    )
}

/**
 * Chrome stays out of the way of the artwork: a flat paper bar with ink text, separated from the
 * canvas by a line rather than by colour.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun balloonerTopAppBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.surface,
    titleContentColor = MaterialTheme.colorScheme.onSurface,
    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
)
