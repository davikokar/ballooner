package com.ballooner.domain.comic

import com.ballooner.domain.model.BalloonType
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Where a balloon's centre falls on the page, in page units.
 *
 * A panel balloon is placed relative to its panel, so it follows the panel when the layout
 * changes; a comic balloon is placed on the page itself and ignores panels entirely.
 */
fun Balloon.centreOnPage(panel: PageRect?, pageHeight: Float): PagePoint = when (scope) {
    is BalloonScope.Panel -> if (panel == null) {
        PagePoint(centre.u, centre.v * pageHeight)
    } else {
        PagePoint(panel.left + centre.u * panel.width, panel.top + centre.v * panel.height)
    }
    BalloonScope.Comic -> PagePoint(centre.u, centre.v * pageHeight)
}

/** Moves the balloon so its centre lands on [point], expressed in page units. */
fun Balloon.withCentreOnPage(point: PagePoint, panel: PageRect?, pageHeight: Float): Balloon {
    val centre = when (scope) {
        is BalloonScope.Panel -> if (panel == null || panel.width <= 0f || panel.height <= 0f) {
            NormalizedPoint(point.x, if (pageHeight > 0f) point.y / pageHeight else 0f)
        } else {
            NormalizedPoint((point.x - panel.left) / panel.width, (point.y - panel.top) / panel.height)
        }
        BalloonScope.Comic -> NormalizedPoint(point.x, if (pageHeight > 0f) point.y / pageHeight else 0f)
    }
    return copy(centre = centre)
}

/**
 * Re-expresses the balloon in [newScope] without moving it on the page.
 *
 * Panel and comic balloons measure their position in different spaces, so switching between them
 * has to convert, or the balloon jumps the moment its scope changes.
 */
fun Balloon.withScope(
    newScope: BalloonScope,
    currentPanel: PageRect?,
    newPanel: PageRect?,
    pageHeight: Float,
): Balloon {
    if (newScope == scope) return this
    val onPage = centreOnPage(currentPanel, pageHeight)
    return copy(scope = newScope).withCentreOnPage(onPage, newPanel, pageHeight)
}

/** The index of the panel this balloon belongs to, or null when it belongs to the whole comic. */
val Balloon.panelIndex: Int?
    get() = (scope as? BalloonScope.Panel)?.panelIndex

/** Balloons in the order they are drawn: panel balloons first, comic balloons over them. */
fun List<Balloon>.inDrawingOrder(): List<Balloon> = sortedBy { it.scope is BalloonScope.Comic }

/**
 * Points the tail at [target] on the page.
 *
 * The tip is measured from the body edge outwards, so a handle placed at the tip follows the
 * finger exactly rather than lagging behind by the width of the balloon.
 */
fun Balloon.withTailAt(target: PagePoint, panel: PageRect?, pageHeight: Float): Balloon {
    val centre = centreOnPage(panel, pageHeight)
    val dx = target.x - centre.x
    val dy = target.y - centre.y
    val distance = hypot(dx, dy)
    if (distance <= 0f) return this
    val angle = atan2(dy, dx)
    val edge = ellipseEdgeRadius(width / 2f, height / 2f, angle)
    return copy(
        tailAngleDegrees = Math.toDegrees(angle.toDouble()).toFloat().mod(360f),
        tailLength = (distance - edge).coerceAtLeast(0f),
    )
}

/** Resizes the balloon so its corner sits under [target], keeping its centre where it is. */
fun Balloon.resizedTo(target: PagePoint, panel: PageRect?, pageHeight: Float): Balloon {
    val centre = centreOnPage(panel, pageHeight)
    return copy(
        width = (2f * abs(target.x - centre.x)).coerceIn(MIN_BALLOON_SIZE, MAX_BALLOON_SIZE),
        height = (2f * abs(target.y - centre.y)).coerceIn(MIN_BALLOON_SIZE, MAX_BALLOON_SIZE),
    )
}

