package com.ballooner.ui.comiceditor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.ComicStyle
import com.ballooner.domain.comic.GridAxis
import com.ballooner.domain.comic.GridLine
import com.ballooner.domain.comic.PageRect
import com.ballooner.domain.comic.PageSizing
import com.ballooner.domain.comic.Panel
import com.ballooner.domain.comic.PanelImage
import com.ballooner.domain.comic.SQUARE_RATIO
import com.ballooner.domain.comic.TALL_RATIO
import com.ballooner.domain.comic.WIDE_RATIO
import com.ballooner.domain.comic.boundaryPositions
import com.ballooner.domain.comic.panelStyleAt
import com.ballooner.domain.comic.transformed
import com.ballooner.ui.comic.PageViewport
import com.ballooner.ui.comic.PanelImageSource
import com.ballooner.ui.comic.imageDrawSpec
import kotlin.math.abs
import kotlin.math.roundToInt

/** The smallest and largest a custom side may be, in whole units. */
internal const val MIN_UNITS = 1
internal const val MAX_UNITS = 24

/**
 * The panel shape choices, shared by every preset that has a panel shape to choose.
 *
 * Every preset shapes its panels the same way, because the page takes its height from one
 * reference panel and the grid gives every other panel the same shape. [uniform] says whether
 * that still holds: panels resized by dragging a gutter are no longer one shape, so no tile is
 * true of them and none is shown active until one is chosen again.
 */
@Composable
internal fun PanelShapeChooser(
    sizing: PageSizing,
    autoCaption: String,
    onChange: (PageSizing) -> Unit,
    uniform: Boolean = true,
) {
    val ratio = (sizing as? PageSizing.Ratio)?.value

    // Custom cannot be read back off the ratio alone, because a custom ratio is free to land
    // exactly on a preset and must still keep its sliders open.
    var custom by rememberSaveable { mutableStateOf(false) }
    var width by rememberSaveable { mutableIntStateOf(customUnitsFor(ratio).first) }
    var height by rememberSaveable { mutableIntStateOf(customUnitsFor(ratio).second) }

    // Only consulted while some other tile is chosen; whenever the comic is one of the two, the
    // comic decides which way round the tile is showing.
    var preferUpright by rememberSaveable { mutableStateOf(false) }
    val upright = when (ratio) {
        TALL_RATIO -> true
        WIDE_RATIO -> false
        else -> preferUpright
    }

    val tile = when {
        !uniform -> null
        sizing is PageSizing.FromImage -> ShapeTile.AUTO
        custom -> ShapeTile.CUSTOM
        ratio == SQUARE_RATIO -> ShapeTile.SQUARE
        ratio == TALL_RATIO || ratio == WIDE_RATIO -> ShapeTile.RATIO
        else -> ShapeTile.CUSTOM
    }
    val orientedRatio = if (upright) TALL_RATIO else WIDE_RATIO

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ShapeTileCard(
                title = "Square",
                caption = "1:1",
                active = tile == ShapeTile.SQUARE,
                onClick = {
                    custom = false
                    onChange(PageSizing.Ratio(SQUARE_RATIO))
                },
                modifier = Modifier.weight(1f),
            ) { active -> RatioSwatch(1f, active) }

            ShapeTileCard(
                title = if (upright) "2:3" else "3:2",
                caption = "Classic",
                active = tile == ShapeTile.RATIO,
                onClick = {
                    custom = false
                    onChange(PageSizing.Ratio(orientedRatio))
                },
                action = {
                    RotateButton(
                        onClick = {
                            val turned = !upright
                            preferUpright = turned
                            custom = false
                            onChange(PageSizing.Ratio(if (turned) TALL_RATIO else WIDE_RATIO))
                        },
                    )
                },
                modifier = Modifier.weight(1f),
            ) { active -> RatioSwatch(orientedRatio, active) }

            ShapeTileCard(
                title = "Custom",
                caption = "$width : $height",
                active = tile == ShapeTile.CUSTOM,
                onClick = {
                    custom = true
                    onChange(PageSizing.Ratio(width.toFloat() / height))
                },
                modifier = Modifier.weight(1f),
            ) { active -> RatioSwatch(width.toFloat() / height, active, filled = true) }

            ShapeTileCard(
                title = "Auto",
                caption = autoCaption,
                active = tile == ShapeTile.AUTO,
                onClick = {
                    custom = false
                    onChange(PageSizing.FromImage)
                },
                modifier = Modifier.weight(1f),
            ) { AutoSwatch() }
        }

        if (tile == ShapeTile.CUSTOM) {
            CustomDimensions(
                width = width,
                height = height,
                onWidth = {
                    width = it
                    onChange(PageSizing.Ratio(it.toFloat() / height))
                },
                onHeight = {
                    height = it
                    onChange(PageSizing.Ratio(width.toFloat() / it))
                },
            )
        }
    }
}

