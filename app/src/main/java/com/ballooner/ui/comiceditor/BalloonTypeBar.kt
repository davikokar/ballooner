package com.ballooner.ui.comiceditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.davide.seddio.ballooner.R
import com.ballooner.domain.model.BalloonType
import com.ballooner.ui.theme.label

/**
 * The balloons that can be added, each as an icon, and at the far end the choice of whether the
 * selected balloon belongs to its panel or to the whole page.
 */
@Composable
internal fun BalloonTypeBar(
    onAdd: (BalloonType) -> Unit,
    inPanel: Boolean?,
    onToggleScope: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // The heading is a fixed height and its text does not fill it, so the bar is lifted
            // into the slack underneath rather than sitting below it twice over.
            .offset(y = -HEADING_SLACK)
            .padding(start = 12.dp, end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BALLOON_BUTTON_ORDER.forEach { type ->
            BalloonTypeButton(type = type, onClick = { onAdd(type) })
        }
        Spacer(modifier = Modifier.weight(1f))
        ScopeToggle(inPanel = inPanel, onClick = onToggleScope)
    }
}

/**
 * Whether the selected balloon is cut off by its panel or free of it. There is nothing to say
 * when no balloon is selected, so it greys out rather than disappearing.
 */
@Composable
private fun ScopeToggle(inPanel: Boolean?, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val tint = if (inPanel == null) {
        scheme.onSurfaceVariant.copy(alpha = 0.38f)
    } else {
        scheme.onSurface
    }
    val label = stringResource(
        when (inPanel) {
            true -> R.string.balloon_scope_free
            false -> R.string.balloon_scope_panel
            null -> R.string.balloon_scope
        },
    )
    Box(
        modifier = Modifier
            .size(SCOPE_BUTTON_SIZE)
            .clip(RoundedCornerShape(8.dp))
            .then(if (inPanel == null) Modifier else Modifier.clickable(onClick = onClick))
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        ScopeGlyph(inPanel = inPanel == true, tint = tint)
    }
}

/** A balloon held inside a panel's frame, or loose over its corner. */
@Composable
private fun ScopeGlyph(inPanel: Boolean, tint: Color) {
    Canvas(modifier = Modifier.size(SCOPE_GLYPH_SIZE)) {
        val thickness = size.minDimension * 0.09f
        val frame = size.minDimension * 0.78f
        drawRect(
            color = tint,
            topLeft = Offset(0f, size.height - frame),
            size = Size(frame, frame),
            style = Stroke(width = thickness),
        )
        // Inside the frame it is the panel's; hanging off the corner it is the page's.
        val centre = if (inPanel) {
            Offset(frame / 2f, size.height - frame / 2f)
        } else {
            Offset(frame, size.height - frame)
        }
        drawCircle(color = tint, radius = frame * 0.3f, center = centre)
    }
}

/** The order the buttons come in, which is not the order the types are declared in. */
private val BALLOON_BUTTON_ORDER = listOf(
    BalloonType.SPEAK,
    BalloonType.THINK,
    BalloonType.YELL,
    BalloonType.WHISPER,
    BalloonType.CAPTION,
)

@Composable
private fun BalloonTypeButton(type: BalloonType, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val label = stringResource(R.string.add_balloon, stringResource(type.label))
    Box(
        modifier = Modifier
            .size(BUTTON_SIZE)
            .clip(BUTTON_SHAPE)
            .background(scheme.surfaceContainerLowest)
            .border(1.5.dp, scheme.outlineVariant, BUTTON_SHAPE)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        BalloonGlyph(type = type, tint = scheme.onSurfaceVariant)
    }
}

/** Each balloon type as an outline on a 24-unit grid. */
@Composable
private fun BalloonGlyph(type: BalloonType, tint: Color) {
    val outline = remember(type) { PathParser().parsePathString(type.glyphPath()).toPath() }
    Canvas(modifier = Modifier.size(GLYPH_SIZE)) {
        scale(scale = size.minDimension / GLYPH_GRID, pivot = Offset.Zero) {
            val line = Stroke(
                width = GLYPH_STROKE,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = if (type == BalloonType.WHISPER) {
                    PathEffect.dashPathEffect(floatArrayOf(2.5f, 2f))
                } else {
                    null
                },
            )
            if (type == BalloonType.SPEAK || type == BalloonType.CAPTION) {
                drawPath(path = outline, color = tint)
            } else {
                drawPath(path = outline, color = tint, style = line)
            }
            if (type == BalloonType.THINK) {
                // The trail of small bubbles the thought is carried on.
                drawCircle(color = tint, radius = 1.5f, center = Offset(7.5f, 19f))
                drawCircle(color = tint, radius = 1f, center = Offset(5f, 21.5f))
            }
        }
    }
}

private fun BalloonType.glyphPath(): String = when (this) {
    BalloonType.SPEAK ->
        "M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"
    BalloonType.THINK ->
        "M18.5 7.5c-.3-2.5-2.5-4.5-5.2-4.5-2.2 0-4.1 1.3-4.8 3.2-.5-.1-1-.2-1.5-.2-2.8 0-5 2.2-5 5 " +
            "0 2.4 1.7 4.4 4 4.9.2.8.7 1.5 1.4 2 .4.3.9.5 1.4.5h8.2c2.5 0 4.5-2 4.5-4.5 " +
            "0-2.2-1.6-4.1-3.7-4.4-.1-.7-.7-1.4-1.3-1.8l2-.2z"
    BalloonType.YELL ->
        "M12 2l2.5 4.5 5-2-2 5 4.5 2.5-4.5 2.5 2 5-5-2-2.5 4.5-2.5-4.5-5 2 2-5-4.5-2.5 4.5-2.5-2-5 5 2z"
    BalloonType.WHISPER ->
        "M20 14c0 1.1-.9 2-2 2H7l-4 4V6c0-1.1.9-2 2-2h13c1.1 0 2 .9 2 2v8z"
    BalloonType.CAPTION ->
        "M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zM4 12h4v2H4v-2z" +
            "m10 6H4v-2h10v2zm6 0h-4v-2h4v2zm0-4H10v-2h10v2z"
}

private const val GLYPH_GRID = 24f
private const val GLYPH_STROKE = 1.75f

/** The button is the outlined rounded square the design's tool buttons are. */
private val BUTTON_SHAPE = RoundedCornerShape(8.dp)
private val BUTTON_SIZE = 48.dp
private val GLYPH_SIZE = 24.dp

/** The scope toggle is a different kind of control and keeps its own plain square. */
private val SCOPE_BUTTON_SIZE = 40.dp
private val SCOPE_GLYPH_SIZE = 18.dp

/** How much room the fixed-height heading leaves under its own text. */
private val HEADING_SLACK = 8.dp
