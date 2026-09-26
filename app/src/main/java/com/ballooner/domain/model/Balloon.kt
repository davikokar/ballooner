package com.ballooner.domain.model

/**
 * A single balloon placed on the comic image.
 *
 * All geometry is stored as fractions of the image (0f..1f) so a balloon keeps
 * its relative position and size regardless of how the image is scaled on screen.
 */
data class Balloon(
    val id: Long,
    val type: BalloonType,
    val text: String = "",
    // Center of the balloon as a fraction of the image (0f..1f).
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
    // Balloon body size as a fraction of the image (0f..1f).
    val width: Float = 0.4f,
    val height: Float = 0.25f,
    // Direction the tail points, in degrees clockwise from the positive x-axis.
    val tailAngleDegrees: Float = 90f,
    // How far the tail extends past the body, as a fraction of the image.
    val tailLength: Float = 0.12f,
    // Body corner roundness: 0f = square corners, 1f = fully rounded.
    val cornerRoundness: Float = 1f,
    // Half-width of the tail base, as a fraction of the body's smaller radius.
    val tailWidth: Float = 0.5f,
    // Text size in scale-independent pixels.
    val fontSize: Float = 14f,
    // Font family used to render the text.
    val font: BalloonFont = BalloonFont.DEFAULT,
)

/**
 * Remaps this balloon when its panel is turned [quarterTurns] quarter turns clockwise,
 * from the panel rect [from] to the rect [to] that the turned panel now occupies.
 *
 * The balloon orbits the panel center with the art and its tail angle advances 90 degrees
 * per turn, so the tail keeps pointing at the same content. The body itself does not turn:
 * [Balloon.width], [Balloon.height] and [Balloon.tailLength] are left alone so the text
 * stays upright and the rendered shape is unchanged. Four turns restore the original values.
 */
fun Balloon.remappedByQuarterTurns(from: RectFraction, to: RectFraction, quarterTurns: Int): Balloon {
    val turns = quarterTurns.mod(4)
    // Panel-local offsets, each normalized to its own axis, so a turn simply swaps the axes.
    val dx = (centerX - (from.left + from.width / 2f)) / from.width
    val dy = (centerY - (from.top + from.height / 2f)) / from.height
    val (u, v) = when (turns) {
        1 -> -dy to dx
        2 -> -dx to -dy
        3 -> dy to -dx
        else -> dx to dy
    }
    return copy(
        centerX = to.left + to.width * (0.5f + u),
        centerY = to.top + to.height * (0.5f + v),
        tailAngleDegrees = (tailAngleDegrees + 90f * turns).mod(360f),
    )
}
