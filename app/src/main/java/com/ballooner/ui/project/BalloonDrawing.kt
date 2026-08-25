package com.ballooner.ui.project

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import com.ballooner.domain.model.Balloon
import com.ballooner.domain.model.BalloonType
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Geometry of a balloon expressed in canvas pixels. */
private data class BalloonGeometry(
    val center: Offset,
    val radiusX: Float,
    val radiusY: Float,
    val tailDir: Offset,
    val tailLengthPx: Float,
) {
    val rect: Rect
        get() = Rect(center.x - radiusX, center.y - radiusY, center.x + radiusX, center.y + radiusY)
    val edge: Offset get() = center + Offset(tailDir.x * radiusX, tailDir.y * radiusY)
    val tip: Offset
        get() = center + Offset(tailDir.x * (radiusX + tailLengthPx), tailDir.y * (radiusY + tailLengthPx))
}

private fun Balloon.geometry(canvasSize: Size): BalloonGeometry {
    val angleRad = Math.toRadians(tailAngleDegrees.toDouble())
    return BalloonGeometry(
        center = Offset(centerX * canvasSize.width, centerY * canvasSize.height),
        radiusX = width * canvasSize.width / 2f,
        radiusY = height * canvasSize.height / 2f,
        tailDir = Offset(cos(angleRad).toFloat(), sin(angleRad).toFloat()),
        tailLengthPx = tailLength * min(canvasSize.width, canvasSize.height),
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
    isSelected: Boolean,
    textMeasurer: TextMeasurer,
    bodyColor: Color,
    outlineColor: Color,
    selectionColor: Color,
    textColor: Color,
) {
    val g = balloon.geometry(canvasSize)
    val strokeWidth = maxOf(canvasSize.minDimension * 0.006f, 2f)

    when (balloon.type) {
        BalloonType.THINK -> drawThinkTail(g, bodyColor, outlineColor, strokeWidth)
        else -> drawTail(g, balloon.type, bodyColor, outlineColor, strokeWidth)
    }

    val bodyPath = bodyPath(balloon.type, g)
    drawPath(bodyPath, color = bodyColor)
    drawPath(
        path = bodyPath,
        color = outlineColor,
        style = Stroke(width = strokeWidth, pathEffect = balloon.type.outlineDash(strokeWidth)),
    )

    if (isSelected) {
        drawRect(
            color = selectionColor,
            topLeft = Offset(g.rect.left, g.rect.top),
            size = Size(g.rect.width, g.rect.height),
            style = Stroke(
                width = strokeWidth,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(strokeWidth * 3, strokeWidth * 3)),
            ),
        )
    }

    drawBalloonText(balloon, g, textMeasurer, textColor)
}

private fun bodyPath(type: BalloonType, g: BalloonGeometry): Path = when (type) {
    BalloonType.YELL -> starburstPath(g)
    else -> Path().apply { addOval(g.rect) }
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

private fun DrawScope.drawTail(
    g: BalloonGeometry,
    type: BalloonType,
    bodyColor: Color,
    outlineColor: Color,
    strokeWidth: Float,
) {
    if (g.tailLengthPx <= 0f) return
    val perp = Offset(-g.tailDir.y, g.tailDir.x)
    val baseHalf = if (type == BalloonType.YELL) g.radiusX * 0.12f else g.radiusX * 0.28f
    val tail = Path().apply {
        moveTo(g.edge.x + perp.x * baseHalf, g.edge.y + perp.y * baseHalf)
        lineTo(g.tip.x, g.tip.y)
        lineTo(g.edge.x - perp.x * baseHalf, g.edge.y - perp.y * baseHalf)
        close()
    }
    drawPath(tail, color = bodyColor)
    drawPath(
        path = tail,
        color = outlineColor,
        style = Stroke(width = strokeWidth, pathEffect = type.outlineDash(strokeWidth)),
    )
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

private fun DrawScope.drawBalloonText(
    balloon: Balloon,
    g: BalloonGeometry,
    textMeasurer: TextMeasurer,
    textColor: Color,
) {
    if (balloon.text.isBlank()) return
    val maxWidth = (g.radiusX * 2f * 0.82f).toInt().coerceAtLeast(1)
    val maxHeight = (g.radiusY * 2f * 0.82f).toInt().coerceAtLeast(1)
    val result = textMeasurer.measure(
        text = balloon.text,
        style = TextStyle(color = textColor, textAlign = TextAlign.Center),
        overflow = TextOverflow.Ellipsis,
        constraints = Constraints(maxWidth = maxWidth, maxHeight = maxHeight),
    )
    drawText(
        textLayoutResult = result,
        topLeft = Offset(
            g.center.x - result.size.width / 2f,
            g.center.y - result.size.height / 2f,
        ),
    )
}
