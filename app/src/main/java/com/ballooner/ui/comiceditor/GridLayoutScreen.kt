package com.ballooner.ui.comiceditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.davide.seddio.ballooner.R
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.Grid
import com.ballooner.domain.comic.GridAxis
import com.ballooner.domain.comic.GridBoundary
import com.ballooner.domain.comic.GridPanel
import com.ballooner.domain.comic.PageRect
import com.ballooner.domain.comic.PageSizing
import com.ballooner.domain.comic.Span
import com.ballooner.domain.comic.contentRect
import com.ballooner.domain.comic.gridBoundaries
import com.ballooner.domain.comic.gridPanels
import com.ballooner.domain.comic.gridTracks
import com.ballooner.domain.comic.hasEvenWeights
import com.ballooner.domain.comic.isRowFree
import com.ballooner.domain.comic.panelShapes
import com.ballooner.domain.comic.panelStyleAt
import com.ballooner.ui.comic.PageViewport
import com.ballooner.ui.comic.PanelImageSource
import com.ballooner.ui.comic.imageDrawSpec
import com.ballooner.ui.comic.pageViewport
import com.ballooner.ui.comic.toPath
import com.ballooner.ui.theme.InkBlack
import kotlin.math.roundToInt
import kotlin.math.roundToInt

/**
 * The fewest rows or columns a grid may have.
 *
 * A grid one cell deep is a strip, and the Strip preset is where that belongs, so a grid keeps at
 * least two of each and the kind the user chose stays the kind they get.
 */
internal const val MIN_GRID_SIDE = 2

/**
 * The Grid preset's own screen.
 *
 * A grid shapes its panels exactly as the other presets do — one reference panel, every cell the
 * same. What a grid adds is how many rows and columns it is divided into, and the freedom to join
 * neighbouring cells into a larger panel.
 */
@Composable
fun GridLayoutScreen(
    comic: Comic,
    images: PanelImageSource,
    selection: List<Span>,
    canMerge: Boolean,
    canUnmerge: Boolean,
    onChange: (PageSizing) -> Unit,
    onGrid: (rows: Int, columns: Int) -> Unit,
    onToggleSelection: (Span) -> Unit,
    onMerge: () -> Unit,
    onUnmerge: () -> Unit,
    resize: PanelResize,
    onRowFree: (row: Int, free: Boolean) -> Unit,
    expanded: Boolean,
    onExpanded: (Boolean) -> Unit,
    onBack: () -> Unit,
    onOptions: () -> Unit,
    onPanelOptions: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val grid = comic.layout.grid
    val down = grid.rows.coerceAtLeast(MIN_GRID_SIDE)
    val across = grid.columns.coerceAtLeast(MIN_GRID_SIDE)
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!expanded) {
            LayoutOptionBreadcrumb(
                current = stringResource(R.string.preset_grid).uppercase(),
                onBack = onBack,
                onOptions = onOptions,
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp).padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!expanded) {
                LayoutControlBar {
                    Stepper(label = stringResource(R.string.rows_label), value = down, min = MIN_GRID_SIDE) {
                        onGrid(it, across)
                    }
                    Stepper(label = stringResource(R.string.columns_label), value = across, min = MIN_GRID_SIDE) {
                        onGrid(down, it)
                    }
                }
                PanelShapeChooser(
                    sizing = comic.sizing,
                    // Every cell of a grid is the same shape, so the first image decides all of them.
                    autoCaption = stringResource(R.string.first_image),
                    onChange = onChange,
                    uniform = grid.hasEvenWeights(),
                )
            }
            PreviewSurface(
                modifier = Modifier.weight(1f),
                hint = stringResource(R.string.grid_merge_hint).takeIf { selection.isEmpty() },
                offersPanelOptions = true,
                onPanelOptions = onPanelOptions,
                expanded = expanded,
                onExpanded = onExpanded,
                floating = { turned ->
                    MergeActions(
                        canMerge = canMerge,
                        canUnmerge = canUnmerge,
                        onMerge = onMerge,
                        onUnmerge = onUnmerge,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                    )
                    // A turned page has its rows running across, so a lock beside each one would
                    // be pointing at nothing.
                    if (!turned) RowLocks(comic = comic, onRowFree = onRowFree)
                },
            ) {
                GridPage(
                    comic = comic,
                    images = images,
                    selection = selection,
                    onToggle = onToggleSelection,
                    resize = resize,
                    onRowFree = onRowFree,
                )
            }
        }
    }
}

/**
 * The page as it will really be drawn, and the surface cells are chosen on.
 *
 * Images are drawn against the panels in reading order and cells are selected against the grid's
 * own spans, which is what merging works on. Dragging a gutter resizes the two cells either side
 * of it: a column line in a row that follows the grid moves in every such row, and one in a row
 * that divides its own width moves there alone. Each row carries the lock that decides which.
 */
