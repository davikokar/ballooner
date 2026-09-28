package com.ballooner.ui.comic

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import com.ballooner.domain.comic.PageRect
import com.ballooner.domain.comic.PagePoint
import com.ballooner.domain.comic.PanelImage
import com.ballooner.domain.comic.Polygon
import com.ballooner.domain.comic.clampedTo
import com.ballooner.domain.comic.coverScale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Where the page sits inside the space available to it.
 *
 * The page is one page unit wide, so [scale] is also the number of pixels in a page unit, and
 * every distance in the document is multiplied by it to reach the screen.
 */
data class PageViewport(val originX: Float, val originY: Float, val scale: Float) {

    fun toScreen(x: Float, y: Float): Offset = Offset(originX + x * scale, originY + y * scale)

    /** The page point under a screen position, for hit testing taps against panels. */
    fun toPage(position: Offset): PagePoint =
        if (scale <= 0f) {
            PagePoint(0f, 0f)
        } else {
            PagePoint((position.x - originX) / scale, (position.y - originY) / scale)
        }
}

/** Fits a page [pageHeight] tall inside [available], centred, without cropping it. */
fun pageViewport(available: Size, pageHeight: Float): PageViewport {
    if (available.width <= 0f || available.height <= 0f || pageHeight <= 0f) {
        return PageViewport(0f, 0f, 0f)
    }
    val scale = minOf(available.width, available.height / pageHeight)
    return PageViewport(
        originX = (available.width - scale) / 2f,
        originY = (available.height - scale * pageHeight) / 2f,
        scale = scale,
    )
}

/**
 * The viewport the comic is drawn through: the whole page, or one panel filling the space when
 * [focus] is given.
 *
 * Everything drawn over the page works this out for itself from the same inputs, so no part of
 * the editor has to be told where anything ended up.
 */
fun comicViewport(available: Size, pageHeight: Float, focus: PageRect?): PageViewport {
    if (focus == null || focus.width <= 0f || focus.height <= 0f) {
        return pageViewport(available, pageHeight)
    }
    if (available.width <= 0f || available.height <= 0f) return PageViewport(0f, 0f, 0f)
    val scale = minOf(available.width / focus.width, available.height / focus.height)
    return PageViewport(
        originX = (available.width - focus.width * scale) / 2f - focus.left * scale,
        originY = (available.height - focus.height * scale) / 2f - focus.top * scale,
        scale = scale,
    )
}

/** The outline of a panel in screen pixels. */
fun Polygon.toPath(viewport: PageViewport): Path = Path().apply {
    vertices.forEachIndexed { index, vertex ->
        val point = viewport.toScreen(vertex.x, vertex.y)
        if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
    }
    close()
}

/** How a panel image is drawn: its size in pixels, and where its top-left corner goes. */
data class ImageDrawSpec(val size: Size, val topLeft: Offset, val pivot: Offset, val angleDegrees: Float)

/**
 * Works out how to draw [image] so that it fills [panel].
 *
 * The image is clamped here rather than trusted: a stored placement can fall outside what covers
 * the panel whenever the panel is reshaped under it, so covering is enforced on the way out. The
 * image turns about its own centre, which is the assumption `coverScale` is built on; the stored
 * centre point then decides where that turned image is placed.
 */
fun imageDrawSpec(
    panel: PageRect,
    image: PanelImage,
    imageAspect: Float,
    viewport: PageViewport,
): ImageDrawSpec {
    val covering = image.clampedTo(panel, imageAspect)
    val width = coverScale(panel, imageAspect, covering.angleDegrees) * covering.zoom.coerceAtLeast(1f)
    val height = if (imageAspect > 0f) width / imageAspect else 0f
    // Offset from the middle of the image to the point the user chose to look at.
    val offsetX = (covering.centre.u - 0.5f) * width
    val offsetY = (covering.centre.v - 0.5f) * height
    val radians = Math.toRadians(covering.angleDegrees.toDouble())
    val cosine = cos(radians).toFloat()
    val sine = sin(radians).toFloat()
    // That point has to land on the panel's centre once the image has turned, so the image's own
    // centre sits the turned offset away from it.
    val target = panel.centre
    val centreX = target.x - (offsetX * cosine - offsetY * sine)
    val centreY = target.y - (offsetX * sine + offsetY * cosine)
    val pivot = viewport.toScreen(centreX, centreY)
    return ImageDrawSpec(
        size = Size(width * viewport.scale, height * viewport.scale),
        topLeft = Offset(pivot.x - width * viewport.scale / 2f, pivot.y - height * viewport.scale / 2f),
        pivot = pivot,
        angleDegrees = covering.angleDegrees,
    )
}
