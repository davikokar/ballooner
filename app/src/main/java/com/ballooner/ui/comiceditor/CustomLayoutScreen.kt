package com.ballooner.ui.comiceditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.CutScope
import com.ballooner.domain.comic.NormalizedPoint
import com.ballooner.domain.comic.PagePoint
import com.ballooner.domain.comic.panelShapes
import com.ballooner.ui.comic.PageViewport
import com.ballooner.ui.comic.PanelImageSource
import com.ballooner.ui.comic.imageDrawSpec
import com.ballooner.ui.comic.toPath
import com.ballooner.ui.theme.InkBlack
import kotlin.math.roundToInt

/** How far a drag has to travel before it counts as a cut rather than a stray touch. */
private const val MIN_TRACE_PIXELS = 24f

/**
 * The Custom preset's own screen.
 *
 * There is nothing to choose here. The layout is made by drawing on it: a line drawn across a
 * panel splits that panel in two, and the panels that result can be cut again.
 */
@Composable
fun CustomLayoutScreen(
    comic: Comic,
    images: PanelImageSource,
    canUndo: Boolean,
    onCut: (from: NormalizedPoint, to: NormalizedPoint, scope: CutScope) -> Unit,
    onStartCutDrag: () -> Unit,
    onMoveCutEnd: (index: Int, start: Boolean, to: NormalizedPoint) -> Unit,
    onEndCutDrag: () -> Unit,
    onUndo: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LayoutOptionBreadcrumb(current = "CUSTOM", onBack = onBack)
        PreviewSurface(
            modifier = Modifier.weight(1f),
            hint = if (comic.layout.cuts.isEmpty()) {
                "Drag across a panel to cut it"
            } else {
                "Drag a handle to adjust a cut"
            },
        ) {
            CuttingPage(
                comic = comic,
                images = images,
                onCut = onCut,
                onStartCutDrag = onStartCutDrag,
                onMoveCutEnd = onMoveCutEnd,
                onEndCutDrag = onEndCutDrag,
            )
            if (canUndo) {
                FloatingAction(
                    label = "Undo",
                    onClick = onUndo,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                )
            }
        }
    }
}

/** One end of a cut, kept in page units so the gesture can place it whatever the canvas size. */
private data class CutHandle(val index: Int, val start: Boolean, val point: PagePoint)