/**
 * Dragging the gutter between two panels, which takes size from one and gives it to the other.
 *
 * The three parts are one gesture: [start] remembers the layout the drag began from, [move]
 * reports how far it has come since, and [end] folds the whole drag into one undo step.
 */
class PanelResize(
    val start: () -> Unit,
    val move: (line: GridLine, delta: Float) -> Unit,
    val end: () -> Unit,
)

/**
 * The panels drawn on a drafting ground, showing the shape as it will really be.
 *
 * [ratio] is one panel's shape; a null one means there is nothing to show yet. [style] is the
 * comic's own, so the Comic style controls are seen here too rather than only on the canvas, and
 * any panel carrying a frame of its own is drawn wearing it. Any image already in a panel is
 * drawn in it, and can be pinched, dragged, and twisted — see [PreviewImage].
 *
 * The weights are the grid's own, so panels that have been resized are shown at the sizes they
 * were given. A [resize] makes the gutters between them draggable.
 */
@Composable
internal fun PanelPreview(
    ratio: Float?,
    panels: List<Panel>,
    images: PanelImageSource,
    style: ComicStyle,
    modifier: Modifier = Modifier,
    rowWeights: List<Float> = listOf(1f),
    columnWeights: List<Float> = listOf(1f),
    selected: Int? = null,
    onSelect: ((Int) -> Unit)? = null,
    resize: PanelResize? = null,
    offersPanelOptions: Boolean = false,
    onPanelOptions: (() -> Unit)? = null,
    expanded: Boolean = false,
    onExpanded: ((Boolean) -> Unit)? = null,
    emptyMessage: String = "The panel takes the shape of the image you choose next.",
) {
    PreviewSurface(
        modifier = modifier,
        offersPanelOptions = offersPanelOptions,
        onPanelOptions = onPanelOptions,
        expanded = expanded,
        onExpanded = onExpanded,
    ) {
        if (ratio == null) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            PanelLattice(
                ratio = ratio,
                panels = panels,
                images = images,
                style = style,
                rowWeights = rowWeights,
                columnWeights = columnWeights,
                selected = selected,
                onSelect = onSelect,
                resize = resize,
            )
        }
    }
}

/**
 * The panel outline a preview strokes: the panel's own border weight, held to a hairline at the
 * thinnest so panels that are there to be tapped on cannot vanish at a zero border.
 */
internal fun previewBorderWidth(comic: Comic, index: Int, viewport: PageViewport): Float =
    (comic.panelStyleAt(index).borderThickness * viewport.scale).coerceAtLeast(MIN_PREVIEW_BORDER)

/** In pixels, since a preview is drawn straight onto a canvas. */
private const val MIN_PREVIEW_BORDER = 2f

