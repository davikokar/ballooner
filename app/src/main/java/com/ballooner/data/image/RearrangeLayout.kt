package com.ballooner.data.image

import com.ballooner.domain.model.ImagePosition

internal data class PixelSize(val width: Int, val height: Int)

internal data class PixelRect(val left: Int, val top: Int, val width: Int, val height: Int)

internal data class RearrangeLayout(
    val canvasWidth: Int,
    val canvasHeight: Int,
    val panelRects: List<PixelRect>,
)

private data class PanelRow(val panels: List<Int>, val leftInset: Int = 0)

internal fun computeRearrangeLayout(
    panelSizes: List<PixelSize>,
    rowSizes: List<Int>,
    fromIndex: Int,
    targetIndex: Int,
    position: ImagePosition,
    gapPx: Int,
): RearrangeLayout {
    val originalRows = mutableListOf<MutableList<Int>>()
    var offset = 0
    rowSizes.forEach { rowSize ->
        originalRows += (offset until offset + rowSize).toMutableList()
        offset += rowSize
    }

    val rows = if (position == ImagePosition.LEFT || position == ImagePosition.RIGHT) {
        val reordered = panelSizes.indices.toMutableList().apply {
            remove(fromIndex)
            val targetPosition = indexOf(targetIndex)
            add(targetPosition + if (position == ImagePosition.RIGHT) 1 else 0, fromIndex)
        }
        var orderIndex = 0
        rowSizes.map { rowSize ->
            PanelRow(reordered.subList(orderIndex, orderIndex + rowSize).also { orderIndex += rowSize })
        }
    } else {
        originalRows.first { fromIndex in it }.remove(fromIndex)
        originalRows.removeAll { it.isEmpty() }
        val targetRow = originalRows.indexOfFirst { targetIndex in it }
        val targetColumn = originalRows[targetRow].indexOf(targetIndex)
        val leftInset = originalRows[targetRow].take(targetColumn).sumOf { panelSizes[it].width } +
            targetColumn * gapPx
        val insertionRow = targetRow + if (position == ImagePosition.BOTTOM) 1 else 0
        originalRows.map { PanelRow(it) }.toMutableList().apply {
            add(insertionRow, PanelRow(listOf(fromIndex), leftInset))
        }
    }

    val rects = MutableList(panelSizes.size) { PixelRect(0, 0, 0, 0) }
    var top = 0
    var canvasWidth = 0

    rows.forEachIndexed { rowIndex, row ->
        var left = row.leftInset
        val rowHeight = row.panels.maxOf { panelSizes[it].height }
        row.panels.forEachIndexed { columnIndex, panelIndex ->
            val size = panelSizes[panelIndex]
            rects[panelIndex] = PixelRect(left, top, size.width, size.height)
            left += size.width
            if (columnIndex < row.panels.lastIndex) left += gapPx
        }
        canvasWidth = maxOf(canvasWidth, left)
        top += rowHeight
        if (rowIndex < rows.lastIndex) top += gapPx
    }

    return RearrangeLayout(canvasWidth, top, rects)
}