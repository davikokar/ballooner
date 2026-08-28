package com.ballooner.data.image

import com.ballooner.domain.model.ImagePosition
import kotlin.math.roundToInt

/** The pixel geometry for compositing two images per [ImagePosition]. */
internal data class ComposeLayout(
    val canvasWidth: Int,
    val canvasHeight: Int,
    // The added image is scaled to this size before being drawn onto the canvas.
    val scaledAddedWidth: Int,
    val scaledAddedHeight: Int,
    val existingRect: RectFraction,
)

/**
 * Computes the composite canvas size and where the existing image lands within it, given the
 * existing and added images' pixel dimensions. The added image is scaled to match the existing
 * image's height (left/right) or width (top/bottom), preserving its own aspect ratio.
 */
internal fun computeComposeLayout(
    existingWidth: Int,
    existingHeight: Int,
    addedWidth: Int,
    addedHeight: Int,
    position: ImagePosition,
): ComposeLayout = when (position) {
    ImagePosition.LEFT, ImagePosition.RIGHT -> {
        val scaledAddedWidth = (addedWidth.toFloat() * existingHeight / addedHeight).roundToInt().coerceAtLeast(1)
        val canvasWidth = existingWidth + scaledAddedWidth
        val existingLeft = if (position == ImagePosition.LEFT) scaledAddedWidth else 0
        ComposeLayout(
            canvasWidth = canvasWidth,
            canvasHeight = existingHeight,
            scaledAddedWidth = scaledAddedWidth,
            scaledAddedHeight = existingHeight,
            existingRect = RectFraction(
                left = existingLeft.toFloat() / canvasWidth,
                top = 0f,
                width = existingWidth.toFloat() / canvasWidth,
                height = 1f,
            ),
        )
    }
    ImagePosition.TOP, ImagePosition.BOTTOM -> {
        val scaledAddedHeight = (addedHeight.toFloat() * existingWidth / addedWidth).roundToInt().coerceAtLeast(1)
        val canvasHeight = existingHeight + scaledAddedHeight
        val existingTop = if (position == ImagePosition.TOP) scaledAddedHeight else 0
        ComposeLayout(
            canvasWidth = existingWidth,
            canvasHeight = canvasHeight,
            scaledAddedWidth = existingWidth,
            scaledAddedHeight = scaledAddedHeight,
            existingRect = RectFraction(
                left = 0f,
                top = existingTop.toFloat() / canvasHeight,
                width = 1f,
                height = existingHeight.toFloat() / canvasHeight,
            ),
        )
    }
}
