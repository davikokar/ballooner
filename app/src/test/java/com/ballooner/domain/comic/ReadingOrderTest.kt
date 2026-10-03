package com.ballooner.domain.comic

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingOrderTest {

    private val noStyle = ComicStyle(gutter = 0f, borderThickness = 0f)

    @Test
    fun `a grid reads left to right then top to bottom`() {
        val shapes = panelShapes(Layout(Grid(rows = 2, columns = 3)), 1f, noStyle)

        val centres = shapes.map { it.bounds.centre }
        centres.take(3).forEach { assertEquals(0.25f, it.y, TOLERANCE) }
        centres.drop(3).forEach { assertEquals(0.75f, it.y, TOLERANCE) }
        assertEquals(1f / 6f, centres[0].x, TOLERANCE)
        assertEquals(0.5f, centres[1].x, TOLERANCE)
        assertEquals(5f / 6f, centres[2].x, TOLERANCE)
    }

    @Test
    fun `panels within a row are ordered even when their heights differ`() {
        val grid = Grid(rows = 2, columns = 2, spans = listOf(Span(0, 1, rowCount = 2, columnCount = 1)))

        val shapes = panelShapes(Layout(grid), 1f, noStyle)

        assertEquals(3, shapes.size)
        assertEquals(0.25f, shapes[0].bounds.centre.x, TOLERANCE)
        assertEquals(0.75f, shapes[1].bounds.centre.x, TOLERANCE)
        assertEquals(0.25f, shapes[2].bounds.centre.x, TOLERANCE)
    }

    @Test
    fun `a tall merged panel is read by its centroid, not by its top edge`() {
        // Pinning a known limitation of centroid ordering: the tall left panel starts at the top
        // of the page, but its centroid sits between the two right-hand panels, so it is read
        // second rather than first.
        val grid = Grid(rows = 2, columns = 2, spans = listOf(Span(0, 0, rowCount = 2, columnCount = 1)))

        val shapes = panelShapes(Layout(grid), 1f, noStyle)

        assertEquals(0.25f, shapes[0].bounds.centre.y, TOLERANCE)
        assertEquals(0.5f, shapes[1].bounds.centre.y, TOLERANCE)
        assertEquals(1f, shapes[1].bounds.height, TOLERANCE)
        assertEquals(0.75f, shapes[2].bounds.centre.y, TOLERANCE)
    }

    @Test
    fun `centroids closer together than the tolerance are read as one row`() {
        val tilted = listOf(
            Polygon.of(PageRect(0.5f, 0.02f, 0.4f, 0.4f)),
            Polygon.of(PageRect(0f, 0f, 0.4f, 0.4f)),
        )

        val ordered = tilted.inReadingOrder(rowTolerance = 0.05f)

        assertEquals(0f, ordered[0].bounds.left, TOLERANCE)
        assertEquals(0.5f, ordered[1].bounds.left, TOLERANCE)
    }

    @Test
    fun `centroids further apart than the tolerance are read as separate rows`() {
        val stacked = listOf(
            Polygon.of(PageRect(0.5f, 0.3f, 0.4f, 0.4f)),
            Polygon.of(PageRect(0f, 0f, 0.4f, 0.4f)),
        )

        val ordered = stacked.inReadingOrder(rowTolerance = 0.05f)

        assertEquals(0f, ordered[0].bounds.left, TOLERANCE)
        assertEquals(0.5f, ordered[1].bounds.left, TOLERANCE)
    }
}
