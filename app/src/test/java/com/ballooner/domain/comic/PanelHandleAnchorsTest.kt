package com.ballooner.domain.comic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Covers where a panel's handles can sit, whatever shape the cuts have left the panel. */
class PanelHandleAnchorsTest {

    private val size = 0.1f

    @Test
    fun `handles sit inside a square panel`() {
        val panel = Polygon.of(PageRect(0f, 0f, 1f, 1f))

        val (start, end) = panelHandleAnchors(panel, size)!!

        assertTrue(panel.contains(start))
        assertTrue(panel.contains(end))
    }

    @Test
    fun `handles take the top of a square panel`() {
        val panel = Polygon.of(PageRect(0f, 0f, 1f, 1f))

        val (start, end) = panelHandleAnchors(panel, size)!!

        assertEquals(size / 2f, start.y, TOLERANCE)
        assertEquals(size / 2f, start.x, TOLERANCE)
        assertEquals(1f - size / 2f, end.x, TOLERANCE)
    }

    @Test
    fun `handles sit inside a panel a cut has left as a triangle`() {
        // A corner-to-corner cut: the box around this panel has three corners outside it.
        val panel = Polygon(listOf(PagePoint(0f, 0f), PagePoint(1f, 1f), PagePoint(0f, 1f)))

        val (start, end) = panelHandleAnchors(panel, size)!!

        assertTrue(panel.contains(start))
        assertTrue(panel.contains(end))
    }

    @Test
    fun `handles drop down a panel that comes to a point at the top`() {
        val panel = Polygon(listOf(PagePoint(0.5f, 0f), PagePoint(1f, 1f), PagePoint(0f, 1f)))

        val (start, _) = panelHandleAnchors(panel, size)!!

        // The tip is nowhere near wide enough for two handles, so they are found further down.
        assertTrue("handles were left in the tip", start.y > size)
        assertTrue(panel.contains(start))
    }

    @Test
    fun `handles stay inside a panel leaning the other way`() {
        val panel = Polygon(listOf(PagePoint(1f, 0f), PagePoint(1f, 1f), PagePoint(0f, 1f)))

        val (start, end) = panelHandleAnchors(panel, size)!!

        assertTrue(panel.contains(start))
        assertTrue(panel.contains(end))
    }

    @Test
    fun `a panel with no room for two handles keeps them in the middle of it`() {
        val panel = Polygon.of(PageRect(0f, 0f, 0.05f, 0.4f))

        val (start, end) = panelHandleAnchors(panel, size)!!

        assertEquals(start, end)
        assertTrue(panel.contains(start))
    }

    @Test
    fun `a panel with no area has nowhere to put a handle`() {
        val panel = Polygon.of(PageRect(0.2f, 0.2f, 0f, 0f))

        assertNull(panelHandleAnchors(panel, size))
    }
}