/**
 * The bordered card and drafting ground every preset's preview sits on.
 *
 * What goes on the ground differs: a lattice of frames for the shape presets, the real page for
 * the grid, which has merged panels to show.
 *
 * [offersPanelOptions] says whether this preset styles panels one at a time at all; a preset
 * whose comic is a single panel does not, since styling that panel is styling the comic.
 * [onPanelOptions] opens the Panel style controls for the one panel that is selected. A null one
 * leaves the button offered but dimmed, since what is missing is a choice of panel rather than
 * the ability to style one.
 *
 * [onExpanded] gives the card over to the whole screen and takes it back again; a null one means
 * this caller has no room to give and the button is not offered. An expanded card also offers to
 * turn the page on its side, which is how a wide page is looked at on a tall screen.
 *
 * [floating] is what sits over the page — merge buttons, an undo, the row locks — rather than
 * being part of it, so it keeps its own way up when the page is turned. It is told whether the
 * page is turned, since a control tied to the page's upright layout has nothing to say then.
 */
@Composable
internal fun PreviewSurface(
    modifier: Modifier = Modifier,
    hint: String? = null,
    offersPanelOptions: Boolean = false,
    onPanelOptions: (() -> Unit)? = null,
    expanded: Boolean = false,
    onExpanded: ((Boolean) -> Unit)? = null,
    floating: @Composable BoxScope.(turned: Boolean) -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    // Turning is a way of looking at an expanded card, so putting it away lays the page flat.
    var turned by rememberSaveable(expanded) { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.surfaceContainerLowest)
            .border(
                border = BorderStroke(1.dp, scheme.outlineVariant),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onExpanded != null) {
                ExpandPreviewButton(expanded = expanded, onClick = { onExpanded(!expanded) })
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    // The strip between the two buttons does what the left one does, so the card
                    // can be opened up without aiming at something 24dp wide.
                    .then(
                        if (onExpanded == null) {
                            Modifier
                        } else {
                            Modifier.pointerInput(expanded) {
                                detectTapGestures(onDoubleTap = { onExpanded(!expanded) })
                            }
                        },
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Panels",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurface,
                )
                if (hint != null) {
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.outline,
                        textAlign = TextAlign.End,
                        // A hint too long for the row wraps under itself rather than over the label.
                        modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp),
                    )
                }
            }
            if (offersPanelOptions) PanelOptionsButton(onClick = onPanelOptions)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                // Rounded by the background rather than by a clip: a control may sit astride the
                // ground's edge, and a clip would cut it in half.
                .background(scheme.surfaceContainer, RoundedCornerShape(8.dp))
                .border(
                    border = BorderStroke(1.dp, scheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(PREVIEW_GROUND_INSET),
            contentAlignment = Alignment.Center,
        ) {
            DraftingDots(colour = scheme.outline)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (turned) Modifier.quarterTurn() else Modifier),
                contentAlignment = Alignment.Center,
                content = content,
            )
            // Over the page rather than in it, so a turned page does not take them with it.
            floating(turned)
            if (expanded) {
                RoundHandle(
                    description = if (turned) "Lay the page flat" else "Turn the page on its side",
                    onClick = { turned = !turned },
                    modifier = Modifier.align(Alignment.BottomEnd),
                ) { tint -> HandleIcon(Icons.Default.Refresh, tint) }
            }
        }
    }
}

/**
 * Lays the content out against the ground turned on its side, then turns it back to fill it.
 *
 * Only the view turns: nothing is written to the comic. A page wider than it is tall leaves most
 * of a phone screen empty, and this is how it is looked at the long way round instead.
 */
private fun Modifier.quarterTurn(): Modifier = this
    .layout { measurable, constraints ->
        val placeable = measurable.measure(
            Constraints(
                minWidth = constraints.minHeight,
                maxWidth = constraints.maxHeight,
                minHeight = constraints.minWidth,
                maxHeight = constraints.maxWidth,
            ),
        )
        // The turn is about the content's own centre, so it is placed so that centre is ours.
        layout(placeable.height, placeable.width) {
            placeable.place(
                x = (placeable.height - placeable.width) / 2,
                y = (placeable.width - placeable.height) / 2,
            )
        }
    }
    .graphicsLayer { rotationZ = 90f }

