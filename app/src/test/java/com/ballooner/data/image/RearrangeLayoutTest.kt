package com.ballooner.data.image

import com.ballooner.domain.model.ImagePosition
import org.junit.Assert.assertEquals
import org.junit.Test

class RearrangeLayoutTest {

    @Test
    fun `moving the fourth panel to the first slot shifts the others in reading order`() {
        val layout = computeRearrangeLayout(
            panelSizes = listOf(
                PixelSize(100, 100),
                PixelSize(100, 100),
                PixelSize(100, 100),
                PixelSize(100, 100),
            ),
            rowSizes = listOf(2, 2),
            fromIndex = 3,
            targetIndex = 0,
            position = ImagePosition.LEFT,
            gapPx = 10,
        )

        assertEquals(PixelRect(110, 0, 100, 100), layout.panelRects[0])
        assertEquals(PixelRect(0, 110, 100, 100), layout.panelRects[1])
        assertEquals(PixelRect(110, 110, 100, 100), layout.panelRects[2])
        assertEquals(PixelRect(0, 0, 100, 100), layout.panelRects[3])
        assertEquals(210, layout.canvasWidth)
        assertEquals(210, layout.canvasHeight)
    }

    @Test
    fun `rearranging preserves each panel dimensions and aligns rows with one gap`() {
        val layout = computeRearrangeLayout(
            panelSizes = listOf(PixelSize(160, 80), PixelSize(60, 120), PixelSize(90, 90)),
            rowSizes = listOf(2, 1),
            fromIndex = 2,
            targetIndex = 0,
            position = ImagePosition.LEFT,
            gapPx = 8,
        )

        assertEquals(PixelRect(98, 0, 160, 80), layout.panelRects[0])
        assertEquals(PixelRect(0, 98, 60, 120), layout.panelRects[1])
        assertEquals(PixelRect(0, 0, 90, 90), layout.panelRects[2])
        assertEquals(258, layout.canvasWidth)
        assertEquals(218, layout.canvasHeight)
    }

    @Test
    fun `moving below a panel creates an aligned row below its row`() {
        val layout = computeRearrangeLayout(
            panelSizes = listOf(
                PixelSize(100, 80),
                PixelSize(120, 80),
                PixelSize(90, 60),
                PixelSize(110, 60),
            ),
            rowSizes = listOf(2, 2),
            fromIndex = 1,
            targetIndex = 2,
            position = ImagePosition.BOTTOM,
            gapPx = 10,
        )

        assertEquals(PixelRect(0, 160, 120, 80), layout.panelRects[1])
        assertEquals(PixelRect(0, 90, 90, 60), layout.panelRects[2])
        assertEquals(210, layout.canvasWidth)
        assertEquals(240, layout.canvasHeight)
    }

    @Test
    fun `moving below the second panel aligns with its left edge`() {
        val layout = computeRearrangeLayout(
            panelSizes = listOf(
                PixelSize(100, 80),
                PixelSize(120, 80),
                PixelSize(90, 60),
            ),
            rowSizes = listOf(2, 1),
            fromIndex = 2,
            targetIndex = 1,
            position = ImagePosition.BOTTOM,
            gapPx = 10,
        )

        assertEquals(PixelRect(110, 90, 90, 60), layout.panelRects[2])
        assertEquals(230, layout.canvasWidth)
        assertEquals(150, layout.canvasHeight)
    }
}
