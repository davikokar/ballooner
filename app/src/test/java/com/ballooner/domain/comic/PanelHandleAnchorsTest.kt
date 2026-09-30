package com.ballooner.domain.comic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Covers where a panel's handles can sit, whatever shape the cuts have left the panel. */
class PanelHandleAnchorsTest {

    private val size = 0.08f
    private val margin = 0.02f

    private fun anchorsOf(panel: Polygon, fromTop: Boolean = true) =
        panelHandleAnchors(panel, size, margin, fromTop)!!

    @Test
    fun `handles sit inside a square panel`() {
        val panel = Polygon.of(PageRect(0f, 0f, 1f, 1f))

        val (start, end) = anchorsOf(panel)

        assertTrue(panel.contains(start))
        assertTrue(panel.contains(end))
    }

    @Test
    fun `handles keep the same distance from the corners of any panel with room for them`() {
        val inset = size / 2f + margin
        val wide = Polygon.of(PageRect(0f, 0f, 1f, 0.4f))
        val tall = Polygon.of(PageRect(0.3f, 0.2f, 0.5f, 0.8f))

        val (wideStart, wideEnd) = anchorsOf(wide)
        val (tallStart, tallEnd) = anchorsOf(tall)

        assertEquals(inset, wideStart.x, TOLERANCE)
        assertEquals(inset, wideStart.y, TOLERANCE)
        assertEquals(1f - inset, wideEnd.x, TOLERANCE)
        assertEquals(0.3f + inset, tallStart.x, TOLERANCE)
        assertEquals(0.2f + inset, tallStart.y, TOLERANCE)
        assertEquals(0.8f - inset, tallEnd.x, TOLERANCE)
    }

    @Test
    fun `handles counted from the bottom keep that same distance from the bottom corners`() {
        val inset = size / 2f + margin
        val panel = Polygon.of(PageRect(0f, 0.2f, 1f, 0.5f))

        val (start, end) = anchorsOf(panel, fromTop = false)

        assertEquals(0.7f - inset, start.y, TOLERANCE)
        assertEquals(inset, start.x, TOLERANCE)
        assertEquals(1f - inset, end.x, TOLERANCE)
    }

    @Test
    fun `a panel too narrow to hold both handles clear of its edges pushes them out to them`() {
        // Wide enough for the two handles themselves, but not for the margin around them as well.
        val panel = Polygon.of(PageRect(0f, 0f, size * 2.2f, 0.4f))

        val (start, end) = anchorsOf(panel)

        assertEquals(size / 2f, start.x, TOLERANCE)
        assertEquals(size * 2.2f - size / 2f, end.x, TOLERANCE)
        assertTrue("the handles overlap", end.x - start.x >= size)
    }

    @Test
    fun `handles sit inside a panel a cut has left as a triangle`() {
        // A corner-to-corner cut: the box around this panel has three corners outside it.
        val panel = Polygon(listOf(PagePoint(0f, 0f), PagePoint(1f, 1f), PagePoint(0f, 1f)))

        val (start, end) = anchorsOf(panel)

        assertTrue(panel.contains(start))
        assertTrue(panel.contains(end))
    }

    @Test
    fun `handles drop down a panel that comes to a point at the top`() {
        val panel = Polygon(listOf(PagePoint(0.5f, 0f), PagePoint(1f, 1f), PagePoint(0f, 1f)))

        val (start, _) = anchorsOf(panel)

        // The tip is nowhere near wide enough for two handles, so they are found further down.
        assertTrue("handles were left in the tip", start.y > size)
        assertTrue(panel.contains(start))
    }

    @Test
    fun `handles climb up a panel that comes to a point at the bottom`() {
        val panel = Polygon(listOf(PagePoint(0f, 0f), PagePoint(1f, 0f), PagePoint(0.5f, 1f)))

        val (start, end) = anchorsOf(panel, fromTop = false)

        assertTrue("handles were left in the tip", start.y < 1f - size)
        assertTrue(panel.contains(start))
        assertTrue(panel.contains(end))
    }

    @Test
    fun `handles stay inside a panel leaning the other way`() {
        val panel = Polygon(listOf(PagePoint(1f, 0f), PagePoint(1f, 1f), PagePoint(0f, 1f)))

        val (start, end) = anchorsOf(panel)

        assertTrue(panel.contains(start))
        assertTrue(panel.contains(end))
    }

    @Test
    fun `a panel with no room for two handles keeps them in the middle of it`() {
        val panel = Polygon.of(PageRect(0f, 0f, size / 2f, 0.4f))

        val (start, end) = anchorsOf(panel)

        assertEquals(start, end)
        assertTrue(panel.contains(start))
    }

    @Test
    fun `a panel with no area has nowhere to put a handle`() {
        val panel = Polygon.of(PageRect(0.2f, 0.2f, 0f, 0f))

        assertNull(panelHandleAnchors(panel, size, margin))
    }
}
