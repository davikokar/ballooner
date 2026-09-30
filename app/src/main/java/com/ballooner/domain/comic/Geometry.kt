package com.ballooner.domain.comic

import kotlin.math.abs
import kotlin.math.hypot

/**
 * A point on the page in page units: the page is always 1 wide and [Comic.pageHeight] tall, so the
 * same number means the same distance on both axes.
 */
data class PagePoint(val x: Float, val y: Float)

/** An axis-aligned rectangle in page units. */
data class PageRect(val left: Float, val top: Float, val width: Float, val height: Float) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
    val centre: PagePoint get() = PagePoint(left + width / 2f, top + height / 2f)
}

/** A convex polygon in page units, with vertices running clockwise in the y-down page space. */
data class Polygon(val vertices: List<PagePoint>) {

    val bounds: PageRect
        get() {
            val left = vertices.minOf { it.x }
            val top = vertices.minOf { it.y }
            return PageRect(left, top, vertices.maxOf { it.x } - left, vertices.maxOf { it.y } - top)
        }

    /** Twice the signed area; positive or negative depending on winding. */
    private val doubleSignedArea: Float
        get() = vertices.indices.sumOf { i ->
            val current = vertices[i]
            val next = vertices[(i + 1) % vertices.size]
            (current.x * next.y - next.x * current.y).toDouble()
        }.toFloat()

    val area: Float get() = abs(doubleSignedArea) / 2f

    /** The area centroid, which is what panel reading order is sorted by. */
    val centroid: PagePoint
        get() {
            val doubleArea = doubleSignedArea
            if (abs(doubleArea) < MIN_POLYGON_AREA) {
                return PagePoint(vertices.map { it.x }.average().toFloat(), vertices.map { it.y }.average().toFloat())
            }
            var x = 0f
            var y = 0f
            for (i in vertices.indices) {
                val current = vertices[i]
                val next = vertices[(i + 1) % vertices.size]
                val cross = current.x * next.y - next.x * current.y
                x += (current.x + next.x) * cross
                y += (current.y + next.y) * cross
            }
            return PagePoint(x / (3f * doubleArea), y / (3f * doubleArea))
        }

    /** Whether [point] falls inside this polygon. Only valid because panels are always convex. */
    fun contains(point: PagePoint): Boolean {
        var positive = false
        var negative = false
        for (i in vertices.indices) {
            val current = vertices[i]
            val next = vertices[(i + 1) % vertices.size]
            val cross = (next.x - current.x) * (point.y - current.y) -
                (next.y - current.y) * (point.x - current.x)
            if (cross > EDGE_TOLERANCE) positive = true
            if (cross < -EDGE_TOLERANCE) negative = true
            if (positive && negative) return false
        }
        return true
    }

    companion object {
        fun of(rect: PageRect): Polygon = Polygon(
            listOf(
                PagePoint(rect.left, rect.top),
                PagePoint(rect.right, rect.top),
                PagePoint(rect.right, rect.bottom),
                PagePoint(rect.left, rect.bottom),
            ),
        )
    }
}

/** The infinite straight line through [a] and [b]. */
data class Line(val a: PagePoint, val b: PagePoint) {

    /** Perpendicular distance from [point], signed by which side of the line it falls on. */
    fun signedDistanceTo(point: PagePoint): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val length = hypot(dx, dy)
        if (length < MIN_LINE_LENGTH) return 0f
        return (dx * (point.y - a.y) - dy * (point.x - a.x)) / length
    }

    fun reversed(): Line = Line(b, a)
}

/**
 * Splits [polygon] along [line] into the pieces on either side, each pushed back from the line by
 * [inset] so that the gap between them is the gutter.
 *
 * Returns null when the line does not cross the polygon, or when the inset would swallow a piece.
 * Clipping a convex polygon this way always yields convex pieces, at any angle, which is what
 * lets diagonal cuts cost no more than horizontal ones.
 */
fun splitConvex(polygon: Polygon, line: Line, inset: Float): Pair<Polygon, Polygon>? {
    val distances = polygon.vertices.map(line::signedDistanceTo)
    if (distances.none { it > EDGE_TOLERANCE } || distances.none { it < -EDGE_TOLERANCE }) return null
    val near = clipToHalfPlane(polygon, line, inset) ?: return null
    val far = clipToHalfPlane(polygon, line.reversed(), inset) ?: return null
    return near to far
}

/**
 * Where a line traced from [from] to [to] first passes inside one of [panels], or null when it
 * passes inside none of them.
 *
 * A trace can begin beside the page rather than on it, so the panel a cut belongs to is the
 * first one its line enters. The point returned lies inside that panel, which is what a cut's
 * anchor has to be.
 */
fun firstPanelPoint(panels: List<Polygon>, from: PagePoint, to: PagePoint): PagePoint? {
    if (panels.any { it.contains(from) }) return from
    val entered = panels.mapNotNull { it.insideStretch(from, to) }.minByOrNull { it.first }
        ?: return null
    val middle = (entered.first + entered.second) / 2f
    return PagePoint(from.x + (to.x - from.x) * middle, from.y + (to.y - from.y) * middle)
}

/**
 * Where a pair of handles [size] across can sit inside [panel]: the leftmost and rightmost
 * centres they can take on the highest run across the panel that has room for them.
 *
 * A cut can leave a panel any convex shape at all, so the corners of its bounding box need not be
 * inside it. Walking down the panel until the whole band a handle covers is inside keeps the
 * handles on the panel they belong to, whatever the cuts have made of it.
 */
