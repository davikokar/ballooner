package com.ballooner.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImagePlacementTest {

    @Test
    fun `panel magnetically aligns when dropped edge approaches another panel`() {
        val anchor = RectFraction(0f, 0f, 0.4f, 0.4f)
        val moving = RectFraction(0.6f, 0f, 0.4f, 0.4f)
        val desired = moving.copy(left = 0.425f, top = 0.01f)

        val aligned = magneticallyAlignedPanel(
            panels = listOf(anchor, moving),
            moving = moving,
            desired = desired,
            canvasWidth = 1000,
            canvasHeight = 1000,
            snapThresholdPx = 24f,
        )

        assertEquals(0.408f, aligned.left, 0.0001f)
        assertEquals(0f, aligned.top, 0.0001f)
    }

    @Test
    fun `panel remains freely positioned outside magnetic range`() {
        val anchor = RectFraction(0f, 0f, 0.4f, 0.4f)
        val moving = RectFraction(0.6f, 0f, 0.4f, 0.4f)
        val desired = moving.copy(left = 0.5f, top = 0.1f)

        val aligned = magneticallyAlignedPanel(
            panels = listOf(anchor, moving),
            moving = moving,
            desired = desired,
            canvasWidth = 1000,
            canvasHeight = 1000,
            snapThresholdPx = 24f,
        )

        assertEquals(desired, aligned)
    }

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
