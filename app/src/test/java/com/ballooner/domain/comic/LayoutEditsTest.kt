package com.ballooner.domain.comic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LayoutEditsTest {

    private val grid = Grid(rows = 3, columns = 3)

    @Test
    fun `merging two adjacent cells makes one panel`() {
        val merged = grid.mergedFrom(listOf(Span(0, 0), Span(0, 1)))!!

        assertEquals(listOf(Span(0, 0, rowCount = 1, columnCount = 2)), merged.spans)
        assertEquals(8, merged.panelSpans().size)
    }

    @Test
    fun `merging a block of cells makes one panel covering the block`() {
        val selection = listOf(Span(0, 0), Span(0, 1), Span(1, 0), Span(1, 1))

        val merged = grid.mergedFrom(selection)!!

        assertEquals(listOf(Span(0, 0, rowCount = 2, columnCount = 2)), merged.spans)
        assertEquals(6, merged.panelSpans().size)
    }

    @Test
    fun `merging an L-shaped selection is refused`() {
        val selection = listOf(Span(0, 0), Span(0, 1), Span(1, 0))

        assertNull(grid.mergedFrom(selection))
    }

    @Test
    fun `merging a selection with a hole is refused`() {
        val selection = listOf(Span(0, 0), Span(0, 2), Span(2, 0), Span(2, 2))

        assertNull(grid.mergedFrom(selection))
    }

    @Test
    fun `merging a single panel is refused`() {
        assertNull(grid.mergedFrom(listOf(Span(0, 0))))
    }

    @Test
    fun `merging the same cell twice is refused`() {
        assertNull(grid.mergedFrom(listOf(Span(0, 0), Span(0, 0))))
    }

    @Test
    fun `merging outside the grid is refused`() {
        assertNull(grid.mergedFrom(listOf(Span(2, 2), Span(2, 3))))
    }

    @Test
    fun `merging a merged panel with its neighbour absorbs it`() {
        val once = grid.mergedFrom(listOf(Span(0, 0), Span(0, 1)))!!

        val twice = once.mergedFrom(listOf(Span(0, 0, rowCount = 1, columnCount = 2), Span(0, 2)))!!

        assertEquals(listOf(Span(0, 0, rowCount = 1, columnCount = 3)), twice.spans)
    }

    @Test
    fun `unmerging restores the covered cells`() {
        val merged = grid.mergedFrom(listOf(Span(0, 0), Span(0, 1)))!!

        val unmerged = merged.unmergedAt(row = 0, column = 1)

        assertEquals(emptyList<Span>(), unmerged.spans)
        assertEquals(9, unmerged.panelSpans().size)
    }

    @Test
    fun `unmerging a cell that is not merged changes nothing`() {
        assertEquals(grid, grid.unmergedAt(row = 2, column = 2))
    }

    @Test
    fun `cuts are added in the order they are drawn and removed by position`() {
        val first = Cut(NormalizedPoint(0f, 0f), NormalizedPoint(1f, 1f), CutScope.WholePage)
        val second = Cut(NormalizedPoint(0f, 1f), NormalizedPoint(1f, 0f), CutScope.WholePage)

        val layout = Layout(grid).withCut(first).withCut(second)

        assertEquals(listOf(first, second), layout.cuts)
        assertEquals(listOf(second), layout.withoutCutAt(0).cuts)
        assertEquals(listOf(first), layout.withoutCutAt(1).cuts)
    }

    @Test
    fun `removing a cut that is not there changes nothing`() {
        val layout = Layout(grid).withCut(Cut(NormalizedPoint(0f, 0f), NormalizedPoint(1f, 1f), CutScope.WholePage))

        assertEquals(layout, layout.withoutCutAt(5))
    }
}
