package com.ballooner.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImagePlacementTest {

    @Test
    fun `two side by side panels expose separate targets above and below each panel`() {
        val left = RectFraction(left = 0f, top = 0f, width = 0.48f, height = 1f)
        val right = RectFraction(left = 0.52f, top = 0f, width = 0.48f, height = 1f)

        val placements = availableImagePlacements(listOf(left, right))

        assertEquals(6, placements.size)
        assertTrue(ImagePlacement(left, ImagePosition.LEFT) in placements)
        assertTrue(ImagePlacement(right, ImagePosition.RIGHT) in placements)
        assertTrue(ImagePlacement(left, ImagePosition.TOP) in placements)
        assertTrue(ImagePlacement(right, ImagePosition.TOP) in placements)
        assertTrue(ImagePlacement(left, ImagePosition.BOTTOM) in placements)
        assertTrue(ImagePlacement(right, ImagePosition.BOTTOM) in placements)
    }
}
