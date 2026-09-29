package com.ballooner.ui.comiceditor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ballooner.domain.comic.PageSizing
import com.ballooner.domain.comic.PanelImage
import com.ballooner.ui.comic.PanelImageSource

/** How many panels a strip may hold. One panel is the Single preset, not a strip. */
internal const val MIN_STRIP_PANELS = 2
internal const val MAX_STRIP_PANELS = 6

/**
 * The Strip preset's own screen.
 *
 * A strip is a grid one cell deep, so its panels all share one shape and the same shape choices
 * the Single preset offers. What a strip adds is which way it runs and how many panels it holds.
 */
@Composable
fun StripLayoutScreen(
    sizing: PageSizing,
    panelRatio: Float,
    horizontal: Boolean,
    panelCount: Int,
    panels: List<PanelImage?>,
    images: PanelImageSource,
    onChange: (PageSizing) -> Unit,
    onStrip: (horizontal: Boolean, count: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val count = panelCount.coerceIn(MIN_STRIP_PANELS, MAX_STRIP_PANELS)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LayoutOptionBreadcrumb(current = "STRIP", onBack = onBack)
        StripControls(
            horizontal = horizontal,
            count = count,
            onDirection = { onStrip(it, count) },
            onCount = { onStrip(horizontal, it) },
        )
        PanelShapeChooser(
            sizing = sizing,
            // Every panel of a strip is the same shape, so the first image decides all of them.
            autoCaption = "First image",
            onChange = onChange,
        )
        PanelPreview(
            ratio = panelRatio.takeIf { sizing !is PageSizing.FromImage || panels.firstOrNull() != null },
            panels = panels,
            images = images,
            modifier = Modifier.weight(1f),
            horizontal = horizontal,
            emptyMessage = "Every panel takes the shape of the first image you choose next.",
        )
    }
}

@Composable
private fun StripControls(
    horizontal: Boolean,
    count: Int,
    onDirection: (Boolean) -> Unit,
    onCount: (Int) -> Unit,
) {
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
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DirectionButton(across = true, active = horizontal) { onDirection(true) }
            DirectionButton(across = false, active = !horizontal) { onDirection(false) }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Panels",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
            Stepper(
                value = count,
                min = MIN_STRIP_PANELS,
                max = MAX_STRIP_PANELS,
                onChange = onCount,
            )
        }
    }
}

/** The direction the strip runs, drawn as the arrangement it makes rather than named. */
@Composable
private fun DirectionButton(across: Boolean, active: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (active) scheme.primary else scheme.surfaceContainerLowest)
            .border(
                border = BorderStroke(1.dp, if (active) scheme.primary else scheme.outlineVariant),
                shape = RoundedCornerShape(4.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val bars = if (active) scheme.onPrimary else scheme.onSurfaceVariant
        Canvas(modifier = Modifier.size(16.dp)) {
            val gap = size.minDimension * 0.12f
            val thickness = (size.minDimension - 2 * gap) / 3f
            repeat(3) { index ->
                val offset = index * (thickness + gap)
                if (across) {
                    drawRect(
                        color = bars,
                        topLeft = Offset(offset, 0f),
                        size = Size(thickness, size.height),
                    )
                } else {
                    drawRect(
                        color = bars,
                        topLeft = Offset(0f, offset),
                        size = Size(size.width, thickness),
                    )
                }
            }
        }
    }
}

/** A whole number nudged one at a time, which takes a third of the room a row of chips would. */
@Composable
private fun Stepper(value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
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
            modifier = Modifier.width(22.dp),
        )
        StepperButton("+", enabled = value < max) { onChange(value + 1) }
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
