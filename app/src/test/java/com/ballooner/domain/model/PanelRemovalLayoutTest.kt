package com.ballooner.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PanelRemovalLayoutTest {

    @Test
    fun `deleting the right panel crops the canvas to the remaining left panel`() {
        val remaining = RectFraction(left = 0f, top = 0f, width = 0.48f, height = 1f)

        val retained = retainedCanvasRect(listOf(remaining))

        assertEquals(remaining, retained)
        assertEquals(RectFraction(0f, 0f, 1f, 1f), remaining.remappedFrom(retained))
    }

    @Test
    fun `deleting the top row crops and expands the bottom row`() {
        val bottomLeft = RectFraction(left = 0f, top = 0.52f, width = 0.48f, height = 0.48f)
        val bottomRight = RectFraction(left = 0.52f, top = 0.52f, width = 0.48f, height = 0.48f)

        val retained = retainedCanvasRect(listOf(bottomLeft, bottomRight))

        assertRectEquals(RectFraction(0f, 0.52f, 1f, 0.48f), retained)
        assertRectEquals(RectFraction(0f, 0f, 0.48f, 1f), bottomLeft.remappedFrom(retained))
        assertRectEquals(RectFraction(0.52f, 0f, 0.48f, 1f), bottomRight.remappedFrom(retained))
    }

    private fun assertRectEquals(expected: RectFraction, actual: RectFraction) {
        assertEquals(expected.left, actual.left, 0.0001f)
        assertEquals(expected.top, actual.top, 0.0001f)
        assertEquals(expected.width, actual.width, 0.0001f)
        assertEquals(expected.height, actual.height, 0.0001f)
    }
}
