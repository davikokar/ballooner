package com.ballooner.domain.model

/** A rectangle expressed as fractions (0f..1f) of a larger canvas. */
data class RectFraction(val left: Float, val top: Float, val width: Float, val height: Float) {

    /** Whether the point ([x], [y]), in the same fractional space, falls within this rect. */
    fun contains(x: Float, y: Float): Boolean =
        x in left..(left + width) && y in top..(top + height)
}
