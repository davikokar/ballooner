package com.ballooner.ui.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.davide.seddio.ballooner.R
import com.ballooner.domain.model.BalloonFont
import com.ballooner.domain.model.BalloonType

/** Human-readable name for each balloon type, shared by every control that offers one. */
internal val BalloonType.label: Int
    @StringRes get() = when (this) {
        BalloonType.SPEAK -> R.string.balloon_type_speak
        BalloonType.THINK -> R.string.balloon_type_think
        BalloonType.WHISPER -> R.string.balloon_type_whisper
        BalloonType.YELL -> R.string.balloon_type_yell
        BalloonType.CAPTION -> R.string.balloon_type_caption
    }

/** Human-readable name for each font, shared by the editor and settings. */
@Composable
internal fun BalloonFont.label(): String = stringResource(when (this) {
    BalloonFont.DEFAULT -> R.string.font_default
    BalloonFont.SANS_SERIF -> R.string.font_sans_serif
    BalloonFont.SERIF -> R.string.font_serif
    BalloonFont.MONOSPACE -> R.string.font_fixed_width
    BalloonFont.CURSIVE -> R.string.font_cursive
    BalloonFont.WIDE -> R.string.font_wide
    BalloonFont.NARROW -> R.string.font_narrow
    BalloonFont.COMIC_SANS_MS -> R.string.font_comic_sans_ms
    BalloonFont.GARAMOND -> R.string.font_garamond
    BalloonFont.GEORGIA -> R.string.font_georgia
    BalloonFont.TAHOMA -> R.string.font_tahoma
    BalloonFont.TREBUCHET -> R.string.font_trebuchet
    BalloonFont.VERDANA -> R.string.font_verdana
    BalloonFont.ANIME_ACE -> R.string.font_anime_ace
})
