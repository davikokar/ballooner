package com.ballooner.domain.comic

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * The display width, in page units, at which an image of [imageAspect] turned by [angleDegrees]
 * exactly covers [frame]. A panel with angled sides is covered via its bounding box.
 *
 * A panel image stores its zoom as a multiple of this, so `zoom >= 1` *is* the rule that an image
 * always fills its panel: it holds in any frame and at any angle, so reshaping a panel or turning
 * its image never needs a re-fitting pass.
 */
fun coverScale(frame: PageRect, imageAspect: Float, angleDegrees: Float): Float {
    if (frame.width <= 0f || frame.height <= 0f || imageAspect <= 0f) return 0f
    val (alongWidth, alongHeight) = panelHalfExtents(frame, angleDegrees)
    return 2f * max(alongWidth, alongHeight * imageAspect)
}

/** Half-extents of [frame] measured along the axes of an image turned by [angleDegrees]. */
internal fun panelHalfExtents(frame: PageRect, angleDegrees: Float): Pair<Float, Float> {
    val radians = Math.toRadians(angleDegrees.toDouble())
    val cosine = abs(cos(radians)).toFloat()
    val sine = abs(sin(radians)).toFloat()
    val halfWidth = frame.width / 2f
    val halfHeight = frame.height / 2f
    return (halfWidth * cosine + halfHeight * sine) to (halfWidth * sine + halfHeight * cosine)
}
