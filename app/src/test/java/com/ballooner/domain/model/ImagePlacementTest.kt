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

    @Test
    fun `default placement is based on the rightmost panel coordinate`() {
        val topLeft = RectFraction(0f, 0f, 0.48f, 0.48f)
        val topRight = RectFraction(0.52f, 0f, 0.48f, 0.48f)
        val bottomLeft = RectFraction(0f, 0.52f, 0.48f, 0.48f)
        val placements = availableImagePlacements(listOf(topLeft, topRight, bottomLeft))

        val default = defaultImagePlacement(placements, listOf(topLeft, topRight, bottomLeft))

        assertEquals(ImagePlacement(topRight, ImagePosition.RIGHT), default)
    }

    @Test
    fun `default placement for one panel is on its right`() {
        val panel = RectFraction(0f, 0f, 1f, 1f)
        val placements = availableImagePlacements(listOf(panel))

        val default = defaultImagePlacement(placements, listOf(panel))

        assertEquals(ImagePlacement(panel, ImagePosition.RIGHT), default)
    }

    @Test
    fun `edge buttons appear only on open right and bottom edges`() {
        val topLeft = RectFraction(0f, 0f, 0.48f, 0.48f)
        val topRight = RectFraction(0.52f, 0f, 0.48f, 0.48f)
        val bottomLeft = RectFraction(0f, 0.52f, 0.48f, 0.48f)

        val placements = edgeImagePlacements(listOf(topLeft, topRight, bottomLeft))

        assertEquals(
            setOf(
                ImagePlacement(topRight, ImagePosition.RIGHT),
                ImagePlacement(topRight, ImagePosition.BOTTOM),
                ImagePlacement(bottomLeft, ImagePosition.RIGHT),
                ImagePlacement(bottomLeft, ImagePosition.BOTTOM),
            ),
            placements.toSet(),
        )
    }
}
