package com.ballooner.ui.comic

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.ballooner.domain.comic.Balloon
import com.ballooner.domain.comic.PageRect
import com.ballooner.domain.comic.TAIL_BASE_INSET
import com.ballooner.domain.comic.centreOnPage
import com.ballooner.domain.model.BalloonType
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** A balloon worked out in screen pixels, ready to be drawn or hit tested. */
data class BalloonGeometry(
    val centre: Offset,
    val radiusX: Float,
    val radiusY: Float,
    val tailDirection: Offset,
    val edgeRadius: Float,
    val tailLengthPx: Float,
    val tailWidth: Float,
    val cornerRoundness: Float,
    /** How thickly the outline is drawn, in page units. */
    val borderThickness: Float,
    val type: BalloonType,
) {
    val rect: Rect
        get() = Rect(centre.x - radiusX, centre.y - radiusY, centre.x + radiusX, centre.y + radiusY)
    val edge: Offset
        get() = centre + Offset(tailDirection.x * edgeRadius, tailDirection.y * edgeRadius)
    val tip: Offset
        get() = centre + Offset(
            tailDirection.x * (edgeRadius + tailLengthPx),
            tailDirection.y * (edgeRadius + tailLengthPx),
        )

    /** Whether [point] falls inside the balloon body. */
    fun contains(point: Offset): Boolean {
        if (radiusX <= 0f || radiusY <= 0f) return false
        if (type == BalloonType.CAPTION) return rect.contains(point)
        val dx = (point.x - centre.x) / radiusX
        val dy = (point.y - centre.y) / radiusY
        return dx * dx + dy * dy <= 1f
    }
}

/**
 * Places [balloon] on screen.
 *
 * Every size is a fraction of the page width, one unit for both axes, so a balloon keeps its
 * shape no matter how the panel under it is shaped.
 */
fun balloonGeometry(
    balloon: Balloon,
    panel: PageRect?,
    pageHeight: Float,
    viewport: PageViewport,
    borderThickness: Float = balloon.borderThickness,
): BalloonGeometry {
    val onPage = balloon.centreOnPage(panel, pageHeight)
    val radiusX = balloon.width * viewport.scale / 2f
    val radiusY = balloon.height * viewport.scale / 2f
    val angle = Math.toRadians(balloon.tailAngleDegrees.toDouble()).toFloat()
    return BalloonGeometry(
        centre = viewport.toScreen(onPage.x, onPage.y),
        radiusX = radiusX,
        radiusY = radiusY,
        tailDirection = Offset(cos(angle), sin(angle)),
        edgeRadius = ellipseEdgeRadius(radiusX, radiusY, angle),
        tailLengthPx = balloon.tailLength * viewport.scale,
        tailWidth = balloon.tailWidth,
        cornerRoundness = balloon.cornerRoundness,
        borderThickness = borderThickness,
        type = balloon.type,
    )
}

fun DrawScope.drawBalloon(
    geometry: BalloonGeometry,
    pageScale: Float,
    bodyColor: Color,
    outlineColor: Color,
    alpha: Float = 1f,
) {
    val strokeWidth = max(pageScale * geometry.borderThickness, 2f)

    if (geometry.type == BalloonType.THINK) {
        drawThinkTail(geometry, bodyColor, outlineColor, strokeWidth, alpha)
        // More bumps for a bigger cloud, but always enough to read as a cloud.
        val bumpCount = ((geometry.radiusX + geometry.radiusY) / (pageScale * 0.08f))
            .roundToInt().coerceIn(9, 20)
        val cloud = cloudPath(geometry, bumpCount)
        drawPath(cloud, color = bodyColor, alpha = alpha)
        drawPath(cloud, color = outlineColor, alpha = alpha, style = Stroke(width = strokeWidth))
        return
    }

    val body = bodyPath(geometry)
    // Merge the tail into the body so they share one seamless outline.
    val silhouette = Path()
    if (geometry.tailLengthPx > strokeWidth) {
        val tail = tailPath(geometry)
        if (!silhouette.op(body, tail, PathOperation.Union)) {
            silhouette.addPath(body)
            silhouette.addPath(tail)
        }
    } else {
        silhouette.addPath(body)
    }

    drawPath(silhouette, color = bodyColor, alpha = alpha)
    drawPath(
        path = silhouette,
        color = outlineColor,
        alpha = alpha,
        style = Stroke(width = strokeWidth, pathEffect = geometry.type.outlineDash(strokeWidth)),
    )
}

