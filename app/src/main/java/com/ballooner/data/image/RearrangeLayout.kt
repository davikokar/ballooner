package com.ballooner.data.image

internal data class PixelSize(val width: Int, val height: Int)

internal data class PixelRect(val left: Int, val top: Int, val width: Int, val height: Int)

internal data class RearrangeLayout(
    val canvasWidth: Int,
    val canvasHeight: Int,
    val panelRects: List<PixelRect>,
    val vacatedRect: PixelRect,
)

internal fun computeRearrangeLayout(
    panelRects: List<PixelRect>,
    fromIndex: Int,
    desiredLeft: Int,
    desiredTop: Int,
): RearrangeLayout {
    val moving = panelRects[fromIndex]
    val destination = moving.copy(left = desiredLeft, top = desiredTop)
    val positioned = panelRects.mapIndexed { index, rect -> if (index == fromIndex) destination else rect }
    val minLeft = positioned.minOf { it.left }.coerceAtMost(0)
    val minTop = positioned.minOf { it.top }.coerceAtMost(0)
    val shifted = positioned.map { it.copy(left = it.left - minLeft, top = it.top - minTop) }
    return RearrangeLayout(
        canvasWidth = shifted.maxOf { it.left + it.width },
        canvasHeight = shifted.maxOf { it.top + it.height },
        panelRects = shifted,
        vacatedRect = moving.copy(left = moving.left - minLeft, top = moving.top - minTop),
    )
}