package com.ballooner.domain.comic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        // A column line belongs to a row, so a grid two rows deep lists each of them twice.
        assertEquals(listOf(0, 0, 1, 1), columns.map { it.line.row })
        assertEquals(listOf(1, 2, 1, 2), columns.map { it.index })
        assertEquals(1f / 3f, columns[0].position, TOLERANCE)
        assertEquals(2f / 3f, columns[1].position, TOLERANCE)
        assertEquals(listOf(1), rows.map { it.index })
        assertEquals(0.5f, rows[0].position, TOLERANCE)
        // Each column line runs only across its own row's band.
        assertEquals(0f, columns[0].from, TOLERANCE)
        assertEquals(0.5f, columns[0].to, TOLERANCE)
        assertEquals(0.5f, columns[2].from, TOLERANCE)
        assertEquals(1f, columns[2].to, TOLERANCE)
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

        val moved = grid.withBoundaryMoved(GridLine(GridAxis.COLUMN, 1), delta = 0.25f)

        assertEquals(listOf(1.5f, 0.5f), moved.columnWeights)
        assertEquals(2f, moved.columnWeights.sum(), TOLERANCE)
    }

    @Test
    fun `dragging a row line leaves the columns alone`() {
        val grid = Grid(rows = 2, columns = 2)

        val moved = grid.withBoundaryMoved(GridLine(GridAxis.ROW, 1), delta = -0.25f)

        assertEquals(listOf(0.5f, 1.5f), moved.rowWeights)
        assertEquals(listOf(1f, 1f), moved.columnWeights)
    }

    @Test
    fun `dragging the middle line of three columns leaves the third alone`() {
        val grid = Grid(rows = 1, columns = 3)

        val moved = grid.withBoundaryMoved(GridLine(GridAxis.COLUMN, 1), delta = 0.1f)

        assertEquals(1f, moved.columnWeights[2], TOLERANCE)
        assertEquals(3f, moved.columnWeights.sum(), TOLERANCE)
    }

    @Test
    fun `a cell cannot be squeezed away entirely`() {
        val grid = Grid(rows = 1, columns = 2)

        val moved = grid.withBoundaryMoved(GridLine(GridAxis.COLUMN, 1), delta = 10f)

        assertTrue("the shrunk cell keeps some width", moved.columnWeights[1] > 0f)
        assertEquals(2f, moved.columnWeights.sum(), TOLERANCE)
    }

    @Test
    fun `an edge is not a grid line`() {
        val grid = Grid(rows = 1, columns = 2)

        assertEquals(grid, grid.withBoundaryMoved(GridLine(GridAxis.COLUMN, 0), delta = 0.1f))
        assertEquals(grid, grid.withBoundaryMoved(GridLine(GridAxis.COLUMN, 2), delta = 0.1f))
    }

    @Test
    fun `a nonsense drag changes nothing`() {
        val grid = Grid(rows = 1, columns = 2)

        assertEquals(grid, grid.withBoundaryMoved(GridLine(GridAxis.COLUMN, 1), delta = Float.NaN))
    }

    @Test
    fun `moving the first line reshapes only the panels either side of it`() {
        val comic = Comic(
            sizing = PageSizing.Ratio(SQUARE_RATIO),
            style = noStyle,
            layout = Layout(Grid(rows = 1, columns = 4)),
            panels = List(4) { Panel() },
        )

        val moved = comic.withBoundaryMoved(GridLine(GridAxis.COLUMN, 1), delta = 0.1f)

        assertEquals(comic.pageHeight, moved.pageHeight, TOLERANCE)
        val before = comic.panelShapes().map { it.bounds.width }
        val after = moved.panelShapes().map { it.bounds.width }
        assertEquals(before[2], after[2], TOLERANCE)
        assertEquals(before[3], after[3], TOLERANCE)
        assertTrue("the first panel widens", after[0] > before[0])
        assertTrue("the second panel narrows", after[1] < before[1])
    }

    @Test
    fun `moving a later line leaves the sizing alone`() {
        val comic = Comic(
            sizing = PageSizing.FromImage,
            style = noStyle,
            layout = Layout(Grid(rows = 1, columns = 3)),
            panels = List(3) { Panel(PanelImage("image", sourceAspect = 2f)) },
        )

        val moved = comic.withBoundaryMoved(GridLine(GridAxis.COLUMN, 2), delta = 0.1f)

        assertEquals(PageSizing.FromImage, moved.sizing)
        assertEquals(comic.pageHeight, moved.pageHeight, TOLERANCE)
    }

    @Test
    fun `moving a line of each axis leaves every cell aligned with its neighbours`() {
        val comic = Comic(
            sizing = PageSizing.Ratio(SQUARE_RATIO),
            style = noStyle,
            layout = Layout(Grid(rows = 2, columns = 2)),
            panels = List(4) { Panel() },
        )

        val moved = comic
            .withBoundaryMoved(GridLine(GridAxis.COLUMN, 1), delta = 0.15f)
            .withBoundaryMoved(GridLine(GridAxis.ROW, 1), delta = -0.1f)

        // A grid line runs the whole way across, so the two cells of a row keep one top and one
        // height, and the two cells of a column keep one left and one width. Nothing can collide.
        val shapes = moved.panelShapes().map { it.bounds }
        assertEquals(shapes[0].top, shapes[1].top, TOLERANCE)
        assertEquals(shapes[0].height, shapes[1].height, TOLERANCE)
        assertEquals(shapes[0].left, shapes[2].left, TOLERANCE)
        assertEquals(shapes[0].width, shapes[2].width, TOLERANCE)
        assertTrue("the first column widens", shapes[0].width > shapes[1].width)
        assertTrue("the first row shortens", shapes[0].height < shapes[2].height)
    }

    @Test
    fun `freeing a row copies the division it already has`() {
        val grid = Grid(rows = 2, columns = 2)
            .withBoundaryMoved(GridLine(GridAxis.COLUMN, 1), delta = 0.2f)

        val freed = grid.withRowFreed(1)

        assertTrue("the row is free", freed.isRowFree(1))
        assertEquals(grid.columnWeights, freed.columnWeightsAt(1))
    }

    @Test
    fun `a freed row is divided on its own and leaves the rest of the grid alone`() {
        val grid = Grid(rows = 2, columns = 2).withRowFreed(1)

        val moved = grid.withBoundaryMoved(GridLine(GridAxis.COLUMN, 1, row = 1), delta = 0.25f)

        assertEquals(listOf(1.5f, 0.5f), moved.columnWeightsAt(1))
        assertEquals(listOf(1f, 1f), moved.columnWeightsAt(0))
        assertEquals(listOf(1f, 1f), moved.columnWeights)
    }

    @Test
    fun `a row that follows the grid moves every other row that follows it`() {
        val grid = Grid(rows = 3, columns = 2).withRowFreed(2)

        val moved = grid.withBoundaryMoved(GridLine(GridAxis.COLUMN, 1, row = 1), delta = 0.25f)

        assertEquals(listOf(1.5f, 0.5f), moved.columnWeightsAt(0))
        assertEquals(listOf(1.5f, 0.5f), moved.columnWeightsAt(1))
        assertEquals(listOf(1f, 1f), moved.columnWeightsAt(2))
    }

    @Test
    fun `aligning a row puts it back on the grid`() {
        val grid = Grid(rows = 2, columns = 2)
            .withRowFreed(1)
            .withBoundaryMoved(GridLine(GridAxis.COLUMN, 1, row = 1), delta = 0.25f)

        val aligned = grid.withRowAligned(1)

        assertFalse("the row follows the grid again", aligned.isRowFree(1))
        assertEquals(listOf(1f, 1f), aligned.columnWeightsAt(1))
    }

    @Test
    fun `a row cannot be freed while a merged panel crosses it`() {
        val grid = Grid(rows = 2, columns = 2, spans = listOf(Span(0, 0, rowCount = 2)))

        assertEquals(grid, grid.withRowFreed(0))
        assertEquals(grid, grid.withRowFreed(1))
    }

    @Test
    fun `a merge spanning rows is refused once one of them is free`() {
        val grid = Grid(rows = 2, columns = 2).withRowFreed(1)

        val acrossRows = grid.mergedFrom(listOf(Span(0, 0), Span(1, 0)))
        val withinARow = grid.mergedFrom(listOf(Span(1, 0), Span(1, 1)))

        assertNull("a vertical merge has no rectangle to cover", acrossRows)
        assertTrue("a merge inside one row is still fine", withinARow != null)
    }

    @Test
    fun `a free row divides only its own panels`() {
        val comic = Comic(
            sizing = PageSizing.Ratio(SQUARE_RATIO),
            style = noStyle,
            layout = Layout(Grid(rows = 2, columns = 2).withRowFreed(1)),
            panels = List(4) { Panel() },
        )

        val moved = comic.withBoundaryMoved(GridLine(GridAxis.COLUMN, 1, row = 1), delta = 0.2f)

        val before = comic.panelShapes().map { it.bounds.width }
        val after = moved.panelShapes().map { it.bounds.width }
        assertEquals(before[0], after[0], TOLERANCE)
        assertEquals(before[1], after[1], TOLERANCE)
        assertTrue("the freed row's first panel widens", after[2] > before[2])
        assertTrue("its second panel narrows", after[3] < before[3])
    }

    @Test
    fun `evening the weights puts every row back on the grid`() {
        val dragged = Grid(rows = 2, columns = 3)
            .withBoundaryMoved(GridLine(GridAxis.COLUMN, 1), delta = 0.2f)
            .withBoundaryMoved(GridLine(GridAxis.ROW, 1), delta = -0.2f)
            .withRowFreed(1)

        val evened = dragged.withEvenWeights()

        assertEquals(listOf(1f, 1f), evened.rowWeights)
        assertEquals(listOf(1f, 1f, 1f), evened.columnWeights)
        assertEquals(emptyMap<Int, List<Float>>(), evened.rowSplits)
    }

    @Test
    fun `moving a line moves the panels it separates`() {
        val grid = Grid(rows = 1, columns = 2).withBoundaryMoved(GridLine(GridAxis.COLUMN, 1), delta = 0.25f)

        val shapes = panelShapes(Layout(grid), 1f, noStyle)

        assertEquals(0.75f, shapes[0].bounds.width, TOLERANCE)
        assertEquals(0.25f, shapes[1].bounds.width, TOLERANCE)
    }
}
