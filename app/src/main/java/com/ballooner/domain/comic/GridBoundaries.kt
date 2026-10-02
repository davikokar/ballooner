package com.ballooner.domain.comic

import kotlin.math.abs

/** Which way a grid line runs. */
enum class GridAxis { ROW, COLUMN }

/**
 * Which line a drag has hold of.
 *
 * A row line spans the page, so [row] is null. A column line belongs to one row: in a row that
 * follows the grid it moves the grid's own weights, and so moves every other row that follows
 * them too; in a freed row it moves that row alone.
 */
data class GridLine(val axis: GridAxis, val index: Int, val row: Int? = null)

/**
 * A draggable grid line, between the cells at [index] - 1 and [index].
 *
 * [position] is where it sits in page units: a y coordinate for a row line, x for a column line.
 * [from] and [to] are how far it runs along the other axis, which for a column line is the band
 * of the row it belongs to.
 */
data class GridBoundary(
    val line: GridLine,
    val position: Float,
    val from: Float,
    val to: Float,
) {
    val axis: GridAxis get() = line.axis
    val index: Int get() = line.index
}

/**
 * Every grid line on the page, edges excluded because the page margin owns those.
 *
 * Column lines are listed per row, since a row that divides its own width has lines of its own.
 */
fun gridBoundaries(grid: Grid, pageHeight: Float, style: ComicStyle): List<GridBoundary> {
    val content = contentRect(pageHeight, style)
    val columns = gridTracks(grid.rowWeights, content.top, content.height, style.gutter)
        .flatMapIndexed { row, track ->
            boundaryPositions(grid.columnWeightsAt(row), content.left, content.width, style.gutter)
                .mapIndexed { index, position ->
                    GridBoundary(
                        line = GridLine(GridAxis.COLUMN, index + 1, row),
                        position = position,
                        from = track.start,
                        to = track.end,
                    )
                }
        }
    val rows = boundaryPositions(grid.rowWeights, content.top, content.height, style.gutter)
        .mapIndexed { index, position ->
            GridBoundary(
                line = GridLine(GridAxis.ROW, index + 1),
                position = position,
                from = content.left,
                to = content.left + content.width,
            )
        }
    return columns + rows
}

/**
 * Moves one grid line, [delta] being the movement as a fraction of the content extent.
 *
 * Weight is taken from one side and given to the other, so the rest of the grid never moves, and
 * neither cell can be squeezed below [MIN_CELL_WEIGHT_FRACTION] of the page.
 */
fun Grid.withBoundaryMoved(line: GridLine, delta: Float): Grid {
    val row = line.row?.takeIf { line.axis == GridAxis.COLUMN && isRowFree(it) }
    val weights = when {
        row != null -> columnWeightsAt(row)
        line.axis == GridAxis.COLUMN -> columnWeights
        else -> rowWeights
    }
    val moved = weights.withWeightMoved(line.index, delta) ?: return this
    return when {
        row != null -> copy(rowSplits = rowSplits + (row to moved))
        line.axis == GridAxis.COLUMN -> copy(columnWeights = moved)
        else -> copy(rowWeights = moved)
    }
}

/** Null when the line is an edge, or the drag is nonsense, or there is no room to give. */
private fun List<Float>.withWeightMoved(index: Int, delta: Float): List<Float>? {
    if (index !in 1 until size) return null
    val total = sum()
    if (total <= 0f || !delta.isFinite()) return null

    val minimum = total * MIN_CELL_WEIGHT_FRACTION
    val before = this[index - 1]
    val after = this[index]
    if (before + after < 2f * minimum) return null
    val shift = (delta * total).coerceIn(minimum - before, after - minimum)

    return toMutableList().also {
        it[index - 1] = before + shift
        it[index] = after - shift
    }
}

/** The same grid with every row and column back on an equal share, as a fresh preset starts. */
fun Grid.withEvenWeights(): Grid = copy(
    rowWeights = List(rows) { 1f },
    columnWeights = List(columns) { 1f },
    rowSplits = emptyMap(),
)

/**
 * Whether every cell still takes an equal share, which is what makes one chosen shape true of
 * every panel. A grid line that has been dragged, or a row dividing its own width, leaves it
 * false.
 */
fun Grid.hasEvenWeights(): Boolean =
    rowSplits.isEmpty() && rowWeights.allEqual() && columnWeights.allEqual()

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
fun Comic.withBoundaryMoved(line: GridLine, delta: Float): Comic {
    val grid = layout.grid.withBoundaryMoved(line, delta)
    if (grid == layout.grid) return this
    val moved = copy(layout = layout.copy(grid = grid))
    // Any other line leaves the reference cell — the top-left one — alone, and so must leave the
    // sizing alone: an Auto comic would otherwise quietly stop following its image.
    val reference = line.index == 1 &&
        (line.axis == GridAxis.ROW || (line.row ?: 0) == 0)
    if (!reference) return moved
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
