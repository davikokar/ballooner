package com.ballooner.data.image

import com.ballooner.domain.model.ImagePosition
import org.junit.Assert.assertEquals
import org.junit.Test

class ComposeLayoutTest {

    @Test
    fun `placing an image to the right scales it to the existing height and appends its width`() {
        val layout = computeComposeLayout(
            existingWidth = 200,
            existingHeight = 100,
            addedWidth = 100,
            addedHeight = 50,
            position = ImagePosition.RIGHT,
        )

        assertEquals(100, layout.canvasHeight)
        assertEquals(200, layout.scaledAddedWidth)
        assertEquals(100, layout.scaledAddedHeight)
        assertEquals(400, layout.canvasWidth)
    }

    @Test
    fun `existing image lands at the start of the canvas when the new image is placed to its right`() {
        val layout = computeComposeLayout(
            existingWidth = 200,
            existingHeight = 100,
            addedWidth = 100,
            addedHeight = 50,
            position = ImagePosition.RIGHT,
        )

        assertEquals(0f, layout.existingRect.left, 0.0001f)
        assertEquals(200f / 400f, layout.existingRect.width, 0.0001f)
        assertEquals(1f, layout.existingRect.height, 0.0001f)
    }

    @Test
    fun `existing image is pushed past the new image's width when placed to its left`() {
        val layout = computeComposeLayout(
            existingWidth = 200,
            existingHeight = 100,
            addedWidth = 100,
            addedHeight = 50,
            position = ImagePosition.LEFT,
        )

        assertEquals(layout.scaledAddedWidth.toFloat() / layout.canvasWidth, layout.existingRect.left, 0.0001f)
    }

    @Test
    fun `placing an image below scales it to the existing width and appends its height`() {
        val layout = computeComposeLayout(
            existingWidth = 200,
            existingHeight = 100,
            addedWidth = 100,
            addedHeight = 50,
            position = ImagePosition.BOTTOM,
        )

        assertEquals(200, layout.canvasWidth)
        assertEquals(200, layout.scaledAddedWidth)
        assertEquals(100, layout.scaledAddedHeight)
        assertEquals(200, layout.canvasHeight)
        assertEquals(0f, layout.existingRect.top, 0.0001f)
        assertEquals(100f / 200f, layout.existingRect.height, 0.0001f)
    }

    @Test
    fun `existing image is pushed past the new image's height when placed above it`() {
        val layout = computeComposeLayout(
            existingWidth = 200,
            existingHeight = 100,
            addedWidth = 100,
            addedHeight = 50,
            position = ImagePosition.TOP,
        )

        assertEquals(layout.scaledAddedHeight.toFloat() / layout.canvasHeight, layout.existingRect.top, 0.0001f)
    }
}
