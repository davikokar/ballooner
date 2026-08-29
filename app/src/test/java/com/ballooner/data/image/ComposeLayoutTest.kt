package com.ballooner.data.image

import com.ballooner.domain.model.ImagePosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposeLayoutTest {

    @Test
    fun `placing an image to the right scales it to the existing height and appends its width`() {
        val layout = computeComposeLayout(
            existingWidth = 200,
            existingHeight = 100,
            position = ImagePosition.RIGHT,
        )

        assertEquals(100, layout.canvasHeight)
        assertEquals(100, layout.scaledAddedWidth)
        assertEquals(100, layout.scaledAddedHeight)
        // The canvas is wider than the two panels combined, leaving room for the gap.
        assertTrue(layout.canvasWidth > 200 + layout.scaledAddedWidth)
    }

    @Test
    fun `leaves a gap between the two panels instead of placing them flush`() {
        val layout = computeComposeLayout(
            existingWidth = 200,
            existingHeight = 100,
            position = ImagePosition.RIGHT,
        )

        val gap = layout.addedLeft - (layout.existingLeft + 200)
        assertTrue("expected a positive gap between panels, was $gap", gap > 0)
    }

    @Test
    fun `draws a border around each panel`() {
        val layout = computeComposeLayout(
            existingWidth = 200,
            existingHeight = 100,
            position = ImagePosition.RIGHT,
        )

        assertTrue(layout.existingBorderPx > 0)
        assertTrue(layout.addedBorderPx > 0)
    }

    @Test
    fun `border thickness has a floor for tiny images`() {
        assertEquals(3, borderThicknessPx(width = 10, height = 10))
    }

    @Test
    fun `existing image lands at the start of the canvas when the new image is placed to its right`() {
        val layout = computeComposeLayout(
            existingWidth = 200,
            existingHeight = 100,
            position = ImagePosition.RIGHT,
        )

        assertEquals(0, layout.existingLeft)
        assertEquals(0f, layout.existingRect.left, 0.0001f)
        assertEquals(200f / layout.canvasWidth, layout.existingRect.width, 0.0001f)
        assertEquals(1f, layout.existingRect.height, 0.0001f)
    }

    @Test
    fun `existing image is pushed past the new image's width and the gap when placed to its left`() {
        val layout = computeComposeLayout(
            existingWidth = 200,
            existingHeight = 100,
            position = ImagePosition.LEFT,
        )

        assertTrue(layout.existingLeft > layout.scaledAddedWidth)
        assertEquals(layout.existingLeft.toFloat() / layout.canvasWidth, layout.existingRect.left, 0.0001f)
    }

    @Test
    fun `placing an image below scales it to the existing width and appends its height`() {
        val layout = computeComposeLayout(
            existingWidth = 200,
            existingHeight = 100,
            position = ImagePosition.BOTTOM,
        )

        assertEquals(200, layout.canvasWidth)
        assertEquals(200, layout.scaledAddedWidth)
        assertEquals(200, layout.scaledAddedHeight)
        assertEquals(0, layout.existingTop)
        assertEquals(0f, layout.existingRect.top, 0.0001f)
        assertEquals(100f / layout.canvasHeight, layout.existingRect.height, 0.0001f)
    }

    @Test
    fun `existing image is pushed past the new image's height and the gap when placed above it`() {
        val layout = computeComposeLayout(
            existingWidth = 200,
            existingHeight = 100,
            position = ImagePosition.TOP,
        )

        assertTrue(layout.existingTop > layout.scaledAddedHeight)
        assertEquals(layout.existingTop.toFloat() / layout.canvasHeight, layout.existingRect.top, 0.0001f)
    }

    @Test
    fun `added rect covers where the added image was actually drawn`() {
        val layout = computeComposeLayout(
            existingWidth = 200,
            existingHeight = 100,
            position = ImagePosition.RIGHT,
        )

        assertEquals(layout.addedLeft.toFloat() / layout.canvasWidth, layout.addedRect.left, 0.0001f)
        assertEquals(layout.scaledAddedWidth.toFloat() / layout.canvasWidth, layout.addedRect.width, 0.0001f)
    }

    @Test
    fun `a width span of 2 doubles the added panel's width and letterboxes the existing content if it grows the canvas`() {
        val layout = computeComposeLayout(
            existingWidth = 200,
            existingHeight = 100,
            position = ImagePosition.RIGHT,
            widthSpan = 2,
        )

        assertEquals(100, layout.canvasHeight)
        assertEquals(200, layout.scaledAddedWidth)
        assertEquals(100, layout.scaledAddedHeight)
        assertEquals(0, layout.existingTop)
    }

    @Test
    fun `a height span of 2 doubles the added panel's height and letterboxes the existing content`() {
        val layout = computeComposeLayout(
            existingWidth = 200,
            existingHeight = 100,
            position = ImagePosition.RIGHT,
            heightSpan = 2,
        )

        assertEquals(200, layout.canvasHeight)
        assertEquals(100, layout.scaledAddedWidth)
        assertEquals(200, layout.scaledAddedHeight)
        // The existing panel no longer fills the canvas height, so it's centered within it.
        assertEquals(50, layout.existingTop)
    }

    @Test
    fun `width and height spans of 1 keep the existing behavior with no letterboxing`() {
        val layout = computeComposeLayout(
            existingWidth = 200,
            existingHeight = 100,
            position = ImagePosition.RIGHT,
            widthSpan = 1,
            heightSpan = 1,
        )

        assertEquals(0, layout.existingTop)
        assertEquals(1f, layout.existingRect.height, 0.0001f)
    }
}

