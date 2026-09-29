package com.ballooner.ui.comiceditor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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

/** Carries the picker's own heading forward, so the step reads as one place the user is inside. */
@Composable
internal fun LayoutOptionBreadcrumb(current: String, onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = PRESET_PICKER_TITLE,
            style = MaterialTheme.typography.labelLarge,
            color = scheme.primary,
            modifier = Modifier.clickable(onClick = onBack),
        )
        Text("/", style = MaterialTheme.typography.labelLarge, color = scheme.outline)
        Text(
            text = current,
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onSurface,
        )
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
    horizontal: Boolean = true,
    emptyMessage: String = "The panel takes the shape of the image you choose next.",
) {
    val scheme = MaterialTheme.colorScheme
    val anyImage = panels.any { it != null && images.bitmapFor(it.sourceUri) != null }
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
            if (anyImage) {
                Text(
                    text = "Pinch and drag to look around",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.outline,
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
            if (ratio == null) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            } else {
                PanelStrip(ratio, panels, images, horizontal)
            }
        }
    }
}

/** The panels themselves, laid out the way the strip runs. One panel is just a strip of one. */
@Composable
private fun PanelStrip(
    ratio: Float,
    panels: List<PanelImage?>,
    images: PanelImageSource,
    horizontal: Boolean,
) {
    val count = panels.size.coerceAtLeast(1)
    val gap = if (count > 1) 4.dp else 0.dp
    val frames: @Composable (Modifier) -> Unit = { frameModifier ->
        panels.forEachIndexed { index, placement ->
            PanelFrame(
                ratio = ratio,
                placement = placement,
                images = images,
                single = count == 1,
                modifier = frameModifier,
                key = index,
            )
        }
    }
    // The whole strip is sized so it fits the ground, and each panel divides it evenly.
    val stripRatio = if (horizontal) ratio * count else ratio / count
    Box(modifier = Modifier.aspectRatio(stripRatio)) {
        if (horizontal) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                frames(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                frames(Modifier.weight(1f).fillMaxWidth())
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