/** Radius from the centre to the ellipse edge along [angleRad]. */
private fun ellipseEdgeRadius(radiusX: Float, radiusY: Float, angleRad: Float): Float {
    val x = radiusY * cos(angleRad)
    val y = radiusX * sin(angleRad)
    val denominator = sqrt(x * x + y * y)
    return if (denominator > 0f) radiusX * radiusY / denominator else max(radiusX, radiusY)
}

private fun bodyPath(g: BalloonGeometry): Path = when (g.type) {
    BalloonType.YELL -> starburstPath(g)
    BalloonType.THINK -> Path().apply { addOval(g.rect) }
    BalloonType.CAPTION -> Path().apply { addRect(g.rect) }
    else -> {
        val radius = g.cornerRoundness.coerceIn(0f, 1f) * min(g.radiusX, g.radiusY)
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
    return Path().apply {
        for (i in 0 until spikes * 2) {
            val angle = Math.PI * i / spikes
            val scale = if (i % 2 == 0) 1f else 0.82f
            val x = g.centre.x + cos(angle).toFloat() * g.radiusX * scale
            val y = g.centre.y + sin(angle).toFloat() * g.radiusY * scale
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

/** A cloud silhouette: a central ellipse merged with [bumpCount] round bumps. */
private fun cloudPath(g: BalloonGeometry, bumpCount: Int): Path {
    val innerRx = g.radiusX * 0.62f
    val innerRy = g.radiusY * 0.62f
    // Size the puffs from their spacing so scallops stay distinct at any size.
    val chord = 2f * ((innerRx + innerRy) / 2f) * sin((Math.PI / bumpCount).toFloat())
    val bumpRadius = max(chord * 0.62f, min(g.radiusX, g.radiusY) * 0.2f)
    var cloud = Path().apply {
        addOval(Rect(g.centre.x - innerRx, g.centre.y - innerRy, g.centre.x + innerRx, g.centre.y + innerRy))
    }
    for (i in 0 until bumpCount) {
        val angle = (2.0 * Math.PI * i / bumpCount).toFloat()
        val bx = g.centre.x + innerRx * cos(angle)
        val by = g.centre.y + innerRy * sin(angle)
        val bump = Path().apply {
            addOval(Rect(bx - bumpRadius, by - bumpRadius, bx + bumpRadius, by + bumpRadius))
        }
        val next = Path()
        if (next.op(cloud, bump, PathOperation.Union)) cloud = next
    }
    return cloud
}

private fun tailBaseCentre(g: BalloonGeometry): Offset = g.centre + Offset(
    g.tailDirection.x * g.edgeRadius * TAIL_BASE_INSET,
    g.tailDirection.y * g.edgeRadius * TAIL_BASE_INSET,
)

private fun tailPath(g: BalloonGeometry): Path {
    val perpendicular = Offset(-g.tailDirection.y, g.tailDirection.x)
    val baseHalf = g.tailWidth * min(g.radiusX, g.radiusY)
    val baseCentre = tailBaseCentre(g)
    val firstBase = baseCentre + perpendicular * baseHalf
    val secondBase = baseCentre - perpendicular * baseHalf
    val curve = baseHalf * 0.25f
    val firstControl = (firstBase + g.tip) / 2f - perpendicular * curve
    val secondControl = (secondBase + g.tip) / 2f + perpendicular * curve
    return Path().apply {
        moveTo(firstBase.x, firstBase.y)
        quadraticTo(firstControl.x, firstControl.y, g.tip.x, g.tip.y)
        quadraticTo(secondControl.x, secondControl.y, secondBase.x, secondBase.y)
        close()
    }
}

private fun DrawScope.drawThinkTail(
    g: BalloonGeometry,
    bodyColor: Color,
    outlineColor: Color,
    strokeWidth: Float,
    alpha: Float,
) {
    if (g.tailLengthPx <= 0f) return
    val bubbleCount = (g.tailLengthPx / (min(g.radiusX, g.radiusY) * 0.5f)).roundToInt().coerceIn(3, 8)
    repeat(bubbleCount) { index ->
        val t = index.toFloat() / (bubbleCount - 1)
        val centre = g.edge + (g.tip - g.edge) * t
        val radius = min(g.radiusX, g.radiusY) * (0.22f * (1f - t) + 0.06f)
        drawCircle(color = bodyColor, radius = radius, center = centre, alpha = alpha)
        drawCircle(
            color = outlineColor,
            radius = radius,
            center = centre,
            alpha = alpha,
            style = Stroke(width = strokeWidth),
        )
    }
}
