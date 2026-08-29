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
    // The added image is center-cropped to this size before being drawn onto the canvas.
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
 * Computes the composite canvas size and where the existing and added images land within it.
 * The added image occupies a box [widthSpan] by [heightSpan] times a standard panel unit — the
 * existing image's height for left/right, its width for top/bottom — so its footprint is always
 * a whole multiple of one panel in each dimension (it gets center-cropped to fit exactly, see
 * [AppImageStore]). A gap is left between the two panels so they don't touch, and the existing
 * content is centered within the canvas if the added panel ends up larger than it.
 */
internal fun computeComposeLayout(
    existingWidth: Int,
    existingHeight: Int,
    position: ImagePosition,
    widthSpan: Int = 1,
    heightSpan: Int = 1,
): ComposeLayout = when (position) {
    ImagePosition.LEFT, ImagePosition.RIGHT -> {
        val unit = existingHeight
        val scaledAddedWidth = unit * widthSpan
        val scaledAddedHeight = unit * heightSpan
        val canvasHeight = maxOf(existingHeight, scaledAddedHeight)
        val gap = (canvasHeight * GAP_FRACTION).roundToInt().coerceAtLeast(MIN_GAP_PX)
        val canvasWidth = existingWidth + gap + scaledAddedWidth
        val existingTop = (canvasHeight - existingHeight) / 2
        val addedTop = (canvasHeight - scaledAddedHeight) / 2
        val existingLeft = if (position == ImagePosition.LEFT) scaledAddedWidth + gap else 0
        val addedLeft = if (position == ImagePosition.LEFT) 0 else existingWidth + gap
        ComposeLayout(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            existingLeft = existingLeft,
            existingTop = existingTop,
            scaledAddedWidth = scaledAddedWidth,
            scaledAddedHeight = scaledAddedHeight,
            addedLeft = addedLeft,
            addedTop = addedTop,
            existingBorderPx = borderThicknessPx(existingWidth, existingHeight),
            addedBorderPx = borderThicknessPx(scaledAddedWidth, scaledAddedHeight),
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
    ImagePosition.TOP, ImagePosition.BOTTOM -> {
        val unit = existingWidth
        val scaledAddedWidth = unit * widthSpan
        val scaledAddedHeight = unit * heightSpan
        val canvasWidth = maxOf(existingWidth, scaledAddedWidth)
        val gap = (canvasWidth * GAP_FRACTION).roundToInt().coerceAtLeast(MIN_GAP_PX)
        val canvasHeight = existingHeight + gap + scaledAddedHeight
        val existingLeft = (canvasWidth - existingWidth) / 2
        val addedLeft = (canvasWidth - scaledAddedWidth) / 2
        val existingTop = if (position == ImagePosition.TOP) scaledAddedHeight + gap else 0
        val addedTop = if (position == ImagePosition.TOP) 0 else existingHeight + gap
        ComposeLayout(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            existingLeft = existingLeft,
            existingTop = existingTop,
            scaledAddedWidth = scaledAddedWidth,
            scaledAddedHeight = scaledAddedHeight,
            addedLeft = addedLeft,
            addedTop = addedTop,
            existingBorderPx = borderThicknessPx(existingWidth, existingHeight),
            addedBorderPx = borderThicknessPx(scaledAddedWidth, scaledAddedHeight),
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
}

