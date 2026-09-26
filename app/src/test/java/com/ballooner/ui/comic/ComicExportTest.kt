package com.ballooner.ui.comic

import androidx.compose.ui.unit.IntSize
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.ComicStyle
import com.ballooner.domain.comic.Grid
import com.ballooner.domain.comic.Layout
import com.ballooner.domain.comic.PageShape
import com.ballooner.domain.comic.Panel
import com.ballooner.domain.comic.PanelImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComicExportTest {

    private val style = ComicStyle(pageMargin = 0f, gutter = 0f, borderThickness = 0f)

    private fun sizesOf(vararg entries: Pair<String, IntSize>): (String) -> IntSize? {
        val sizes = entries.toMap()
        return { sizes[it] }
    }

    private fun comic(panels: List<Panel>, columns: Int = panels.size) = Comic(
        pageShape = PageShape.SQUARE,
        style = style,
        layout = Layout(Grid(rows = 1, columns = columns)),
        panels = panels,
    )

    @Test
    fun `one image filling the page exports at its own width`() {
        val comic = comic(listOf(Panel(PanelImage("a"))), columns = 1)

        val width = comicPixelWidth(comic, sizesOf("a" to IntSize(1200, 1200)))

        assertEquals(1200, width)
    }

    @Test
    fun `an image filling half the page asks for twice its own width`() {
        val comic = comic(listOf(Panel(PanelImage("a")), Panel(PanelImage("b"))))

        // Each panel is half the page wide and shaped like these images, so drawing a's 600px
        // across half a page needs a 1200px page.
        val width = comicPixelWidth(comic, sizesOf("a" to IntSize(600, 1200), "b" to IntSize(300, 600)))

        assertEquals(1200, width)
    }

    @Test
    fun `the most detailed image decides the size`() {
        val comic = comic(listOf(Panel(PanelImage("a")), Panel(PanelImage("b"))))

        val width = comicPixelWidth(comic, sizesOf("a" to IntSize(400, 800), "b" to IntSize(1000, 2000)))

        assertEquals(2000, width)
    }

    @Test
    fun `a zoomed image needs more page to keep its detail`() {
        val zoomed = Panel(PanelImage("a", zoom = 2f))
        val comic = comic(listOf(zoomed), columns = 1)

        val width = comicPixelWidth(comic, sizesOf("a" to IntSize(1000, 1000)))

        // At twice the covering scale only half the image is on the page, so the page is smaller.
        assertEquals(500, width)
    }

    @Test
    fun `a comic with no images still exports at a sensible size`() {
        val comic = comic(listOf(Panel(), Panel()))

        val width = comicPixelWidth(comic, sizesOf())

        assertTrue("a usable default", width in 320..4096)
    }

    @Test
    fun `an enormous image is capped rather than exported whole`() {
        val comic = comic(listOf(Panel(PanelImage("a"))), columns = 1)

        val width = comicPixelWidth(comic, sizesOf("a" to IntSize(20000, 20000)))

        assertEquals(4096, width)
    }

    @Test
    fun `a tiny image is not exported smaller than the floor`() {
        val comic = comic(listOf(Panel(PanelImage("a"))), columns = 1)

        val width = comicPixelWidth(comic, sizesOf("a" to IntSize(16, 16)))

        assertEquals(320, width)
    }

    @Test
    fun `an image that has not loaded yet is left out of the reckoning`() {
        val comic = comic(listOf(Panel(PanelImage("missing")), Panel(PanelImage("b"))))

        val width = comicPixelWidth(comic, sizesOf("b" to IntSize(500, 1000)))

        assertEquals(1000, width)
    }

    @Test
    fun `a square image in a tall panel spans the whole page`() {
        val comic = comic(listOf(Panel(PanelImage("a")), Panel()))

        // Covering a half-width, full-height panel with a square image makes it page-width, so
        // its own pixels already span the page.
        val width = comicPixelWidth(comic, sizesOf("a" to IntSize(900, 900)))

        assertEquals(900, width)
    }
}
