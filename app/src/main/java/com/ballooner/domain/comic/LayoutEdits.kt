package com.ballooner.domain.comic

/**
 * Combines [selection] into a single panel.
 *
 * Returns null when the selection is not a rectangle — an L-shaped merge, a selection with a
 * hole, or fewer than two panels — so the caller can refuse it rather than guess.
 */
fun Grid.mergedFrom(selection: List<Span>): Grid? {
    if (selection.size < 2) return null
    val firstRow = selection.minOf { it.firstRow }
    val firstColumn = selection.minOf { it.firstColumn }
    val lastRow = selection.maxOf { it.lastRow }
    val lastColumn = selection.maxOf { it.lastColumn }
    if (firstRow < 0 || firstColumn < 0 || lastRow >= rows || lastColumn >= columns) return null

    val covered = Array(rows) { BooleanArray(columns) }
    selection.forEach { span ->
        for (row in span.firstRow..span.lastRow) {
            for (column in span.firstColumn..span.lastColumn) {
                if (row !in 0 until rows || column !in 0 until columns) return null
                if (covered[row][column]) return null
                covered[row][column] = true
            }
        }
    }
    for (row in firstRow..lastRow) {
        for (column in firstColumn..lastColumn) {
            if (!covered[row][column]) return null
        }
    }

    val merged = Span(firstRow, firstColumn, lastRow - firstRow + 1, lastColumn - firstColumn + 1)
    val untouched = spans.filterNot { it.isInside(merged) }
    return copy(spans = untouched + merged)
}

/** Splits the panel covering the cell at [row] and [column] back into single cells. */
fun Grid.unmergedAt(row: Int, column: Int): Grid =
    copy(spans = spans.filterNot { row in it.firstRow..it.lastRow && column in it.firstColumn..it.lastColumn })

fun Layout.withCut(cut: Cut): Layout = copy(cuts = cuts + cut)

fun Layout.withoutCutAt(index: Int): Layout =
    if (index in cuts.indices) copy(cuts = cuts.filterIndexed { i, _ -> i != index }) else this

private fun Span.isInside(other: Span): Boolean =
    firstRow >= other.firstRow && lastRow <= other.lastRow &&
        firstColumn >= other.firstColumn && lastColumn <= other.lastColumn
