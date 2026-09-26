package com.ballooner.domain.comic

import org.junit.Assert.assertEquals
import org.junit.Test

class PanelShapesTest {

    private val noStyle = ComicStyle(pageMargin = 0f, gutter = 0f, borderThickness = 0f)

    @Test
    fun `a single panel layout fills the page`() {
        val shapes = panelShapes(Layout(Grid(rows = 1, columns = 1)), PageShape.SQUARE, noStyle)

        assertEquals(1, shapes.size)
        assertRect(PageRect(0f, 0f, 1f, 1f), shapes[0].bounds)
    }

    @Test
    fun `a page shape taller than it is wide makes taller panels`() {
        val shapes = panelShapes(Layout(Grid(rows = 1, columns = 1)), PageShape.PORTRAIT, noStyle)

        assertRect(PageRect(0f, 0f, 1f, 4f / 3f), shapes[0].bounds)
    }

    @Test
    fun `a grid divides the page evenly`() {
        val shapes = panelShapes(Layout(Grid(rows = 2, columns = 2)), PageShape.SQUARE, noStyle)

        assertEquals(4, shapes.size)
        assertRect(PageRect(0f, 0f, 0.5f, 0.5f), shapes[0].bounds)
        assertRect(PageRect(0.5f, 0f, 0.5f, 0.5f), shapes[1].bounds)
        assertRect(PageRect(0f, 0.5f, 0.5f, 0.5f), shapes[2].bounds)
        assertRect(PageRect(0.5f, 0.5f, 0.5f, 0.5f), shapes[3].bounds)
    }

    @Test
    fun `the margin insets the page and the gutter separates neighbours`() {
        val style = ComicStyle(pageMargin = 0.1f, gutter = 0.1f, borderThickness = 0f)

        val shapes = panelShapes(Layout(Grid(rows = 1, columns = 2)), PageShape.SQUARE, style)

        assertRect(PageRect(0.1f, 0.1f, 0.35f, 0.8f), shapes[0].bounds)
        assertRect(PageRect(0.55f, 0.1f, 0.35f, 0.8f), shapes[1].bounds)
    }

    @Test
    fun `column weights change panel proportions`() {
        val grid = Grid(rows = 1, columns = 2, columnWeights = listOf(3f, 1f))

        val shapes = panelShapes(Layout(grid), PageShape.SQUARE, noStyle)

        assertRect(PageRect(0f, 0f, 0.75f, 1f), shapes[0].bounds)
        assertRect(PageRect(0.75f, 0f, 0.25f, 1f), shapes[1].bounds)
    }

    @Test
    fun `merging the top row of a grid makes one wide panel`() {
        val grid = Grid(rows = 2, columns = 2, spans = listOf(Span(0, 0, rowCount = 1, columnCount = 2)))

        val shapes = panelShapes(Layout(grid), PageShape.SQUARE, noStyle)

        assertEquals(3, shapes.size)
        assertRect(PageRect(0f, 0f, 1f, 0.5f), shapes[0].bounds)
        assertRect(PageRect(0f, 0.5f, 0.5f, 0.5f), shapes[1].bounds)
        assertRect(PageRect(0.5f, 0.5f, 0.5f, 0.5f), shapes[2].bounds)
    }

    @Test
    fun `merging a column of a grid makes one tall panel`() {
        val grid = Grid(rows = 2, columns = 2, spans = listOf(Span(0, 0, rowCount = 2, columnCount = 1)))

        val shapes = panelShapes(Layout(grid), PageShape.SQUARE, noStyle)

        assertEquals(3, shapes.size)
        assertEquals(1, shapes.count { it.bounds.height > 0.9f })
    }

    @Test
    fun `a page cut splits every panel it crosses`() {
        val cut = Cut(NormalizedPoint(0.5f, 0f), NormalizedPoint(0.5f, 1f), CutScope.WholePage)

        val shapes = panelShapes(Layout(Grid(1, 1), listOf(cut)), PageShape.SQUARE, noStyle)

        assertEquals(2, shapes.size)
        assertRect(PageRect(0f, 0f, 0.5f, 1f), shapes[0].bounds)
        assertRect(PageRect(0.5f, 0f, 0.5f, 1f), shapes[1].bounds)
    }

