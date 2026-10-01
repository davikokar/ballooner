package com.ballooner.ui.comic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.ballooner.domain.comic.Balloon
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.Polygon
import com.ballooner.domain.comic.inDrawingOrder
import com.ballooner.domain.comic.panelIndex
import com.ballooner.domain.comic.panelShapes
import com.ballooner.ui.theme.InkBlack
import com.ballooner.ui.theme.PaperWhite
import com.ballooner.ui.theme.toFontFamily
import kotlin.math.roundToInt

/** Supplies the bitmap behind a panel image's source uri, or null while it is unavailable. */
fun interface PanelImageSource {
    fun bitmapFor(sourceUri: String): ImageBitmap?
}

/**
 * Draws [comic] and nothing else: no handles, no gestures, no selection.
 *
 * Every panel is a projection of the document through the current viewport, so this is also what
 * export renders, just at a different scale.
 */
@Composable
fun ComicPage(
    comic: Comic,
    images: PanelImageSource,
    modifier: Modifier = Modifier,
    imageAlpha: Float = 1f,
    balloonAlpha: Float = 1f,
    focus: Polygon? = null,
    /** The balloon whose text is being typed in place, and so must not be drawn twice. */
    editingBalloon: Long? = null,
) {
    // A focused panel is drawn scaled up, so without this the page spills over its neighbours
    // in the layout.
    Box(modifier = modifier.clipToBounds()) {
        val textMeasurer = rememberTextMeasurer()
        Canvas(modifier = Modifier.fillMaxSize()) {
            val viewport = comicViewport(size, comic.pageHeight, focus?.bounds)
            if (viewport.scale <= 0f) return@Canvas
            drawComicPage(
                comic = comic,
                images = images,
                viewport = viewport,
                textMeasurer = textMeasurer,
                imageAlpha = imageAlpha,
                balloonAlpha = balloonAlpha,
                focus = focus,
                editingBalloon = editingBalloon,
            )
        }
    }
}

/**
 * Draws a whole comic through [viewport].
 *
 * Export renders through this too, just at a different scale, so what is exported cannot drift
 * away from what was on screen.
 */
fun DrawScope.drawComicPage(
    comic: Comic,
    images: PanelImageSource,
    viewport: PageViewport,
    textMeasurer: TextMeasurer,
    imageAlpha: Float = 1f,
    balloonAlpha: Float = 1f,
    focus: Polygon? = null,
    editingBalloon: Long? = null,
) {
    if (viewport.scale <= 0f) return
    val drawEverything = {
        drawPage(comic, viewport)
        val shapes = panelShapes(comic.layout, comic.pageHeight, comic.style)
        shapes.forEachIndexed { index, shape ->
            drawPanelImage(comic, index, shape, viewport, images, imageAlpha)
        }
        // Balloons belonging to a panel go under its border, so they can never paint over
        // the edge of the panel that is supposed to be cutting them off.
        if (balloonAlpha > 0f) {
            drawBalloons(comic, shapes, viewport, balloonAlpha, textMeasurer, true, editingBalloon)
        }
        shapes.forEach { drawPanelBorder(comic, it, viewport) }
        if (balloonAlpha > 0f) {
            drawBalloons(comic, shapes, viewport, balloonAlpha, textMeasurer, false, editingBalloon)
        }
    }
    // Focusing shows one panel alone, so its neighbours are cut away rather than hidden.
    if (focus != null) {
        clipPath(focus.toPath(viewport, comic.style.cornerRadius)) { drawEverything() }
    } else {
        drawEverything()
    }
}

