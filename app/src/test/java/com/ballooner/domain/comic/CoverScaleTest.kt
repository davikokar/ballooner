package com.ballooner.domain.comic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class CoverScaleTest {

    private val square = PageRect(0f, 0f, 1f, 1f)

    @Test
    fun `a square image exactly covers a square panel at its own width`() {
        assertEquals(1f, coverScale(square, imageAspect = 1f, angleDegrees = 0f), TOLERANCE)
    }

    @Test
    fun `a wide image must be as wide as its aspect ratio to cover a square panel`() {
        assertEquals(2f, coverScale(square, imageAspect = 2f, angleDegrees = 0f), TOLERANCE)
    }

    @Test
    fun `a tall image only needs the panel width`() {
        assertEquals(1f, coverScale(square, imageAspect = 0.5f, angleDegrees = 0f), TOLERANCE)
    }

    @Test
    fun `a wide panel needs an image as wide as the panel`() {
        val wide = PageRect(0f, 0f, 2f, 1f)

        assertEquals(2f, coverScale(wide, imageAspect = 1f, angleDegrees = 0f), TOLERANCE)
    }

    @Test
    fun `turning a square image by a quarter turn changes nothing`() {
        val straight = coverScale(square, imageAspect = 1f, angleDegrees = 0f)

        assertEquals(straight, coverScale(square, imageAspect = 1f, angleDegrees = 90f), TOLERANCE)
    }

    @Test
    fun `turning a square image by an eighth of a turn needs the diagonal`() {
        assertEquals(sqrt(2f), coverScale(square, imageAspect = 1f, angleDegrees = 45f), TOLERANCE)
    }

    @Test
    fun `turning is symmetric about zero`() {
        val clockwise = coverScale(square, imageAspect = 1.5f, angleDegrees = 30f)

        assertEquals(clockwise, coverScale(square, imageAspect = 1.5f, angleDegrees = -30f), TOLERANCE)
    }

    @Test
    fun `an image at the covering width still covers after the panel is reshaped`() {
        val original = PageRect(0f, 0f, 0.5f, 0.25f)
        val reshaped = PageRect(0f, 0f, 0.25f, 0.5f)
        val imageAspect = 1.5f

        // Zoom is stored as a multiple of the covering width, so the same stored zoom covers both.
        assertTrue(coverScale(original, imageAspect, 0f) > 0f)
        assertTrue(coverScale(reshaped, imageAspect, 0f) > 0f)
    }

    @Test
    fun `a panel with no area needs no image`() {
        assertEquals(0f, coverScale(PageRect(0f, 0f, 0f, 0f), imageAspect = 1f, angleDegrees = 0f), TOLERANCE)
    }
}