/** How far inside its own edge the drafting ground lays out what it holds. */
internal val PREVIEW_GROUND_INSET = 8.dp

/**
 * The corner brackets that give the card the whole step and hand it back, drawn as the panel
 * handle that does the same thing to the page.
 *
 * Not an `IconButton`, for the same reason [PanelOptionsButton] is not.
 */
@Composable
private fun ExpandPreviewButton(expanded: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .semantics { contentDescription = if (expanded) "Collapse panels" else "Expand panels" }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ExpandGlyph(expanded = expanded, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * The same gear the Comic style wears, because it opens the same kind of thing for one panel.
 *
 * Not an `IconButton`: that reserves a 48dp touch target however small its glyph, which would
 * push the preview down the screen by more than the gear is worth.
 */
@Composable
private fun PanelOptionsButton(onClick: (() -> Unit)?) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .padding(start = 8.dp)
            .size(24.dp)
            // On the clickable chain, not on the icon: on the icon it becomes a node of its own
            // and the button itself is left with no label and no enabled state to read.
            .semantics { contentDescription = "Panel options" }
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            tint = if (onClick != null) {
                scheme.onSurfaceVariant
            } else {
                scheme.onSurface.copy(alpha = 0.38f)
            },
            modifier = Modifier.size(16.dp),
        )
    }
}

/** The panels themselves, in reading order. One panel and a strip are both just small lattices. */
@Composable
private fun PanelLattice(
    ratio: Float,
    panels: List<Panel>,
    images: PanelImageSource,
    style: ComicStyle,
    rowWeights: List<Float>,
    columnWeights: List<Float>,
    selected: Int?,
    onSelect: ((Int) -> Unit)?,
    resize: PanelResize?,
) {
    val down = rowWeights.size.coerceAtLeast(1)
    val across = columnWeights.size.coerceAtLeast(1)
    val scheme = MaterialTheme.colorScheme
    val tracks = LatticeTracks(rowWeights, columnWeights, style.gutter)
    var held by remember { mutableStateOf<LatticeLine?>(null) }
    val latest = rememberUpdatedState(tracks)
    val grabRadius = with(LocalDensity.current) { GUTTER_GRAB_RADIUS.toPx() }

    // The whole lattice is sized so it fits the ground, and the weights divide it between cells.
    BoxWithConstraints(
        modifier = Modifier
            .aspectRatio(latticeAspect(ratio, rowWeights, columnWeights, style.gutter))
            // On the lattice rather than on an overlay: a gutter is too narrow to aim at, so the
            // grab reaches over the panels either side, and only a node above them can take a
            // touch the panels would otherwise have had.
            .then(
                if (resize == null) {
                    Modifier
                } else {
                    Modifier.gutterDrag(latest, grabRadius, resize) { held = it }
                },
            ),
    ) {
        // The lattice stands in for the page, so a page unit is its width and every style
        // distance is read at the same fraction the comic will draw it at.
        val pageWidth = maxWidth
        val gap = pageWidth * style.gutter
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            repeat(down) { row ->
                Row(
                    modifier = Modifier.weight(rowWeights.weightAt(row)),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    repeat(across) { column ->
                        val index = row * across + column
                        val panel = panels.getOrNull(index)
                        val frame = panel?.style ?: style.panelStyle
                        PanelFrame(
                            ratio = ratio,
                            placement = panel?.image,
                            images = images,
                            border = (pageWidth * frame.borderThickness).coerceAtLeast(MIN_PREVIEW_FRAME),
                            corner = pageWidth * frame.cornerRadius,
                            selected = index == selected,
                            onSelect = onSelect?.let { select -> { select(index) } },
                            key = index,
                            modifier = Modifier.weight(columnWeights.weightAt(column)).fillMaxHeight(),
                        )
                    }
                }
            }
        }
        if (resize != null) {
            GutterGrips(
                tracks = tracks,
                held = held,
                colour = scheme.primary.copy(alpha = 0.35f),
                heldColour = scheme.primary,
            )
        }
    }
}

