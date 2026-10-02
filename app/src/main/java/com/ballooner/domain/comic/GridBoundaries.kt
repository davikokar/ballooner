package com.ballooner.domain.comic

import kotlin.math.abs

/** Which way a grid line runs. */
enum class GridAxis { ROW, COLUMN }

/**
 * A draggable grid line, between the cells at [index] - 1 and [index].
 *
 * [position] is where it sits in page units: a y coordinate for a row line, x for a column line.
 */
data class GridBoundary(val axis: GridAxis, val index: Int, val position: Float)

/** Every grid line on the page, edges excluded because the page margin owns those. */
fun gridBoundaries(grid: Grid, pageHeight: Float, style: ComicStyle): List<GridBoundary> {
    val content = contentRect(pageHeight, style)
    val columns = boundaryPositions(grid.columnWeights, content.left, content.width, style.gutter)
        .mapIndexed { index, position -> GridBoundary(GridAxis.COLUMN, index + 1, position) }
    val rows = boundaryPositions(grid.rowWeights, content.top, content.height, style.gutter)
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

/** The same grid with every row and column back on an equal share, as a fresh preset starts. */
fun Grid.withEvenWeights(): Grid =
    copy(rowWeights = List(rows) { 1f }, columnWeights = List(columns) { 1f })

/**
 * Whether every cell still takes an equal share, which is what makes one chosen shape true of
 * every panel. A grid line that has been dragged leaves it false.
 */
fun Grid.hasEvenWeights(): Boolean = rowWeights.allEqual() && columnWeights.allEqual()

private fun List<Float>.allEqual(): Boolean {
    val first = firstOrNull() ?: return true
    return all { abs(it - first) <= EQUAL_WEIGHT_TOLERANCE * first }
}

private const val EQUAL_WEIGHT_TOLERANCE = 0.001f

/**
 * The comic with one grid line moved, the page keeping the height it had.
 *
 * A line only ever resizes the two cells either side of it, but the FIRST line on its axis moves
 * the reference cell, and the page takes its height from that cell's shape. Left alone, dragging
 * that one line would reshape the page and every other panel with it. So the sizing is re-read
 * off the cell the drag has just made: the reference cell is still what gives the page its
 * height, it has simply been given a new shape, which is what the drag asked for.
 */
fun Comic.withBoundaryMoved(axis: GridAxis, index: Int, delta: Float): Comic {
    val grid = layout.grid.withBoundaryMoved(axis, index, delta)
    if (grid == layout.grid) return this
    val moved = copy(layout = layout.copy(grid = grid))
    // Any other line leaves the reference cell alone, and so must leave the sizing alone — an
    // Auto comic would otherwise quietly stop following its image.
    if (index != 1) return moved
    return moved.copy(sizing = PageSizing.Ratio(referenceRatioOf(moved.layout, style, pageHeight)))
}

/**
 * A grid line sits in the middle of the gutter it opens.
 *
 * The units are whatever [extent] is given in, so a preview can ask for its lines in pixels.
 */
internal fun boundaryPositions(
    weights: List<Float>,
    start: Float,
    extent: Float,
    gutter: Float,
): List<Float> {
    val tracks = gridTracks(weights, start, extent, gutter)
    return tracks.dropLast(1).map { it.end + gutter / 2f }
}

private const val MIN_CELL_WEIGHT_FRACTION = 0.08f
