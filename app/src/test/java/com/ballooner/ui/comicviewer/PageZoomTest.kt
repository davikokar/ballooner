package com.ballooner.ui.comicviewer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class PageZoomTest {

    @Test
    fun `the whole page is shown to begin with`() {
        val zoom = PageZoom()

        assertEquals(MIN_PAGE_ZOOM, zoom.scale, TOLERANCE)
        assertEquals(0f, zoom.offset.x, TOLERANCE)
        assertEquals(0f, zoom.offset.y, TOLERANCE)
    }

    @Test
    fun `the point under the fingers stays under them`() {
        val pagePoint = PageZoom().pageUnder(CORNER, VIEW)

        val zoom = PageZoom().transformedBy(centroid = CORNER, pan = Offset.Zero, zoomBy = 2f, size = VIEW)

        assertEquals(CORNER.x, zoom.pointOf(pagePoint, VIEW).x, TOLERANCE)
        assertEquals(CORNER.y, zoom.pointOf(pagePoint, VIEW).y, TOLERANCE)
    }

    @Test
    fun `the page cannot be carried past the edge of the view`() {
        val zoomedIn = PageZoom().transformedBy(CENTRE, Offset.Zero, 2f, VIEW)

        val carried = zoomedIn.transformedBy(CENTRE, Offset(1000f, 1000f), 1f, VIEW)

        // At twice the size the page overhangs the view by half of it on each side.
        assertEquals(200f, carried.offset.x, TOLERANCE)
        assertEquals(200f, carried.offset.y, TOLERANCE)
    }

    @Test
    fun `there is nowhere to carry the whole page to`() {
        val carried = PageZoom().transformedBy(CENTRE, Offset(120f, -80f), 1f, VIEW)

        assertEquals(0f, carried.offset.x, TOLERANCE)
        assertEquals(0f, carried.offset.y, TOLERANCE)
    }

    @Test
    fun `the page is never taken further in than it can be read at`() {
        val zoom = PageZoom().transformedBy(CENTRE, Offset.Zero, 100f, VIEW)

        assertEquals(MAX_PAGE_ZOOM, zoom.scale, TOLERANCE)
    }

    @Test
    fun `pinching back out stops at the whole page`() {
        val zoomedIn = PageZoom().transformedBy(CENTRE, Offset.Zero, 4f, VIEW)

        val zoomedOut = zoomedIn.transformedBy(CENTRE, Offset.Zero, 0.01f, VIEW)

        assertEquals(MIN_PAGE_ZOOM, zoomedOut.scale, TOLERANCE)
        assertEquals(0f, zoomedOut.offset.x, TOLERANCE)
        assertEquals(0f, zoomedOut.offset.y, TOLERANCE)
    }

    @Test
    fun `a view with no room for the page leaves it as it was`() {
        val zoom = PageZoom().transformedBy(CENTRE, Offset(10f, 10f), 2f, Size.Zero)

        assertEquals(PageZoom(), zoom)
    }
}

/** Where on the page a position in the view falls, as a distance from the page's own centre. */
private fun PageZoom.pageUnder(position: Offset, size: Size): Offset {
    val centre = Offset(size.width / 2f, size.height / 2f)
    return (position - centre - offset) / scale
}

/** The inverse: where a point of the page lands in the view, exactly as `graphicsLayer` puts it. */
private fun PageZoom.pointOf(pagePoint: Offset, size: Size): Offset {
    val centre = Offset(size.width / 2f, size.height / 2f)
    return centre + pagePoint * scale + offset
}

private val VIEW = Size(400f, 400f)
private val CENTRE = Offset(200f, 200f)
private val CORNER = Offset(100f, 100f)
private const val TOLERANCE = 0.001f
