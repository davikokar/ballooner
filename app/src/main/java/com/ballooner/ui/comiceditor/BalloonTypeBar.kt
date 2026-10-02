package com.ballooner.ui.comiceditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ballooner.domain.comic.BalloonScope
import com.ballooner.domain.comic.newBalloon
import com.ballooner.domain.model.BalloonType
import com.ballooner.ui.comic.PageViewport
import com.ballooner.ui.comic.balloonGeometry
import com.ballooner.ui.comic.drawBalloon
import com.ballooner.ui.theme.InkBlack
import com.ballooner.ui.theme.PaperWhite

/**
 * The balloons that can be added, each drawn as the balloon it makes, and at the far end the
 * choice of whether the selected balloon belongs to its panel or to the whole page.
 *
 * The buttons are the same shapes the page will get, built from the same document and drawn by
 * the same renderer, so a button cannot end up advertising something the comic does not do.
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BalloonType.entries.forEach { type ->
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
    Box(
        modifier = Modifier
            .size(SCOPE_BUTTON_SIZE)
            .clip(RoundedCornerShape(8.dp))
            .then(if (inPanel == null) Modifier else Modifier.clickable(onClick = onClick))
            .semantics {
                contentDescription = when (inPanel) {
                    true -> "Free the balloon from its panel"
                    false -> "Put the balloon in a panel"
                    null -> "Balloon scope"
                }
            },
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

@Composable
private fun BalloonTypeButton(type: BalloonType, onClick: () -> Unit) {
    val balloon = remember(type) {
        // Turned off the vertical so the tail reads as a tail at this size rather than a stem.
        newBalloon(id = 0, type = type, scope = BalloonScope.Comic)
            .copy(tailAngleDegrees = TAIL_ANGLE, tailWidth = TAIL_WIDTH)
    }
    Box(
        modifier = Modifier
            .size(BUTTON_SIZE)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Add ${type.name.lowercase()} balloon" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(GLYPH_SIZE)) {
            // Sized and placed so the body and the tail it throws both land inside the glyph.
            val scale = size.width * 0.8f / balloon.width
            val viewport = PageViewport(
                originX = size.width / 2f - balloon.centre.u * scale,
                originY = size.height * 0.42f - balloon.centre.v * scale,
                scale = scale,
            )
            drawBalloon(
                geometry = balloonGeometry(balloon, panel = null, pageHeight = 1f, viewport = viewport),
                // Only sets the outline weight here, and an icon needs a heavier line than a page.
                pageScale = size.minDimension * 7f,
                bodyColor = PaperWhite,
                outlineColor = InkBlack,
            )
        }
    }
}

/** Down and to the left, the way a balloon hangs over whoever is speaking. */
private const val TAIL_ANGLE = 135f
private const val TAIL_WIDTH = 0.45f

/** The balloon sits on a disc, so the button's own bounds are what the user sees. */
private val BUTTON_SIZE = 36.dp
private val GLYPH_SIZE = 22.dp

/** The scope toggle is a different kind of control and keeps its own plain square. */
private val SCOPE_BUTTON_SIZE = 40.dp
private val SCOPE_GLYPH_SIZE = 18.dp

/** How much room the fixed-height heading leaves under its own text. */
private val HEADING_SLACK = 8.dp
