package com.ballooner.domain.comic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GridBoundariesTest {

    private val noStyle = ComicStyle(gutter = 0f, borderThickness = 0f)

    @Test
    fun `a single panel has no grid lines`() {
        assertEquals(emptyList<GridBoundary>(), gridBoundaries(Grid(1, 1), 1f, noStyle))
    }

    @Test
    fun `an even grid puts its lines at even intervals`() {
        val boundaries = gridBoundaries(Grid(rows = 2, columns = 3), 1f, noStyle)

        val columns = boundaries.filter { it.axis == GridAxis.COLUMN }
        val rows = boundaries.filter { it.axis == GridAxis.ROW }
        assertEquals(listOf(1, 2), columns.map { it.index })
        assertEquals(1f / 3f, columns[0].position, TOLERANCE)
        assertEquals(2f / 3f, columns[1].position, TOLERANCE)
        assertEquals(listOf(1), rows.map { it.index })
        assertEquals(0.5f, rows[0].position, TOLERANCE)
    }

    @Test
    fun `the page margin shifts the grid lines inwards`() {
        val style = ComicStyle(gutter = 0.1f, borderThickness = 0f)

        val boundaries = gridBoundaries(Grid(rows = 1, columns = 2), 1f, style)

        assertEquals(0.5f, boundaries.single().position, TOLERANCE)
    }

    @Test
    fun `dragging a column line takes weight from one side and gives it to the other`() {
        val grid = Grid(rows = 1, columns = 2)

        val moved = grid.withBoundaryMoved(GridAxis.COLUMN, index = 1, delta = 0.25f)

        assertEquals(listOf(1.5f, 0.5f), moved.columnWeights)
        assertEquals(2f, moved.columnWeights.sum(), TOLERANCE)
    }

    @Test
    fun `dragging a row line leaves the columns alone`() {
        val grid = Grid(rows = 2, columns = 2)

        val moved = grid.withBoundaryMoved(GridAxis.ROW, index = 1, delta = -0.25f)

        assertEquals(listOf(0.5f, 1.5f), moved.rowWeights)
        assertEquals(listOf(1f, 1f), moved.columnWeights)
    }

    @Test
    fun `dragging the middle line of three columns leaves the third alone`() {
        val grid = Grid(rows = 1, columns = 3)

        val moved = grid.withBoundaryMoved(GridAxis.COLUMN, index = 1, delta = 0.1f)

        assertEquals(1f, moved.columnWeights[2], TOLERANCE)
        assertEquals(3f, moved.columnWeights.sum(), TOLERANCE)
    }

    @Test
    fun `a cell cannot be squeezed away entirely`() {
        val grid = Grid(rows = 1, columns = 2)

        val moved = grid.withBoundaryMoved(GridAxis.COLUMN, index = 1, delta = 10f)

        assertTrue("the shrunk cell keeps some width", moved.columnWeights[1] > 0f)
        assertEquals(2f, moved.columnWeights.sum(), TOLERANCE)
    }

    @Test
    fun `an edge is not a grid line`() {
        val grid = Grid(rows = 1, columns = 2)

        assertEquals(grid, grid.withBoundaryMoved(GridAxis.COLUMN, index = 0, delta = 0.1f))
        assertEquals(grid, grid.withBoundaryMoved(GridAxis.COLUMN, index = 2, delta = 0.1f))
    }

    @Test
    fun `a nonsense drag changes nothing`() {
        val grid = Grid(rows = 1, columns = 2)

        assertEquals(grid, grid.withBoundaryMoved(GridAxis.COLUMN, index = 1, delta = Float.NaN))
    }

    @Test
    fun `moving a line moves the panels it separates`() {
        val grid = Grid(rows = 1, columns = 2).withBoundaryMoved(GridAxis.COLUMN, index = 1, delta = 0.25f)

        val shapes = panelShapes(Layout(grid), 1f, noStyle)

        assertEquals(0.75f, shapes[0].bounds.width, TOLERANCE)
        assertEquals(0.25f, shapes[1].bounds.width, TOLERANCE)
    }
}
