package com.ballooner.domain.comic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PageSizingTest {

    private val noStyle = ComicStyle(pageMargin = 0f, gutter = 0f, borderThickness = 0f)

    @Test
    fun `a square single panel gives a square page`() {
        val comic = Comic(sizing = PageSizing.Ratio(SQUARE_RATIO), style = noStyle)

        assertEquals(1f, comic.pageHeight, TOLERANCE)
    }

    @Test
    fun `a tall single panel gives a page as tall as the panel asks for`() {
        val comic = Comic(sizing = PageSizing.Ratio(TALL_RATIO), style = noStyle)

        assertEquals(1.5f, comic.pageHeight, TOLERANCE)
    }

    @Test
    fun `a wide single panel gives a page as wide as the panel asks for`() {
        val comic = Comic(sizing = PageSizing.Ratio(WIDE_RATIO), style = noStyle)

        assertEquals(1f / 1.5f, comic.pageHeight, TOLERANCE)
    }

    @Test
    fun `the single panel really is the shape that was asked for`() {
        val comic = Comic(sizing = PageSizing.Ratio(WIDE_RATIO), style = noStyle)

        val panel = comic.panelShapes().single().bounds

        assertEquals(WIDE_RATIO, panel.width / panel.height, TOLERANCE)
    }

    @Test
    fun `the panel keeps its shape when the margin changes under it`() {
        val comic = Comic(
            sizing = PageSizing.Ratio(WIDE_RATIO),
            style = ComicStyle(pageMargin = 0.12f, gutter = 0f, borderThickness = 0f),
        )

        val panel = comic.panelShapes().single().bounds

        assertEquals(WIDE_RATIO, panel.width / panel.height, TOLERANCE)
    }

    @Test
    fun `the panel keeps its shape when the gutter changes under it`() {
        val comic = Comic(
            sizing = PageSizing.Ratio(SQUARE_RATIO),
            style = ComicStyle(pageMargin = 0f, gutter = 0.08f, borderThickness = 0f),
            layout = Layout(Grid(rows = 2, columns = 2)),
            panels = List(4) { Panel() },
        )

        val first = comic.panelShapes().first().bounds

        assertEquals(SQUARE_RATIO, first.width / first.height, TOLERANCE)
    }

    @Test
    fun `square cells in a grid give a page as tall as the grid is deep`() {
        val comic = Comic(
            sizing = PageSizing.Ratio(SQUARE_RATIO),
            style = noStyle,
            layout = Layout(Grid(rows = 3, columns = 2)),
            panels = List(6) { Panel() },
        )

        assertEquals(1.5f, comic.pageHeight, TOLERANCE)
    }

    @Test
    fun `an auto page takes its height from the image in the reference panel`() {
        val comic = Comic(
            sizing = PageSizing.FromImage,
            style = noStyle,
            panels = listOf(Panel(PanelImage("sample", sourceAspect = 2f))),
        )

        assertEquals(0.5f, comic.pageHeight, TOLERANCE)
    }

    @Test
    fun `an auto page with no image yet waits as a square`() {
        val comic = Comic(sizing = PageSizing.FromImage, style = noStyle)

        assertEquals(1f, comic.pageHeight, TOLERANCE)
    }

    @Test
    fun `an auto page reshapes when the image is replaced`() {
        val comic = Comic(
            sizing = PageSizing.FromImage,
            style = noStyle,
            panels = listOf(Panel(PanelImage("tall", sourceAspect = 0.5f))),
        )

        val swapped = comic.copy(panels = listOf(Panel(PanelImage("wide", sourceAspect = 2f))))

        assertEquals(2f, comic.pageHeight, TOLERANCE)
        assertEquals(0.5f, swapped.pageHeight, TOLERANCE)
    }

    @Test
    fun `a ratio that would collapse the page is held back`() {
        val comic = Comic(sizing = PageSizing.Ratio(1000f), style = noStyle)

        assertEquals(MIN_PAGE_HEIGHT, comic.pageHeight, TOLERANCE)
    }

    @Test
    fun `merging the reference panel keeps the other panels' images`() {
        val comic = Comic(
            sizing = PageSizing.Ratio(SQUARE_RATIO),
            style = noStyle,
            layout = Layout(Grid(rows = 2, columns = 2)),
            panels = List(4) { index -> Panel(PanelImage("image-$index")) },
        )
        val merged = comic.layout.grid.mergedFrom(listOf(Span(0, 0), Span(0, 1)))!!

        val change = comic.withLayout(comic.layout.copy(grid = merged))

        // The merge doubles the page's height, which must not count as losing the bottom row.
        assertTrue(comic.pageHeight < change.comic.pageHeight)
        assertEquals(1, change.removedImages)
        assertEquals(listOf("image-0", "image-2", "image-3"), change.comic.panels.mapNotNull { it.image?.sourceUri })
    }

    private companion object {
        const val TOLERANCE = 1e-4f
    }
}
