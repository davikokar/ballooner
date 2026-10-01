package com.ballooner.ui.comic

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.ballooner.domain.comic.Balloon
import com.ballooner.domain.comic.MAX_BALLOON_TEXT_SIZE
import com.ballooner.domain.comic.MIN_BALLOON_TEXT_SIZE
import com.ballooner.ui.theme.InkBlack
import com.ballooner.ui.theme.toFontFamily
import kotlin.math.roundToInt

/** How wide the text is wrapped to inside [geometry], in pixels. */
internal fun textWidth(geometry: BalloonGeometry): Int =
    (geometry.radiusX * 2f * TEXT_INSET).roundToInt().coerceAtLeast(1)

/** How the balloon's words are set, at a size already worked out in pixels. */
internal fun balloonTextStyle(balloon: Balloon, sizePx: Float, density: Density) = TextStyle(
    color = InkBlack,
    fontSize = with(density) { sizePx.toSp() },
    fontFamily = balloon.font.toFontFamily(),
    textAlign = TextAlign.Center,
)

/**
 * The size the balloon's words are set at, in pixels.
 *
 * A balloon left to size itself takes the largest size its words still fit it at, found by
 * halving the range rather than by stepping, so a long line costs no more than a short one. The
 * same answer has to come out wherever it is asked, or the text typed into a balloon would not
 * match the text drawn in it.
 */
internal fun balloonTextSize(
    balloon: Balloon,
    geometry: BalloonGeometry,
    textMeasurer: TextMeasurer,
    pageScale: Float,
    density: Density,
): Float {
    if (!balloon.autoSize) return balloon.fontSize * pageScale
    val width = textWidth(geometry)
    val height = (geometry.radiusY * 2f * TEXT_INSET).coerceAtLeast(1f)
    if (balloon.text.isBlank()) return balloon.fontSize * pageScale
    var tooSmall = MIN_BALLOON_TEXT_SIZE * pageScale
    var tooBig = MAX_BALLOON_TEXT_SIZE * pageScale
    repeat(AUTO_SIZE_STEPS) {
        val size = (tooSmall + tooBig) / 2f
        val measured = textMeasurer.measure(
            text = balloon.text,
            style = balloonTextStyle(balloon, size, density),
            constraints = Constraints(maxWidth = width),
        )
        if (measured.size.height <= height && measured.size.width <= width) tooSmall = size else tooBig = size
    }
    return tooSmall
}

/** Enough halvings to land within a pixel or so of the largest size that fits. */
private const val AUTO_SIZE_STEPS = 7
