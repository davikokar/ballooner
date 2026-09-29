package com.ballooner.ui.comiceditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.PagePoint
import com.ballooner.domain.comic.PageRect
import com.ballooner.domain.comic.Polygon
import com.ballooner.domain.comic.panelShapes
import com.ballooner.ui.comic.PageViewport
import com.ballooner.ui.comic.PanelImageSource
import com.ballooner.ui.comic.comicViewport
import com.ballooner.ui.comic.toPath
import com.ballooner.ui.theme.InkBlack
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * The Placement step's editing surface: fit an image inside its panel, or press and hold to carry
 * it to another panel and trade places.
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
    onPickImage: (Int) -> Unit = {},
) {
    val shapes = remember(comic) { panelShapes(comic.layout, comic.pageHeight, comic.style) }
    var carrying by remember { mutableStateOf<Int?>(null) }
    var over by remember { mutableStateOf<Int?>(null) }
    var area by remember { mutableStateOf(IntSize.Zero) }

    // A gesture edits the comic, so keying the gesture on the comic would cancel it on its own
    // first move. These let a running gesture see current values without being restarted.
    val latest by rememberUpdatedState(PlacementInputs(comic, shapes, focus, images))

    fun panelAt(point: PagePoint) =
        latest.shapes.indexOfFirst { it.contains(point) }.takeIf { it >= 0 }

    Box(modifier = modifier.fillMaxSize().onSizeChanged { area = it }) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val viewport = comicViewport(size.toSize(), latest.comic.pageHeight, latest.focus?.bounds)
                        awaitFirstDown(requireUnconsumed = false)
                        // Nothing is consumed here: consuming the press would cancel the pinch
                        // before it began, and a tap is only a tap if no one else claimed it.
                        val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                        actions.selectPanel(panelAt(viewport.toPage(up.position)))
                    }
                }
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { start ->
                            val viewport = comicViewport(size.toSize(), latest.comic.pageHeight, latest.focus?.bounds)
                            carrying = panelAt(viewport.toPage(start))
                            over = carrying
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val viewport = comicViewport(size.toSize(), latest.comic.pageHeight, latest.focus?.bounds)
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
                .pointerInput(Unit) {
                    detectPanelTransform(
                        viewportOf = {
                            comicViewport(size.toSize(), latest.comic.pageHeight, latest.focus?.bounds)
                        },
                        panelAt = { panelAt(it) },
                        isCarrying = { carrying != null },
                        imageAspect = { index ->
                            latest.comic.panels.getOrNull(index)?.image?.sourceUri
                                ?.let { latest.images.bitmapFor(it) }
                                ?.let { it.width.toFloat() / it.height }
                        },
                        actions = actions,
                    )
                },
        ) {
            val viewport = comicViewport(size, comic.pageHeight, focus?.bounds)
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
        // The panel being placed carries its own controls, so what can be done to it is on it
        // rather than somewhere else on the screen.
        val bounds = activePanel?.let { shapes.getOrNull(it)?.bounds }
        if (bounds != null && carrying == null && area.width > 0 && area.height > 0) {
            PanelActions(
                bounds = bounds,
                viewport = comicViewport(area.toSize(), comic.pageHeight, focus?.bounds),
                hasImage = comic.panels.getOrNull(activePanel)?.image != null,
                expanded = focus != null,
                onAdd = { onPickImage(activePanel) },
                onExpand = { actions.focusPanel(if (focus != null) null else activePanel) },
                onRemove = { actions.setPanelImage(activePanel, null) },
            )
        }
    }
}

/**
 * The controls in the top corners of the panel being placed.
 *
 * What is offered depends on what is there: an empty panel can only be filled, and a filled one
 * can be opened up for closer work or emptied again. Adding sits where expanding does, because
 * both are what the panel is asking for next; emptying keeps the far corner to itself.
 */
@Composable
private fun PanelActions(
    bounds: PageRect,
    viewport: PageViewport,
    hasImage: Boolean,
    expanded: Boolean,
    onAdd: () -> Unit,
    onExpand: () -> Unit,
    onRemove: () -> Unit,
) {
    if (viewport.scale <= 0f) return
    val scheme = MaterialTheme.colorScheme
    val corner = viewport.toScreen(bounds.left, bounds.top)
    val opposite = viewport.toScreen(bounds.right, bounds.top)
    if (hasImage) {
        PanelAction(
            centre = corner,
            towardsX = 1f,
            onClick = onExpand,
            background = scheme.surfaceContainerLowest,
        ) {
            ExpandGlyph(expanded = expanded, tint = scheme.onSurface)
        }
        PanelAction(
            centre = opposite,
            towardsX = -1f,
            onClick = onRemove,
            background = scheme.secondary,
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove image",
                tint = scheme.onSecondary,
                modifier = Modifier.size(GLYPH_SIZE),
            )
        }
    } else {
        PanelAction(
            centre = corner,
            towardsX = 1f,
            onClick = onAdd,
            background = scheme.surfaceContainerLowest,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add image",
                tint = scheme.onSurface,
                modifier = Modifier.size(GLYPH_SIZE),
            )
        }
    }
}

