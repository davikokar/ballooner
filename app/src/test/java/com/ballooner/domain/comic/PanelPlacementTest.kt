package com.ballooner.domain.comic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PanelPlacementTest {

    private val square = PageRect(0f, 0f, 1f, 1f)
    private val wide = PageRect(0f, 0f, 1f, 0.5f)
    private val image = PanelImage("uri")

    @Test
    fun `a turn close to straight snaps to straight`() {
        assertEquals(0f, snapToQuarterTurn(3f), TOLERANCE)
        assertEquals(0f, snapToQuarterTurn(-4f), TOLERANCE)
        assertEquals(90f, snapToQuarterTurn(87f), TOLERANCE)
        assertEquals(270f, snapToQuarterTurn(273f), TOLERANCE)
    }

    @Test
    fun `a turn away from a quarter is left alone`() {
        assertEquals(45f, snapToQuarterTurn(45f), TOLERANCE)
        assertEquals(30f, snapToQuarterTurn(30f), TOLERANCE)
    }

    @Test
    fun `a full turn comes back to straight`() {
        assertEquals(0f, snapToQuarterTurn(360f), TOLERANCE)
        assertEquals(0f, snapToQuarterTurn(359f), TOLERANCE)
    }

    @Test
    fun `an image that exactly covers its panel cannot be panned`() {
        val panned = image.transformed(square, imageAspect = 1f, panX = 0.3f, panY = 0.3f)

        assertEquals(0.5f, panned.centre.u, TOLERANCE)
        assertEquals(0.5f, panned.centre.v, TOLERANCE)
    }

    @Test
    fun `a zoomed image can be panned, but only so far`() {
        val zoomed = image.copy(zoom = 2f)

        val nudged = zoomed.transformed(square, imageAspect = 1f, panX = 0.1f)
        val shoved = zoomed.transformed(square, imageAspect = 1f, panX = 10f)

        assertEquals(0.45f, nudged.centre.u, TOLERANCE)
        // At twice the covering scale a quarter of the image hangs off each side.
        assertEquals(0.25f, shoved.centre.u, TOLERANCE)
    }

    @Test
    fun `dragging right moves the looked-at point left`() {
        val zoomed = image.copy(zoom = 2f)

        val dragged = zoomed.transformed(square, imageAspect = 1f, panX = 0.1f, panY = 0.1f)

        assertTrue(dragged.centre.u < 0.5f)
        assertTrue(dragged.centre.v < 0.5f)
    }

    @Test
    fun `a wide image in a square panel can be panned sideways but not up`() {
        val panned = image.transformed(square, imageAspect = 2f, panX = 1f, panY = 1f)

        assertEquals(0.25f, panned.centre.u, TOLERANCE)
        assertEquals(0.5f, panned.centre.v, TOLERANCE)
    }

    @Test
    fun `zoom never drops below covering the panel`() {
        val shrunk = image.transformed(square, imageAspect = 1f, zoomBy = 0.1f)

        assertEquals(MIN_PANEL_ZOOM, shrunk.zoom, TOLERANCE)
    }

    @Test
    fun `zoom stops at the maximum`() {
        val huge = image.transformed(square, imageAspect = 1f, zoomBy = 1000f)

        assertEquals(MAX_PANEL_ZOOM, huge.zoom, TOLERANCE)
    }

    @Test
    fun `turning a square image in a square panel leaves its pan alone`() {
        val panned = image.copy(zoom = 1.2f).transformed(square, imageAspect = 1f, panX = 10f)

        val turned = panned.transformed(square, imageAspect = 1f, rotateBy = 45f)

        // Zoom is a multiple of the covering scale, so the room to pan does not depend on the
        // angle unless the turn changes which side of the image is the binding one.
        assertEquals(panned.centre.u, turned.centre.u, TOLERANCE)
    }

    @Test
    fun `turning the image changes which way it is free to move`() {
        val panned = image.copy(zoom = 1.2f).transformed(wide, imageAspect = 1f, panY = 10f)
        // A square image over a wide panel overhangs top and bottom, so it moves that way freely.
        assertEquals(0.2083f, panned.centre.v, 0.01f)

        val turned = panned.transformed(wide, imageAspect = 1f, rotateBy = 90f)

        // The overhang swaps axes with the turn, so most of the old pan is pulled back...
        assertEquals(0.4167f, turned.centre.v, 0.01f)

        // ...and dragging up the screen now walks along the image's other axis, which is free.
        val afterwards = turned.transformed(wide, imageAspect = 1f, panY = 10f)
        assertEquals(0.2083f, afterwards.centre.u, 0.01f)
    }

    @Test
    fun `panning a turned image follows the image, not the screen`() {
        val turned = image.copy(zoom = 2f, angleDegrees = 90f)

        val dragged = turned.transformed(square, imageAspect = 1f, panX = 0.1f, panY = 0f)

        // At a quarter turn a sideways drag walks along the image's vertical axis.
        assertEquals(0.5f, dragged.centre.u, TOLERANCE)
        assertTrue(dragged.centre.v != 0.5f)
    }

    @Test
    fun `a turn is applied with its magnet`() {
        val turned = image.straightened(square, imageAspect = 1f, rotateBy = 88f)

        assertEquals(90f, turned.angleDegrees, TOLERANCE)
    }

    @Test
    fun `a twist delivered one frame at a time still turns the image`() {
        // A real gesture reports a fraction of a degree per frame, never a whole turn at once.
        var twisting = image
        repeat(90) { twisting = twisting.transformed(square, imageAspect = 1f, rotateBy = 1f) }

        assertEquals(90f, twisting.angleDegrees, TOLERANCE)
    }

    @Test
    fun `the magnet only bites when the twist is let go of`() {
        var twisting = image
        repeat(88) { twisting = twisting.transformed(square, imageAspect = 1f, rotateBy = 1f) }
        assertEquals(88f, twisting.angleDegrees, TOLERANCE)

        assertEquals(90f, twisting.straightened(square, imageAspect = 1f).angleDegrees, TOLERANCE)
    }

    @Test
    fun `an image already outside its limits is pulled back`() {
        val strayed = image.copy(centre = NormalizedPoint(0.9f, 0.1f))

        val clamped = strayed.clampedTo(square, imageAspect = 1f)

        assertEquals(0.5f, clamped.centre.u, TOLERANCE)
        assertEquals(0.5f, clamped.centre.v, TOLERANCE)
    }

    @Test
    fun `reshaping the panel pulls a pan back within the new limits`() {
        val panned = image.copy(zoom = 2f).transformed(square, imageAspect = 1f, panX = 10f)

        val reclamped = panned.clampedTo(wide, imageAspect = 1f)

        assertTrue("still covers the new panel", reclamped.centre.u >= 0.5f - 0.5f)
        assertEquals(panned.zoom, reclamped.zoom, TOLERANCE)
    }

    @Test
    fun `a nonsense gesture changes nothing`() {
        assertEquals(image, image.transformed(square, imageAspect = 1f, panX = Float.NaN))
        assertEquals(image, image.transformed(square, imageAspect = 1f, zoomBy = Float.NaN))
    }

    @Test
    fun `a panel with no area changes nothing`() {
        val empty = PageRect(0f, 0f, 0f, 0f)

        assertEquals(image, image.transformed(empty, imageAspect = 1f, panX = 0.5f))
    }

    @Test
    fun `the display size covers the panel at every angle`() {
        listOf(0f, 15f, 45f, 90f, 137f).forEach { angle ->
            val turned = image.copy(angleDegrees = angle)
            val (width, height) = turned.displaySize(square, imageAspect = 1f)
            val (alongWidth, alongHeight) = panelHalfExtents(square, angle)

            assertTrue("width covers at $angle", width / 2f >= alongWidth - TOLERANCE)
            assertTrue("height covers at $angle", height / 2f >= alongHeight - TOLERANCE)
        }
    }
}
