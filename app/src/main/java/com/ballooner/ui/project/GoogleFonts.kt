package com.ballooner.ui.project

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.ballooner.R

private val googleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

/** Builds a downloadable Google Font family; falls back to the system font until it loads. */
fun googleFontFamily(name: String): FontFamily =
    FontFamily(Font(googleFont = GoogleFont(name), fontProvider = googleFontsProvider))
