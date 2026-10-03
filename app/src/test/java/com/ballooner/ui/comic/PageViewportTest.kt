package com.ballooner.ui.comic

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.ballooner.domain.comic.MAX_CORNER_RADIUS
import com.ballooner.domain.comic.NormalizedPoint
import com.ballooner.domain.comic.PageRect
import com.ballooner.domain.comic.PanelImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class PageViewportTest {

    @Test
    fun `a square page fills a square space`() {
        val viewport = pageViewport(Size(400f, 400f), 1f)

        assertEquals(400f, viewport.scale, TOLERANCE)
        assertEquals(0f, viewport.originX, TOLERANCE)
        assertEquals(0f, viewport.originY, TOLERANCE)
    }

    @Test
    fun `a page taller than the space is limited by the height and centred`() {
        val viewport = pageViewport(Size(400f, 400f), 4f / 3f)

        assertEquals(300f, viewport.scale, TOLERANCE)
        assertEquals(50f, viewport.originX, TOLERANCE)
        assertEquals(0f, viewport.originY, TOLERANCE)
    }

    @Test
    fun `a page wider than the space is limited by the width and centred`() {
        val viewport = pageViewport(Size(400f, 400f), 0.75f)

        assertEquals(400f, viewport.scale, TOLERANCE)
        assertEquals(0f, viewport.originX, TOLERANCE)
        assertEquals(50f, viewport.originY, TOLERANCE)
    }

    @Test
    fun `an empty space has no viewport`() {
        assertEquals(0f, pageViewport(Size(0f, 100f), 1f).scale, TOLERANCE)
    }

    @Test
    fun `an unturned image is centred on its panel and covers it`() {
        val viewport = PageViewport(originX = 0f, originY = 0f, scale = 100f)
        val panel = PageRect(0f, 0f, 1f, 1f)

        val spec = imageDrawSpec(panel, PanelImage("uri"), imageAspect = 1f, viewport = viewport)

        assertEquals(100f, spec.size.width, TOLERANCE)
        assertEquals(0f, spec.topLeft.x, TOLERANCE)
        assertEquals(0f, spec.topLeft.y, TOLERANCE)
        assertEquals(50f, spec.pivot.x, TOLERANCE)
    }

    @Test
    fun `a wide image is drawn wide enough to cover a square panel`() {
        val viewport = PageViewport(originX = 0f, originY = 0f, scale = 100f)
        val panel = PageRect(0f, 0f, 1f, 1f)

        val spec = imageDrawSpec(panel, PanelImage("uri"), imageAspect = 2f, viewport = viewport)

        assertEquals(200f, spec.size.width, TOLERANCE)
        assertEquals(100f, spec.size.height, TOLERANCE)
        assertEquals(-50f, spec.topLeft.x, TOLERANCE)
        assertEquals(0f, spec.topLeft.y, TOLERANCE)
    }

    @Test
    fun `zoom enlarges the image around the panel centre`() {
        val viewport = PageViewport(originX = 0f, originY = 0f, scale = 100f)
        val panel = PageRect(0f, 0f, 1f, 1f)

        val spec = imageDrawSpec(panel, PanelImage("uri", zoom = 2f), imageAspect = 1f, viewport = viewport)

        assertEquals(200f, spec.size.width, TOLERANCE)
        assertEquals(-50f, spec.topLeft.x, TOLERANCE)
        assertEquals(-50f, spec.topLeft.y, TOLERANCE)
    }

    @Test
    fun `zoom below one is treated as exactly covering`() {
        val viewport = PageViewport(originX = 0f, originY = 0f, scale = 100f)
        val panel = PageRect(0f, 0f, 1f, 1f)

        val spec = imageDrawSpec(panel, PanelImage("uri", zoom = 0.2f), imageAspect = 1f, viewport = viewport)

        assertEquals(100f, spec.size.width, TOLERANCE)
    }

    @Test
    fun `looking at a corner of the image moves the image the other way`() {
        val viewport = PageViewport(originX = 0f, originY = 0f, scale = 100f)
        val panel = PageRect(0f, 0f, 1f, 1f)
        val image = PanelImage("uri", centre = NormalizedPoint(0.25f, 0.5f), zoom = 2f)

        val spec = imageDrawSpec(panel, image, imageAspect = 1f, viewport = viewport)

        // The image point at a quarter of its width has to sit at the panel centre.
        assertEquals(50f, spec.topLeft.x + spec.size.width * 0.25f, TOLERANCE)
        assertEquals(50f, spec.topLeft.y + spec.size.height * 0.5f, TOLERANCE)
    }

    @Test
    fun `a turned image grows so it still covers the panel`() {
        val viewport = PageViewport(originX = 0f, originY = 0f, scale = 100f)
        val panel = PageRect(0f, 0f, 1f, 1f)

        val straight = imageDrawSpec(panel, PanelImage("uri"), 1f, viewport).size.width
        val turned = imageDrawSpec(panel, PanelImage("uri", angleDegrees = 45f), 1f, viewport).size.width

        assertTrue("a turned image must be larger", turned > straight)
        assertEquals(141.42f, turned, 0.1f)
    }

    @Test
    fun `the viewport offsets every panel on the page`() {
        val viewport = PageViewport(originX = 10f, originY = 20f, scale = 100f)

        val spec = imageDrawSpec(PageRect(0f, 0f, 1f, 1f), PanelImage("uri"), 1f, viewport)

        assertEquals(10f, spec.topLeft.x, TOLERANCE)
        assertEquals(20f, spec.topLeft.y, TOLERANCE)
    }

    @Test
    fun `focusing a panel fills the space with it`() {
        val panel = PageRect(0.5f, 0f, 0.5f, 0.5f)

        val viewport = comicViewport(Size(400f, 400f), 1f, panel)

        // The panel is a quarter of the page, so it is drawn at four times the scale.
        assertEquals(800f, viewport.scale, TOLERANCE)
        assertEquals(0f, viewport.toScreen(panel.left, panel.top).x, TOLERANCE)
        assertEquals(0f, viewport.toScreen(panel.left, panel.top).y, TOLERANCE)
        assertEquals(400f, viewport.toScreen(panel.right, panel.bottom).x, TOLERANCE)
    }

    @Test
    fun `a focused panel is centred when its shape differs from the space`() {
        val panel = PageRect(0f, 0f, 1f, 0.25f)

        val viewport = comicViewport(Size(400f, 400f), 1f, panel)

        assertEquals(400f, viewport.scale, TOLERANCE)
        assertEquals(0f, viewport.toScreen(0f, 0f).x, TOLERANCE)
        assertEquals(150f, viewport.toScreen(0f, 0f).y, TOLERANCE)
    }

    @Test
    fun `no focus means the whole page`() {
        val focused = comicViewport(Size(400f, 400f), 4f / 3f, null)

        assertEquals(pageViewport(Size(400f, 400f), 4f / 3f), focused)
    }

    @Test
    fun `a rounded corner leaves both of its edges the same distance back`() {
        val corner = Offset(100f, 100f)

        val (back, forward) = roundedCorner(
            previous = Offset(0f, 100f),
            corner = corner,
            next = Offset(100f, 200f),
            radius = 20f,
        )

        assertEquals(80f, back.x, TOLERANCE)
        assertEquals(100f, back.y, TOLERANCE)
        assertEquals(100f, forward.x, TOLERANCE)
        assertEquals(120f, forward.y, TOLERANCE)
    }

    @Test
    fun `a corner never takes more than half of the edge it sits on`() {
        val corner = Offset(10f, 0f)

        val (back, forward) = roundedCorner(
            previous = Offset(0f, 0f),
            corner = corner,
            next = Offset(10f, 4f),
            radius = 1000f,
        )

        assertEquals(5f, back.x, TOLERANCE)
        assertEquals(2f, forward.y, TOLERANCE)
    }

    @Test
    fun `no corner radius leaves the corner where it is`() {
        val corner = Offset(10f, 10f)

        val (back, forward) = roundedCorner(Offset(0f, 10f), corner, Offset(10f, 20f), radius = 0f)

        assertEquals(corner, back)
        assertEquals(corner, forward)
    }

    @Test
    fun `the widest panel there can be is still roundable at the largest radius`() {
        // A panel can be at most the page's full width, drawn here at 400 pixels to the page unit.
        val scale = 400f
        val corner = Offset(400f, 0f)

        val (back, forward) = roundedCorner(
            previous = Offset(0f, 0f),
            corner = corner,
            next = Offset(400f, 400f),
            radius = MAX_CORNER_RADIUS * scale,
        )

        // Both ends land on the edge midpoints, which is the corner being a full quarter circle.
        assertEquals(200f, back.x, TOLERANCE)
        assertEquals(200f, forward.y, TOLERANCE)
    }

    @Test
    fun `focusing a panel with no area falls back to the whole page`() {
        val viewport = comicViewport(Size(400f, 400f), 1f, PageRect(0f, 0f, 0f, 0f))

        assertEquals(400f, viewport.scale, TOLERANCE)
    }

    @Test
    fun `a stored placement that no longer covers is pulled back before drawing`() {
        val viewport = PageViewport(originX = 0f, originY = 0f, scale = 100f)
        val panel = PageRect(0f, 0f, 1f, 1f)
        // A placement like this survives a layout change that reshaped the panel under it.
        val strayed = PanelImage("uri", centre = NormalizedPoint(0.2f, 0.2f), zoom = 2f, angleDegrees = -15f)

        val spec = imageDrawSpec(panel, strayed, imageAspect = 1f, viewport = viewport)

        assertCovers(panel, spec, viewport)
    }

    @Test
    fun `every placement covers its panel however it was stored`() {
        val viewport = PageViewport(originX = 7f, originY = 11f, scale = 100f)
        val panels = listOf(PageRect(0f, 0f, 1f, 1f), PageRect(0f, 0f, 1f, 0.4f), PageRect(0f, 0f, 0.3f, 1f))
        val placements = listOf(
            PanelImage("uri"),
            PanelImage("uri", centre = NormalizedPoint(0f, 0f)),
            PanelImage("uri", centre = NormalizedPoint(1f, 1f), zoom = 1.2f),
            PanelImage("uri", centre = NormalizedPoint(0.1f, 0.9f), zoom = 3f, angleDegrees = 37f),
            PanelImage("uri", centre = NormalizedPoint(0.9f, 0.1f), zoom = 1f, angleDegrees = 90f),
        )

        panels.forEach { panel ->
            placements.forEach { placement ->
                listOf(0.5f, 1f, 2.5f).forEach { aspect ->
                    assertCovers(panel, imageDrawSpec(panel, placement, aspect, viewport), viewport)
                }
            }
        }
    }

    /** Checks every corner of the panel falls inside the turned image that is about to be drawn. */
    private fun assertCovers(panel: PageRect, spec: ImageDrawSpec, viewport: PageViewport) {
        val radians = Math.toRadians(-spec.angleDegrees.toDouble())
        val cosine = cos(radians).toFloat()
        val sine = sin(radians).toFloat()
        listOf(
            viewport.toScreen(panel.left, panel.top),
            viewport.toScreen(panel.right, panel.top),
            viewport.toScreen(panel.right, panel.bottom),
            viewport.toScreen(panel.left, panel.bottom),
        ).forEach { corner ->
            val dx = corner.x - spec.pivot.x
            val dy = corner.y - spec.pivot.y
            val localX = dx * cosine - dy * sine
            val localY = dx * sine + dy * cosine
            assertTrue(
                "a corner of the panel sticks out of the image",
                abs(localX) <= spec.size.width / 2f + 0.01f && abs(localY) <= spec.size.height / 2f + 0.01f,
            )
        }
    }
}

private const val TOLERANCE = 1e-3f
