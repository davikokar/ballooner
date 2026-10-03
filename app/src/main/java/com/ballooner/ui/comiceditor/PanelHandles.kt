package com.ballooner.ui.comiceditor

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.davide.seddio.ballooner.R
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.Polygon
import com.ballooner.domain.comic.panelHandleAnchors
import com.ballooner.ui.comic.PageViewport
import com.ballooner.ui.theme.InkBlack
import kotlin.math.roundToInt

/** What a panel handle offers. Each kind carries its own glyph and its name. */
internal enum class PanelHandleKind(@StringRes val description: Int) {
    ADD_IMAGE(R.string.add_image),
    EXPAND(R.string.expand_panel),
    COLLAPSE(R.string.show_whole_page),
    REMOVE_IMAGE(R.string.remove_image),
    PREVIOUS_PANEL(R.string.previous_panel),
    NEXT_PANEL(R.string.next_panel),
}

/** One handle to put on the selected panel. */
internal data class PanelHandle(val kind: PanelHandleKind, val onClick: () -> Unit)

/** What a double tap on a panel does, which is whatever that panel's own handle would have done. */
internal enum class PanelOpening {
    /** Fill the panel, as its Add image handle would. */
    PICK_IMAGE,

    /** Fill the canvas with the panel, as its Expand handle would. */
    FOCUS,

    /** Give the whole page back, as the Collapse handle would. */
    UNFOCUS,

    /** Nothing: a comic of one panel is already the whole page. */
    NOTHING,
}

/**
 * Which of those a double tap on the panel at [index] asks for.
 *
 * [offersImages] is true only in the step that owns them, so elsewhere an empty panel is opened
 * up like any other rather than asking to be filled.
 */
internal fun panelOpening(
    comic: Comic,
    index: Int,
    focused: Boolean,
    offersImages: Boolean = true,
): PanelOpening = when {
    // Closing comes first whatever the panel holds: in the focused view it is the way back, and
    // an empty panel is offered no collapse handle to get there with.
    focused -> PanelOpening.UNFOCUS
    offersImages && comic.panels.getOrNull(index)?.image == null -> PanelOpening.PICK_IMAGE
    comic.panels.size <= 1 -> PanelOpening.NOTHING
    else -> PanelOpening.FOCUS
}

/**
 * Stepping between panels, offered only while one of them fills the canvas and there is another
 * to step to.
 */
internal fun focusHandle(
    focus: Polygon?,
    alone: Boolean,
    kind: PanelHandleKind,
    actions: ComicEditorActions,
): PanelHandle? = focus?.takeIf { !alone }?.let {
    PanelHandle(kind) { actions.focusNeighbour(forward = kind == PanelHandleKind.NEXT_PANEL) }
}

/**
 * The handles along the top and bottom of the selected panel, so what can be done to a panel is
 * offered on the panel itself. Any slot may be empty.
 *
 * A cut can leave a panel any shape at all, so they are put on the first run across the panel wide
 * enough to hold them rather than on the corners of the box around it.
 */
@Composable
internal fun PanelHandles(
    panel: Polygon,
    viewport: PageViewport,
    start: PanelHandle? = null,
    end: PanelHandle? = null,
    bottomStart: PanelHandle? = null,
    bottomEnd: PanelHandle? = null,
) {
    if (viewport.scale <= 0f) return
    val density = LocalDensity.current
    val size = with(density) { HANDLE_SIZE.toPx() } / viewport.scale
    val margin = with(density) { HANDLE_MARGIN.toPx() } / viewport.scale
    val top = remember(panel, size, margin) { panelHandleAnchors(panel, size, margin) } ?: return
    val bottom = remember(panel, size, margin) {
        panelHandleAnchors(panel, size, margin, fromTop = false)
    } ?: return
    if (start != null) HandleButton(start, viewport.toScreen(top.first.x, top.first.y))
    if (end != null) HandleButton(end, viewport.toScreen(top.second.x, top.second.y))
    if (bottomStart != null) HandleButton(bottomStart, viewport.toScreen(bottom.first.x, bottom.first.y))
    if (bottomEnd != null) HandleButton(bottomEnd, viewport.toScreen(bottom.second.x, bottom.second.y))
}

@Composable
private fun HandleButton(handle: PanelHandle, centre: Offset) {
    RoundHandle(
        description = stringResource(handle.kind.description),
        modifier = Modifier.offset {
            val half = HANDLE_SIZE.toPx() / 2f
            IntOffset((centre.x - half).roundToInt(), (centre.y - half).roundToInt())
        },
        onClick = handle.onClick,
        destructive = handle.kind == PanelHandleKind.REMOVE_IMAGE,
    ) { tint ->
        when (handle.kind) {
            PanelHandleKind.ADD_IMAGE -> HandleIcon(Icons.Default.Add, tint)
            PanelHandleKind.REMOVE_IMAGE -> HandleIcon(Icons.Default.Close, tint)
            PanelHandleKind.PREVIOUS_PANEL ->
                HandleIcon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, tint)
            PanelHandleKind.NEXT_PANEL ->
                HandleIcon(Icons.AutoMirrored.Filled.KeyboardArrowRight, tint)
            PanelHandleKind.EXPAND -> ExpandGlyph(expanded = false, tint = tint)
            PanelHandleKind.COLLAPSE -> ExpandGlyph(expanded = true, tint = tint)
        }
    }
}

/** The round, ink-bordered button every handle over the page is made of. */
@Composable
internal fun RoundHandle(
    description: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    destructive: Boolean = false,
    glyph: @Composable (tint: Color) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(HANDLE_SIZE)
            .clip(CircleShape)
            .background(if (destructive) scheme.secondary else scheme.surfaceContainerLowest)
            .border(2.dp, InkBlack, CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            // A glyph may be drawn rather than written, so the handle has to say what it is.
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        glyph(if (destructive) scheme.onSecondary else scheme.onSurface)
    }
}

/** An icon sized to sit in a [RoundHandle]. The handle itself carries the name. */
@Composable
internal fun HandleIcon(icon: ImageVector, tint: Color) {
    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(GLYPH_SIZE))
}

/** Corner brackets that open outwards to fill the canvas, and face inwards to give it back. */
@Composable
internal fun ExpandGlyph(expanded: Boolean, tint: Color) {
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

/** How wide every handle over the page is. */
internal val HANDLE_SIZE = 32.dp

/** How far a handle is kept off the edges of its panel. */
private val HANDLE_MARGIN = 6.dp

private val GLYPH_SIZE = 16.dp