/** A weight of nothing would collapse a cell, and Compose will not lay one out at all. */
private fun List<Float>.weightAt(index: Int): Float =
    (getOrNull(index) ?: 1f).coerceAtLeast(MIN_LATTICE_WEIGHT)

private const val MIN_LATTICE_WEIGHT = 0.01f

/**
 * The shape of the whole lattice: one page wide, over the height that holds the first cell at
 * [ratio]. That is how the page itself is sized, so uneven weights reshape the preview exactly
 * as they reshape the comic. The page margin is left out: the lattice draws panels, not a page.
 */
private fun latticeAspect(
    ratio: Float,
    rowWeights: List<Float>,
    columnWeights: List<Float>,
    gutter: Float,
): Float {
    val columnTotal = columnWeights.sum()
    val rowTotal = rowWeights.sum()
    val firstColumn = columnWeights.firstOrNull() ?: 0f
    val firstRow = rowWeights.firstOrNull() ?: 0f
    if (ratio <= 0f || firstColumn <= 0f || firstRow <= 0f) return ratio
    val available = (1f - gutter * (columnWeights.size - 1)).coerceAtLeast(0f)
    val width = available * firstColumn / columnTotal
    if (width <= 0f) return ratio
    val height = width / ratio * rowTotal / firstRow + gutter * (rowWeights.size - 1)
    return if (height > 0f) 1f / height else ratio
}

/** One draggable gutter, [at] being where it sits across the lattice in pixels. */
private data class LatticeLine(val axis: GridAxis, val index: Int, val at: Float) {
    // A strip is one row or one column, so its lines are always the grid's own.
    val line: GridLine get() = GridLine(axis, index)
}

/** What the lattice divides its space by, which is all a gutter drag needs to know. */
private data class LatticeTracks(
    val rowWeights: List<Float>,
    val columnWeights: List<Float>,
    val gutter: Float,
) {

    /** Every gutter of a lattice [width] by [height] pixels, edges excluded. */
    fun lines(width: Float, height: Float): List<LatticeLine> {
        val gap = width * gutter
        val columns = boundaryPositions(columnWeights, 0f, width, gap)
            .mapIndexed { index, at -> LatticeLine(GridAxis.COLUMN, index + 1, at) }
        val rows = boundaryPositions(rowWeights, 0f, height, gap)
            .mapIndexed { index, at -> LatticeLine(GridAxis.ROW, index + 1, at) }
        return columns + rows
    }

    fun nearest(position: Offset, width: Float, height: Float, radius: Float): LatticeLine? =
        lines(width, height)
            .minByOrNull { it.distanceTo(position) }
            ?.takeIf { it.distanceTo(position) <= radius }
}

private fun LatticeLine.distanceTo(position: Offset): Float =
    abs(at - if (axis == GridAxis.COLUMN) position.x else position.y)

/**
 * Dragging a gutter to resize the panels either side of it.
 *
 * The touch is watched on the initial pass, before the panels underneath see it, but is only
 * taken once it has travelled far enough to be a drag rather than a tap. Until then a tap still
 * selects the panel it landed on and an image can still be looked around.
 */
private fun Modifier.gutterDrag(
    tracks: State<LatticeTracks>,
    grabRadius: Float,
    resize: PanelResize,
    onHold: (LatticeLine?) -> Unit,
) = pointerInput(Unit) {
    val slop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val width = size.width.toFloat()
        val height = size.height.toFloat()
        val line = tracks.value.nearest(down.position, width, height, grabRadius)
            ?: return@awaitEachGesture
        val extent = if (line.axis == GridAxis.COLUMN) width else height
        if (extent <= 0f) return@awaitEachGesture

        var origin: Float? = null
        val from = down.position.axis(line.axis)
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            // A second finger means a pinch, which belongs to the image under it.
            if (event.changes.size > 1) break
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            val along = change.position.axis(line.axis)
            val held = origin
            if (held == null) {
                if (abs(along - from) < slop) continue
                origin = along
                onHold(line)
                resize.start()
            } else {
                resize.move(line.line, (along - held) / extent)
            }
            change.consume()
        }
        if (origin != null) {
            resize.end()
            onHold(null)
        }
    }
}

