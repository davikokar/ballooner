package com.ballooner.ui.comiceditor

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.PagePoint
import com.ballooner.domain.comic.Polygon
import com.ballooner.domain.comic.panelShapes
import com.ballooner.ui.comic.PageViewport
import com.ballooner.ui.comic.PanelImageSource
import com.ballooner.ui.comic.comicViewport
import com.ballooner.ui.comic.toPath
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
    showHandles: Boolean = true,
    importingPanels: Set<Int> = emptySet(),
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
                val path = shape.toPath(viewport, comic.style.cornerRadius)
                when {
                    carrying != null && index == over && index != carrying ->
                        drawPath(path, color = DropTargetFill)
                    index == carrying -> drawPath(path, color = CarriedFill)
                    index == activePanel -> drawPath(path, color = ActiveStroke, style = Stroke(width = 6f))
                    comic.panels.getOrNull(index)?.image == null -> drawPath(path, color = EmptyHint)
                }
            }
        }
        // A panel is still waiting while its picked image is being copied in, and again while the
        // copy is being decoded, so the wait is marked on the panel the image is going into.
        if (area.width > 0 && area.height > 0) {
            val viewport = comicViewport(area.toSize(), comic.pageHeight, focus?.bounds)
            // A focused panel is drawn scaled up, which puts the others off the canvas entirely.
            Box(modifier = Modifier.matchParentSize().clipToBounds()) {
                comic.panels.forEachIndexed { index, panel ->
                    val waiting = index in importingPanels ||
                        panel.image?.sourceUri?.let { images.bitmapFor(it) == null } == true
                    val centre = shapes.getOrNull(index)?.centroid
                    if (waiting && centre != null) {
                        PanelProgress(viewport.toScreen(centre.x, centre.y))
                    }
                }
            }
        }
        // The panel being placed carries its own handles, so what can be done to it is on it
        // rather than somewhere else on the screen.
        val shape = activePanel?.let { shapes.getOrNull(it) }
        if (shape != null && showHandles && carrying == null && area.width > 0 && area.height > 0) {
            val hasImage = comic.panels.getOrNull(activePanel)?.image != null
            // One panel is already the whole page, so there is nothing to open up or step to.
            val alone = comic.panels.size <= 1
            val expand = PanelHandle(
                kind = if (focus != null) PanelHandleKind.COLLAPSE else PanelHandleKind.EXPAND,
                onClick = { actions.focusPanel(if (focus != null) null else activePanel) },
            )
            PanelHandles(
                panel = shape,
                viewport = comicViewport(area.toSize(), comic.pageHeight, focus?.bounds),
                // An empty panel can only be filled; a filled one can be opened up or emptied.
                start = when {
                    !hasImage -> PanelHandle(PanelHandleKind.ADD_IMAGE) { onPickImage(activePanel) }
                    alone -> null
                    else -> expand
                },
                end = if (hasImage) {
                    PanelHandle(PanelHandleKind.REMOVE_IMAGE) { actions.setPanelImage(activePanel, null) }
                } else {
                    null
                },
                bottomStart = focusHandle(focus, alone, PanelHandleKind.PREVIOUS_PANEL, actions),
                bottomEnd = focusHandle(focus, alone, PanelHandleKind.NEXT_PANEL, actions),
            )
        }
    }
}

/** Says that a panel's image is on its way, while it is copied in and decoded. */
@Composable
private fun PanelProgress(centre: Offset) {
    CircularProgressIndicator(
        modifier = Modifier
            .offset {
                val half = PROGRESS_SIZE.toPx() / 2f
                IntOffset((centre.x - half).roundToInt(), (centre.y - half).roundToInt())
            }
            .size(PROGRESS_SIZE)
            .semantics { contentDescription = "Loading image" },
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        strokeWidth = 4.dp,
    )
}

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
private val PROGRESS_SIZE = 40.dp