private fun DrawScope.drawBalloons(
    comic: Comic,
    shapes: List<Polygon>,
    viewport: PageViewport,
    alpha: Float,
    textMeasurer: TextMeasurer,
    panelScoped: Boolean,
    editingBalloon: Long?,
) {
    comic.balloons.inDrawingOrder().forEach { balloon ->
        val panelIndex = balloon.panelIndex
        if ((panelIndex != null) != panelScoped) return@forEach
        val panel = panelIndex?.let { shapes.getOrNull(it) }
        if (panelIndex != null && panel == null) return@forEach
        val geometry = balloonGeometry(
            balloon = balloon,
            panel = panel?.bounds,
            pageHeight = comic.pageHeight,
            viewport = viewport,
            borderThickness = balloon.borderWidth(comic.style),
        )
        val draw = {
            drawBalloon(
                geometry = geometry,
                pageScale = viewport.scale,
                bodyColor = PaperWhite,
                outlineColor = InkBlack,
                alpha = alpha,
            )
            if (balloon.id != editingBalloon) {
                drawBalloonText(balloon, geometry, viewport.scale, textMeasurer, alpha)
            }
        }
        // A panel balloon is cut off at its panel's edge; a comic balloon is free of them.
        if (panel != null) {
            clipPath(panel.toPath(viewport, comic.style.cornerRadius)) { draw() }
        } else {
            draw()
        }
    }
}

private fun DrawScope.drawBalloonText(
    balloon: Balloon,
    geometry: BalloonGeometry,
    pageScale: Float,
    textMeasurer: TextMeasurer,
    alpha: Float,
) {
    if (balloon.text.isBlank() || geometry.radiusX <= 0f) return
    val layout = textMeasurer.measure(
        text = balloon.text,
        style = balloonTextStyle(
            balloon = balloon,
            sizePx = balloonTextSize(balloon, geometry, textMeasurer, pageScale, this),
            density = this,
        ),
        constraints = Constraints(maxWidth = textWidth(geometry)),
    )
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(
            geometry.centre.x - layout.size.width / 2f,
            geometry.centre.y - layout.size.height / 2f,
        ),
        alpha = alpha,
    )
}

private fun DrawScope.drawPage(comic: Comic, viewport: PageViewport) {
    drawRect(
        color = PaperWhite,
        topLeft = Offset(viewport.originX, viewport.originY),
        size = Size(viewport.scale, viewport.scale * comic.pageHeight),
    )
}

private fun DrawScope.drawPanelImage(
    comic: Comic,
    index: Int,
    shape: Polygon,
    viewport: PageViewport,
    images: PanelImageSource,
    imageAlpha: Float,
) {
    val path = shape.toPath(viewport, comic.style.cornerRadius)
    val image = comic.panels.getOrNull(index)?.image
    val bitmap = image?.let { images.bitmapFor(it.sourceUri) }
    clipPath(path) {
        if (image == null || bitmap == null) {
            drawPath(path, color = EmptyPanelFill)
            return@clipPath
        }
        val spec = imageDrawSpec(
            panel = shape.bounds,
            image = image,
            imageAspect = bitmap.width.toFloat() / bitmap.height,
            viewport = viewport,
        )
        withTransform({ rotate(spec.angleDegrees, spec.pivot) }) {
            drawImage(
                image = bitmap,
                dstOffset = spec.topLeft.toIntOffset(),
                dstSize = spec.size.toIntSize(),
                alpha = imageAlpha,
            )
        }
    }
}

private fun DrawScope.drawPanelBorder(comic: Comic, shape: Polygon, viewport: PageViewport) {
    val borderWidth = comic.style.borderThickness * viewport.scale
    if (borderWidth > 0f) {
        drawPath(
            path = shape.toPath(viewport, comic.style.cornerRadius),
            color = InkBlack,
            style = Stroke(width = borderWidth),
        )
    }
}

private fun Offset.toIntOffset() = IntOffset(x.roundToInt(), y.roundToInt())

private fun Size.toIntSize() = IntSize(
    width.roundToInt().coerceAtLeast(1),
    height.roundToInt().coerceAtLeast(1),
)

private val EmptyPanelFill = Color(0xFFE8E8E8)

// Keeps text off the balloon outline.
/** How much of the balloon's width its text is wrapped to. */
internal const val TEXT_INSET = 0.82f
