package com.ballooner.ui.comiceditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ballooner.domain.comic.Polygon
import com.ballooner.domain.comic.panelHandleAnchors
import com.ballooner.ui.comic.PageViewport
import com.ballooner.ui.theme.InkBlack
import kotlin.math.roundToInt

/** What a panel handle offers. Each kind carries its own glyph and its name. */
internal enum class PanelHandleKind(val description: String) {
    ADD_IMAGE("Add image"),
    EXPAND("Expand panel"),
    COLLAPSE("Show the whole page"),
    REMOVE_IMAGE("Remove image"),
}

/** One handle to put on the selected panel. */
internal data class PanelHandle(val kind: PanelHandleKind, val onClick: () -> Unit)

/**
 * The handles along the top of the selected panel, so what can be done to a panel is offered on
 * the panel itself.
 *
 * A cut can leave a panel any shape at all, so they are put on the highest run across the panel
 * wide enough to hold them rather than on the corners of the box around it.
 */
@Composable
internal fun PanelHandles(
    panel: Polygon,
    viewport: PageViewport,
    start: PanelHandle,
    end: PanelHandle? = null,
) {
    if (viewport.scale <= 0f) return
    val reach = with(LocalDensity.current) { (HANDLE_SIZE + HANDLE_MARGIN * 2).toPx() } / viewport.scale
    val anchors = remember(panel, reach) { panelHandleAnchors(panel, reach) } ?: return
    HandleButton(start, viewport.toScreen(anchors.first.x, anchors.first.y))
    if (end != null) HandleButton(end, viewport.toScreen(anchors.second.x, anchors.second.y))
}

@Composable
private fun HandleButton(handle: PanelHandle, centre: Offset) {
    val scheme = MaterialTheme.colorScheme
    val destructive = handle.kind == PanelHandleKind.REMOVE_IMAGE
    Box(
        modifier = Modifier
            .offset {
                val half = HANDLE_SIZE.toPx() / 2f
                IntOffset((centre.x - half).roundToInt(), (centre.y - half).roundToInt())
            }
            .size(HANDLE_SIZE)
            .clip(CircleShape)
            .background(if (destructive) scheme.secondary else scheme.surfaceContainerLowest)
            .border(2.dp, InkBlack, CircleShape)
            .clickable(onClick = handle.onClick)
            // A glyph may be drawn rather than written, so the handle has to say what it is.
            .semantics { contentDescription = handle.kind.description },
        contentAlignment = Alignment.Center,
    ) {
        val tint = if (destructive) scheme.onSecondary else scheme.onSurface
        when (handle.kind) {
            PanelHandleKind.ADD_IMAGE -> Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(GLYPH_SIZE),
            )
            PanelHandleKind.REMOVE_IMAGE -> Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(GLYPH_SIZE),
            )
            PanelHandleKind.EXPAND -> ExpandGlyph(expanded = false, tint = tint)
            PanelHandleKind.COLLAPSE -> ExpandGlyph(expanded = true, tint = tint)
        }
    }
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

private val HANDLE_SIZE = 32.dp

/** How far a handle is kept off the edges of its panel. */
private val HANDLE_MARGIN = 6.dp

private val GLYPH_SIZE = 16.dp
