package com.ballooner.data.image

import org.junit.Assert.assertEquals
import org.junit.Test

class RearrangeLayoutTest {

    @Test
    fun `moving a panel snaps beside the nearest panel without reflowing others`() {
        val layout = computeRearrangeLayout(
            panelRects = listOf(
                PixelRect(0, 0, 100, 80),
                PixelRect(110, 0, 120, 80),
                PixelRect(0, 90, 90, 60),
            ),
            fromIndex = 2,
            desiredLeft = 225,
            desiredTop = 12,
            gapPx = 10,
        )

        assertEquals(PixelRect(0, 0, 100, 80), layout.panelRects[0])
        assertEquals(PixelRect(110, 0, 120, 80), layout.panelRects[1])
        assertEquals(PixelRect(240, 10, 90, 60), layout.panelRects[2])
        assertEquals(330, layout.canvasWidth)
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
            gapPx = 10,
        )

        assertEquals(PixelRect(0, 90, 100, 80), layout.panelRects[0])
        assertEquals(PixelRect(0, 0, 120, 80), layout.panelRects[1])
        assertEquals(120, layout.canvasWidth)
        assertEquals(170, layout.canvasHeight)
    }

    @Test
    fun `blocked attachment snaps to the nearest available side`() {
        val layout = computeRearrangeLayout(
            panelRects = listOf(
                PixelRect(0, 0, 100, 100),
                PixelRect(110, 0, 100, 100),
                PixelRect(220, 0, 100, 100),
            ),
            fromIndex = 2,
            desiredLeft = 105,
            desiredTop = 80,
            gapPx = 10,
        )

        assertEquals(PixelRect(110, 110, 100, 100), layout.panelRects[2])
    }
}
