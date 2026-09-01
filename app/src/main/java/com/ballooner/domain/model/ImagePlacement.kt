package com.ballooner.domain.model

import kotlin.math.abs

/** A new image position relative to one existing panel. */
data class ImagePlacement(
    val anchor: RectFraction,
    val position: ImagePosition,
)

data class PanelGridCell(val column: Int, val row: Int)

fun panelGridCells(panels: List<RectFraction>): Map<RectFraction, PanelGridCell> {
    val columns = axisCenters(panels.map { it.left + it.width / 2f })
    val rows = axisCenters(panels.map { it.top + it.height / 2f })
    return panels.associateWith { panel ->
        PanelGridCell(
            column = columns.nearestIndex(panel.left + panel.width / 2f),
            row = rows.nearestIndex(panel.top + panel.height / 2f),
        )
    }
}

fun panelsInReadingOrder(panels: List<RectFraction>): List<RectFraction> {
    val cells = panelGridCells(panels)
    return panels.sortedWith(compareBy({ cells.getValue(it).row }, { cells.getValue(it).column }))
}

fun ImagePlacement.gridCell(panelCells: Map<RectFraction, PanelGridCell>): PanelGridCell {
    val anchorCell = panelCells.getValue(anchor)
    return when (position) {
        ImagePosition.LEFT -> anchorCell.copy(column = anchorCell.column - 1)
        ImagePosition.RIGHT -> anchorCell.copy(column = anchorCell.column + 1)
        ImagePosition.TOP -> anchorCell.copy(row = anchorCell.row - 1)
        ImagePosition.BOTTOM -> anchorCell.copy(row = anchorCell.row + 1)
    }
}

fun defaultImagePlacement(
    placements: List<ImagePlacement>,
    panels: List<RectFraction>,
): ImagePlacement? {
    val cells = panelGridCells(panels)
    return placements
        .filter { it.position == ImagePosition.RIGHT }
        .maxWithOrNull(compareBy<ImagePlacement>({ cells.getValue(it.anchor).row }, { cells.getValue(it.anchor).column }))
        ?: placements.firstOrNull()
}

fun availableImagePlacements(panels: List<RectFraction>): List<ImagePlacement> {
    val panelCells = panelGridCells(panels)
    val occupiedCells = panelCells.values.toSet()
    return panels.flatMap { anchor ->
        ImagePosition.entries.map { position -> ImagePlacement(anchor, position) }
    }.filter { placement ->
        val candidate = placement.targetRect()
        placement.gridCell(panelCells) !in occupiedCells &&
            panels.none { panel -> panel != placement.anchor && candidate.overlaps(panel) }
    }.groupBy { it.gridCell(panelCells) }
        .values
        .map { candidates -> candidates.minBy { it.position.duplicatePriority } }
}

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

private fun axisCenters(values: List<Float>): List<Float> = values.sorted().fold(emptyList()) { centers, value ->
    if (centers.lastOrNull()?.let { abs(value - it) < GRID_TOLERANCE } == true) centers else centers + value
}

private fun List<Float>.nearestIndex(value: Float): Int = indices.minBy { abs(this[it] - value) }

private val ImagePosition.duplicatePriority: Int
    get() = when (this) {
        ImagePosition.RIGHT -> 0
        ImagePosition.LEFT -> 1
        ImagePosition.BOTTOM -> 2
        ImagePosition.TOP -> 3
    }

private const val GRID_TOLERANCE = 0.01f
