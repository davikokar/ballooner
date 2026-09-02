package com.ballooner.domain.model

import kotlin.math.pow
import kotlin.math.roundToInt

/** A new image position relative to one existing panel. */
data class ImagePlacement(
    val anchor: RectFraction,
    val position: ImagePosition,
)

fun defaultImagePlacement(
    placements: List<ImagePlacement>,
    panels: List<RectFraction>,
): ImagePlacement? = placements
    .filter { it.position == ImagePosition.RIGHT }
    .maxWithOrNull(compareBy<ImagePlacement>({ it.anchor.left + it.anchor.width }, { it.anchor.top }))
    ?: placements.minByOrNull { placement ->
        val target = placement.targetRect()
        panels.minOf { panel -> target.centerDistanceSquared(panel) }
    }

fun availableImagePlacements(panels: List<RectFraction>): List<ImagePlacement> =
    panels.flatMap { anchor ->
        ImagePosition.entries.map { position -> ImagePlacement(anchor, position) }
    }.filter { placement ->
        val candidate = placement.targetRect()
        panels.none { panel -> panel != placement.anchor && candidate.overlaps(panel) }
    }.distinctBy { it.targetRect().coordinateKey() }

fun edgeImagePlacements(panels: List<RectFraction>): List<ImagePlacement> = panels.flatMap { anchor ->
    listOf(
        ImagePlacement(anchor, ImagePosition.RIGHT),
        ImagePlacement(anchor, ImagePosition.BOTTOM),
    )
}.filter { placement ->
    val candidate = placement.targetRect()
    panels.none { panel -> panel != placement.anchor && candidate.overlaps(panel) }
}

fun ImagePlacement.targetRect(): RectFraction {
    val targetWidth = anchor.width
    val targetHeight = anchor.height
    return when (position) {
        ImagePosition.LEFT -> RectFraction(
            anchor.left - targetWidth,
            anchor.top + (anchor.height - targetHeight) / 2,
            targetWidth,
            targetHeight,
        )
        ImagePosition.RIGHT -> RectFraction(
            anchor.left + anchor.width,
            anchor.top + (anchor.height - targetHeight) / 2,
            targetWidth,
            targetHeight,
        )
        ImagePosition.TOP -> RectFraction(
            anchor.left + (anchor.width - targetWidth) / 2,
            anchor.top - targetHeight,
            targetWidth,
            targetHeight,
        )
        ImagePosition.BOTTOM -> RectFraction(
            anchor.left + (anchor.width - targetWidth) / 2,
            anchor.top + anchor.height,
            targetWidth,
            targetHeight,
        )
    }
}

private fun RectFraction.overlaps(other: RectFraction): Boolean =
    left < other.left + other.width && left + width > other.left &&
        top < other.top + other.height && top + height > other.top

private fun RectFraction.centerDistanceSquared(other: RectFraction): Float {
    val dx = left + width / 2f - (other.left + other.width / 2f)
    val dy = top + height / 2f - (other.top + other.height / 2f)
    return dx * dx + dy * dy
}

private fun RectFraction.coordinateKey(): List<Int> = listOf(left, top, width, height).map {
    (it * COORDINATE_PRECISION).toInt()
}

private const val COORDINATE_PRECISION = 100_000

fun magneticallyAlignedPanel(
    panels: List<RectFraction>,
    moving: RectFraction,
    desired: RectFraction,
    canvasWidth: Int,
    canvasHeight: Int,
    snapThresholdPx: Float,
): RectFraction {
    val gapPx = panels.minOf { panel ->
        minOf(panel.width * canvasWidth, panel.height * canvasHeight)
    }.times(PANEL_GAP_FRACTION).roundToInt().coerceAtLeast(MIN_PANEL_GAP_PX)
    val candidates = panels.asSequence()
        .filter { it != moving }
        .flatMap { anchor -> desired.snapCandidates(anchor, gapPx, canvasWidth, canvasHeight) }
        .filter { candidate -> panels.none { it != moving && candidate.overlaps(it) } }
        .map { candidate -> candidate to candidate.pixelDistanceSquared(desired, canvasWidth, canvasHeight) }
        .filter { (_, distance) -> distance <= snapThresholdPx.toDouble().pow(2) }
    return candidates.minByOrNull { (_, distance) -> distance }?.first ?: desired
}

