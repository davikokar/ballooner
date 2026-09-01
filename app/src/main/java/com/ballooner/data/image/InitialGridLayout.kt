package com.ballooner.data.image

import com.ballooner.domain.model.RectFraction
import kotlin.math.roundToInt

internal data class InitialGridLayout(
    val canvasWidth: Int,
    val canvasHeight: Int,
    val imageRects: List<PixelRect>,
) {
    val panelRects: List<RectFraction>
        get() = imageRects.map { rect ->
            RectFraction(
                left = rect.left.toFloat() / canvasWidth,
                top = rect.top.toFloat() / canvasHeight,
                width = rect.width.toFloat() / canvasWidth,
                height = rect.height.toFloat() / canvasHeight,
            )
        }
}

internal fun computeInitialGridLayout(
    imageSizes: List<PixelSize>,
    columns: Int,
    columnWidth: Int,
    gapPx: Int,
): InitialGridLayout {
    require(imageSizes.isNotEmpty())
    val actualColumns = columns.coerceAtLeast(1).coerceAtMost(imageSizes.size)
    val scaledSizes = imageSizes.map { size ->
        PixelSize(
            width = columnWidth,
            height = (size.height.toFloat() / size.width * columnWidth).roundToInt().coerceAtLeast(1),
        )
    }
    val rowHeights = scaledSizes.chunked(actualColumns).map { row -> row.maxOf { it.height } }
    val rowTops = rowHeights.runningFold(0) { top, height -> top + height + gapPx }.dropLast(1)
    val rects = scaledSizes.mapIndexed { index, size ->
        val row = index / actualColumns
        val column = index % actualColumns
        PixelRect(
            left = column * (columnWidth + gapPx),
            top = rowTops[row],
            width = size.width,
            height = size.height,
        )
    }
    return InitialGridLayout(
        canvasWidth = actualColumns * columnWidth + (actualColumns - 1) * gapPx,
        canvasHeight = rowHeights.sum() + (rowHeights.size - 1) * gapPx,
        imageRects = rects,
    )
}