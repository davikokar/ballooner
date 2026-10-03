package com.ballooner.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

private val UiWeights = listOf(
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold,
)

/**
 * Headings, labels, and badges: mechanical corners and geometric punch, in the spirit of graphic
 * novel title lettering.
 */
private val Display = googleFontFamily("Space Grotesk", UiWeights)

/** Interface copy and tool readouts, chosen to stay legible down to 11sp. */
private val Body = googleFontFamily("Plus Jakarta Sans", UiWeights)

private fun display(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    tracking: Float,
    family: FontFamily = Display,
) = TextStyle(
    fontFamily = family,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight,
    letterSpacing = tracking.em,
)

/**
 * The type scale, mapped onto Material's slots so every existing screen picks it up.
 *
 * Material's slots are used for what they are named rather than for their default sizes: the
 * whole scale is deliberately smaller than Material's, because chrome is meant to leave the
 * canvas the screen.
 */
val BalloonerTypography = Typography(
    displayLarge = display(32, 38, FontWeight.Bold, -0.03f),
    displayMedium = display(26, 32, FontWeight.Bold, -0.02f),
    displaySmall = display(22, 28, FontWeight.Bold, -0.02f),
    headlineLarge = display(24, 30, FontWeight.Bold, -0.02f),
    headlineMedium = display(20, 26, FontWeight.Bold, -0.02f),
    headlineSmall = display(16, 22, FontWeight.SemiBold, -0.01f),
    titleLarge = display(20, 26, FontWeight.Bold, -0.02f),
    titleMedium = display(16, 22, FontWeight.SemiBold, -0.01f),
    titleSmall = display(14, 20, FontWeight.SemiBold, 0f),
    bodyLarge = display(15, 22, FontWeight.Medium, 0f, Body),
    bodyMedium = display(13, 18, FontWeight.Normal, 0f, Body),
    bodySmall = display(11, 15, FontWeight.Normal, 0f, Body),
    labelLarge = display(12, 16, FontWeight.Bold, 0.03f),
    labelMedium = display(10, 14, FontWeight.Bold, 0.04f),
    // The caps token: used for tool presets, counters, and badges.
    labelSmall = display(9, 12, FontWeight.Bold, 0.08f),
)
