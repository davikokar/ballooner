package com.ballooner.ui.comiceditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.CutScope
import com.ballooner.domain.comic.GridAxis
import com.ballooner.domain.comic.GridBoundary
import com.ballooner.domain.comic.GridPanel
import com.ballooner.domain.comic.NormalizedPoint
import com.ballooner.domain.comic.PageRect
import com.ballooner.domain.comic.Span
import com.ballooner.domain.comic.contentRect
import com.ballooner.domain.comic.gridBoundaries
import com.ballooner.domain.comic.gridPanels
import com.ballooner.ui.comic.PageViewport
import com.ballooner.ui.comic.pageViewport
import com.ballooner.ui.comic.toPath

/**
 * The Layout step's editing surface: selecting panels, dragging grid lines, and tracing cuts.
 *
 * It sits above the page and computes the same viewport the page does, so it never has to be told
 * where anything was drawn.
 */
@Composable
internal fun LayoutStepOverlay(
    comic: Comic,
    tool: LayoutTool,
    selection: List<Span>,
    actions: ComicEditorActions,
    modifier: Modifier = Modifier,
) {
    val panels = remember(comic) { gridPanels(comic.layout.grid, comic.pageShape, comic.style) }
    val boundaries = remember(comic) { gridBoundaries(comic.layout.grid, comic.pageShape, comic.style) }
    val content = remember(comic) { contentRect(comic.pageShape, comic.style) }
    val grabRadius = with(LocalDensity.current) { GRAB_RADIUS.toPx() }

    var tracing by remember { mutableStateOf<Pair<Offset, Offset>?>(null) }
    var dragging by remember { mutableStateOf<GridBoundary?>(null) }
    var dragOrigin by remember { mutableStateOf(Offset.Zero) }

    // A boundary drag edits the comic on every move, so keying the gesture on the comic would
    // cancel the drag on its own first step. This lets a running gesture read current values.
    val latest by rememberUpdatedState(
        LayoutInputs(comic, tool, panels, boundaries, content),
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (latest.tool != LayoutTool.SELECT) return@detectTapGestures
                    val point = pageViewport(size.toSize(), latest.comic.pageShape).toPage(offset)
                    latest.panels.firstOrNull { it.shape.contains(point) }
                        ?.let { actions.toggleSelection(it.span) }
                }
            }
            .pointerInput(Unit) {
                fun viewport() = pageViewport(size.toSize(), latest.comic.pageShape)
                detectDragGestures(
                    onDragStart = { start ->
                        dragOrigin = start
                        if (latest.tool == LayoutTool.SELECT) {
                            dragging = latest.boundaries.nearestTo(start, viewport(), grabRadius)
                            if (dragging != null) actions.startBoundaryDrag()
                        } else {
                            tracing = start to start
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val current = change.position
                        val line = dragging
                        if (line != null) {
                            val box = latest.content
                            val extent = if (line.axis == GridAxis.COLUMN) box.width else box.height
                            val moved = if (line.axis == GridAxis.COLUMN) {
                                current.x - dragOrigin.x
                            } else {
                                current.y - dragOrigin.y
                            }
                            val scale = viewport().scale
                            val delta = if (scale > 0f && extent > 0f) {
                                moved / scale / extent
                            } else {
                                0f
                            }
                            actions.moveBoundary(line.axis, line.index, delta)
                        } else if (tracing != null) {
                            tracing = dragOrigin to current
                        }
                    },
                    onDragEnd = {
                        if (dragging != null) actions.endBoundaryDrag()
                        tracing?.let { (start, end) ->
                            actions.traceCut(start, end, viewport(), latest.tool, latest.comic)
                        }
                        dragging = null
                        tracing = null
                    },
                    onDragCancel = {
                        if (dragging != null) actions.endBoundaryDrag()
                        dragging = null
                        tracing = null
                    },
                )
            },
    ) {
        val viewport = pageViewport(size, comic.pageShape)
        if (viewport.scale <= 0f) return@Canvas
        if (tool == LayoutTool.SELECT) {
            panels.filter { it.span in selection }.forEach { panel ->
                val path = panel.shape.toPath(viewport)
                drawPath(path, color = SelectionFill)
                drawPath(path, color = SelectionStroke, style = Stroke(width = 4f))
            }
            boundaries.forEach { drawBoundary(it, viewport, comic) }
        }
        tracing?.let { (start, end) ->
            drawLine(color = SelectionStroke, start = start, end = end, strokeWidth = 4f)
        }
    }
}

private fun DrawScope.drawBoundary(boundary: GridBoundary, viewport: PageViewport, comic: Comic) {
    val dashes = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
    val pageHeight = comic.pageShape.pageHeight
    val (start, end) = if (boundary.axis == GridAxis.COLUMN) {
        viewport.toScreen(boundary.position, 0f) to viewport.toScreen(boundary.position, pageHeight)
    } else {
        viewport.toScreen(0f, boundary.position) to viewport.toScreen(1f, boundary.position)
    }
    drawLine(color = BoundaryColour, start = start, end = end, strokeWidth = 2f, pathEffect = dashes)
}

/** The values a running layout gesture needs to keep reading as the comic changes. */
private data class LayoutInputs(
    val comic: Comic,
    val tool: LayoutTool,
    val panels: List<GridPanel>,
    val boundaries: List<GridBoundary>,
    val content: PageRect,
)

private fun List<GridBoundary>.nearestTo(
    position: Offset,
    viewport: PageViewport,
    radius: Float,
): GridBoundary? = minByOrNull { boundary ->
    val screen = if (boundary.axis == GridAxis.COLUMN) {
        kotlin.math.abs(viewport.toScreen(boundary.position, 0f).x - position.x)
    } else {
        kotlin.math.abs(viewport.toScreen(0f, boundary.position).y - position.y)
    }
    screen
}?.takeIf { boundary ->
    val screen = if (boundary.axis == GridAxis.COLUMN) {
        kotlin.math.abs(viewport.toScreen(boundary.position, 0f).x - position.x)
    } else {
        kotlin.math.abs(viewport.toScreen(0f, boundary.position).y - position.y)
    }
    screen <= radius
}

/** Turns a traced drag into a cut, choosing its scope from the active tool. */
private fun ComicEditorActions.traceCut(
    start: Offset,
    end: Offset,
    viewport: PageViewport,
    tool: LayoutTool,
    comic: Comic,
) {
    if ((end - start).getDistance() < MIN_TRACE_PIXELS) return
    val pageHeight = comic.pageShape.pageHeight
    fun normalized(offset: Offset): NormalizedPoint {
        val point = viewport.toPage(offset)
        return NormalizedPoint(point.x, if (pageHeight > 0f) point.y / pageHeight else 0f)
    }
    val from = normalized(start)
    val to = normalized(end)
    val scope = when (tool) {
        LayoutTool.CUT_PANEL -> CutScope.AtPoint(from)
        else -> CutScope.WholePage
    }
    addCut(from, to, scope)
}

private val GRAB_RADIUS = 20.dp
private const val MIN_TRACE_PIXELS = 24f
private val SelectionFill = Color(0x332962FF)
private val SelectionStroke = Color(0xFF2962FF)
private val BoundaryColour = Color(0x66000000)
