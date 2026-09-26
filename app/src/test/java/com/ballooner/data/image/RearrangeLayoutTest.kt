package com.ballooner.data.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RearrangeLayoutTest {

    @Test
    fun `moving a panel preserves the previewed coordinates without reflowing others`() {
        val layout = computeRearrangeLayout(
            panelRects = listOf(
                PixelRect(0, 0, 100, 80),
                PixelRect(110, 0, 120, 80),
                PixelRect(0, 90, 90, 60),
            ),
            fromIndex = 2,
            desiredLeft = 225,
            desiredTop = 12,
        )

        assertEquals(PixelRect(0, 0, 100, 80), layout.panelRects[0])
        assertEquals(PixelRect(110, 0, 120, 80), layout.panelRects[1])
        assertEquals(PixelRect(225, 12, 90, 60), layout.panelRects[2])
        assertEquals(315, layout.canvasWidth)
        assertEquals(80, layout.canvasHeight)
    }

    @Test
    fun `moving above the canvas shifts every panel while preserving their spacing`() {
        val layout = computeRearrangeLayout(
            panelRects = listOf(
                PixelRect(0, 0, 100, 80),
                PixelRect(110, 0, 120, 80),
            ),
            fromIndex = 1,
            desiredLeft = 4,
            desiredTop = -70,
        )

        assertEquals(PixelRect(0, 70, 100, 80), layout.panelRects[0])
        assertEquals(PixelRect(4, 0, 120, 80), layout.panelRects[1])
        assertEquals(124, layout.canvasWidth)
        assertEquals(150, layout.canvasHeight)
    }

    @Test
    fun `rearrangement keeps an exact freeform destination`() {
        val layout = computeRearrangeLayout(
            panelRects = listOf(
                PixelRect(0, 0, 100, 100),
                PixelRect(110, 0, 100, 100),
                PixelRect(220, 0, 100, 100),
            ),
            fromIndex = 2,
            desiredLeft = 105,
            desiredTop = 80,
        )

        assertEquals(PixelRect(105, 80, 100, 100), layout.panelRects[2])
    }

    @Test
    fun `resizing a panel repositions its neighbor without covering it`() {
        val layout = computeRearrangeLayout(
            panelRects = listOf(
                PixelRect(0, 0, 100, 100),
                PixelRect(110, 0, 100, 100),
            ),
            fromIndex = 0,
            desiredLeft = 0,
            desiredTop = 0,
            desiredWidth = 160,
            desiredHeight = 140,
        )

        assertEquals(PixelRect(0, 0, 160, 140), layout.panelRects[0])
        assertEquals(PixelRect(170, 0, 100, 100), layout.panelRects[1])
        assertEquals(270, layout.canvasWidth)
        assertEquals(140, layout.canvasHeight)
    }

    @Test
    fun `a swapped-dimension destination rebounds the canvas to fit the widened panel`() {
        val layout = computeRearrangeLayout(
            panelRects = listOf(
                PixelRect(0, 0, 100, 300),
                PixelRect(110, 0, 100, 300),
            ),
            fromIndex = 0,
            // The quarter turn of the tall panel: width and height swapped about its top-left corner.
            desiredLeft = 0,
            desiredTop = 0,
            desiredWidth = 300,
            desiredHeight = 100,
        )

        assertEquals(PixelRect(0, 0, 300, 100), layout.panelRects[0])
        assertEquals(PixelRect(310, 0, 100, 300), layout.panelRects[1])
        assertEquals(410, layout.canvasWidth)
        assertEquals(300, layout.canvasHeight)
    }

    @Test
    fun `a turned panel stays on the origin it was turned from`() {
        val layout = computeRearrangeLayout(
            panelRects = listOf(
                PixelRect(20, 30, 100, 300),
                PixelRect(130, 30, 100, 300),
                PixelRect(20, 340, 100, 100),
            ),
            fromIndex = 0,
            desiredLeft = 20,
            desiredTop = 30,
            desiredWidth = 300,
            desiredHeight = 100,
        )

        assertEquals(20, layout.panelRects[0].left)
        assertEquals(30, layout.panelRects[0].top)
    }

    @Test
    fun `a turned panel never drags a neighbour left or up`() {
        val panelRects = listOf(
            PixelRect(20, 30, 100, 300),
            PixelRect(130, 30, 100, 300),
            PixelRect(20, 340, 100, 100),
        )

        val layout = computeRearrangeLayout(
            panelRects = panelRects,
            fromIndex = 0,
            desiredLeft = 20,
            desiredTop = 30,
            desiredWidth = 300,
            desiredHeight = 100,
        )

        // A top-left anchored turn cannot produce a negative coordinate, so the origin shift in
        // computeRearrangeLayout is a no-op and untouched panels only ever move out of the way.
        panelRects.indices.drop(1).forEach { index ->
            val moved = layout.panelRects[index]
            val original = panelRects[index]
            assertTrue("panel $index moved left: $moved was $original", moved.left >= original.left)
            assertTrue("panel $index moved up: $moved was $original", moved.top >= original.top)
        }
    }

    @Test
    fun `rotation deliberately leaves the gap where the panel shrank, an accepted ADR-0002 limitation`() {
        val layout = computeRearrangeLayout(
            panelRects = listOf(
                PixelRect(0, 0, 100, 300),
                PixelRect(0, 310, 100, 100),
            ),
            fromIndex = 0,
            desiredLeft = 0,
            desiredTop = 0,
            desiredWidth = 300,
            desiredHeight = 100,
        )

        // The reflow only pushes neighbours apart to make room for growth, never pulls them back
        // in, so the 10px gap below the tall panel becomes a 210px gap below the turned one.
        // ADR-0002 accepts this and ADR-0003 only changes where the gap lands, not that it exists.
        // Closing it is explicitly out of scope. Do not "fix" this test.
        assertEquals(PixelRect(0, 0, 300, 100), layout.panelRects[0])
        assertEquals(PixelRect(0, 310, 100, 100), layout.panelRects[1])
        assertEquals(210, layout.panelRects[1].top - (layout.panelRects[0].top + layout.panelRects[0].height))
    }
}
