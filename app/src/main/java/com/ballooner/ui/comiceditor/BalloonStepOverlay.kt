package com.ballooner.ui.comiceditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
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
import com.ballooner.domain.comic.moveHandle
import com.ballooner.domain.comic.panelIndex
import com.ballooner.domain.comic.panelShapes
import com.ballooner.domain.comic.resizeHandle
import com.ballooner.domain.comic.tailTip
import com.ballooner.domain.comic.tailWidthHandle
import com.ballooner.ui.comic.PageViewport
import com.ballooner.ui.comic.comicViewport

/** What a drag on a selected balloon is doing. */
private enum class BalloonGrab { BODY, MOVE, RESIZE, TAIL, TAIL_WIDTH }

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
    activePanel: Int?,
    focus: Polygon?,
    actions: ComicEditorActions,
    modifier: Modifier = Modifier,
    showHandles: Boolean = true,
    onEditBalloon: () -> Unit = {},
) {
    val shapes = remember(comic) { panelShapes(comic.layout, comic.pageHeight, comic.style) }
    val pageHeight = comic.pageHeight
    val grabRadius = with(LocalDensity.current) { GRAB_RADIUS.toPx() }
    val selected = comic.balloons.firstOrNull { it.id == selectedBalloon }
    var area by remember { mutableStateOf(IntSize.Zero) }

    fun panelOf(balloon: Balloon): PageRect? = balloon.panelIndex?.let { shapes.getOrNull(it)?.bounds }

    var grab by remember { mutableStateOf<BalloonGrab?>(null) }
    var held by remember { mutableStateOf<Balloon?>(null) }

    // Dragging a balloon edits the comic on every move, so keying the gesture on the comic would
    // cancel the drag on its own first step. This lets a running gesture read current values.
    val latest by rememberUpdatedState(BalloonInputs(comic, shapes, focus, selected))

    Box(modifier = modifier.fillMaxSize().onSizeChanged { area = it }) {
        Canvas(
            modifier = Modifier
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
                                val top = balloon.moveHandle(panel, height)
                                val hasTail = balloon.tailLength > 0f
                                // A short tail puts its two handles within a finger of each other,
                                // so the nearer one wins rather than whichever is asked first.
                                val onTail = hasTail && near(viewport(), point, tail, grabRadius)
                                val onBase = hasTail && near(viewport(), point, tailBase, grabRadius)
                                val tailIsNearer = distance(viewport(), point, tail) <=
                                    distance(viewport(), point, tailBase)
                                grab = when {
                                    onTail && (tailIsNearer || !onBase) -> BalloonGrab.TAIL
                                    onBase -> BalloonGrab.TAIL_WIDTH
                                    near(viewport(), point, corner, grabRadius) -> BalloonGrab.RESIZE
                                    near(viewport(), point, top, grabRadius) -> BalloonGrab.MOVE
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
                                // The handle rides the top edge, so the centre trails half a
                                // balloon below wherever it is dragged to.
                                BalloonGrab.MOVE ->
                                    actions.moveBalloon(balloon.id, point.x, point.y + balloon.height / 2f)
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
            // Only the tail's own handles are drawn: everything else the selected balloon offers
            // is a control of its own, sitting on the balloon.
            if (selected.tailLength > 0f) {
                drawHandle(viewport, selected.tailTip(panel, pageHeight))
                drawHandle(viewport, selected.tailWidthHandle(panel, pageHeight), small = true)
            }
        }
        // Lettering is close work, so the panel being lettered offers the same way in as the
        // Placement step does. Its images belong to that step, so nothing here changes them.
        val shape = activePanel?.let { shapes.getOrNull(it) }        // One panel is already the whole page, so there is nothing to open up or step to.
        val alone = comic.panels.size <= 1
        if (shape != null && showHandles && !alone && area.width > 0 && area.height > 0) {
            PanelHandles(
                panel = shape,
                viewport = comicViewport(area.toSize(), comic.pageHeight, focus?.bounds),
                start = PanelHandle(
                    kind = if (focus != null) PanelHandleKind.COLLAPSE else PanelHandleKind.EXPAND,
                    onClick = { actions.focusPanel(if (focus != null) null else activePanel) },
                ),
                bottomStart = focusHandle(focus, alone, PanelHandleKind.PREVIOUS_PANEL, actions),
                bottomEnd = focusHandle(focus, alone, PanelHandleKind.NEXT_PANEL, actions),
            )
        }
        if (selected != null && area.width > 0 && area.height > 0) {
            BalloonEditingLayer(
                balloon = selected,
                panel = panelOf(selected),
                pageHeight = pageHeight,
                viewport = comicViewport(area.toSize(), comic.pageHeight, focus?.bounds),
                onText = { actions.setBalloonText(selected.id, it) },
                onDelete = { actions.deleteBalloon(selected.id) },
                onEdit = onEditBalloon,
            )
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

/** Solid, so a tail handle reads as something to take hold of rather than a ring on the page. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHandle(
    viewport: PageViewport,
    at: PagePoint,
    small: Boolean = false,
) {
    val centre = viewport.toScreen(at.x, at.y)
    val radius = if (small) 13f else 18f
    drawCircle(color = SelectionStroke, radius = radius, center = centre)
    drawCircle(color = HandleRim, radius = radius, center = centre, style = Stroke(width = 3f))
}

private fun distance(viewport: PageViewport, point: PagePoint, target: PagePoint): Float {
    val a = viewport.toScreen(point.x, point.y)
    val b = viewport.toScreen(target.x, target.y)
    return (a - b).getDistance()
}

private fun near(viewport: PageViewport, point: PagePoint, target: PagePoint, radius: Float): Boolean =
    distance(viewport, point, target) <= radius

private val GRAB_RADIUS = 28.dp
private val SelectionStroke = Color(0xFF2962FF)
private val HandleRim = Color(0xCCFFFFFF)

private operator fun Offset.minus(other: Offset) = Offset(x - other.x, y - other.y)
