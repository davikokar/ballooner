package com.ballooner.ui.comiceditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.ballooner.domain.comic.PageSizing
import com.ballooner.domain.comic.PanelImage
import com.ballooner.ui.comic.PanelImageSource

/** The fewest panels a strip may hold. One panel is the Single preset, not a strip. */
internal const val MIN_STRIP_PANELS = 2

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
    val count = panelCount.coerceAtLeast(MIN_STRIP_PANELS)
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LayoutOptionBreadcrumb(current = "STRIP", onBack = onBack)
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp).padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LayoutControlBar {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Orientation",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DirectionButton(across = true, active = horizontal) { onStrip(true, count) }
                        DirectionButton(across = false, active = !horizontal) { onStrip(false, count) }
                    }
                }
                Stepper(label = "Panels", value = count, min = MIN_STRIP_PANELS) {
                    onStrip(horizontal, it)
                }
            }
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
                rows = if (horizontal) 1 else count,
                columns = if (horizontal) count else 1,
                emptyMessage = "Every panel takes the shape of the first image you choose next.",
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
                    drawRect(bars, Offset(offset, 0f), Size(thickness, size.height))
                } else {
                    drawRect(bars, Offset(0f, offset), Size(size.width, thickness))
                }
            }
        }
    }
}
