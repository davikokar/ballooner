package com.ballooner.ui.project

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.ballooner.domain.model.Balloon
import com.ballooner.domain.model.BalloonType
import kotlin.math.atan2
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// How far inside the body edge the tail base sits, so the union overlaps cleanly.
private const val TAIL_BASE_INSET = 0.7f

/** Geometry of a balloon expressed in canvas pixels. */
private data class BalloonGeometry(
    val center: Offset,
    val radiusX: Float,
    val radiusY: Float,
    val tailDir: Offset,
    val edgeRadius: Float,
    val tailLengthPx: Float,
) {
    val rect: Rect
        get() = Rect(center.x - radiusX, center.y - radiusY, center.x + radiusX, center.y + radiusY)
    val edge: Offset get() = center + Offset(tailDir.x * edgeRadius, tailDir.y * edgeRadius)
    val tip: Offset
        get() = center + Offset(tailDir.x * (edgeRadius + tailLengthPx), tailDir.y * (edgeRadius + tailLengthPx))
}

/** Radius from the center to the ellipse edge along [angleRad]. */
private fun ellipseEdgeRadius(radiusX: Float, radiusY: Float, angleRad: Float): Float {
    val denom = sqrt((radiusY * cos(angleRad)) * (radiusY * cos(angleRad)) + (radiusX * sin(angleRad)) * (radiusX * sin(angleRad)))
    return if (denom > 0f) radiusX * radiusY / denom else max(radiusX, radiusY)
}

private fun Balloon.geometry(canvasSize: Size): BalloonGeometry {
    val angleRad = Math.toRadians(tailAngleDegrees.toDouble()).toFloat()
    val radiusX = width * canvasSize.width / 2f
    val radiusY = height * canvasSize.height / 2f
    return BalloonGeometry(
        center = Offset(centerX * canvasSize.width, centerY * canvasSize.height),
        radiusX = radiusX,
        radiusY = radiusY,
        tailDir = Offset(cos(angleRad), sin(angleRad)),
        edgeRadius = ellipseEdgeRadius(radiusX, radiusY, angleRad),
        tailLengthPx = tailLength * min(canvasSize.width, canvasSize.height),
    )
}

/** Center of the balloon body in canvas pixels. */
fun Balloon.bodyCenter(canvasSize: Size): Offset = geometry(canvasSize).center

/** The tip of the tail in canvas pixels. */
fun Balloon.tailTip(canvasSize: Size): Offset = geometry(canvasSize).tip

/** One of the two points where the tail base meets the body, in canvas pixels. */
fun Balloon.tailBaseHandle(canvasSize: Size): Offset {
    val g = geometry(canvasSize)
    val perp = Offset(-g.tailDir.y, g.tailDir.x)
    val baseCenter = tailBaseCenter(g)
    val baseHalf = tailWidth * min(g.radiusX, g.radiusY)
    return baseCenter + Offset(perp.x * baseHalf, perp.y * baseHalf)
}

/** Returns a copy whose tail base half-width matches [target]'s distance from the tail axis. */
fun Balloon.tailWidthFromPoint(target: Offset, canvasSize: Size): Balloon {
    val g = geometry(canvasSize)
    val perp = Offset(-g.tailDir.y, g.tailDir.x)
    val baseCenter = tailBaseCenter(g)
    val signed = (target.x - baseCenter.x) * perp.x + (target.y - baseCenter.y) * perp.y
    val minRadius = min(g.radiusX, g.radiusY)
    val newWidth = if (minRadius > 0f) abs(signed) / minRadius else tailWidth
    return copy(tailWidth = newWidth)
}

private fun tailBaseCenter(g: BalloonGeometry): Offset =
    g.center + Offset(g.tailDir.x * g.edgeRadius * TAIL_BASE_INSET, g.tailDir.y * g.edgeRadius * TAIL_BASE_INSET)

/**
 * Returns a copy whose tail points at [target] (in canvas pixels). Because the
 * tail tip is radial, the resulting [tailTip] equals [target], so a drag handle
 * placed at the tip follows the finger exactly.
 */
fun Balloon.tailAtPoint(target: Offset, canvasSize: Size): Balloon {
    val radiusX = width * canvasSize.width / 2f
    val radiusY = height * canvasSize.height / 2f
    val center = Offset(centerX * canvasSize.width, centerY * canvasSize.height)
    val ux = target.x - center.x
    val uy = target.y - center.y
    val angle = atan2(uy, ux)
    val edgeRadius = ellipseEdgeRadius(radiusX, radiusY, angle)
    val minDim = min(canvasSize.width, canvasSize.height)
    val length = ((hypot(ux, uy) - edgeRadius) / minDim).coerceAtLeast(0f)
    return copy(
        tailAngleDegrees = Math.toDegrees(angle.toDouble()).toFloat(),
        tailLength = length,
    )
}

