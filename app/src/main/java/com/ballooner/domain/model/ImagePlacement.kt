package com.ballooner.domain.model

/** A new image position relative to one existing panel. */
data class ImagePlacement(
    val anchor: RectFraction,
    val position: ImagePosition,
)

fun availableImagePlacements(
    panels: List<RectFraction>,
    widthSpan: Int = 1,
    heightSpan: Int = 1,
): List<ImagePlacement> = panels.flatMap { anchor ->
    ImagePosition.entries.map { position -> ImagePlacement(anchor, position) }
}.filter { placement ->
    val candidate = placement.targetRect(widthSpan, heightSpan)
    panels.none { panel -> panel != placement.anchor && candidate.overlaps(panel) }
}.distinctBy { it.targetRect(widthSpan, heightSpan).roundedKey() }

fun ImagePlacement.targetRect(widthSpan: Int = 1, heightSpan: Int = 1): RectFraction {
    val targetWidth = anchor.width * widthSpan
    val targetHeight = anchor.height * heightSpan
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

private fun RectFraction.roundedKey(): List<Int> = listOf(left, top, width, height).map { (it * 10_000).toInt() }