fun magneticallyResizedPanel(
    panels: List<RectFraction>,
    moving: RectFraction,
    desired: RectFraction,
    canvasWidth: Int,
    canvasHeight: Int,
    snapThresholdPx: Float,
): RectFraction {
    val originalWidthPx = moving.width * canvasWidth
    val originalHeightPx = moving.height * canvasHeight
    val desiredWidthPx = desired.width * canvasWidth
    val desiredHeightPx = desired.height * canvasHeight
    val minimumScale = maxOf(
        MIN_RESIZED_PANEL_PX / originalWidthPx,
        MIN_RESIZED_PANEL_PX / originalHeightPx,
    )
    val desiredScale = (
        desiredWidthPx * originalWidthPx + desiredHeightPx * originalHeightPx
    ) / (
        originalWidthPx * originalWidthPx + originalHeightPx * originalHeightPx
    )
    val proportionalScale = desiredScale.coerceAtLeast(minimumScale)
    val desiredRight = moving.left + moving.width * proportionalScale
    val desiredBottom = moving.top + moving.height * proportionalScale
    val surrounding = panels.filter { it != moving }
    val rightEdges = surrounding.flatMap { listOf(it.left, it.left + it.width) }
    val bottomEdges = surrounding.flatMap { listOf(it.top, it.top + it.height) }
    val rightScales = rightEdges
        .filter { edge -> kotlin.math.abs(edge - desiredRight) * canvasWidth <= snapThresholdPx }
        .map { edge ->
            val scale = (edge - moving.left) / moving.width
            scale to kotlin.math.abs(edge - desiredRight) * canvasWidth
        }
        .filter { (scale) -> scale >= minimumScale }
    val bottomScales = bottomEdges
        .filter { edge -> kotlin.math.abs(edge - desiredBottom) * canvasHeight <= snapThresholdPx }
        .map { edge ->
            val scale = (edge - moving.top) / moving.height
            scale to kotlin.math.abs(edge - desiredBottom) * canvasHeight
        }
        .filter { (scale) -> scale >= minimumScale }
    val scale = (rightScales + bottomScales).minByOrNull { (_, distance) -> distance }?.first
        ?: proportionalScale
    return moving.copy(
        width = moving.width * scale,
        height = moving.height * scale,
    )
}

private fun RectFraction.snapCandidates(
    anchor: RectFraction,
    gapPx: Int,
    canvasWidth: Int,
    canvasHeight: Int,
): Sequence<RectFraction> {
    val gapX = gapPx.toFloat() / canvasWidth
    val gapY = gapPx.toFloat() / canvasHeight
    val alignedTop = nearest(top, anchor.top, anchor.top + (anchor.height - height) / 2f, anchor.top + anchor.height - height)
    val alignedLeft = nearest(left, anchor.left, anchor.left + (anchor.width - width) / 2f, anchor.left + anchor.width - width)
    return sequenceOf(
        copy(left = anchor.left - gapX - width, top = alignedTop),
        copy(left = anchor.left + anchor.width + gapX, top = alignedTop),
        copy(left = alignedLeft, top = anchor.top - gapY - height),
        copy(left = alignedLeft, top = anchor.top + anchor.height + gapY),
    )
}

private fun nearest(value: Float, vararg candidates: Float): Float = candidates.minBy { candidate ->
    kotlin.math.abs(candidate - value)
}

private fun RectFraction.pixelDistanceSquared(other: RectFraction, canvasWidth: Int, canvasHeight: Int): Double {
    val dx = (left - other.left) * canvasWidth
    val dy = (top - other.top) * canvasHeight
    return dx.toDouble().pow(2) + dy.toDouble().pow(2)
}

private const val PANEL_GAP_FRACTION = 0.02f
private const val MIN_PANEL_GAP_PX = 8
private const val MIN_RESIZED_PANEL_PX = 48f
