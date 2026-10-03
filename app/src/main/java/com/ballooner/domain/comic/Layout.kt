package com.ballooner.domain.comic

/** One panel covering a rectangular block of grid cells. An unmerged cell is a 1x1 span. */
data class Span(
    val firstRow: Int,
    val firstColumn: Int,
    val rowCount: Int = 1,
    val columnCount: Int = 1,
) {
    val lastRow: Int get() = firstRow + rowCount - 1
    val lastColumn: Int get() = firstColumn + columnCount - 1
}

/** Which panels a [Cut] divides. */
sealed interface CutScope {

    /** Splits every panel the cut crosses. */
    data object WholePage : CutScope

    /**
     * Splits only the panel containing [anchor], the point where the user began the trace.
     * Resolving the target by position rather than by index is what keeps cuts independent of
     * each other: removing an earlier cut can never orphan a later one.
     */
    data class AtPoint(val anchor: NormalizedPoint) : CutScope
}

/** The straight line through [a] and [b], dividing the panels named by [scope]. */
data class Cut(val a: NormalizedPoint, val b: NormalizedPoint, val scope: CutScope)

/** The rows and columns a page is divided into, plus any merged panels. */
data class Grid(
    val rows: Int,
    val columns: Int,
    val rowWeights: List<Float> = List(rows) { 1f },
    val columnWeights: List<Float> = List(columns) { 1f },
    /**
     * The column weights of rows that divide their own width, by row index. A row that is not
     * here follows [columnWeights]. See
     * [ADR-0010](../../../../../../../docs/architecture/decisions/0010-a-page-is-a-stack-of-tiers.md).
     */
    val rowSplits: Map<Int, List<Float>> = emptyMap(),
    val spans: List<Span> = emptyList(),
) {

    /** Every panel of the grid in grid reading order, merged spans included exactly once. */
    fun panelSpans(): List<Span> {
        val spanByOrigin = spans.associateBy { it.firstRow to it.firstColumn }
        val covered = Array(rows) { BooleanArray(columns) }
        spans.forEach { span ->
            for (row in span.firstRow..span.lastRow) {
                for (column in span.firstColumn..span.lastColumn) {
                    if (row in 0 until rows && column in 0 until columns) covered[row][column] = true
                }
            }
        }
        val result = mutableListOf<Span>()
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val span = spanByOrigin[row to column]
                when {
                    span != null -> result += span
                    !covered[row][column] -> result += Span(row, column)
                }
            }
        }
        return result
    }
}

/** A page layout: one grid, then the cuts applied to it in the order they were drawn. */
data class Layout(val grid: Grid, val cuts: List<Cut> = emptyList())

/**
 * How the row at [row] is divided across: its own weights when it has been freed from the grid,
 * and the grid's otherwise. The one place that question is answered.
 */
fun Grid.columnWeightsAt(row: Int): List<Float> = rowSplits[row] ?: columnWeights

/** Whether the row at [row] divides its own width rather than following the grid. */
fun Grid.isRowFree(row: Int): Boolean = row in rowSplits

/**
 * The same grid with the row at [row] dividing its own width, starting from the division it has
 * now, so freeing a row changes nothing on the page until it is dragged.
 *
 * A merged span covering more than one row has no rectangle once the rows it covers stop
 * agreeing, so freeing is refused while one crosses this row.
 */
fun Grid.withRowFreed(row: Int): Grid = when {
    row !in 0 until rows || isRowFree(row) -> this
    spans.any { it.rowCount > 1 && row in it.firstRow..it.lastRow } -> this
    else -> copy(rowSplits = rowSplits + (row to columnWeightsAt(row)))
}

/** The same grid with the row at [row] back on the grid's own division. */
fun Grid.withRowAligned(row: Int): Grid =
    if (isRowFree(row)) copy(rowSplits = rowSplits - row) else this

/** Whether a span covering [firstRow]..[lastRow] has a rectangle to cover at all. */
fun Grid.rowsAgree(firstRow: Int, lastRow: Int): Boolean =
    firstRow == lastRow || (firstRow..lastRow).none { isRowFree(it) }
