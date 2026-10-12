package com.ballooner.ui.comicviewer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/** The smallest the page is ever drawn: the whole of it, fitted to the space it is shown in. */
const val MIN_PAGE_ZOOM = 1f

/** How far in the page may be taken, as a multiple of the whole page fitted to the screen. */
const val MAX_PAGE_ZOOM = 8f

/**
 * How closely the page is being looked at: a scale about the centre of the view, and the
 * translation applied after it.
 *
 * Those are exactly the terms `graphicsLayer` works in, so what this says is what is drawn.
 */
data class PageZoom(val scale: Float = MIN_PAGE_ZOOM, val offset: Offset = Offset.Zero) {

    /**
     * The zoom after a pinch of [zoomBy] about [centroid] that also carried the page by [pan].
     *
     * The point of the page under the fingers stays under them, and the page is never carried
     * past the edges of the view, so there is nowhere to end up looking at nothing.
     */
    fun transformedBy(centroid: Offset, pan: Offset, zoomBy: Float, size: Size): PageZoom {
        if (size.width <= 0f || size.height <= 0f) return this
        val scaled = (scale * zoomBy).coerceIn(MIN_PAGE_ZOOM, MAX_PAGE_ZOOM)
        val centre = Offset(size.width / 2f, size.height / 2f)
        val held = centroid - centre - offset
        return PageZoom(scaled, (centroid + pan - centre - held * (scaled / scale)).withinEdges(scaled, size))
    }
}

/** Holds a translation to what the page can cover: at [MIN_PAGE_ZOOM] there is nowhere to go. */
private fun Offset.withinEdges(scale: Float, size: Size): Offset {
    val limitX = size.width * (scale - MIN_PAGE_ZOOM) / 2f
    val limitY = size.height * (scale - MIN_PAGE_ZOOM) / 2f
    return Offset(x.coerceIn(-limitX, limitX), y.coerceIn(-limitY, limitY))
}
