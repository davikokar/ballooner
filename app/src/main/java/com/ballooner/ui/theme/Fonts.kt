package com.ballooner.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.ballooner.R

/** Comic-lettering font bundled with the app, used for balloon text and app branding. */
val AnimeAceFontFamily = FontFamily(
    Font(R.font.animeace2_reg, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.animeace2_bld, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.animeace2_ital, FontWeight.Normal, FontStyle.Italic),
)
