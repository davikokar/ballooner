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
    val halfGutter = style.gutter / 2f
    return grid.panelSpans().map { span ->
        GridPanel(span, Polygon.of(grid.rectOf(span, content, halfGutter)))
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

private fun Grid.rectOf(span: Span, content: PageRect, halfGutter: Float): PageRect {
    val columnEdges = edges(columnWeights, content.left, content.width)
    val rowEdges = edges(rowWeights, content.top, content.height)
    val left = columnEdges[span.firstColumn] + if (span.firstColumn > 0) halfGutter else 0f
    val right = columnEdges[span.lastColumn + 1] - if (span.lastColumn < columns - 1) halfGutter else 0f
    val top = rowEdges[span.firstRow] + if (span.firstRow > 0) halfGutter else 0f
    val bottom = rowEdges[span.lastRow + 1] - if (span.lastRow < rows - 1) halfGutter else 0f
    return PageRect(left, top, (right - left).coerceAtLeast(0f), (bottom - top).coerceAtLeast(0f))
}

private fun edges(weights: List<Float>, start: Float, extent: Float): List<Float> {
    val total = weights.sum().takeIf { it > 0f } ?: return List(weights.size + 1) { start }
    val result = mutableListOf(start)
    var running = 0f
    weights.forEach { weight ->
        running += weight
        result += start + extent * running / total
    }
    return result
}

/**
 * How far apart two panel centroids can be, in page units, and still be read as the same row.
 * Diagonally cut pages have no exact row structure, so this is a tuned value rather than a rule.
 */
const val READING_ORDER_ROW_TOLERANCE = 0.05f
