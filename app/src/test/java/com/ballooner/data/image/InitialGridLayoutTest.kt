package com.ballooner.data.image

import org.junit.Assert.assertEquals
import org.junit.Test

class InitialGridLayoutTest {

    @Test
    fun `wraps images using the configured number of columns`() {
        val layout = computeInitialGridLayout(
            imageSizes = List(5) { PixelSize(200, 100) },
            columns = 4,
            columnWidth = 100,
            gapPx = 10,
        )

        assertEquals(430, layout.canvasWidth)
        assertEquals(110, layout.canvasHeight)
        assertEquals(PixelRect(0, 60, 100, 50), layout.imageRects[4])
        assertEquals(5, layout.panelRects.size)
    }

    @Test
    fun `preserves aspect ratios and starts the next row after its tallest image`() {
        val layout = computeInitialGridLayout(
            imageSizes = listOf(PixelSize(100, 200), PixelSize(200, 100), PixelSize(100, 100)),
            columns = 2,
            columnWidth = 100,
            gapPx = 8,
        )

        assertEquals(PixelRect(0, 0, 100, 200), layout.imageRects[0])
        assertEquals(PixelRect(108, 0, 100, 50), layout.imageRects[1])
        assertEquals(PixelRect(0, 208, 100, 100), layout.imageRects[2])
        assertEquals(308, layout.canvasHeight)
    }
}