package com.ballooner.data.image

import com.ballooner.domain.model.RectFraction
import com.ballooner.domain.model.repositionPanelsAfterResize
import kotlin.math.roundToInt

internal data class PixelSize(val width: Int, val height: Int)

internal data class PixelRect(val left: Int, val top: Int, val width: Int, val height: Int)

internal data class RearrangeLayout(
    val canvasWidth: Int,
    val canvasHeight: Int,
    val panelRects: List<PixelRect>,
)

internal fun computeRearrangeLayout(
    panelRects: List<PixelRect>,
    fromIndex: Int,
    desiredLeft: Int,
    desiredTop: Int,
    desiredWidth: Int? = null,
    desiredHeight: Int? = null,
): RearrangeLayout {
    val moving = panelRects[fromIndex]
    val destination = moving.copy(
        left = desiredLeft,
        top = desiredTop,
        width = desiredWidth ?: moving.width,
        height = desiredHeight ?: moving.height,
    )
    val positioned = repositionPanelsAfterResize(
        panels = panelRects.map { it.toRectFraction() },
        moving = moving.toRectFraction(),
        resized = destination.toRectFraction(),
    ).map { it.toPixelRect() }
    val minLeft = positioned.minOf { it.left }.coerceAtMost(0)
    val minTop = positioned.minOf { it.top }.coerceAtMost(0)
    val shifted = positioned.map { it.copy(left = it.left - minLeft, top = it.top - minTop) }
    return RearrangeLayout(
        canvasWidth = shifted.maxOf { it.left + it.width },
        canvasHeight = shifted.maxOf { it.top + it.height },
        panelRects = shifted,
    )
}

private fun PixelRect.toRectFraction() = RectFraction(
    left = left.toFloat(),
    top = top.toFloat(),
    width = width.toFloat(),
    height = height.toFloat(),
)

private fun RectFraction.toPixelRect() = PixelRect(
    left = left.roundToInt(),
    top = top.roundToInt(),
    width = width.roundToInt(),
    height = height.roundToInt(),
)