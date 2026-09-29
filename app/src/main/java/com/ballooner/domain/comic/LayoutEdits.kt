package com.ballooner.domain.comic

import kotlin.math.abs

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

/**
 * The scope a cut traced from [from] to [to] really has, given the [panels] it is drawn over.
 *
 * A trace can begin beside the page rather than on a panel, so a panel cut whose anchor has
 * landed on nothing is re-anchored where its line first meets one. Null when the line meets no
 * panel at all, and so the cut would separate nothing.
 */
fun CutScope.anchoredIn(
    panels: List<Polygon>,
    from: NormalizedPoint,
    to: NormalizedPoint,
    pageHeight: Float,
): CutScope? {
    if (this !is CutScope.AtPoint) return this
    if (panels.any { it.contains(anchor.onPage(pageHeight)) }) return this
    val met = firstPanelPoint(panels, from.onPage(pageHeight), to.onPage(pageHeight)) ?: return null
    return CutScope.AtPoint(met.normalized(pageHeight))
}

fun Layout.withoutCutAt(index: Int): Layout =
    if (index in cuts.indices) copy(cuts = cuts.filterIndexed { i, _ -> i != index }) else this

/**
 * Whether the cut at [index] still separates anything.
 *
 * A cut's ends only give its line a direction, so swinging both of them clear of the panel the
 * cut was made in leaves a line that divides nothing and cannot be seen on the page.
 */
fun Layout.cutDivides(index: Int, pageHeight: Float, style: ComicStyle): Boolean {
    if (index !in cuts.indices) return false
    return panelShapes(this, pageHeight, style).size >
        panelShapes(withoutCutAt(index), pageHeight, style).size
}

/**
 * Moves one end of the cut at [index], leaving the other where it is.
 *
 * The two ends only give the line its direction, so dragging one swings the cut about the other.
 * A move that would put both ends in the same place is refused: a line needs two points.
 */
fun Layout.withCutEndMoved(index: Int, start: Boolean, to: NormalizedPoint): Layout {
    val cut = cuts.getOrNull(index) ?: return this
    val other = if (start) cut.b else cut.a
    if (abs(other.u - to.u) < MIN_CUT_SPAN && abs(other.v - to.v) < MIN_CUT_SPAN) return this
    val moved = if (start) cut.copy(a = to) else cut.copy(b = to)
    return copy(cuts = cuts.mapIndexed { i, existing -> if (i == index) moved else existing })
}

/** How far apart a cut's two ends must stay, in normalized page units. */
private const val MIN_CUT_SPAN = 0.01f

private fun Span.isInside(other: Span): Boolean =
    firstRow >= other.firstRow && lastRow <= other.lastRow &&
        firstColumn >= other.firstColumn && lastColumn <= other.lastColumn
