package com.ballooner.domain.model

/** The visual style of a balloon, which changes how it is drawn. */
enum class BalloonType {
    SPEAK,
    THINK,
    WHISPER,
    YELL,
    // A tailless rectangle for captions / narration text.
    CAPTION,
}