/** One round control, sitting just inside the panel corner at [centre]. */
@Composable
private fun PanelAction(
    centre: Offset,
    towardsX: Float,
    onClick: () -> Unit,
    background: Color,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .offset {
                // Pulled inside the panel so a control on an edge panel is never half off the page.
                val inset = ACTION_INSET.toPx()
                val half = ACTION_SIZE.toPx() / 2f
                IntOffset(
                    (centre.x + towardsX * inset - half).roundToInt(),
                    (centre.y + inset - half).roundToInt(),
                )
            }
            .size(ACTION_SIZE)
            .clip(CircleShape)
            .background(background)
            .border(2.dp, InkBlack, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

/** Corner brackets that open outwards to fill the canvas, and face inwards to give it back. */
@Composable
private fun ExpandGlyph(expanded: Boolean, tint: Color) {
    Canvas(modifier = Modifier.size(GLYPH_SIZE)) {
        val thickness = size.minDimension * 0.14f
        val arm = size.minDimension * 0.32f
        val pad = thickness / 2f
        // Each corner is an L: its vertex hugs the corner to grow, and faces the middle to shrink.
        val direction = if (expanded) -1f else 1f
        listOf(0f to 0f, 1f to 0f, 0f to 1f, 1f to 1f).forEach { (atRight, atBottom) ->
            val cornerX = if (atRight == 0f) pad else size.width - pad
            val cornerY = if (atBottom == 0f) pad else size.height - pad
            val towardsX = if (atRight == 0f) arm else -arm
            val towardsY = if (atBottom == 0f) arm else -arm
            val vertexX = if (expanded) cornerX + towardsX else cornerX
            val vertexY = if (expanded) cornerY + towardsY else cornerY
            val vertex = Offset(vertexX, vertexY)
            drawLine(tint, vertex, Offset(vertexX + towardsX * direction, vertexY), thickness, StrokeCap.Round)
            drawLine(tint, vertex, Offset(vertexX, vertexY + towardsY * direction), thickness, StrokeCap.Round)
        }
    }
}

private val ACTION_SIZE = 32.dp
private val ACTION_INSET = 22.dp
private val GLYPH_SIZE = 16.dp

/** The values a running placement gesture needs to keep reading as the comic changes. */
private data class PlacementInputs(
    val comic: Comic,
    val shapes: List<Polygon>,
    val focus: Polygon?,
    val images: PanelImageSource,
)

/**
 * Pinch, drag, and twist the image in whichever panel the gesture starts over.
 *
 * Compose's own `detectTransformGestures` never returns, so it cannot say when a gesture ended,
 * and a whole pinch has to count as one undo step. This reports the start and the end as well as
 * the movement between them.
 */
private suspend fun PointerInputScope.detectPanelTransform(
    viewportOf: () -> PageViewport,
    panelAt: (PagePoint) -> Int?,
    isCarrying: () -> Boolean,
    imageAspect: (Int) -> Float?,
    actions: ComicEditorActions,
) {
    awaitEachGesture {
        val viewport = viewportOf()
        val down = awaitFirstDown(requireUnconsumed = false)
        val panel = panelAt(viewport.toPage(down.position)) ?: return@awaitEachGesture
        val aspect = imageAspect(panel) ?: return@awaitEachGesture
        if (viewport.scale <= 0f) return@awaitEachGesture

        var started = false
        var pastSlop = false
        var panSoFar = Offset.Zero
        var zoomSoFar = 1f
        var rotationSoFar = 0f

        do {
            val event = awaitPointerEvent()
            if (event.changes.any { it.isConsumed } || isCarrying()) break

            val zoom = event.calculateZoom()
            val rotation = event.calculateRotation()
            val pan = event.calculatePan()

            if (!pastSlop) {
                panSoFar += pan
                zoomSoFar *= zoom
                rotationSoFar += rotation
                val size = event.calculateCentroidSize(useCurrent = false)
                val zoomMotion = abs(1f - zoomSoFar) * size
                val rotationMotion = abs(rotationSoFar * kotlin.math.PI.toFloat() * size / 180f)
                val panMotion = panSoFar.getDistance()
                if (sqrt(zoomMotion * zoomMotion + rotationMotion * rotationMotion + panMotion * panMotion) >
                    viewConfiguration.touchSlop
                ) {
                    pastSlop = true
                }
            }

            if (pastSlop && (zoom != 1f || rotation != 0f || pan != Offset.Zero)) {
                if (!started) {
                    started = true
                    actions.selectPanel(panel)
                    actions.startPlacementGesture()
                }
                actions.transformPanelImage(
                    index = panel,
                    imageAspect = aspect,
                    // Fingers move in pixels; the document thinks in page units.
                    panX = pan.x / viewport.scale,
                    panY = pan.y / viewport.scale,
                    zoomBy = zoom,
                    rotateBy = rotation,
                )
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            }
        } while (event.changes.any { it.pressed })

        if (started) actions.endPlacementGesture(panel, aspect)
    }
}

private val ActiveStroke = Color(0xFF2962FF)
private val EmptyHint = Color(0x22000000)
private val CarriedFill = Color(0x332962FF)
private val DropTargetFill = Color(0x5500C853)