private fun Offset.axis(axis: GridAxis): Float = if (axis == GridAxis.COLUMN) x else y

/** How far from a gutter a touch may land and still be a drag of it. */
internal val GUTTER_GRAB_RADIUS = 20.dp

/**
 * The grip drawn in each gutter, which is the only sign that it can be dragged.
 *
 * It is drawn rather than laid out because a gutter is a gap between panels and has no view of
 * its own; the grip is painted over that gap and takes no touches, so the panels keep theirs.
 */
@Composable
private fun GutterGrips(
    tracks: LatticeTracks,
    held: LatticeLine?,
    colour: Color,
    heldColour: Color,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val thickness = (size.width * tracks.gutter)
            .coerceIn(GRIP_MIN_THICKNESS.toPx(), GRIP_MAX_THICKNESS.toPx())
        tracks.lines(size.width, size.height).forEach { line ->
            val across = line.axis == GridAxis.COLUMN
            val span = if (across) size.height else size.width
            val length = (span * GRIP_SHARE).coerceAtMost(GRIP_MAX_LENGTH.toPx())
            val ink = if (line.axis == held?.axis && line.index == held.index) heldColour else colour
            drawRoundRect(
                color = ink,
                topLeft = if (across) {
                    Offset(line.at - thickness / 2f, (size.height - length) / 2f)
                } else {
                    Offset((size.width - length) / 2f, line.at - thickness / 2f)
                },
                size = if (across) Size(thickness, length) else Size(length, thickness),
                cornerRadius = CornerRadius(thickness / 2f),
            )
        }
    }
}

/** A grip is as thick as the gutter it sits in, within reason, and a short stroke of its length. */
private val GRIP_MIN_THICKNESS = 2.dp
private val GRIP_MAX_THICKNESS = 4.dp
private val GRIP_MAX_LENGTH = 28.dp
private const val GRIP_SHARE = 0.2f

/** A preview frame keeps a visible edge however thin the comic's border is. */
private val MIN_PREVIEW_FRAME = 1.dp

@Composable
private fun PanelFrame(
    ratio: Float,
    placement: PanelImage?,
    images: PanelImageSource,
    border: Dp,
    corner: Dp,
    selected: Boolean,
    onSelect: (() -> Unit)?,
    key: Int,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val bitmap = placement?.let { images.bitmapFor(it.sourceUri) }
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = modifier
            .clip(shape)
            .background(scheme.surfaceContainerLowest)
            .border(BorderStroke(border, scheme.onSurface), shape)
            .then(if (onSelect != null) Modifier.clickable(onClick = onSelect) else Modifier),
    ) {
        if (bitmap != null) {
            PreviewImage(ratio = ratio, placement = placement, bitmap = bitmap, key = key)
        }
        // Drawn over the image, so the panel being styled is clear whatever is inside it.
        if (selected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(scheme.primary.copy(alpha = 0.2f), shape)
                    .border(BorderStroke(2.dp, scheme.primary), shape),
            )
        }
    }
}

/**
 * The image inside a preview frame, with pinch, drag, and twist.
 *
 * Nothing here is written back to the comic: this is a way of looking at how an image sits in a
 * shape while choosing that shape, and the Placement step remains the only place a panel image is
 * actually positioned.
 */