fun panelHandleAnchors(panel: Polygon, size: Float): Pair<PagePoint, PagePoint>? {
    val bounds = panel.bounds
    if (bounds.width <= 0f || bounds.height <= 0f || size <= 0f) return null
    // A panel shallower than the handle still gets one: it is centred in what height there is.
    val reach = minOf(size, bounds.height) / 2f
    var best: Triple<Float, Float, Float>? = null
    for (step in 0..HANDLE_SEARCH_STEPS) {
        val y = bounds.top + reach + (bounds.height - reach * 2f) * step / HANDLE_SEARCH_STEPS
        val (from, to) = panel.runAcross(y, reach) ?: continue
        if (to - from >= size * 2f) return anchorsAcross(from, to, y, size)
        if (best == null || to - from > best.second - best.first) best = Triple(from, to, y)
    }
    return best?.let { anchorsAcross(it.first, it.second, it.third, size) }
}

private fun anchorsAcross(from: Float, to: Float, y: Float, size: Float): Pair<PagePoint, PagePoint> {
    val half = size / 2f
    // Too narrow for two side by side: they share the middle rather than hang over the edges.
    if (to - from < size) {
        val middle = PagePoint((from + to) / 2f, y)
        return middle to middle
    }
    return PagePoint(from + half, y) to PagePoint(to - half, y)
}

/** The run across the panel that is inside it at [y] and [reach] either side of it. */
private fun Polygon.runAcross(y: Float, reach: Float): Pair<Float, Float>? {
    var from = Float.NEGATIVE_INFINITY
    var to = Float.POSITIVE_INFINITY
    for (offset in listOf(-reach, 0f, reach)) {
        val (left, right) = runAt(y + offset) ?: return null
        from = maxOf(from, left)
        to = minOf(to, right)
    }
    return (from to to).takeIf { to > from }
}

/** Where a level line at [y] enters and leaves the panel. */
private fun Polygon.runAt(y: Float): Pair<Float, Float>? {
    val bounds = bounds
    // Reaching in from outside the panel: a segment that starts on its own edge is the one case
    // the clipping cannot call either way.
    val left = bounds.left - bounds.width
    val right = bounds.right + bounds.width
    val stretch = insideStretch(PagePoint(left, y), PagePoint(right, y)) ?: return null
    val span = right - left
    return (left + span * stretch.first) to (left + span * stretch.second)
}

/**
 * How far along the segment [from]..[to] this polygon is entered and left again, as fractions of
 * the segment, or null when the segment misses it.
 */
private fun Polygon.insideStretch(from: PagePoint, to: PagePoint): Pair<Float, Float>? {
    var enter = 0f
    var leave = 1f
    val interior = centroid
    for (i in vertices.indices) {
        val edge = Line(vertices[i], vertices[(i + 1) % vertices.size])
        val inward = if (edge.signedDistanceTo(interior) >= 0f) edge else edge.reversed()
        val atFrom = inward.signedDistanceTo(from)
        val change = inward.signedDistanceTo(to) - atFrom
        if (abs(change) < MIN_LINE_LENGTH) {
            // Parallel to this edge: either wholly on the inside of it, or missing it entirely.
            if (atFrom < 0f) return null
            continue
        }
        val crossing = -atFrom / change
        if (change > 0f) enter = maxOf(enter, crossing) else leave = minOf(leave, crossing)
        if (enter > leave) return null
    }
    return enter to leave
}

/**
 * The area two convex polygons have in common, used to work out which old panel each new panel
 * came from after a layout edit.
 */
fun intersectionArea(a: Polygon, b: Polygon): Float {
    var clipped: Polygon = a
    val interior = b.centroid
    for (i in b.vertices.indices) {
        val edge = Line(b.vertices[i], b.vertices[(i + 1) % b.vertices.size])
        val inward = if (edge.signedDistanceTo(interior) >= 0f) edge else edge.reversed()
        clipped = clipToHalfPlane(clipped, inward, 0f) ?: return 0f
    }
    return clipped.area
}

/** Keeps the part of [polygon] at least [minDistance] to the positive side of [line]. */
private fun clipToHalfPlane(polygon: Polygon, line: Line, minDistance: Float): Polygon? {
    val kept = mutableListOf<PagePoint>()
    val vertices = polygon.vertices
    for (i in vertices.indices) {
        val current = vertices[i]
        val next = vertices[(i + 1) % vertices.size]
        val currentDistance = line.signedDistanceTo(current) - minDistance
        val nextDistance = line.signedDistanceTo(next) - minDistance
        if (currentDistance >= 0f) kept += current
        // Strict signs only: an edge ending exactly on the line is kept as a vertex, not doubled.
        val crosses = (currentDistance > 0f && nextDistance < 0f) ||
            (currentDistance < 0f && nextDistance > 0f)
        if (crosses) {
            val denominator = currentDistance - nextDistance
            if (abs(denominator) > MIN_LINE_LENGTH) {
                val t = currentDistance / denominator
                kept += PagePoint(
                    current.x + (next.x - current.x) * t,
                    current.y + (next.y - current.y) * t,
                )
            }
        }
    }
    if (kept.size < 3) return null
    return Polygon(kept).takeIf { it.area > MIN_POLYGON_AREA }
}

private const val EDGE_TOLERANCE = 1e-6f
private const val MIN_LINE_LENGTH = 1e-6f
private const val MIN_POLYGON_AREA = 1e-8f

/** How many levels down a panel are tried before settling for its widest one. */
private const val HANDLE_SEARCH_STEPS = 24
