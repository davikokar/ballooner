package com.ballooner.data.image

import kotlin.math.pow

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
    gapPx: Int,
): RearrangeLayout {
    val moving = panelRects[fromIndex]
    val desired = moving.copy(left = desiredLeft, top = desiredTop)
    val stationary = panelRects.filterIndexed { index, _ -> index != fromIndex }
    val snapped = stationary
        .flatMap { anchor -> snapCandidates(desired, anchor, gapPx) }
        .filter { candidate -> stationary.none { it.overlaps(candidate) } }
        .minByOrNull { candidate ->
            (candidate.left - desired.left).toDouble().pow(2) +
                (candidate.top - desired.top).toDouble().pow(2)
        } ?: desired

    val positioned = panelRects.mapIndexed { index, rect -> if (index == fromIndex) snapped else rect }
    val minLeft = positioned.minOf { it.left }.coerceAtMost(0)
    val minTop = positioned.minOf { it.top }.coerceAtMost(0)
    val shifted = positioned.map { it.copy(left = it.left - minLeft, top = it.top - minTop) }
    return RearrangeLayout(
        canvasWidth = shifted.maxOf { it.left + it.width },
        canvasHeight = shifted.maxOf { it.top + it.height },
        panelRects = shifted,
    )
}

private fun snapCandidates(moving: PixelRect, anchor: PixelRect, gapPx: Int): List<PixelRect> {
    val alignedTop = nearest(
        moving.top,
        anchor.top,
        anchor.top + (anchor.height - moving.height) / 2,
        anchor.top + anchor.height - moving.height,
    )
    val alignedLeft = nearest(
        moving.left,
        anchor.left,
        anchor.left + (anchor.width - moving.width) / 2,
        anchor.left + anchor.width - moving.width,
    )
    return listOf(
        moving.copy(left = anchor.left - gapPx - moving.width, top = alignedTop),
        moving.copy(left = anchor.left + anchor.width + gapPx, top = alignedTop),
        moving.copy(left = alignedLeft, top = anchor.top - gapPx - moving.height),
        moving.copy(left = alignedLeft, top = anchor.top + anchor.height + gapPx),
    )
}

private fun nearest(value: Int, vararg candidates: Int): Int = candidates.minBy { candidate ->
    kotlin.math.abs(candidate - value)
}

private fun PixelRect.overlaps(other: PixelRect): Boolean =
    left < other.left + other.width && left + width > other.left &&
        top < other.top + other.height && top + height > other.top