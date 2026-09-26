package com.ballooner.ui.comiceditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.toSize
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.PagePoint
import com.ballooner.domain.comic.Polygon
import com.ballooner.domain.comic.panelShapes
import com.ballooner.ui.comic.PanelImageSource
import com.ballooner.ui.comic.comicViewport
import com.ballooner.ui.comic.toPath

/**
 * The Placement step's editing surface: pick a panel and fit its image, or press and hold to
 * carry an image to another panel and trade places with it.
 *
 * The gesture only ever reports what the fingers did; keeping the image over its panel is the
 * document's job, not this composable's.
 */
@Composable
internal fun PlacementStepOverlay(
    comic: Comic,
    activePanel: Int?,
    focus: Polygon?,
    images: PanelImageSource,
    actions: ComicEditorActions,
    modifier: Modifier = Modifier,
) {
    val shapes = remember(comic) { panelShapes(comic.layout, comic.pageShape, comic.style) }
    val aspect = remember(comic, activePanel) {
        val uri = activePanel?.let { comic.panels.getOrNull(it)?.image?.sourceUri }
        uri?.let { images.bitmapFor(it) }?.let { it.width.toFloat() / it.height } ?: 1f
    }
    var carrying by remember { mutableStateOf<Int?>(null) }
    var over by remember { mutableStateOf<Int?>(null) }

    fun panelAt(point: PagePoint) = shapes.indexOfFirst { it.contains(point) }.takeIf { it >= 0 }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(shapes, focus) {
                val viewport = comicViewport(size.toSize(), comic.pageShape, focus?.bounds)
                detectTapGestures(
                    onTap = { actions.selectPanel(panelAt(viewport.toPage(it))) },
                    // Double tap is the quick way in and out of a closer look.
                    onDoubleTap = {
                        actions.focusPanel(if (focus != null) null else panelAt(viewport.toPage(it)))
                    },
                )
            }
            .pointerInput(shapes, focus) {
                val viewport = comicViewport(size.toSize(), comic.pageShape, focus?.bounds)
                detectDragGesturesAfterLongPress(
                    onDragStart = { start ->
                        carrying = panelAt(viewport.toPage(start))
                        over = carrying
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        over = panelAt(viewport.toPage(change.position))
                    },
                    onDragEnd = {
                        val from = carrying
                        val to = over
                        if (from != null && to != null) actions.swapPanelImages(from, to)
                        carrying = null
                        over = null
                    },
                    onDragCancel = {
                        carrying = null
                        over = null
                    },
                )
            }
            .pointerInput(activePanel, comic, aspect, focus) {
                if (activePanel == null) return@pointerInput
                val viewport = comicViewport(size.toSize(), comic.pageShape, focus?.bounds)
                var gesturing = false
                detectTransformGestures { _, pan, zoom, rotation ->
                    // A carried image is being moved between panels, not fitted inside one.
                    if (carrying != null) return@detectTransformGestures
                    if (!gesturing) {
                        gesturing = true
                        actions.startPlacementGesture()
                    }
                    if (viewport.scale <= 0f) return@detectTransformGestures
                    actions.transformPanelImage(
                        index = activePanel,
                        imageAspect = aspect,
                        // Fingers move in pixels; the document thinks in page units.
                        panX = pan.x / viewport.scale,
                        panY = pan.y / viewport.scale,
                        zoomBy = zoom,
                        rotateBy = rotation,
                    )
                }
                if (gesturing) actions.endPlacementGesture()
            },
    ) {
        val viewport = comicViewport(size, comic.pageShape, focus?.bounds)
        if (viewport.scale <= 0f) return@Canvas
        shapes.forEachIndexed { index, shape ->
            val path = shape.toPath(viewport)
            when {
                carrying != null && index == over && index != carrying ->
                    drawPath(path, color = DropTargetFill)
                index == carrying -> drawPath(path, color = CarriedFill)
                index == activePanel -> drawPath(path, color = ActiveStroke, style = Stroke(width = 6f))
                comic.panels.getOrNull(index)?.image == null -> drawPath(path, color = EmptyHint)
            }
        }
    }
}

private val ActiveStroke = Color(0xFF2962FF)
private val EmptyHint = Color(0x22000000)
private val CarriedFill = Color(0x332962FF)
private val DropTargetFill = Color(0x5500C853)