/** The page as it will really be drawn, and the surface cuts are traced and adjusted on. */
@Composable
private fun CuttingPage(
    comic: Comic,
    images: PanelImageSource,
    onCut: (NormalizedPoint, NormalizedPoint, CutScope) -> Unit,
    onStartCutDrag: () -> Unit,
    onMoveCutEnd: (Int, Boolean, NormalizedPoint) -> Unit,
    onEndCutDrag: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = remember(comic) { panelShapes(comic.layout, comic.pageHeight, comic.style) }
    val handles = remember(comic) {
        comic.layout.cuts.flatMapIndexed { index, cut ->
            listOf(
                CutHandle(index, true, cut.a.onPage(comic.pageHeight)),
                CutHandle(index, false, cut.b.onPage(comic.pageHeight)),
            )
        }
    }
    val latest = rememberUpdatedState(comic.pageHeight to handles)
    var start by remember { mutableStateOf<Offset?>(null) }
    var end by remember { mutableStateOf<Offset?>(null) }
    var held by remember { mutableStateOf<CutHandle?>(null) }

    Canvas(
        modifier = Modifier
            .aspectRatio(1f / comic.pageHeight)
            // Keyed on nothing: keying on the comic would tear the handler down the moment a cut
            // moves and the rest of the drag would be lost.
            .pointerInput(Unit) {
                val grabRadius = GRAB_RADIUS.toPx()
                // The page fills this canvas exactly, so a page unit is its width.
                fun viewport() = PageViewport(0f, 0f, size.width.toFloat())
                fun normalised(offset: Offset): NormalizedPoint {
                    val point = viewport().toPage(offset)
                    val pageHeight = latest.value.first
                    return NormalizedPoint(point.x, if (pageHeight > 0f) point.y / pageHeight else 0f)
                }
                detectDragGestures(
                    onDragStart = { offset ->
                        val grabbed = latest.value.second.minByOrNull {
                            (viewport().toScreen(it.point.x, it.point.y) - offset).getDistance()
                        }?.takeIf {
                            (viewport().toScreen(it.point.x, it.point.y) - offset)
                                .getDistance() <= grabRadius
                        }
                        held = grabbed
                        if (grabbed != null) {
                            onStartCutDrag()
                        } else {
                            start = offset
                            end = offset
                        }
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        val grabbed = held
                        if (grabbed != null) {
                            onMoveCutEnd(grabbed.index, grabbed.start, normalised(change.position))
                        } else {
                            end = (end ?: change.position) + amount
                        }
                    },
                    onDragEnd = {
                        if (held != null) {
                            held = null
                            onEndCutDrag()
                            return@detectDragGestures
                        }
                        val from = start
                        val to = end
                        start = null
                        end = null
                        if (from == null || to == null) return@detectDragGestures
                        if ((to - from).getDistance() < MIN_TRACE_PIXELS) return@detectDragGestures
                        val a = normalised(from)
                        // Where the drag began says which panel the line is meant to cut.
                        onCut(a, normalised(to), CutScope.AtPoint(a))
                    },
                    onDragCancel = {
                        if (held != null) {
                            held = null
                            onEndCutDrag()
                        }
                        start = null
                        end = null
                    },
                )
            },
    ) {
        val viewport = PageViewport(0f, 0f, size.width)
        shapes.forEachIndexed { index, shape ->
            val path = shape.toPath(viewport)
            val image = comic.panels.getOrNull(index)?.image
            val bitmap = image?.let { images.bitmapFor(it.sourceUri) }
            clipPath(path) {
                if (image == null || bitmap == null) {
                    drawPath(path, color = scheme.surfaceContainerLowest)
                } else {
                    val spec = imageDrawSpec(
                        panel = shape.bounds,
                        image = image,
                        imageAspect = bitmap.width.toFloat() / bitmap.height,
                        viewport = viewport,
                    )
                    withTransform({ rotate(spec.angleDegrees, spec.pivot) }) {
                        drawImage(
                            image = bitmap,
                            dstOffset = IntOffset(
                                spec.topLeft.x.roundToInt(),
                                spec.topLeft.y.roundToInt(),
                            ),
                            dstSize = IntSize(
                                spec.size.width.roundToInt(),
                                spec.size.height.roundToInt(),
                            ),
                        )
                    }
                }
            }
            drawPath(path, color = InkBlack, style = Stroke(width = 4f))
        }
        drawCutHandles(handles, viewport, scheme.primary)
        val from = start
        val to = end
        if (from != null && to != null) {
            drawLine(
                color = scheme.primary,
                start = from,
                end = to,
                strokeWidth = 5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f)),
            )
        }
    }
}

/** The ends the user drew, joined faintly so it is clear which two belong to the same cut. */
private fun DrawScope.drawCutHandles(
    handles: List<CutHandle>,
    viewport: PageViewport,
    colour: Color,
) {
    handles.chunked(2).forEach { pair ->
        val points = pair.map { viewport.toScreen(it.point.x, it.point.y) }
        if (points.size == 2) {
            drawLine(
                color = colour.copy(alpha = 0.4f),
                start = points[0],
                end = points[1],
                strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
            )
        }
        points.forEach { centre ->
            drawCircle(color = Color.White, radius = HANDLE_RADIUS, center = centre)
            drawCircle(
                color = colour,
                radius = HANDLE_RADIUS,
                center = centre,
                style = Stroke(width = 5f),
            )
        }
    }
}

private val GRAB_RADIUS = 22.dp
private const val HANDLE_RADIUS = 16f
