package com.ballooner.domain.model

/** Whether balloon text uses a fixed size or auto-fits the balloon. */
enum class TextSizeMode { MANUAL, AUTO }

/** App-wide user preferences applied to every project. */
data class AppSettings(
    val defaultFont: BalloonFont = BalloonFont.DEFAULT,
    val hideFontSelector: Boolean = false,
    val textSizeMode: TextSizeMode = TextSizeMode.MANUAL,
)
