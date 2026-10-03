package com.ballooner.ui.comic

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Path
import com.ballooner.domain.comic.MAX_CORNER_RADIUS
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
 * Fills [available] with a page [pageHeight] tall, centred, cropping whichever way it overflows.
 *
 * A list of comics shows them all at one size, so a page shaped unlike the space it is given is
 * cropped rather than letterboxed or squashed.
 */
fun pageCoverViewport(available: Size, pageHeight: Float): PageViewport {
    if (available.width <= 0f || available.height <= 0f || pageHeight <= 0f) {
        return PageViewport(0f, 0f, 0f)
    }
    val scale = maxOf(available.width, available.height / pageHeight)
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

/**
 * The outline of a panel in screen pixels.
 *
 * [cornerRadius] is in page units, like every other distance in the document. A corner is rounded
 * by pulling back along both of its edges and arcing between the two points, so a panel of any
 * shape — including one a cut has left with corners that are not square — rounds the same way.
 * A corner never eats more than half of the shortest edge meeting it, which is what lets the
 * radius run all the way up to [MAX_CORNER_RADIUS] and turn a square panel into a circle.
 */
fun Polygon.toPath(viewport: PageViewport, cornerRadius: Float = 0f): Path = Path().apply {
    val points = vertices.map { viewport.toScreen(it.x, it.y) }
    val radius = cornerRadius * viewport.scale
    if (points.size < 3 || radius <= 0f) {
        points.forEachIndexed { index, point ->
            if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
        }
        close()
        return@apply
    }
    points.forEachIndexed { index, corner ->
        val previous = points[(index + points.size - 1) % points.size]
        val next = points[(index + 1) % points.size]
        val (back, forward) = roundedCorner(previous, corner, next, radius)
        if (index == 0) moveTo(back.x, back.y) else lineTo(back.x, back.y)
        // A cubic pulled this far towards the corner is a circular arc to within a rounding
        // error, so four maxed-out corners of a square really do close into a circle.
        val first = lerp(back, corner, ARC_CONTROL)
        val second = lerp(forward, corner, ARC_CONTROL)
        cubicTo(first.x, first.y, second.x, second.y, forward.x, forward.y)
    }
    close()
}

/** How far towards the corner a control point sits: the usual circle-from-cubics constant. */
private const val ARC_CONTROL = 0.5523f

/**
 * Where a rounded corner leaves the edge running into [corner], and where it rejoins the one
 * leaving it.
 *
 * A corner never takes more than half of either edge, so two corners sharing a short edge cannot
 * both eat it and leave the panel with no straight side at all.
 */
internal fun roundedCorner(
    previous: Offset,
    corner: Offset,
    next: Offset,
    radius: Float,
): Pair<Offset, Offset> = corner.pulledTowards(previous, radius) to corner.pulledTowards(next, radius)

/** A point [by] pixels from this one along the way to [target], at most halfway there. */
private fun Offset.pulledTowards(target: Offset, by: Float): Offset {
    val span = (target - this).getDistance()
    if (span <= 0f) return this
    val fraction = minOf(by / span, 0.5f)
    return this + (target - this) * fraction
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