/** The corner the resize handle sits on, in page units. */
fun Balloon.resizeHandle(panel: PageRect?, pageHeight: Float): PagePoint {
    val centre = centreOnPage(panel, pageHeight)
    return PagePoint(centre.x + width / 2f, centre.y + height / 2f)
}

/** The tip of the tail, in page units, which is where its handle sits. */
fun Balloon.tailTip(panel: PageRect?, pageHeight: Float): PagePoint {
    val centre = centreOnPage(panel, pageHeight)
    val angle = Math.toRadians(tailAngleDegrees.toDouble()).toFloat()
    val reach = ellipseEdgeRadius(width / 2f, height / 2f, angle) + tailLength
    return PagePoint(centre.x + cos(angle) * reach, centre.y + sin(angle) * reach)
}

/** One side of where the tail meets the body, in page units, which is where its handle sits. */
fun Balloon.tailWidthHandle(panel: PageRect?, pageHeight: Float): PagePoint {
    val base = tailBaseCentre(panel, pageHeight)
    val angle = Math.toRadians(tailAngleDegrees.toDouble()).toFloat()
    val halfBase = tailWidth * min(width / 2f, height / 2f)
    // Perpendicular to the tail, so the handle sits beside the base rather than along it.
    return PagePoint(base.x - sin(angle) * halfBase, base.y + cos(angle) * halfBase)
}

/** Widens or narrows the tail base so it reaches [target]. */
fun Balloon.withTailWidthAt(target: PagePoint, panel: PageRect?, pageHeight: Float): Balloon {
    val base = tailBaseCentre(panel, pageHeight)
    val angle = Math.toRadians(tailAngleDegrees.toDouble()).toFloat()
    val alongPerpendicular = (target.x - base.x) * -sin(angle) + (target.y - base.y) * cos(angle)
    val shortestRadius = min(width / 2f, height / 2f)
    if (shortestRadius <= 0f) return this
    return copy(tailWidth = (abs(alongPerpendicular) / shortestRadius).coerceIn(MIN_TAIL_WIDTH, MAX_TAIL_WIDTH))
}

private fun Balloon.tailBaseCentre(panel: PageRect?, pageHeight: Float): PagePoint {
    val centre = centreOnPage(panel, pageHeight)
    val angle = Math.toRadians(tailAngleDegrees.toDouble()).toFloat()
    val reach = ellipseEdgeRadius(width / 2f, height / 2f, angle) * TAIL_BASE_INSET
    return PagePoint(centre.x + cos(angle) * reach, centre.y + sin(angle) * reach)
}

/** Whether [point], in page units, falls inside the balloon body. */
fun Balloon.contains(point: PagePoint, panel: PageRect?, pageHeight: Float): Boolean {
    if (width <= 0f || height <= 0f) return false
    val centre = centreOnPage(panel, pageHeight)
    val dx = (point.x - centre.x) / (width / 2f)
    val dy = (point.y - centre.y) / (height / 2f)
    return if (type == BalloonType.CAPTION) {
        abs(dx) <= 1f && abs(dy) <= 1f
    } else {
        dx * dx + dy * dy <= 1f
    }
}

/** Distance from the centre to the body edge along [angleRad]. */
private fun ellipseEdgeRadius(radiusX: Float, radiusY: Float, angleRad: Float): Float {
    val x = radiusY * cos(angleRad)
    val y = radiusX * sin(angleRad)
    val denominator = sqrt(x * x + y * y)
    return if (denominator > 0f) radiusX * radiusY / denominator else max(radiusX, radiusY)
}

const val MIN_BALLOON_SIZE = 0.05f
const val MAX_BALLOON_SIZE = 1.5f
const val MIN_TAIL_WIDTH = 0.1f
const val MAX_TAIL_WIDTH = 1.2f
const val MIN_BALLOON_TEXT_SIZE = 0.015f
const val MAX_BALLOON_TEXT_SIZE = 0.12f

/** How far inside the body edge the tail base sits, so the two overlap cleanly. */
const val TAIL_BASE_INSET = 0.7f
