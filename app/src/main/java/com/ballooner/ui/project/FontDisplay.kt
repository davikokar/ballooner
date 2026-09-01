package com.ballooner.ui.project

import com.ballooner.domain.model.BalloonFont

/** Human-readable name for each font, shared by the editor and settings. */
internal fun BalloonFont.label(): String = when (this) {
    BalloonFont.DEFAULT -> "Default"
    BalloonFont.SANS_SERIF -> "Sans serif"
    BalloonFont.SERIF -> "Serif"
    BalloonFont.MONOSPACE -> "Fixed width"
    BalloonFont.CURSIVE -> "Cursive"
    BalloonFont.WIDE -> "Wide"
    BalloonFont.NARROW -> "Narrow"
    BalloonFont.COMIC_SANS_MS -> "Comic Sans MS"
    BalloonFont.GARAMOND -> "Garamond"
    BalloonFont.GEORGIA -> "Georgia"
    BalloonFont.TAHOMA -> "Tahoma"
    BalloonFont.TREBUCHET -> "Trebuchet"
    BalloonFont.VERDANA -> "Verdana"
    BalloonFont.ANIME_ACE -> "Anime Ace"
}