@Composable
private fun GridPage(
    comic: Comic,
    images: PanelImageSource,
    selection: List<Span>,
    onToggle: (Span) -> Unit,
    resize: PanelResize,
    onRowFree: (row: Int, free: Boolean) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val cells = remember(comic) { gridPanels(comic.layout.grid, comic.pageHeight, comic.style) }
    val shapes = remember(comic) { panelShapes(comic.layout, comic.pageHeight, comic.style) }
    val boundaries = remember(comic) {
        gridBoundaries(comic.layout.grid, comic.pageHeight, comic.style)
    }
    val content = remember(comic) { contentRect(comic.pageHeight, comic.style) }
    val grabRadius = with(LocalDensity.current) { GUTTER_GRAB_RADIUS.toPx() }
    var held by remember { mutableStateOf<GridBoundary?>(null) }
    // A drag edits the comic on every move, so the gesture cannot be keyed on it: it would be
    // torn down on its own first step. These are read as the drag runs instead.
    val latest = rememberUpdatedState(GridPageInputs(comic.pageHeight, cells, boundaries, content))

    Canvas(
        modifier = Modifier
            .aspectRatio(1f / comic.pageHeight)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // The page fills this canvas exactly, so a page unit is its width.
                    val viewport = PageViewport(0f, 0f, size.width.toFloat())
                    val point = viewport.toPage(offset)
                    latest.value.cells.firstOrNull { it.shape.contains(point) }
                        ?.let { onToggle(it.span) }
                }
            }
            .pointerInput(Unit) {
                var line: GridBoundary? = null
                var origin = Offset.Zero
                fun viewport() = PageViewport(0f, 0f, size.width.toFloat())
                detectDragGestures(
                    onDragStart = { start ->
                        origin = start
                        line = latest.value.boundaries.nearestTo(start, viewport(), grabRadius)
                        held = line
                        if (line != null) resize.start()
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val grabbed = line ?: return@detectDragGestures
                        val box = latest.value.content
                        val extent =
                            if (grabbed.axis == GridAxis.COLUMN) box.width else box.height
                        val moved = if (grabbed.axis == GridAxis.COLUMN) {
                            change.position.x - origin.x
                        } else {
                            change.position.y - origin.y
                        }
                        val scale = viewport().scale
                        if (scale <= 0f || extent <= 0f) return@detectDragGestures
                        resize.move(grabbed.line, moved / scale / extent)
                    },
                    onDragEnd = {
                        if (line != null) resize.end()
                        line = null
                        held = null
                    },
                    onDragCancel = {
                        if (line != null) resize.end()
                        line = null
                        held = null
                    },
                )
            },
    ) {
        val viewport = PageViewport(0f, 0f, size.width)
        shapes.forEachIndexed { index, shape ->
            val frame = comic.panelStyleAt(index)
            val path = shape.toPath(viewport, frame.cornerRadius)
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
            drawPath(path, color = InkBlack, style = Stroke(width = previewBorderWidth(comic, index, viewport)))
        }
        cells.filter { it.span in selection }.forEach { cell ->
            val path = cell.shape.toPath(viewport, comic.style.cornerRadius)
            drawPath(path, color = scheme.primary.copy(alpha = 0.3f))
            drawPath(path, color = scheme.primary, style = Stroke(width = 8f))
        }
        boundaries.forEach { boundary ->
            drawGutterGrip(
                boundary = boundary,
                gutter = comic.style.gutter,
                viewport = viewport,
                colour = if (boundary.line == held?.line) {
                    scheme.primary
                } else {
                    scheme.primary.copy(alpha = 0.35f)
                },
            )
        }
    }
}

/**
 * The lock each row wears, which is the whole of whether that row divides its own width.
 *
 * The locks sit astride the ground's own edge, clear of the panels, so they read as something
 * said about the row rather than something drawn on it. They are placed against the same fitted
 * page the renderer uses, so they line up with their rows whatever the preview's size. A row too
 * short to hold one goes without rather than have them overlap.
 */
@Composable
private fun BoxScope.RowLocks(comic: Comic, onRowFree: (row: Int, free: Boolean) -> Unit) {
    val grid = comic.layout.grid
    val content = remember(comic) { contentRect(comic.pageHeight, comic.style) }
    val tracks = remember(comic) {
        gridTracks(grid.rowWeights, content.top, content.height, comic.style.gutter)
    }
    val density = LocalDensity.current
    val reach = with(density) { ROW_LOCK_SIZE.toPx() }
    val edge = with(density) { PREVIEW_GROUND_INSET.toPx() }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val viewport = pageViewport(
            available = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat()),
            pageHeight = comic.pageHeight,
        )
        tracks.forEachIndexed { row, track ->
            if ((track.end - track.start) * viewport.scale < reach) return@forEachIndexed
            val free = grid.isRowFree(row)
            // A merged panel covering more than one row has no rectangle once those rows stop
            // agreeing, so such a row cannot be freed until the merge is undone.
            val merged = !free && grid.spans.any { it.rowCount > 1 && row in it.firstRow..it.lastRow }
            val centre = viewport.toScreen(0f, (track.start + track.end) / 2f)
            RowLock(
                free = free,
                merged = merged,
                onClick = { onRowFree(row, !free) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    // Centred on the ground's own edge, which is one inset left of here.
                    .offset {
                        IntOffset(
                            x = (-edge - reach / 2f).roundToInt(),
                            y = (centre.y - reach / 2f).roundToInt(),
                        )
                    },
            )
        }
    }
}

