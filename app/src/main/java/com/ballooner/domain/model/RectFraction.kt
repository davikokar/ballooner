package com.ballooner.domain.model

/** A rectangle expressed as fractions (0f..1f) of a larger canvas. */
data class RectFraction(val left: Float, val top: Float, val width: Float, val height: Float) {

    /** Whether the point ([x], [y]), in the same fractional space, falls within this rect. */
    fun contains(x: Float, y: Float): Boolean =
        x >= left && x < left + width && y >= top && y < top + height
}

/** Returns the containing panel whose center is nearest the pressed point. */
fun List<RectFraction>.panelAt(x: Float, y: Float): RectFraction? =
    asSequence()
        .filter { it.contains(x, y) }
        .minByOrNull { panel ->
            val dx = x - (panel.left + panel.width / 2f)
            val dy = y - (panel.top + panel.height / 2f)
            dx * dx + dy * dy
        }

/** The smallest canvas rectangle containing every panel. */
fun retainedCanvasRect(panels: List<RectFraction>): RectFraction {
    val left = panels.minOf { it.left }
    val top = panels.minOf { it.top }
    val right = panels.maxOf { it.left + it.width }
    val bottom = panels.maxOf { it.top + it.height }
    return RectFraction(left, top, right - left, bottom - top)
}

/** Converts this rectangle from the old canvas into coordinates relative to [container]. */
fun RectFraction.remappedFrom(container: RectFraction): RectFraction = RectFraction(
    left = (left - container.left) / container.width,
    top = (top - container.top) / container.height,
    width = width / container.width,
    height = height / container.height,
)
