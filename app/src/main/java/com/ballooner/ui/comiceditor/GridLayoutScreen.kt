package com.ballooner.ui.comiceditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ballooner.domain.comic.PageSizing
import com.ballooner.domain.comic.PanelImage
import com.ballooner.ui.comic.PanelImageSource

/**
 * The fewest rows or columns a grid may have.
 *
 * A grid one cell deep is a strip, and the Strip preset is where that belongs, so a grid keeps at
 * least two of each and the kind the user chose stays the kind they get.
 */
internal const val MIN_GRID_SIDE = 2

/**
 * The Grid preset's own screen.
 *
 * A grid shapes its panels exactly as the other presets do — one reference panel, every cell the
 * same. What a grid adds is how many rows and columns it is divided into.
 */
@Composable
fun GridLayoutScreen(
    sizing: PageSizing,
    panelRatio: Float,
    rows: Int,
    columns: Int,
    panels: List<PanelImage?>,
    images: PanelImageSource,
    onChange: (PageSizing) -> Unit,
    onGrid: (rows: Int, columns: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val down = rows.coerceAtLeast(MIN_GRID_SIDE)
    val across = columns.coerceAtLeast(MIN_GRID_SIDE)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LayoutOptionBreadcrumb(current = "GRID", onBack = onBack)
        LayoutControlBar {
            Stepper(label = "Rows", value = down, min = MIN_GRID_SIDE) { onGrid(it, across) }
            Stepper(label = "Columns", value = across, min = MIN_GRID_SIDE) { onGrid(down, it) }
        }
        PanelShapeChooser(
            sizing = sizing,
            // Every cell of a grid is the same shape, so the first image decides all of them.
            autoCaption = "First image",
            onChange = onChange,
        )
        PanelPreview(
            ratio = panelRatio.takeIf { sizing !is PageSizing.FromImage || panels.firstOrNull() != null },
            panels = panels,
            images = images,
            modifier = Modifier.weight(1f),
            rows = down,
            columns = across,
            emptyMessage = "Every panel takes the shape of the first image you choose next.",
        )
    }
}
