package com.ballooner.domain.comic

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Snaps [angleDegrees] onto a quarter turn when it is already close to one, so a picture that is
 * meant to be straight is easy to get back to by hand.
 */
fun snapToQuarterTurn(angleDegrees: Float, thresholdDegrees: Float = QUARTER_TURN_SNAP): Float {
    if (!angleDegrees.isFinite()) return 0f
    val normalized = angleDegrees.mod(360f)
    val nearest = (normalized / 90f).roundToInt() * 90f
    return if (abs(normalized - nearest) <= thresholdDegrees) nearest.mod(360f) else normalized
}

/**
 * Applies one step of a placement gesture to a panel image.
 *
 * [panX] and [panY] are the distance the finger moved across the page, in page units. [zoomBy]
 * and [rotateBy] are the pinch and twist since the last step. The result always still covers the
 * panel: zoom is a multiple of the covering scale so it cannot go below it, and the centre is
 * clamped so no corner of the panel can be left empty.
 */
fun PanelImage.transformed(
    panel: PageRect,
    imageAspect: Float,
    panX: Float = 0f,
    panY: Float = 0f,
    zoomBy: Float = 1f,
    rotateBy: Float = 0f,
): PanelImage {
    if (!panX.isFinite() || !panY.isFinite() || !zoomBy.isFinite() || !rotateBy.isFinite()) return this
    if (panel.width <= 0f || panel.height <= 0f || imageAspect <= 0f) return this

    val angle = snapToQuarterTurn(angleDegrees + rotateBy)
    val zoomed = (zoom * zoomBy).coerceIn(MIN_PANEL_ZOOM, MAX_PANEL_ZOOM)
    val turned = copy(angleDegrees = angle, zoom = zoomed)

    val (width, height) = turned.displaySize(panel, imageAspect)
    if (width <= 0f || height <= 0f) return turned.clampedTo(panel, imageAspect)

    // Dragging moves the image, so the point being looked at moves the opposite way, measured
    // along the image's own axes rather than the screen's.
    val radians = Math.toRadians(-angle.toDouble())
    val cosine = cos(radians).toFloat()
    val sine = sin(radians).toFloat()
    val localX = panX * cosine - panY * sine
    val localY = panX * sine + panY * cosine

    return turned
        .copy(centre = NormalizedPoint(centre.u - localX / width, centre.v - localY / height))
        .clampedTo(panel, imageAspect)
}

/** Pulls the looked-at point back inside the range that keeps the panel covered. */
fun PanelImage.clampedTo(panel: PageRect, imageAspect: Float): PanelImage {
    if (panel.width <= 0f || panel.height <= 0f || imageAspect <= 0f) return this
    val (width, height) = displaySize(panel, imageAspect)
    if (width <= 0f || height <= 0f) return this
    val (alongWidth, alongHeight) = panelHalfExtents(panel, angleDegrees)
    val limitU = (0.5f - alongWidth / width).coerceAtLeast(0f)
    val limitV = (0.5f - alongHeight / height).coerceAtLeast(0f)
    return copy(
        centre = NormalizedPoint(
            u = centre.u.coerceIn(0.5f - limitU, 0.5f + limitU),
            v = centre.v.coerceIn(0.5f - limitV, 0.5f + limitV),
        ),
        zoom = zoom.coerceIn(MIN_PANEL_ZOOM, MAX_PANEL_ZOOM),
    )
}

/** The size this image is drawn at, in page units. */
fun PanelImage.displaySize(panel: PageRect, imageAspect: Float): Pair<Float, Float> {
    val width = coverScale(panel, imageAspect, angleDegrees) * zoom.coerceAtLeast(MIN_PANEL_ZOOM)
    return width to (if (imageAspect > 0f) width / imageAspect else 0f)
}

const val MIN_PANEL_ZOOM = 1f
const val MAX_PANEL_ZOOM = 8f
private const val QUARTER_TURN_SNAP = 6f