/** True when [point] (in canvas pixels) lies inside the balloon body ellipse. */
fun Balloon.containsPoint(point: Offset, canvasSize: Size): Boolean {
    val g = geometry(canvasSize)
    if (g.radiusX <= 0f || g.radiusY <= 0f) return false
    val dx = (point.x - g.center.x) / g.radiusX
    val dy = (point.y - g.center.y) / g.radiusY
    return dx * dx + dy * dy <= 1f
}

fun DrawScope.drawBalloon(
    balloon: Balloon,
    canvasSize: Size,
    bodyColor: Color,
    outlineColor: Color,
) {
    val g = balloon.geometry(canvasSize)
    val strokeWidth = maxOf(canvasSize.minDimension * 0.006f, 2f)
    val dash = balloon.type.outlineDash(strokeWidth)

    if (balloon.type == BalloonType.THINK) {
        drawThinkTail(g, bodyColor, outlineColor, strokeWidth)
        val body = Path().apply { addOval(g.rect) }
        drawPath(body, color = bodyColor)
        drawPath(body, color = outlineColor, style = Stroke(width = strokeWidth))
        return
    }

    val body = balloon.bodyPath(g)
    // Merge the tail into the body so they share one seamless outline.
    val silhouette = Path()
    if (g.tailLengthPx > strokeWidth) {
        val tail = tailPath(g, balloon.tailWidth)
        val merged = silhouette.op(body, tail, PathOperation.Union)
        if (!merged) {
            silhouette.addPath(body)
            silhouette.addPath(tail)
        }
    } else {
        silhouette.addPath(body)
    }

    drawPath(silhouette, color = bodyColor)
    drawPath(silhouette, color = outlineColor, style = Stroke(width = strokeWidth, pathEffect = dash))
}

private fun Balloon.bodyPath(g: BalloonGeometry): Path = when (type) {
    BalloonType.YELL -> starburstPath(g)
    BalloonType.THINK -> Path().apply { addOval(g.rect) }
    else -> {
        val radius = cornerRoundness.coerceIn(0f, 1f) * min(g.radiusX, g.radiusY)
        Path().apply { addRoundRect(RoundRect(g.rect, CornerRadius(radius, radius))) }
    }
}

private fun BalloonType.outlineDash(strokeWidth: Float): PathEffect? =
    if (this == BalloonType.WHISPER) {
        PathEffect.dashPathEffect(floatArrayOf(strokeWidth * 4, strokeWidth * 3))
    } else {
        null
    }

private fun starburstPath(g: BalloonGeometry): Path {
    val spikes = 14
    val outer = 1f
    val inner = 0.82f
    return Path().apply {
        for (i in 0 until spikes * 2) {
            val angle = Math.PI * i / spikes
            val scale = if (i % 2 == 0) outer else inner
            val x = g.center.x + cos(angle).toFloat() * g.radiusX * scale
            val y = g.center.y + sin(angle).toFloat() * g.radiusY * scale
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

private fun tailPath(g: BalloonGeometry, tailWidth: Float): Path {
    val perp = Offset(-g.tailDir.y, g.tailDir.x)
    val baseHalf = tailWidth * min(g.radiusX, g.radiusY)
    val baseCenter = tailBaseCenter(g)
    return Path().apply {
        moveTo(baseCenter.x + perp.x * baseHalf, baseCenter.y + perp.y * baseHalf)
        lineTo(g.tip.x, g.tip.y)
        lineTo(baseCenter.x - perp.x * baseHalf, baseCenter.y - perp.y * baseHalf)
        close()
    }
}

private fun DrawScope.drawThinkTail(
    g: BalloonGeometry,
    bodyColor: Color,
    outlineColor: Color,
    strokeWidth: Float,
) {
    if (g.tailLengthPx <= 0f) return
    val bubbles = 3
    for (i in 1..bubbles) {
        val t = i.toFloat() / bubbles
        val pos = g.edge + (g.tip - g.edge) * t
        val radius = min(g.radiusX, g.radiusY) * (0.22f * (1f - t) + 0.06f)
        drawCircle(color = bodyColor, radius = radius, center = pos)
        drawCircle(color = outlineColor, radius = radius, center = pos, style = Stroke(width = strokeWidth))
    }
}
