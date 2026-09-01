package com.ballooner.domain.model

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
