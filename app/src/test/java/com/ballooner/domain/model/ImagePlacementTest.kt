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
    fun `panel resize magnetically aligns an edge while preserving proportions`() {
        val moving = RectFraction(0f, 0f, 0.3f, 0.3f)
        val rightNeighbor = RectFraction(0.5f, 0f, 0.2f, 0.3f)
        val bottomNeighbor = RectFraction(0f, 0.6f, 0.3f, 0.2f)

        val resized = magneticallyResizedPanel(
            panels = listOf(moving, rightNeighbor, bottomNeighbor),
            moving = moving,
            desired = moving.copy(width = 0.49f, height = 0.49f),
            canvasWidth = 1000,
            canvasHeight = 1000,
            snapThresholdPx = 20f,
        )

        assertEquals(0.5f, resized.width, 0.0001f)
        assertEquals(0.5f, resized.height, 0.0001f)
    }

    @Test
    fun `panel resize preserves proportions away from magnetic edges`() {
        val moving = RectFraction(0f, 0f, 0.3f, 0.3f)
        val neighbor = RectFraction(0.7f, 0.7f, 0.2f, 0.2f)
        val desired = moving.copy(width = 0.45f, height = 0.48f)

        val resized = magneticallyResizedPanel(
            panels = listOf(moving, neighbor),
            moving = moving,
            desired = desired,
            canvasWidth = 1000,
            canvasHeight = 1000,
            snapThresholdPx = 20f,
        )

        assertEquals(0.465f, resized.width, 0.0001f)
        assertEquals(0.465f, resized.height, 0.0001f)
    }

    @Test
    fun `panel resize preserves pixel aspect ratio on a non-square canvas`() {
        val moving = RectFraction(0f, 0f, 0.25f, 0.4f)

        val resized = magneticallyResizedPanel(
            panels = listOf(moving),
            moving = moving,
            desired = moving.copy(width = 0.4f, height = 0.5f),
            canvasWidth = 1200,
            canvasHeight = 800,
            snapThresholdPx = 20f,
        )

        val originalPixelAspectRatio = moving.width * 1200 / (moving.height * 800)
        val resizedPixelAspectRatio = resized.width * 1200 / (resized.height * 800)
        assertEquals(originalPixelAspectRatio, resizedPixelAspectRatio, 0.0001f)
    }

    @Test
    fun `panel resize repositions every neighbor it would cover`() {
        val moving = RectFraction(0f, 0f, 0.4f, 0.4f)
        val right = RectFraction(0.45f, 0f, 0.4f, 0.4f)
        val bottom = RectFraction(0f, 0.45f, 0.4f, 0.4f)
        val diagonal = RectFraction(0.45f, 0.45f, 0.4f, 0.4f)

        val repositioned = repositionPanelsAfterResize(
            panels = listOf(moving, right, bottom, diagonal),
            moving = moving,
            resized = moving.copy(width = 0.6f, height = 0.6f),
        )

        assertEquals(RectFraction(0f, 0f, 0.6f, 0.6f), repositioned[0])
        assertEquals(RectFraction(0.65f, 0f, 0.4f, 0.4f), repositioned[1])
        assertEquals(RectFraction(0f, 0.65f, 0.4f, 0.4f), repositioned[2])
        assertEquals(RectFraction(0.65f, 0.65f, 0.4f, 0.4f), repositioned[3])
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
