package com.ballooner.data.image

internal data class PixelSize(val width: Int, val height: Int)

internal data class PixelRect(val left: Int, val top: Int, val width: Int, val height: Int)

internal data class RearrangeLayout(
    val canvasWidth: Int,
    val canvasHeight: Int,
    val panelRects: List<PixelRect>,
)

internal fun computeRearrangeLayout(
    panelSizes: List<PixelSize>,
    rowSizes: List<Int>,
    fromIndex: Int,
    toIndex: Int,
    gapPx: Int,
): RearrangeLayout {
    val reordered = panelSizes.indices.toMutableList().apply {
        val moved = removeAt(fromIndex)
        add(toIndex, moved)
    }
    val rects = MutableList(panelSizes.size) { PixelRect(0, 0, 0, 0) }
    var top = 0
    var canvasWidth = 0
    var orderIndex = 0

    rowSizes.forEachIndexed { rowIndex, rowSize ->
        val row = reordered.subList(orderIndex, orderIndex + rowSize)
        var left = 0
        val rowHeight = row.maxOf { panelSizes[it].height }
        row.forEachIndexed { columnIndex, panelIndex ->
            val size = panelSizes[panelIndex]
            rects[panelIndex] = PixelRect(left, top, size.width, size.height)
            left += size.width
            if (columnIndex < row.lastIndex) left += gapPx
        }
        canvasWidth = maxOf(canvasWidth, left)
        top += rowHeight
        if (rowIndex < rowSizes.lastIndex) top += gapPx
        orderIndex += rowSize
    }

    return RearrangeLayout(canvasWidth, top, rects)
}