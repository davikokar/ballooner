package com.ballooner.domain.comic

/**
 * The shape of every panel on the page, in reading order.
 *
 * Panel geometry is never stored: it is computed from the layout, the page height, and the style
 * every time it is needed, so there is nothing to keep in sync after an edit.
 */
fun panelShapes(
    layout: Layout,
    pageHeight: Float,
    style: ComicStyle,
    rowTolerance: Float = READING_ORDER_ROW_TOLERANCE,
): List<Polygon> {
    var panels = gridPanels(layout.grid, pageHeight, style).map { it.shape }
    layout.cuts.forEach { cut -> panels = cut.applyTo(panels, pageHeight, style.gutter / 2f) }
    return panels.inReadingOrder(rowTolerance)
}

/** One panel of the grid before any cut is applied, paired with the cells it covers. */
data class GridPanel(val span: Span, val shape: Polygon)

/**
 * The grid's own panels, in grid order.
 *
 * Merging and unmerging work on cells rather than on the panels a cut leaves behind, so the
 * Layout step selects against these rather than against the final shapes.
 */
fun gridPanels(grid: Grid, pageHeight: Float, style: ComicStyle): List<GridPanel> {
    val content = contentRect(pageHeight, style)
    return grid.panelSpans().map { span ->
        GridPanel(span, Polygon.of(grid.rectOf(span, content, style.gutter)))
    }
}

/** The area panels are laid out in: the page, less its margin. */
fun contentRect(pageHeight: Float, style: ComicStyle): PageRect = PageRect(
    left = style.pageMargin,
    top = style.pageMargin,
    width = (1f - 2f * style.pageMargin).coerceAtLeast(0f),
    height = (pageHeight - 2f * style.pageMargin).coerceAtLeast(0f),
)

/**
 * Panels are ordered by centroid, top to bottom then left to right, with [rowTolerance] deciding
 * how far apart two centroids can be and still count as the same row.
 */
fun List<Polygon>.inReadingOrder(rowTolerance: Float = READING_ORDER_ROW_TOLERANCE): List<Polygon> {
    val byCentroid = map { it to it.centroid }.sortedBy { (_, centroid) -> centroid.y }
    val rows = mutableListOf<MutableList<Pair<Polygon, PagePoint>>>()
    byCentroid.forEach { entry ->
        val currentRow = rows.lastOrNull()
        if (currentRow != null && entry.second.y - currentRow.first().second.y <= rowTolerance) {
            currentRow += entry
        } else {
            rows += mutableListOf(entry)
        }
    }
    return rows.flatMap { row -> row.sortedBy { (_, centroid) -> centroid.x }.map { (polygon, _) -> polygon } }
}

private fun Cut.applyTo(panels: List<Polygon>, pageHeight: Float, inset: Float): List<Polygon> {
    val line = Line(a.onPage(pageHeight), b.onPage(pageHeight))
    return when (val scope = scope) {
        CutScope.WholePage -> panels.flatMap { panel ->
            splitConvex(panel, line, inset)?.toList() ?: listOf(panel)
        }
        is CutScope.AtPoint -> {
            val anchor = scope.anchor.onPage(pageHeight)
            val index = panels.indexOfFirst { it.contains(anchor) }
            // An anchor stranded in a gutter leaves its cut inactive rather than guessing.
            if (index < 0) return panels
            val pieces = splitConvex(panels[index], line, inset) ?: return panels
            panels.take(index) + pieces.toList() + panels.drop(index + 1)
        }
    }
}

private fun Grid.rectOf(span: Span, content: PageRect, gutter: Float): PageRect {
    val columnTracks = gridTracks(columnWeights, content.left, content.width, gutter)
    val rowTracks = gridTracks(rowWeights, content.top, content.height, gutter)
    val left = columnTracks[span.firstColumn].start
    val right = columnTracks[span.lastColumn].end
    val top = rowTracks[span.firstRow].start
    val bottom = rowTracks[span.lastRow].end
    return PageRect(left, top, (right - left).coerceAtLeast(0f), (bottom - top).coerceAtLeast(0f))
}

/** Where one row or column begins and ends, in page units. */
internal data class GridTrack(val start: Float, val end: Float)

/**
 * The rows or columns of a grid, with a whole gutter standing between each pair of neighbours.
 *
 * The gutters are taken out of the extent before the weights divide what is left, so equal
 * weights really do produce equal cells however many of them there are.
 */
internal fun gridTracks(
    weights: List<Float>,
    start: Float,
    extent: Float,
    gutter: Float,
): List<GridTrack> {
    val total = weights.sum().takeIf { it > 0f } ?: return weights.map { GridTrack(start, start) }
    val available = (extent - gutter * (weights.size - 1)).coerceAtLeast(0f)
    var cursor = start
    return weights.map { weight ->
        val size = available * weight / total
        GridTrack(cursor, cursor + size).also { cursor += size + gutter }
    }
}

/**
 * How far apart two panel centroids can be, in page units, and still be read as the same row.
 * Diagonally cut pages have no exact row structure, so this is a tuned value rather than a rule.
 */
const val READING_ORDER_ROW_TOLERANCE = 0.05f
