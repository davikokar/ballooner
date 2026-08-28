package com.ballooner.data.image

import com.ballooner.domain.model.ImagePosition
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
    val borderPx: Int,
    val existingRect: RectFraction,
)

// A small gap keeps the two panels from touching; both it and the border scale with the
// matched dimension so they look consistent regardless of the source photos' resolution.
private const val GAP_FRACTION = 0.02f
private const val MIN_GAP_PX = 8
private const val BORDER_FRACTION = 0.006f
private const val MIN_BORDER_PX = 3

/**
 * Computes the composite canvas size and where the existing and added images land within it,
 * given their pixel dimensions. The added image is scaled to match the existing image's height
 * (left/right) or width (top/bottom), preserving its own aspect ratio, and a gap is left between
 * the two so they don't touch.
 */
internal fun computeComposeLayout(
    existingWidth: Int,
    existingHeight: Int,
    addedWidth: Int,
    addedHeight: Int,
    position: ImagePosition,
): ComposeLayout = when (position) {
    ImagePosition.LEFT, ImagePosition.RIGHT -> {
        val gap = (existingHeight * GAP_FRACTION).roundToInt().coerceAtLeast(MIN_GAP_PX)
        val border = (existingHeight * BORDER_FRACTION).roundToInt().coerceAtLeast(MIN_BORDER_PX)
        val scaledAddedWidth = (addedWidth.toFloat() * existingHeight / addedHeight).roundToInt().coerceAtLeast(1)
        val canvasWidth = existingWidth + gap + scaledAddedWidth
        val existingLeft = if (position == ImagePosition.LEFT) scaledAddedWidth + gap else 0
        val addedLeft = if (position == ImagePosition.LEFT) 0 else existingWidth + gap
        ComposeLayout(
            canvasWidth = canvasWidth,
            canvasHeight = existingHeight,
            existingLeft = existingLeft,
            existingTop = 0,
            scaledAddedWidth = scaledAddedWidth,
            scaledAddedHeight = existingHeight,
            addedLeft = addedLeft,
            addedTop = 0,
            borderPx = border,
            existingRect = RectFraction(
                left = existingLeft.toFloat() / canvasWidth,
                top = 0f,
                width = existingWidth.toFloat() / canvasWidth,
                height = 1f,
            ),
        )
    }
    ImagePosition.TOP, ImagePosition.BOTTOM -> {
        val gap = (existingWidth * GAP_FRACTION).roundToInt().coerceAtLeast(MIN_GAP_PX)
        val border = (existingWidth * BORDER_FRACTION).roundToInt().coerceAtLeast(MIN_BORDER_PX)
        val scaledAddedHeight = (addedHeight.toFloat() * existingWidth / addedWidth).roundToInt().coerceAtLeast(1)
        val canvasHeight = existingHeight + gap + scaledAddedHeight
        val existingTop = if (position == ImagePosition.TOP) scaledAddedHeight + gap else 0
        val addedTop = if (position == ImagePosition.TOP) 0 else existingHeight + gap
        ComposeLayout(
            canvasWidth = existingWidth,
            canvasHeight = canvasHeight,
            existingLeft = 0,
            existingTop = existingTop,
            scaledAddedWidth = existingWidth,
            scaledAddedHeight = scaledAddedHeight,
            addedLeft = 0,
            addedTop = addedTop,
            borderPx = border,
            existingRect = RectFraction(
                left = 0f,
                top = existingTop.toFloat() / canvasHeight,
                width = 1f,
                height = existingHeight.toFloat() / canvasHeight,
            ),
        )
    }
}

