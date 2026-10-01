package com.ballooner.ui.comiceditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ballooner.domain.comic.Balloon
import com.ballooner.domain.comic.PagePoint
import com.ballooner.domain.comic.PageRect
import com.ballooner.domain.comic.centreOnPage
import com.ballooner.domain.comic.deleteHandle
import com.ballooner.domain.comic.editHandle
import com.ballooner.domain.comic.moveHandle
import com.ballooner.domain.comic.resizeHandle
import com.ballooner.ui.comic.PageViewport
import com.ballooner.ui.comic.TEXT_INSET
import com.ballooner.ui.comic.balloonGeometry
import com.ballooner.ui.comic.balloonTextSize
import com.ballooner.ui.comic.balloonTextStyle
import com.ballooner.ui.theme.InkBlack
import kotlin.math.roundToInt

/**
 * What the selected balloon offers: its words, typed where they will be read, and the handles
 * that move, resize, and delete it.
 *
 * Only deleting is a tap. Moving and resizing are drags, and those are read by the surface under
 * these handles, which does not move with them — a handle that carried its own drag would chase
 * the finger it was following.
 */
@Composable
internal fun BalloonEditingLayer(
    balloon: Balloon,
    panel: PageRect?,
    pageHeight: Float,
    viewport: PageViewport,
    onText: (String) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    if (viewport.scale <= 0f) return
    BalloonWords(balloon, panel, pageHeight, viewport, onText)
    BalloonHandle("Move balloon", balloon.moveHandle(panel, pageHeight), viewport) { MoveGlyph(it) }
    BalloonHandle("Resize balloon", balloon.resizeHandle(panel, pageHeight), viewport) { ResizeGlyph(it) }
    BalloonHandle(
        description = "Balloon style",
        at = balloon.editHandle(panel, pageHeight),
        viewport = viewport,
        onClick = onEdit,
    ) { tint ->
        HandleIcon(Icons.Default.Edit, tint)
    }
    BalloonHandle(
        description = "Delete balloon",
        at = balloon.deleteHandle(panel, pageHeight),
        viewport = viewport,
        onClick = onDelete,
        destructive = true,
    ) { tint ->
        HandleIcon(Icons.Default.Close, tint)
    }
}

/** The balloon's text, typed in place, in the letters and at the size it will be read in. */
@Composable
private fun BalloonWords(
    balloon: Balloon,
    panel: PageRect?,
    pageHeight: Float,
    viewport: PageViewport,
    onText: (String) -> Unit,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val geometry = balloonGeometry(balloon, panel, pageHeight, viewport)
    val width = balloon.width * viewport.scale * TEXT_INSET
    val height = balloon.height * viewport.scale * TEXT_INSET
    val onPage = balloon.centreOnPage(panel, pageHeight)
    val centre = viewport.toScreen(onPage.x, onPage.y)
    BasicTextField(
        value = balloon.text,
        onValueChange = onText,
        // The same words at the same size the page would draw them, so typing shows the result.
        textStyle = balloonTextStyle(
            balloon = balloon,
            sizePx = balloonTextSize(balloon, geometry, measurer, viewport.scale, density),
            density = density,
        ),
        cursorBrush = SolidColor(InkBlack),
        modifier = Modifier
            .offset {
                IntOffset((centre.x - width / 2f).roundToInt(), (centre.y - height / 2f).roundToInt())
            }
            .size(with(density) { width.toDp() }, with(density) { height.toDp() }),
        decorationBox = { field ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (balloon.text.isEmpty()) {
                    Text(
                        text = "Say something",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkBlack.copy(alpha = 0.35f),
                    )
                }
                field()
            }
        },
    )
}

@Composable
private fun BalloonHandle(
    description: String,
    at: PagePoint,
    viewport: PageViewport,
    onClick: (() -> Unit)? = null,
    destructive: Boolean = false,
    glyph: @Composable (Color) -> Unit,
) {
    val centre = viewport.toScreen(at.x, at.y)
    RoundHandle(
        description = description,
        modifier = Modifier.offset {
            val half = HANDLE_SIZE.toPx() / 2f
            IntOffset((centre.x - half).roundToInt(), (centre.y - half).roundToInt())
        },
        onClick = onClick,
        destructive = destructive,
        glyph = glyph,
    )
}

/** A four-way arrow: the handle that carries the balloon wherever it is wanted. */
@Composable
private fun MoveGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(BALLOON_GLYPH_SIZE)) {
        val middle = Offset(size.width / 2f, size.height / 2f)
        val arm = size.minDimension * 0.4f
        val thickness = size.minDimension * 0.1f
        drawLine(tint, middle - Offset(arm, 0f), middle + Offset(arm, 0f), thickness, StrokeCap.Round)
        drawLine(tint, middle - Offset(0f, arm), middle + Offset(0f, arm), thickness, StrokeCap.Round)
        listOf(Offset(1f, 0f), Offset(-1f, 0f), Offset(0f, 1f), Offset(0f, -1f)).forEach { way ->
            drawPath(arrowHead(middle + Offset(way.x * arm, way.y * arm), way, arm * 0.5f), tint)
        }
    }
}

/** A double-headed diagonal, pointing the way the corner it sits on can be pulled. */
@Composable
private fun ResizeGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(BALLOON_GLYPH_SIZE)) {
        val middle = Offset(size.width / 2f, size.height / 2f)
        val arm = size.minDimension * 0.34f
        val thickness = size.minDimension * 0.1f
        val way = Offset(0.707f, 0.707f)
        val reach = Offset(way.x * arm, way.y * arm)
        drawLine(tint, middle - reach, middle + reach, thickness, StrokeCap.Round)
        drawPath(arrowHead(middle + reach, way, arm * 0.62f), tint)
        drawPath(arrowHead(middle - reach, Offset(-way.x, -way.y), arm * 0.62f), tint)
    }
}

/** A filled triangle at [tip], pointing along [way]. */
private fun arrowHead(tip: Offset, way: Offset, length: Float): Path {
    val back = Offset(tip.x - way.x * length, tip.y - way.y * length)
    val side = Offset(-way.y * length * 0.5f, way.x * length * 0.5f)
    return Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(back.x + side.x, back.y + side.y)
        lineTo(back.x - side.x, back.y - side.y)
        close()
    }
}

private val BALLOON_GLYPH_SIZE = 16.dp
