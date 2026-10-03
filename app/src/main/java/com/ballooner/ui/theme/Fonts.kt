package com.ballooner.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.davide.seddio.ballooner.R
import com.ballooner.domain.model.BalloonFont

/** Comic-lettering font bundled with the app, used for balloon text and app branding. */
val AnimeAceFontFamily = FontFamily(
    Font(R.font.animeace2_reg, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.animeace2_bld, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.animeace2_ital, FontWeight.Normal, FontStyle.Italic),
)

/** The typeface a balloon's text is drawn in. */
fun BalloonFont.toFontFamily(): FontFamily = when (this) {
    BalloonFont.DEFAULT -> FontFamily.Default
    BalloonFont.SANS_SERIF -> FontFamily.SansSerif
    BalloonFont.SERIF -> FontFamily.Serif
    BalloonFont.MONOSPACE -> FontFamily.Monospace
    BalloonFont.CURSIVE -> FontFamily.Cursive
    BalloonFont.WIDE -> googleFontFamily("Michroma")
    BalloonFont.NARROW -> googleFontFamily("Archivo Narrow")
    BalloonFont.COMIC_SANS_MS -> googleFontFamily("Comic Neue")
    BalloonFont.GARAMOND -> googleFontFamily("EB Garamond")
    BalloonFont.GEORGIA -> googleFontFamily("Gelasio")
    BalloonFont.TAHOMA -> googleFontFamily("PT Sans")
    BalloonFont.TREBUCHET -> googleFontFamily("Fira Sans")
    BalloonFont.VERDANA -> googleFontFamily("Noto Sans")
    BalloonFont.ANIME_ACE -> AnimeAceFontFamily
}
