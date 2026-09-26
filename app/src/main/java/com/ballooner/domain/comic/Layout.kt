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
