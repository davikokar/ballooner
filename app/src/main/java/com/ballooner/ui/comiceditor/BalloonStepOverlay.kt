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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.ballooner.domain.comic.Balloon
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.PagePoint
import com.ballooner.domain.comic.PageRect
import com.ballooner.domain.comic.Polygon
import com.ballooner.domain.comic.centreOnPage
import com.ballooner.domain.comic.contains
import com.ballooner.domain.comic.inDrawingOrder
import com.ballooner.domain.comic.panelIndex
import com.ballooner.domain.comic.panelShapes
import com.ballooner.domain.comic.resizeHandle
import com.ballooner.domain.comic.tailTip
import com.ballooner.domain.comic.tailWidthHandle
import com.ballooner.ui.comic.PageViewport
import com.ballooner.ui.comic.comicViewport

/** What a drag on a selected balloon is doing. */
private enum class BalloonGrab { BODY, RESIZE, TAIL, TAIL_WIDTH }

/**
 * The Balloon step's editing surface: pick a balloon, then move it, resize it, or aim its tail.
 *
 * Handles are placed from the document, so they sit where the balloon is actually drawn however
 * its panel has been reshaped underneath it.
 */
@Composable
internal fun BalloonStepOverlay(
    comic: Comic,
    selectedBalloon: Long?,
    focus: Polygon?,
    actions: ComicEditorActions,
    modifier: Modifier = Modifier,
) {
    val shapes = remember(comic) { panelShapes(comic.layout, comic.pageHeight, comic.style) }
    val pageHeight = comic.pageHeight
    val grabRadius = with(LocalDensity.current) { GRAB_RADIUS.toPx() }
    val selected = comic.balloons.firstOrNull { it.id == selectedBalloon }

    fun panelOf(balloon: Balloon): PageRect? = balloon.panelIndex?.let { shapes.getOrNull(it)?.bounds }

    var grab by remember { mutableStateOf<BalloonGrab?>(null) }
    var held by remember { mutableStateOf<Balloon?>(null) }

    // Dragging a balloon edits the comic on every move, so keying the gesture on the comic would
    // cancel the drag on its own first step. This lets a running gesture read current values.
    val latest by rememberUpdatedState(BalloonInputs(comic, shapes, focus, selected))

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val viewport =
                        comicViewport(size.toSize(), latest.comic.pageHeight, latest.focus?.bounds)
                    val point = viewport.toPage(offset)
                    val height = latest.comic.pageHeight
                    // Topmost first, so the balloon you can see is the one you get.
                    val hit = latest.comic.balloons.inDrawingOrder().lastOrNull {
                        it.contains(PagePoint(point.x, point.y), panelOf(it), height)
                    }
                    actions.selectBalloon(hit?.id)
                    // Tapping bare panel also aims where the next balloon will be added.
                    if (hit == null) {
                        actions.selectPanel(
                            latest.shapes.indexOfFirst { it.contains(point) }.takeIf { it >= 0 },
                        )
                    }
                }
            }
            .pointerInput(Unit) {
                fun viewport() =
                    comicViewport(size.toSize(), latest.comic.pageHeight, latest.focus?.bounds)
                detectDragGestures(
                    onDragStart = { start ->
                        val balloon = latest.selected
                        if (balloon == null) {
                            grab = null
                        } else {
                            val height = latest.comic.pageHeight
                            val panel = panelOf(balloon)
                            val point = viewport().toPage(start)
                            val tail = balloon.tailTip(panel, height)
                            val tailBase = balloon.tailWidthHandle(panel, height)
                            val corner = balloon.resizeHandle(panel, height)
                            val hasTail = balloon.tailLength > 0f
                            grab = when {
                                hasTail && near(viewport(), point, tail, grabRadius) -> BalloonGrab.TAIL
                                hasTail && near(viewport(), point, tailBase, grabRadius) ->
                                    BalloonGrab.TAIL_WIDTH
                                near(viewport(), point, corner, grabRadius) -> BalloonGrab.RESIZE
                                balloon.contains(point, panel, height) -> BalloonGrab.BODY
                                else -> null
                            }
                            held = balloon.takeIf { grab != null }
                            if (grab != null) actions.startBalloonGesture()
                        }
                    },
                    onDrag = { change, _ ->
                        val how = grab ?: return@detectDragGestures
                        val balloon = held ?: return@detectDragGestures
                        change.consume()
                        val point = viewport().toPage(change.position)
                        when (how) {
                            BalloonGrab.BODY -> actions.moveBalloon(balloon.id, point.x, point.y)
                            BalloonGrab.RESIZE -> actions.resizeBalloon(balloon.id, point.x, point.y)
                            BalloonGrab.TAIL -> actions.pointBalloonTail(balloon.id, point.x, point.y)
                            BalloonGrab.TAIL_WIDTH ->
                                actions.setBalloonTailWidth(balloon.id, point.x, point.y)
                        }
                    },
                    onDragEnd = {
                        if (grab != null) actions.endBalloonGesture()
                        grab = null
                        held = null
                    },
                    onDragCancel = {
                        if (grab != null) actions.endBalloonGesture()
                        grab = null
                        held = null
                    },
                )
            },
    ) {
        val viewport = comicViewport(size, comic.pageHeight, focus?.bounds)
        if (viewport.scale <= 0f || selected == null) return@Canvas
        val panel = panelOf(selected)
        val centre = selected.centreOnPage(panel, pageHeight)
        val topLeft = viewport.toScreen(centre.x - selected.width / 2f, centre.y - selected.height / 2f)
        val bottomRight = viewport.toScreen(centre.x + selected.width / 2f, centre.y + selected.height / 2f)
        drawRect(
            color = SelectionStroke,
            topLeft = topLeft,
            size = androidx.compose.ui.geometry.Size(bottomRight.x - topLeft.x, bottomRight.y - topLeft.y),
            style = Stroke(width = 3f),
        )
        drawHandle(viewport, selected.resizeHandle(panel, pageHeight))
        if (selected.tailLength > 0f) {
            drawHandle(viewport, selected.tailTip(panel, pageHeight))
            drawHandle(viewport, selected.tailWidthHandle(panel, pageHeight), small = true)
        }
    }
}

/** The values a running balloon gesture needs to keep reading as the comic changes. */
private data class BalloonInputs(
    val comic: Comic,
    val shapes: List<Polygon>,
    val focus: Polygon?,
    val selected: Balloon?,
)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHandle(
    viewport: PageViewport,
    at: PagePoint,
    small: Boolean = false,
) {    val centre = viewport.toScreen(at.x, at.y)
    val radius = if (small) 13f else 18f
    drawCircle(color = HandleFill, radius = radius, center = centre)
    drawCircle(color = SelectionStroke, radius = radius, center = centre, style = Stroke(width = 3f))
}

private fun near(viewport: PageViewport, point: PagePoint, target: PagePoint, radius: Float): Boolean {
    val a = viewport.toScreen(point.x, point.y)
    val b = viewport.toScreen(target.x, target.y)
    return (a - b).getDistance() <= radius
}

private val GRAB_RADIUS = 28.dp
private val SelectionStroke = Color(0xFF2962FF)
private val HandleFill = Color(0xCCFFFFFF)

private operator fun Offset.minus(other: Offset) = Offset(x - other.x, y - other.y)
