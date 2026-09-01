package com.ballooner.data.image

import com.ballooner.domain.model.ImagePosition
import com.ballooner.domain.model.RectFraction
import kotlin.math.roundToInt

/** The pixel geometry for compositing two images per [ImagePosition]. */
internal data class PanelBorder(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
    val strokeWidth: Int,
)

internal data class ComposeLayout(
    val canvasWidth: Int,
    val canvasHeight: Int,
    val existingLeft: Int,
    val existingTop: Int,
    // The added image is proportionally scaled to this size before being drawn.
    val scaledAddedWidth: Int,
    val scaledAddedHeight: Int,
    val addedLeft: Int,
    val addedTop: Int,
    // Existing pixels already contain their individual borders; only the new panel needs one.
    val bordersToDraw: List<PanelBorder>,
    val existingRect: RectFraction,
    val addedRect: RectFraction,
)

// A small gap keeps the two panels from touching, scaled with the matched dimension so it
// looks consistent regardless of the source photos' resolution.
private const val GAP_FRACTION = 0.02f
private const val MIN_GAP_PX = 8
private const val BORDER_FRACTION = 0.006f
private const val MIN_BORDER_PX = 3
internal const val COMIC_CANVAS_BACKGROUND_COLOR = 0x00000000

/** Border thickness for an image of this size, matching the balloons' thin outline ratio. */
internal fun borderThicknessPx(width: Int, height: Int): Int =
    (minOf(width, height) * BORDER_FRACTION).roundToInt().coerceAtLeast(MIN_BORDER_PX)

/** Computes the canvas geometry for a new image placed beside [anchor]. */
internal fun computeComposeLayout(
    existingWidth: Int,
    existingHeight: Int,
    addedWidth: Int,
    addedHeight: Int,
    position: ImagePosition,
    anchor: RectFraction = RectFraction(0f, 0f, 1f, 1f),
): ComposeLayout {
    val anchorLeft = (anchor.left * existingWidth).roundToInt()
    val anchorTop = (anchor.top * existingHeight).roundToInt()
    val anchorWidth = (anchor.width * existingWidth).roundToInt().coerceAtLeast(1)
    val anchorHeight = (anchor.height * existingHeight).roundToInt().coerceAtLeast(1)
    val scale = when (position) {
        ImagePosition.LEFT, ImagePosition.RIGHT -> anchorHeight.toFloat() / addedHeight
        ImagePosition.TOP, ImagePosition.BOTTOM -> anchorWidth.toFloat() / addedWidth
    }
    val scaledAddedWidth = (addedWidth * scale).roundToInt().coerceAtLeast(1)
    val scaledAddedHeight = (addedHeight * scale).roundToInt().coerceAtLeast(1)
    val matchedExtent = when (position) {
        ImagePosition.LEFT, ImagePosition.RIGHT -> maxOf(existingHeight, scaledAddedHeight)
        ImagePosition.TOP, ImagePosition.BOTTOM -> maxOf(existingWidth, scaledAddedWidth)
    }
    val gap = (matchedExtent * GAP_FRACTION).roundToInt().coerceAtLeast(MIN_GAP_PX)
    val unshiftedAddedLeft = when (position) {
        ImagePosition.LEFT -> anchorLeft - gap - scaledAddedWidth
        ImagePosition.RIGHT -> anchorLeft + anchorWidth + gap
        ImagePosition.TOP, ImagePosition.BOTTOM -> anchorLeft + (anchorWidth - scaledAddedWidth) / 2
    }
    val unshiftedAddedTop = when (position) {
        ImagePosition.TOP -> anchorTop - gap - scaledAddedHeight
        ImagePosition.BOTTOM -> anchorTop + anchorHeight + gap
        ImagePosition.LEFT, ImagePosition.RIGHT -> anchorTop + (anchorHeight - scaledAddedHeight) / 2
    }
    val minLeft = minOf(0, unshiftedAddedLeft)
    val minTop = minOf(0, unshiftedAddedTop)
    val canvasWidth = maxOf(existingWidth, unshiftedAddedLeft + scaledAddedWidth) - minLeft
    val canvasHeight = maxOf(existingHeight, unshiftedAddedTop + scaledAddedHeight) - minTop
    val existingLeft = -minLeft
    val existingTop = -minTop
    val addedLeft = unshiftedAddedLeft - minLeft
    val addedTop = unshiftedAddedTop - minTop

    return ComposeLayout(
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        existingLeft = existingLeft,
        existingTop = existingTop,
        scaledAddedWidth = scaledAddedWidth,
        scaledAddedHeight = scaledAddedHeight,
        addedLeft = addedLeft,
        addedTop = addedTop,
        bordersToDraw = listOf(
            PanelBorder(
                left = addedLeft,
                top = addedTop,
                width = scaledAddedWidth,
                height = scaledAddedHeight,
                strokeWidth = borderThicknessPx(scaledAddedWidth, scaledAddedHeight),
            ),
        ),
        existingRect = RectFraction(
            left = existingLeft.toFloat() / canvasWidth,
            top = existingTop.toFloat() / canvasHeight,
            width = existingWidth.toFloat() / canvasWidth,
            height = existingHeight.toFloat() / canvasHeight,
        ),
        addedRect = RectFraction(
            left = addedLeft.toFloat() / canvasWidth,
            top = addedTop.toFloat() / canvasHeight,
            width = scaledAddedWidth.toFloat() / canvasWidth,
            height = scaledAddedHeight.toFloat() / canvasHeight,
        ),
    )
}