@Composable
private fun PreviewImage(ratio: Float, placement: PanelImage, bitmap: ImageBitmap, key: Int) {
    val imageAspect = bitmap.width.toFloat() / bitmap.height
    // Starts from where the Placement step left it, then drifts freely and is thrown away.
    var looking by remember(key, placement.sourceUri) { mutableStateOf(placement) }
    val latest = rememberUpdatedState(ratio to imageAspect)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            // Keyed on nothing: keying on what the gesture edits would tear the handler down on
            // the first move and the drag would barely travel.
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    val (currentRatio, aspect) = latest.value
                    val scale = size.height.toFloat()
                    if (scale <= 0f) return@detectTransformGestures
                    looking = looking.transformed(
                        panel = PageRect(0f, 0f, currentRatio, 1f),
                        imageAspect = aspect,
                        panX = pan.x / scale,
                        panY = pan.y / scale,
                        zoomBy = zoom,
                        rotateBy = rotation,
                    )
                }
            },
    ) {
        val spec = imageDrawSpec(
            panel = PageRect(0f, 0f, ratio, 1f),
            image = looking,
            imageAspect = imageAspect,
            viewport = PageViewport(0f, 0f, size.height),
        )
        withTransform({ rotate(spec.angleDegrees, spec.pivot) }) {
            drawImage(
                image = bitmap,
                dstOffset = IntOffset(spec.topLeft.x.roundToInt(), spec.topLeft.y.roundToInt()),
                dstSize = IntSize(spec.size.width.roundToInt(), spec.size.height.roundToInt()),
            )
        }
    }
}

@Composable
private fun DraftingDots(colour: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val step = 12.dp.toPx()
        var y = step / 2f
        while (y < size.height) {
            var x = step / 2f
            while (x < size.width) {
                drawCircle(color = colour.copy(alpha = 0.15f), radius = 1.5f, center = Offset(x, y))
                x += step
            }
            y += step
        }
    }
}

private enum class ShapeTile { SQUARE, RATIO, CUSTOM, AUTO }

@Composable
private fun ShapeTileCard(
    title: String,
    caption: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
    swatch: @Composable (Boolean) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Leaves the badge room to sit on the border rather than over the title.
                .padding(top = 7.dp)
                .height(86.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (active) scheme.primary.copy(alpha = 0.08f) else scheme.surfaceContainerLow)
                .border(
                    border = BorderStroke(2.dp, if (active) scheme.primary else scheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(8.dp),
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 5.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) scheme.primary else scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                action?.invoke()
            }
            swatch(active)
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) scheme.primary else scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (active) {
            Text(
                text = "ACTIVE",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(4.dp))
                    .background(scheme.primary)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
private fun RotateButton(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(scheme.surfaceContainerLowest)
            .border(
                border = BorderStroke(1.dp, scheme.outlineVariant),
                shape = RoundedCornerShape(percent = 50),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Turn the panel on its side",
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(11.dp),
        )
    }
}

/** A miniature of the shape itself. The numbers live in the tile, where there is room for them. */
@Composable
private fun RatioSwatch(ratio: Float, active: Boolean, filled: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val solid = filled && active
    Box(
        modifier = Modifier
            .height(26.dp)
            .aspectRatio(ratio.coerceIn(0.45f, 2.2f))
            .clip(RoundedCornerShape(3.dp))
            .background(
                when {
                    solid -> scheme.primary
                    active -> scheme.primary.copy(alpha = 0.12f)
                    else -> scheme.surfaceContainerLowest
                },
            )
            .border(
                border = BorderStroke(2.dp, if (active) scheme.primary else scheme.outlineVariant),
                shape = RoundedCornerShape(3.dp),
            ),
    )
}

