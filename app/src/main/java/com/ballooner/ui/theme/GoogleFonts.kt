package com.ballooner.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.davide.seddio.ballooner.R

private val googleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

/**
 * Builds a downloadable Google Font family; falls back to the system font until it loads.
 *
 * Each weight is requested in its own right rather than left to be synthesised, because a faked
 * bold of a geometric face loses the sharp corners it was chosen for.
 */
fun googleFontFamily(
    name: String,
    weights: List<FontWeight> = listOf(FontWeight.Normal),
): FontFamily = FontFamily(
    weights.map { weight ->
        Font(googleFont = GoogleFont(name), fontProvider = googleFontsProvider, weight = weight)
    },
)
