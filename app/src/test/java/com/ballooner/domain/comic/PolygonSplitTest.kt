package com.ballooner.domain.comic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PolygonSplitTest {

    private val unitSquare = Polygon.of(PageRect(0f, 0f, 1f, 1f))

    @Test
    fun `a vertical cut leaves a gutter between the two pieces`() {
        val line = Line(PagePoint(0.5f, 0f), PagePoint(0.5f, 1f))

        val (first, second) = splitConvex(unitSquare, line, inset = 0.05f)!!

        val left = listOf(first, second).minBy { it.bounds.left }
        val right = listOf(first, second).maxBy { it.bounds.left }
        assertEquals(0.45f, left.bounds.right, TOLERANCE)
        assertEquals(0.55f, right.bounds.left, TOLERANCE)
    }

    @Test
    fun `a line that misses the polygon does not split it`() {
        val line = Line(PagePoint(2f, 0f), PagePoint(2f, 1f))

        assertNull(splitConvex(unitSquare, line, inset = 0f))
    }

    @Test
    fun `a line along the polygon edge does not split it`() {
        val line = Line(PagePoint(0f, 0f), PagePoint(0f, 1f))

        assertNull(splitConvex(unitSquare, line, inset = 0f))
    }

    @Test
    fun `a diagonal cut through opposite corners makes two triangles`() {
        val line = Line(PagePoint(0f, 0f), PagePoint(1f, 1f))

        val (first, second) = splitConvex(unitSquare, line, inset = 0f)!!

        assertEquals(3, first.vertices.size)
        assertEquals(3, second.vertices.size)
        assertEquals(0.5f, first.area, TOLERANCE)
        assertEquals(0.5f, second.area, TOLERANCE)
    }

    @Test
    fun `a diagonal cut across one corner makes a triangle and a pentagon`() {
        val line = Line(PagePoint(0f, 0.5f), PagePoint(0.5f, 0f))

        val (first, second) = splitConvex(unitSquare, line, inset = 0f)!!

        val sizes = listOf(first.vertices.size, second.vertices.size).sorted()
        assertEquals(listOf(3, 5), sizes)
        assertEquals(1f, first.area + second.area, TOLERANCE)
    }

    @Test
    fun `an inset wider than the polygon swallows a piece and cancels the split`() {
        val line = Line(PagePoint(0.5f, 0f), PagePoint(0.5f, 1f))

        assertNull(splitConvex(unitSquare, line, inset = 0.6f))
    }

    @Test
    fun `the pieces of a cut stay convex`() {
        val line = Line(PagePoint(0f, 0.2f), PagePoint(1f, 0.9f))

        val (first, second) = splitConvex(unitSquare, line, inset = 0.01f)!!

        assertTrue(first.isConvex())
        assertTrue(second.isConvex())
    }

    @Test
    fun `a polygon contains its own centroid but not a point outside it`() {
        val triangle = Polygon(listOf(PagePoint(0f, 0f), PagePoint(1f, 0f), PagePoint(0f, 1f)))

        assertTrue(triangle.contains(triangle.centroid))
        assertFalse(triangle.contains(PagePoint(0.9f, 0.9f)))
    }

    @Test
    fun `a trace that begins inside a panel is anchored where it began`() {
        val point = firstPanelPoint(listOf(unitSquare), PagePoint(0.3f, 0.3f), PagePoint(0.9f, 0.9f))

        assertEquals(PagePoint(0.3f, 0.3f), point)
    }

    @Test
    fun `a trace that begins beside a panel is anchored inside it`() {
        val point = firstPanelPoint(listOf(unitSquare), PagePoint(-0.5f, 0.5f), PagePoint(1.5f, 0.5f))!!

        assertTrue(unitSquare.contains(point))
    }

    @Test
    fun `a trace is anchored in the first panel it enters`() {
        val left = Polygon.of(PageRect(0f, 0f, 0.4f, 1f))
        val right = Polygon.of(PageRect(0.6f, 0f, 0.4f, 1f))

        val point = firstPanelPoint(listOf(left, right), PagePoint(-0.5f, 0.5f), PagePoint(1.5f, 0.5f))!!

        assertTrue(left.contains(point))
    }

    @Test
    fun `a trace that meets no panel is anchored nowhere`() {
        assertNull(firstPanelPoint(listOf(unitSquare), PagePoint(2f, 0f), PagePoint(2f, 1f)))
    }

    private fun Polygon.isConvex(): Boolean {
        var positive = false
        var negative = false
        for (i in vertices.indices) {
            val a = vertices[i]
            val b = vertices[(i + 1) % vertices.size]
            val c = vertices[(i + 2) % vertices.size]
            val cross = (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x)
            if (cross > TOLERANCE) positive = true
            if (cross < -TOLERANCE) negative = true
        }
        return !(positive && negative)
    }
}

internal const val TOLERANCE = 1e-4f