@Composable
private fun RowLock(
    free: Boolean,
    merged: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val label = stringResource(
        when {
            merged -> R.string.row_locked_merged
            free -> R.string.row_align
            else -> R.string.row_free
        },
    )
    Box(
        modifier = modifier
            .size(ROW_LOCK_SIZE)
            .semantics { contentDescription = label }
            .clickable(enabled = !merged, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        LockGlyph(
            open = free,
            tint = when {
                merged -> scheme.onSurfaceVariant.copy(alpha = 0.38f)
                free -> scheme.primary
                else -> scheme.onSurfaceVariant
            },
        )
    }
}

/**
 * A padlock, drawn rather than found: material-icons-core has a closed lock but no open one, and
 * the pair is the whole point.
 */
@Composable
private fun LockGlyph(open: Boolean, tint: Color) {
    Canvas(modifier = Modifier.size(LOCK_GLYPH_SIZE)) {
        val width = size.minDimension
        val thickness = width * 0.12f
        val body = Size(width * 0.66f, width * 0.46f)
        drawRoundRect(
            color = tint,
            topLeft = Offset((size.width - body.width) / 2f, size.height - body.height),
            size = body,
            cornerRadius = CornerRadius(thickness),
        )
        // The shackle lifts clear on one side when open, which is the only difference between them.
        val radius = width * 0.22f
        val shackleX = size.width / 2f + if (open) radius else 0f
        val shackleY = size.height - body.height - if (open) width * 0.1f else 0f
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(shackleX - radius, shackleY - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = thickness, cap = StrokeCap.Round),
        )
    }
}

/** The lock is a glyph astride the ground's edge, not a handle on the page: it stays small. */
private val ROW_LOCK_SIZE = 20.dp
private val LOCK_GLYPH_SIZE = 14.dp

/** The values a running gutter drag keeps reading as the grid changes under it. */
private data class GridPageInputs(
    val pageHeight: Float,
    val cells: List<GridPanel>,
    val boundaries: List<GridBoundary>,
    val content: PageRect,
)

/**
 * The grip that says a gutter can be dragged, drawn at the middle of the stretch its line runs.
 *
 * A column line runs only across its own row, so a freed row's grips sit beside the rest rather
 * than on top of them, and a row line's grip never lands on a column line's.
 */
private fun DrawScope.drawGutterGrip(
    boundary: GridBoundary,
    gutter: Float,
    viewport: PageViewport,
    colour: Color,
) {
    val (start, end) = boundary.endsOnScreen(viewport)
    val centre = (start + end) / 2f
    val run = (end - start).getDistance()
    val across = boundary.axis == GridAxis.COLUMN
    val thickness = (gutter * viewport.scale).coerceIn(GRIP_MIN_THICKNESS, GRIP_MAX_THICKNESS)
    val length = (run * GRIP_SHARE).coerceAtMost(GRIP_MAX_LENGTH)
    val size = if (across) Size(thickness, length) else Size(length, thickness)
    drawRoundRect(
        color = colour,
        topLeft = Offset(centre.x - size.width / 2f, centre.y - size.height / 2f),
        size = size,
        cornerRadius = CornerRadius(thickness / 2f),
    )
}

/** In pixels, since a grip is drawn straight onto the page canvas. */
private const val GRIP_MIN_THICKNESS = 6f
private const val GRIP_MAX_THICKNESS = 12f
private const val GRIP_MAX_LENGTH = 84f
private const val GRIP_SHARE = 0.2f

/**
 * The panel the cell [span] is part of, by index in reading order, or null when it is not on the
 * page. A grid has no cuts, so a cell always lies wholly within one panel.
 */
internal fun panelIndexOfCell(comic: Comic, span: Span): Int? {
    val cell = gridPanels(comic.layout.grid, comic.pageHeight, comic.style)
        .firstOrNull { it.span == span } ?: return null
    val centre = cell.shape.centroid
    return panelShapes(comic.layout, comic.pageHeight, comic.style)
        .indexOfFirst { it.contains(centre) }
        .takeIf { it >= 0 }
}

/** Only offered when the selection can actually take them, so they say what is possible. */
@Composable
private fun MergeActions(
    canMerge: Boolean,
    canUnmerge: Boolean,
    onMerge: () -> Unit,
    onUnmerge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        canMerge -> FloatingAction(stringResource(R.string.merge), onMerge, modifier)
        canUnmerge -> FloatingAction(stringResource(R.string.unmerge), onUnmerge, modifier)
    }
}
