package com.ballooner.ui.comic

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.coverScale
import com.ballooner.domain.comic.panelShapes
import kotlin.math.roundToInt

/**
 * How wide the exported page should be, in pixels, to hold every image at its own resolution.
 *
 * Each panel asks for the page width that would draw its image pixel for pixel; the widest such
 * request wins, so nothing is exported softer than it was supplied. [sizeOf] reports the pixel
 * size of a decoded image, or null while it is still loading.
 */
fun comicPixelWidth(comic: Comic, sizeOf: (String) -> IntSize?): Int {
    val shapes = panelShapes(comic.layout, comic.pageHeight, comic.style)
    val requested = comic.panels.mapIndexedNotNull { index, panel ->
        val image = panel.image ?: return@mapIndexedNotNull null
        val size = sizeOf(image.sourceUri) ?: return@mapIndexedNotNull null
        val bounds = shapes.getOrNull(index)?.bounds ?: return@mapIndexedNotNull null
        if (size.height <= 0) return@mapIndexedNotNull null
        val aspect = size.width.toFloat() / size.height
        val displayWidth = coverScale(bounds, aspect, image.angleDegrees) * image.zoom.coerceAtLeast(1f)
        if (displayWidth <= 0f) null else size.width / displayWidth
    }
    val widest = requested.maxOrNull() ?: DEFAULT_EXPORT_WIDTH.toFloat()
    return widest.roundToInt().coerceIn(MIN_EXPORT_WIDTH, MAX_EXPORT_WIDTH)
}

/** Renders [comic] into a bitmap [pixelWidth] across, through the same drawing the editor uses. */
fun renderComic(
    comic: Comic,
    images: PanelImageSource,
    pixelWidth: Int,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
): ImageBitmap {
    val width = pixelWidth.coerceIn(MIN_EXPORT_WIDTH, MAX_EXPORT_WIDTH)
    val height = (width * comic.pageHeight).roundToInt().coerceAtLeast(1)
    val bitmap = ImageBitmap(width, height)
    val size = Size(width.toFloat(), height.toFloat())
    CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(bitmap), size) {
        drawComicPage(
            comic = comic,
            images = images,
            // The page is one unit wide, so the scale is simply the width being rendered.
            viewport = PageViewport(originX = 0f, originY = 0f, scale = width.toFloat()),
            textMeasurer = TextMeasurer(fontFamilyResolver, density, LayoutDirection.Ltr),
        )
    }
    return bitmap
}

private const val DEFAULT_EXPORT_WIDTH = 1600
private const val MIN_EXPORT_WIDTH = 320
private const val MAX_EXPORT_WIDTH = 4096
