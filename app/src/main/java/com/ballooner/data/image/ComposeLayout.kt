package com.ballooner.data.image

import com.ballooner.domain.model.ImagePosition
import com.ballooner.domain.model.RectFraction
import kotlin.math.roundToInt

/** The pixel geometry for compositing two images per [ImagePosition]. */
internal data class ComposeLayout(
    val canvasWidth: Int,
    val canvasHeight: Int,
    val existingLeft: Int,
    val existingTop: Int,
    // The added image is scaled to this size before being drawn onto the canvas.
    val scaledAddedWidth: Int,
    val scaledAddedHeight: Int,
    val addedLeft: Int,
    val addedTop: Int,
    // Thickness of the border drawn around each panel, matching the balloons' thin outline.
    val existingBorderPx: Int,
    val addedBorderPx: Int,
    val existingRect: RectFraction,
    val addedRect: RectFraction,
)

// A small gap keeps the two panels from touching, scaled with the matched dimension so it
// looks consistent regardless of the source photos' resolution.
private const val GAP_FRACTION = 0.02f
private const val MIN_GAP_PX = 8
private const val BORDER_FRACTION = 0.006f
private const val MIN_BORDER_PX = 3

/** Border thickness for an image of this size, matching the balloons' thin outline ratio. */
internal fun borderThicknessPx(width: Int, height: Int): Int =
    (minOf(width, height) * BORDER_FRACTION).roundToInt().coerceAtLeast(MIN_BORDER_PX)

/**
 * Computes the composite canvas size and where the existing and added images land within it,
 * given their pixel dimensions. The added image is scaled so its matched dimension (height for
 * left/right, width for top/bottom) is [sizeSpan] times the existing image's — letting it occupy
 * the footprint of one or two standard panels — preserving its own aspect ratio. A gap is left
 * between the two panels so they don't touch, and the existing content is centered within the
 * canvas if the added panel's span makes it larger than the existing content.
 */
internal fun computeComposeLayout(
    existingWidth: Int,
    existingHeight: Int,
    addedWidth: Int,
    addedHeight: Int,
    position: ImagePosition,
    sizeSpan: Int = 1,
): ComposeLayout = when (position) {
    ImagePosition.LEFT, ImagePosition.RIGHT -> {
        val canvasHeight = existingHeight * sizeSpan
        val scaledAddedWidth = (addedWidth.toFloat() * canvasHeight / addedHeight).roundToInt().coerceAtLeast(1)
        val gap = (canvasHeight * GAP_FRACTION).roundToInt().coerceAtLeast(MIN_GAP_PX)
        val canvasWidth = existingWidth + gap + scaledAddedWidth
        val existingTop = (canvasHeight - existingHeight) / 2
        val existingLeft = if (position == ImagePosition.LEFT) scaledAddedWidth + gap else 0
        val addedLeft = if (position == ImagePosition.LEFT) 0 else existingWidth + gap
        ComposeLayout(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            existingLeft = existingLeft,
            existingTop = existingTop,
            scaledAddedWidth = scaledAddedWidth,
            scaledAddedHeight = canvasHeight,
            addedLeft = addedLeft,
            addedTop = 0,
            existingBorderPx = borderThicknessPx(existingWidth, existingHeight),
            addedBorderPx = borderThicknessPx(scaledAddedWidth, canvasHeight),
            existingRect = RectFraction(
                left = existingLeft.toFloat() / canvasWidth,
                top = existingTop.toFloat() / canvasHeight,
                width = existingWidth.toFloat() / canvasWidth,
                height = existingHeight.toFloat() / canvasHeight,
            ),
            addedRect = RectFraction(
                left = addedLeft.toFloat() / canvasWidth,
                top = 0f,
                width = scaledAddedWidth.toFloat() / canvasWidth,
                height = 1f,
            ),
        )
    }
    ImagePosition.TOP, ImagePosition.BOTTOM -> {
        val canvasWidth = existingWidth * sizeSpan
        val scaledAddedHeight = (addedHeight.toFloat() * canvasWidth / addedWidth).roundToInt().coerceAtLeast(1)
        val gap = (canvasWidth * GAP_FRACTION).roundToInt().coerceAtLeast(MIN_GAP_PX)
        val canvasHeight = existingHeight + gap + scaledAddedHeight
        val existingLeft = (canvasWidth - existingWidth) / 2
        val existingTop = if (position == ImagePosition.TOP) scaledAddedHeight + gap else 0
        val addedTop = if (position == ImagePosition.TOP) 0 else existingHeight + gap
        ComposeLayout(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            existingLeft = existingLeft,
            existingTop = existingTop,
            scaledAddedWidth = canvasWidth,
            scaledAddedHeight = scaledAddedHeight,
            addedLeft = 0,
            addedTop = addedTop,
            existingBorderPx = borderThicknessPx(existingWidth, existingHeight),
            addedBorderPx = borderThicknessPx(canvasWidth, scaledAddedHeight),
            existingRect = RectFraction(
                left = existingLeft.toFloat() / canvasWidth,
                top = existingTop.toFloat() / canvasHeight,
                width = existingWidth.toFloat() / canvasWidth,
                height = existingHeight.toFloat() / canvasHeight,
            ),
            addedRect = RectFraction(
                left = 0f,
                top = addedTop.toFloat() / canvasHeight,
                width = 1f,
                height = scaledAddedHeight.toFloat() / canvasHeight,
            ),
        )
    }
}

