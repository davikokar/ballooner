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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** The Layout step's heading. The preset options continue it as a breadcrumb. */
internal const val PRESET_PICKER_TITLE = "SELECT PANEL PRESET"

/**
 * The Layout step's landing view: the four kinds of layout, each shown as the arrangement it
 * makes, with the one the comic already is marked as active.
 */
@Composable
fun LayoutPresetPicker(
    active: LayoutKind,
    onSelect: (LayoutKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = PRESET_PICKER_TITLE,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
        // Two fixed rows rather than a lazy grid: there are exactly four, and they should all be
        // on screen at once.
        LayoutKinds.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { preset ->
                    PresetCard(
                        preset = preset,
                        active = preset.kind == active,
                        onClick = { onSelect(preset.kind) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetCard(
    preset: LayoutPresetCard,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.surfaceContainerLowest)
                .border(
                    border = BorderStroke(
                        width = if (active) 3.dp else 2.dp,
                        color = if (active) scheme.primary else scheme.surfaceContainerHigh,
                    ),
                    shape = RoundedCornerShape(12.dp),
                )
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(scheme.surfaceContainerLow)
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                PresetPreview(preset.kind, active)
            }
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = preset.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (active) scheme.primary else scheme.onSurface,
                    )
                    if (active) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = scheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Text(
                    text = preset.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        if (active) {
            Text(
                text = "ACTIVE",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = (-8).dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(scheme.primary)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

/** Each kind drawn as the arrangement it makes, so the card is its own explanation. */
@Composable
private fun PresetPreview(kind: LayoutKind, active: Boolean) {
    when (kind) {
        LayoutKind.SINGLE -> PreviewCell("1", active, Modifier.fillMaxSize())
        LayoutKind.STRIP -> Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(DEFAULT_STRIP_PANELS) {
                PreviewCell("${it + 1}", active, Modifier.weight(1f).fillMaxHeight())
            }
        }
        LayoutKind.GRID -> Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(1 to 2, 3 to 4).forEach { (left, right) ->
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    PreviewCell("P$left", active, Modifier.weight(1f).fillMaxHeight())
                    PreviewCell("P$right", active, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
        LayoutKind.CUSTOM -> Column(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(4.dp),
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Create,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "SLICE & CUT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PreviewCell(label: String, active: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(scheme.surfaceContainerHighest)
            .border(
                border = BorderStroke(
                    width = 2.dp,
                    color = if (active) scheme.primary.copy(alpha = 0.4f) else scheme.onSurface.copy(alpha = 0.2f),
                ),
                shape = RoundedCornerShape(4.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) scheme.primary else scheme.onSurfaceVariant,
        )
    }
}

private data class LayoutPresetCard(
    val kind: LayoutKind,
    val title: String,
    val description: String,
)

private val LayoutKinds = listOf(
    LayoutPresetCard(LayoutKind.SINGLE, "Single", "Full splash (1 panel)"),
    LayoutPresetCard(LayoutKind.STRIP, "Strip", "Horizontal strip ($DEFAULT_STRIP_PANELS)"),
    LayoutPresetCard(
        kind = LayoutKind.GRID,
        title = "Grid",
        description = "Classic ${DEFAULT_GRID_SIDE}x$DEFAULT_GRID_SIDE " +
            "(${DEFAULT_GRID_SIDE * DEFAULT_GRID_SIDE} panels)",
    ),
    LayoutPresetCard(LayoutKind.CUSTOM, "Custom", "Asymmetric slice"),
)