@Composable
private fun AutoSwatch() {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(width = 30.dp, height = 26.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(scheme.surfaceContainerLowest)
            .border(
                border = BorderStroke(1.dp, scheme.outlineVariant),
                shape = RoundedCornerShape(3.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "AUTO",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun CustomDimensions(
    width: Int,
    height: Int,
    onWidth: (Int) -> Unit,
    onHeight: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(scheme.surfaceContainerLow)
            .border(
                border = BorderStroke(1.dp, scheme.surfaceContainerHigh),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Custom dimensions ratio",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurface,
            )
            Text(
                text = "$width : $height",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(scheme.primary)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        UnitSlider("Width (W)", width, onWidth)
        UnitSlider("Height (H)", height, onHeight)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitSlider(label: String, value: Int, onChange: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "$label:",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurface,
                )
                Text(
                    text = "$value",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.primary,
                )
            }
            Text(
                text = "$MIN_UNITS to $MAX_UNITS units",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.outline,
            )
        }
        Slider(
            value = value.toFloat(),
            // Rounding, not truncating: a snapped step lands on 15.999999 as readily as on 16,
            // and truncating it drops whole numbers out of the range.
            onValueChange = { onChange(it.roundToInt().coerceIn(MIN_UNITS, MAX_UNITS)) },
            valueRange = MIN_UNITS.toFloat()..MAX_UNITS.toFloat(),
            steps = MAX_UNITS - MIN_UNITS - 1,
            modifier = Modifier.height(20.dp),
            // No thumb at all: the filled track says where the value is, and the number above
            // says it exactly. A thumb would only cost height.
            thumb = {},
            track = { FilledTrack(fraction = (value - MIN_UNITS).toFloat() / (MAX_UNITS - MIN_UNITS)) },
        )
    }
}

/** A plain filled bar, without Material's gaps, tick marks, or end stop. */
@Composable
internal fun FilledTrack(fraction: Float, enabled: Boolean = true) {
    val scheme = MaterialTheme.colorScheme
    Canvas(modifier = Modifier.fillMaxWidth().height(6.dp)) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(color = scheme.surfaceContainerHighest, cornerRadius = radius)
        drawRoundRect(
            color = if (enabled) scheme.primary else scheme.onSurface.copy(alpha = 0.38f),
            size = Size(size.width * fraction.coerceIn(0f, 1f), size.height),
            cornerRadius = radius,
        )
    }
}

/**
 * A whole number nudged one at a time, which takes a fraction of the room a row of chips would.
 *
 * There is a floor but no ceiling: how many panels a layout holds is the user's business.
 */
@Composable
internal fun Stepper(label: String, value: Int, min: Int, onChange: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .border(BorderStroke(1.dp, scheme.outlineVariant), RoundedCornerShape(4.dp))
                .background(scheme.surfaceContainerLowest),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepperButton("\u2212", enabled = value > min) { onChange(value - 1) }
            Text(
                text = "$value",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 22.dp),
            )
            StepperButton("+", enabled = true) { onChange(value + 1) }
        }
    }
}

@Composable
private fun StepperButton(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(28.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) scheme.primary else scheme.outlineVariant,
        )
    }
}

/** The bordered strip the layout's own controls sit in, above the shape tiles. */
@Composable
internal fun LayoutControlBar(content: @Composable RowScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(scheme.surfaceContainerLow)
            .border(
                border = BorderStroke(1.dp, scheme.surfaceContainerHigh),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** An action that floats over the preview, offered only while it applies. */
@Composable
internal fun FloatingAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * The sides the Custom sliders open on: the shape the comic is already wearing when that shape is
 * one of its own, and [DEFAULT_CUSTOM_UNITS] when it is a preset or an Auto shape, which say
 * nothing about what a custom one should be.
 */
internal fun customUnitsFor(ratio: Float?): Pair<Int, Int> = when (ratio) {
    null, SQUARE_RATIO, TALL_RATIO, WIDE_RATIO -> DEFAULT_CUSTOM_UNITS
    else -> unitsFor(ratio)
}

/** Widescreen, the shape a custom panel starts from. */
internal val DEFAULT_CUSTOM_UNITS = 16 to 9

/**
 * The whole-unit pair closest to [ratio], so reopening the sliders shows the sides that made the
 * shape rather than starting from scratch.
 */
internal fun unitsFor(ratio: Float): Pair<Int, Int> {
    var best = 1 to 1
    var error = Float.MAX_VALUE
    for (h in MIN_UNITS..MAX_UNITS) {
        for (w in MIN_UNITS..MAX_UNITS) {
            val difference = abs(w.toFloat() / h - ratio)
            if (difference < error - 1e-6f) {
                error = difference
                best = w to h
            }
        }
    }
    return best
}
