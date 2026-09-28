package com.ballooner.ui.comiceditor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ballooner.domain.comic.PageSizing
import com.ballooner.domain.comic.SQUARE_RATIO
import com.ballooner.domain.comic.TALL_RATIO
import com.ballooner.domain.comic.WIDE_RATIO
import androidx.compose.foundation.Canvas
import kotlin.math.abs

/** The smallest and largest a custom side may be, in whole units. */
internal const val MIN_UNITS = 1
internal const val MAX_UNITS = 24

private enum class ShapeTile { SQUARE, RATIO, CUSTOM, AUTO }

/**
 * The Single preset's own screen: the shape of the one panel, which for a single-panel comic is
 * the shape of the whole comic.
 */
@Composable
fun SinglePanelShapeScreen(
    sizing: PageSizing,
    panelRatio: Float,
    onChange: (PageSizing) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ratio = (sizing as? PageSizing.Ratio)?.value

    // Custom cannot be read back off the ratio alone, because a custom ratio is free to land
    // exactly on a preset and must still keep its sliders open.
    var custom by rememberSaveable { mutableStateOf(false) }
    var width by rememberSaveable { mutableIntStateOf(0) }
    var height by rememberSaveable { mutableIntStateOf(0) }
    if (width == 0 || height == 0) {
        val (w, h) = unitsFor(ratio ?: SQUARE_RATIO)
        width = w
        height = h
    }
    var upright by rememberSaveable { mutableStateOf(false) }
    if (ratio == TALL_RATIO) upright = true

    val tile = when {
        sizing is PageSizing.FromImage -> ShapeTile.AUTO
        custom -> ShapeTile.CUSTOM
        ratio == SQUARE_RATIO -> ShapeTile.SQUARE
        ratio == TALL_RATIO || ratio == WIDE_RATIO -> ShapeTile.RATIO
        else -> ShapeTile.CUSTOM
    }
    val orientedRatio = if (upright) TALL_RATIO else WIDE_RATIO

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Breadcrumb(onBack = onBack)
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
                            upright = !upright
                            val flipped = if (upright) WIDE_RATIO else TALL_RATIO
                            custom = false
                            onChange(PageSizing.Ratio(flipped))
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
                caption = "Fit image",
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

        PanelPreview(
            // The panel as it will really be: a ratio the page cannot reach is held back, and
            // the preview has to say so rather than promise the number on the slider.
            ratio = panelRatio.takeIf { tile != ShapeTile.AUTO },
        )
    }
}

/** Carries the picker's own heading forward, so the step reads as one place the user is inside. */
@Composable
private fun Breadcrumb(onBack: () -> Unit) {
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
            text = "SINGLE",
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onSurface,
        )
    }
}

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
            .size(width = 30.dp, height = 22.dp)
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
            onValueChange = { onChange(it.toInt().coerceIn(MIN_UNITS, MAX_UNITS)) },
            valueRange = MIN_UNITS.toFloat()..MAX_UNITS.toFloat(),
            steps = MAX_UNITS - MIN_UNITS - 1,
            colors = SliderDefaults.colors(
                thumbColor = scheme.primary,
                activeTrackColor = scheme.primary,
                inactiveTrackColor = scheme.surfaceContainerHighest,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
    }
}

/** The shape itself, on a drafting ground, so the numbers above are never the only feedback. */
@Composable
private fun PanelPreview(ratio: Float?) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
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
        Text(
            text = "Panel preview",
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurface,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
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
                    text = "The panel takes the shape of the image you choose next.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            } else {
                Box(
                    modifier = Modifier
                        .aspectRatio(ratio)
                        .background(scheme.surfaceContainerLowest)
                        .border(BorderStroke(3.dp, scheme.onSurface)),
                )
            }
        }
    }
}

@Composable
private fun DraftingDots(colour: Color) {
    Canvas(modifier = Modifier.fillMaxWidth().height(134.dp)) {
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
