package com.ballooner.domain.comic

/** Which way a grid line runs. */
enum class GridAxis { ROW, COLUMN }

/**
 * A draggable grid line, between the cells at [index] - 1 and [index].
 *
 * [position] is where it sits in page units: a y coordinate for a row line, x for a column line.
 */
data class GridBoundary(val axis: GridAxis, val index: Int, val position: Float)

/** Every grid line on the page, edges excluded because the page margin owns those. */
fun gridBoundaries(grid: Grid, pageShape: PageShape, style: ComicStyle): List<GridBoundary> {
    val content = contentRect(pageShape, style)
    val columns = boundaryPositions(grid.columnWeights, content.left, content.width)
        .mapIndexed { index, position -> GridBoundary(GridAxis.COLUMN, index + 1, position) }
    val rows = boundaryPositions(grid.rowWeights, content.top, content.height)
        .mapIndexed { index, position -> GridBoundary(GridAxis.ROW, index + 1, position) }
    return columns + rows
}

/**
 * Moves one grid line, [delta] being the movement as a fraction of the content extent.
 *
 * Weight is taken from one side and given to the other, so the rest of the grid never moves, and
 * neither cell can be squeezed below [MIN_CELL_WEIGHT_FRACTION] of the page.
 */
fun Grid.withBoundaryMoved(axis: GridAxis, index: Int, delta: Float): Grid {
    val weights = if (axis == GridAxis.COLUMN) columnWeights else rowWeights
    if (index !in 1 until weights.size) return this
    val total = weights.sum()
    if (total <= 0f || !delta.isFinite()) return this

    val minimum = total * MIN_CELL_WEIGHT_FRACTION
    val before = weights[index - 1]
    val after = weights[index]
    val room = before + after
    if (room < 2f * minimum) return this
    val shift = (delta * total).coerceIn(minimum - before, after - minimum)

    val moved = weights.toMutableList()
    moved[index - 1] = before + shift
    moved[index] = after - shift
    return if (axis == GridAxis.COLUMN) copy(columnWeights = moved) else copy(rowWeights = moved)
}

private fun boundaryPositions(weights: List<Float>, start: Float, extent: Float): List<Float> {
    val total = weights.sum().takeIf { it > 0f } ?: return emptyList()
    var running = 0f
    return weights.dropLast(1).map { weight ->
        running += weight
        start + extent * running / total
    }
}

private const val MIN_CELL_WEIGHT_FRACTION = 0.08f
