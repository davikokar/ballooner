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
import kotlin.math.roundToInt
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

/** True when [point] (in canvas pixels) lies inside the balloon body. */
fun Balloon.containsPoint(point: Offset, canvasSize: Size): Boolean {
    val g = geometry(canvasSize)
    if (g.radiusX <= 0f || g.radiusY <= 0f) return false
    if (type == BalloonType.CAPTION) return g.rect.contains(point)
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
        // More bumps for a bigger cloud, but always enough to read as a cloud.
        val bumpCount = ((g.radiusX + g.radiusY) / (canvasSize.minDimension * 0.08f))
            .roundToInt().coerceIn(9, 20)
        val cloud = cloudPath(g, bumpCount)
        drawPath(cloud, color = bodyColor)
        drawPath(cloud, color = outlineColor, style = Stroke(width = strokeWidth))
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
    BalloonType.CAPTION -> Path().apply { addRect(g.rect) }
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

/** A cloud silhouette: a central ellipse merged with [bumpCount] round bumps. */
private fun cloudPath(g: BalloonGeometry, bumpCount: Int): Path {
    val cx = g.center.x
    val cy = g.center.y
    val innerRx = g.radiusX * 0.62f
    val innerRy = g.radiusY * 0.62f
    // Size the puffs from their spacing so scallops stay distinct at any size.
    val avgInner = (innerRx + innerRy) / 2f
    val chord = 2f * avgInner * sin((Math.PI / bumpCount).toFloat())
    val bumpRadius = max(chord * 0.62f, min(g.radiusX, g.radiusY) * 0.2f)
    var cloud = Path().apply { addOval(Rect(cx - innerRx, cy - innerRy, cx + innerRx, cy + innerRy)) }
    for (i in 0 until bumpCount) {
        val angle = (2.0 * Math.PI * i / bumpCount).toFloat()
        val bx = cx + innerRx * cos(angle)
        val by = cy + innerRy * sin(angle)
        val bump = Path().apply { addOval(Rect(bx - bumpRadius, by - bumpRadius, bx + bumpRadius, by + bumpRadius)) }
        val next = Path()
        if (next.op(cloud, bump, PathOperation.Union)) cloud = next
    }
    return cloud
}

internal data class CurvedTail(
    val firstBase: Offset,
    val firstControl: Offset,
    val tip: Offset,
    val secondControl: Offset,
    val secondBase: Offset,
)

internal fun Balloon.curvedTail(canvasSize: Size): CurvedTail = curvedTail(geometry(canvasSize), tailWidth)

private fun curvedTail(g: BalloonGeometry, tailWidth: Float): CurvedTail {
    val perp = Offset(-g.tailDir.y, g.tailDir.x)
    val baseHalf = tailWidth * min(g.radiusX, g.radiusY)
    val baseCenter = tailBaseCenter(g)
    val firstBase = baseCenter + perp * baseHalf
    val secondBase = baseCenter - perp * baseHalf
    val firstMidpoint = (firstBase + g.tip) / 2f
    val secondMidpoint = (secondBase + g.tip) / 2f
    val curve = baseHalf * 0.25f
    return CurvedTail(
        firstBase = firstBase,
        firstControl = firstMidpoint + perp * curve,
        tip = g.tip,
        secondControl = secondMidpoint - perp * curve,
        secondBase = secondBase,
    )
}

private fun tailPath(g: BalloonGeometry, tailWidth: Float): Path {
    val tail = curvedTail(g, tailWidth)
    return Path().apply {
        moveTo(tail.firstBase.x, tail.firstBase.y)
        quadraticTo(tail.firstControl.x, tail.firstControl.y, tail.tip.x, tail.tip.y)
        quadraticTo(tail.secondControl.x, tail.secondControl.y, tail.secondBase.x, tail.secondBase.y)
        close()
    }
}

internal data class ThinkTailBubble(
    val center: Offset,
    val radius: Float,
)

internal fun Balloon.thinkTailBubbles(canvasSize: Size): List<ThinkTailBubble> =
    thinkTailBubbles(geometry(canvasSize))

private fun thinkTailBubbles(g: BalloonGeometry): List<ThinkTailBubble> {
    if (g.tailLengthPx <= 0f) return emptyList()
    val bubbleCount = (g.tailLengthPx / (min(g.radiusX, g.radiusY) * 0.5f))
        .roundToInt().coerceIn(3, 8)
    return List(bubbleCount) { index ->
        val t = index.toFloat() / (bubbleCount - 1)
        ThinkTailBubble(
            center = g.edge + (g.tip - g.edge) * t,
            radius = min(g.radiusX, g.radiusY) * (0.22f * (1f - t) + 0.06f),
        )
    }
}

private fun DrawScope.drawThinkTail(
    g: BalloonGeometry,
    bodyColor: Color,
    outlineColor: Color,
    strokeWidth: Float,
) {
    for (bubble in thinkTailBubbles(g)) {
        drawCircle(color = bodyColor, radius = bubble.radius, center = bubble.center)
        drawCircle(color = outlineColor, radius = bubble.radius, center = bubble.center, style = Stroke(width = strokeWidth))
    }
}
