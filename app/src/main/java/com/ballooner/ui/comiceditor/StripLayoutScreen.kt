package com.ballooner.ui.comiceditor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val counts = (MIN_STRIP_PANELS..MAX_STRIP_PANELS).toList()
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
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ChoiceRow(
            label = "Direction",
            options = listOf("Across", "Down"),
            selectedIndex = if (horizontal) 0 else 1,
            onSelect = { onDirection(it == 0) },
        )
        ChoiceRow(
            label = "Panels",
            options = counts.map { "$it" },
            selectedIndex = counts.indexOf(count).coerceAtLeast(0),
            onSelect = { onCount(counts[it]) },
        )
    }
}