    @Test
    fun `a page cut crossing a two row grid splits both rows`() {
        val cut = Cut(NormalizedPoint(0.5f, 0f), NormalizedPoint(0.5f, 1f), CutScope.WholePage)

        val shapes = panelShapes(Layout(Grid(rows = 2, columns = 1), listOf(cut)), PageShape.SQUARE, noStyle)

        assertEquals(4, shapes.size)
    }

    @Test
    fun `a panel cut splits only the panel holding its anchor`() {
        val cut = Cut(
            a = NormalizedPoint(0.5f, 0f),
            b = NormalizedPoint(0.5f, 1f),
            scope = CutScope.AtPoint(NormalizedPoint(0.25f, 0.25f)),
        )

        val shapes = panelShapes(Layout(Grid(rows = 2, columns = 1), listOf(cut)), PageShape.SQUARE, noStyle)

        assertEquals(3, shapes.size)
        assertRect(PageRect(0f, 0f, 0.5f, 0.5f), shapes[0].bounds)
        assertRect(PageRect(0.5f, 0f, 0.5f, 0.5f), shapes[1].bounds)
        assertRect(PageRect(0f, 0.5f, 1f, 0.5f), shapes[2].bounds)
    }

    @Test
    fun `a diagonal cut produces angled panels that still cover the page`() {
        val cut = Cut(NormalizedPoint(0f, 0f), NormalizedPoint(1f, 1f), CutScope.WholePage)

        val shapes = panelShapes(Layout(Grid(1, 1), listOf(cut)), PageShape.SQUARE, noStyle)

        assertEquals(2, shapes.size)
        assertEquals(1f, shapes.sumOf { it.area.toDouble() }.toFloat(), TOLERANCE)
        assertEquals(3, shapes[0].vertices.size)
    }

    @Test
    fun `a panel cut whose anchor lands in a gutter is skipped`() {
        val style = ComicStyle(pageMargin = 0f, gutter = 0.2f, borderThickness = 0f)
        val cut = Cut(
            a = NormalizedPoint(0.5f, 0f),
            b = NormalizedPoint(0.5f, 1f),
            scope = CutScope.AtPoint(NormalizedPoint(0.5f, 0.5f)),
        )

        val shapes = panelShapes(Layout(Grid(rows = 1, columns = 2), listOf(cut)), PageShape.SQUARE, style)

        assertEquals(2, shapes.size)
    }

    @Test
    fun `removing an earlier cut leaves a later panel cut still working`() {
        val first = Cut(NormalizedPoint(0f, 0.5f), NormalizedPoint(1f, 0.5f), CutScope.WholePage)
        val second = Cut(
            a = NormalizedPoint(0.5f, 0f),
            b = NormalizedPoint(0.5f, 1f),
            scope = CutScope.AtPoint(NormalizedPoint(0.25f, 0.25f)),
        )

        val withBoth = panelShapes(Layout(Grid(1, 1), listOf(first, second)), PageShape.SQUARE, noStyle)
        val withoutFirst = panelShapes(Layout(Grid(1, 1), listOf(second)), PageShape.SQUARE, noStyle)

        assertEquals(3, withBoth.size)
        assertEquals(2, withoutFirst.size)
        assertRect(PageRect(0f, 0f, 0.5f, 1f), withoutFirst[0].bounds)
    }

    private fun assertRect(expected: PageRect, actual: PageRect) {
        assertEquals("left", expected.left, actual.left, TOLERANCE)
        assertEquals("top", expected.top, actual.top, TOLERANCE)
        assertEquals("width", expected.width, actual.width, TOLERANCE)
        assertEquals("height", expected.height, actual.height, TOLERANCE)
    }
}
