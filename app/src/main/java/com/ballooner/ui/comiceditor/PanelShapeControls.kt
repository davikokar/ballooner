package com.ballooner.ui.comiceditor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ballooner.domain.comic.PageRect
import com.ballooner.domain.comic.PageSizing
import com.ballooner.domain.comic.PanelImage
import com.ballooner.domain.comic.SQUARE_RATIO
import com.ballooner.domain.comic.TALL_RATIO
import com.ballooner.domain.comic.WIDE_RATIO
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
 * reference panel and the grid gives every other panel the same shape.
 */
@Composable
internal fun PanelShapeChooser(
    sizing: PageSizing,
    autoCaption: String,
    onChange: (PageSizing) -> Unit,
) {
    val ratio = (sizing as? PageSizing.Ratio)?.value

    // Custom cannot be read back off the ratio alone, because a custom ratio is free to land
    // exactly on a preset and must still keep its sliders open.
    var custom by rememberSaveable { mutableStateOf(false) }
    var width by rememberSaveable { mutableIntStateOf(unitsFor(ratio ?: SQUARE_RATIO).first) }
    var height by rememberSaveable { mutableIntStateOf(unitsFor(ratio ?: SQUARE_RATIO).second) }

    // Only consulted while some other tile is chosen; whenever the comic is one of the two, the
    // comic decides which way round the tile is showing.
    var preferUpright by rememberSaveable { mutableStateOf(false) }
    val upright = when (ratio) {
        TALL_RATIO -> true
        WIDE_RATIO -> false
        else -> preferUpright
    }

    val tile = when {
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
 * The panels drawn on a drafting ground, showing the shape as it will really be.
 *
 * [ratio] is one panel's shape; a null one means there is nothing to show yet. Any image already
 * in a panel is drawn in it, and can be pinched, dragged, and twisted — see [PreviewImage].
 */
@Composable
internal fun PanelPreview(
    ratio: Float?,
    panels: List<PanelImage?>,
    images: PanelImageSource,
    modifier: Modifier = Modifier,
    rows: Int = 1,
    columns: Int = 1,
    emptyMessage: String = "The panel takes the shape of the image you choose next.",
) {
    val anyImage = panels.any { it != null && images.bitmapFor(it.sourceUri) != null }
    PreviewSurface(
        modifier = modifier,
        hint = "Pinch and drag to look around".takeIf { anyImage },
    ) {
        if (ratio == null) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            PanelLattice(ratio, panels, images, rows, columns)
        }
    }
}

/**
 * The bordered card and drafting ground every preset's preview sits on.
 *
 * What goes on the ground differs: a lattice of frames for the shape presets, the real page for
 * the grid, which has merged panels to show.
 */
@Composable
internal fun PreviewSurface(
    modifier: Modifier = Modifier,
    hint: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Panel preview",
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(scheme.surfaceContainer)
                .border(
                    border = BorderStroke(1.dp, scheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            DraftingDots(colour = scheme.outline)
            content()
        }
    }
}

/** The panels themselves, in reading order. One panel and a strip are both just small lattices. */
@Composable
private fun PanelLattice(
    ratio: Float,
    panels: List<PanelImage?>,
    images: PanelImageSource,
    rows: Int,
    columns: Int,
) {
    val down = rows.coerceAtLeast(1)
    val across = columns.coerceAtLeast(1)
    val single = down == 1 && across == 1
    val gap = if (single) 0.dp else 4.dp
    // The whole lattice is sized so it fits the ground, and the cells divide it evenly.
    Box(modifier = Modifier.aspectRatio(ratio * across / down)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            repeat(down) { row ->
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    repeat(across) { column ->
                        val index = row * across + column
                        PanelFrame(
                            ratio = ratio,
                            placement = panels.getOrNull(index),
                            images = images,
                            single = single,
                            key = index,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelFrame(
    ratio: Float,
    placement: PanelImage?,
    images: PanelImageSource,
    single: Boolean,
    key: Int,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val bitmap = placement?.let { images.bitmapFor(it.sourceUri) }
    Box(
        modifier = modifier
            .background(scheme.surfaceContainerLowest)
            .border(BorderStroke(if (single) 3.dp else 2.dp, scheme.onSurface)),
    ) {
        if (bitmap != null) {
            PreviewImage(ratio = ratio, placement = placement, bitmap = bitmap, key = key)
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
private fun FilledTrack(fraction: Float) {
    val scheme = MaterialTheme.colorScheme
    Canvas(modifier = Modifier.fillMaxWidth().height(6.dp)) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(color = scheme.surfaceContainerHighest, cornerRadius = radius)
        drawRoundRect(
            color = scheme.primary,
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
