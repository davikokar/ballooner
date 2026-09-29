package com.ballooner.ui.comiceditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.PageSizing
import com.ballooner.domain.comic.Span
import com.ballooner.domain.comic.gridPanels
import com.ballooner.domain.comic.panelShapes
import com.ballooner.ui.comic.PageViewport
import com.ballooner.ui.comic.PanelImageSource
import com.ballooner.ui.comic.imageDrawSpec
import com.ballooner.ui.comic.toPath
import com.ballooner.ui.theme.InkBlack
import kotlin.math.roundToInt

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
 * same. What a grid adds is how many rows and columns it is divided into, and the freedom to join
 * neighbouring cells into a larger panel.
 */
@Composable
fun GridLayoutScreen(
    comic: Comic,
    images: PanelImageSource,
    selection: List<Span>,
    canMerge: Boolean,
    canUnmerge: Boolean,
    onChange: (PageSizing) -> Unit,
    onGrid: (rows: Int, columns: Int) -> Unit,
    onToggleSelection: (Span) -> Unit,
    onMerge: () -> Unit,
    onUnmerge: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val grid = comic.layout.grid
    val down = grid.rows.coerceAtLeast(MIN_GRID_SIDE)
    val across = grid.columns.coerceAtLeast(MIN_GRID_SIDE)
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
            sizing = comic.sizing,
            // Every cell of a grid is the same shape, so the first image decides all of them.
            autoCaption = "First image",
            onChange = onChange,
        )
        PreviewSurface(
            modifier = Modifier.weight(1f),
            hint = "Tap cells to join them".takeIf { selection.isEmpty() },
        ) {
            GridPage(
                comic = comic,
                images = images,
                selection = selection,
                onToggle = onToggleSelection,
            )
            MergeActions(
                canMerge = canMerge,
                canUnmerge = canUnmerge,
                onMerge = onMerge,
                onUnmerge = onUnmerge,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
            )
        }
    }
}

/**
 * The page as it will really be drawn, and the surface cells are chosen on.
 *
 * Images are drawn against the panels in reading order and cells are selected against the grid's
 * own spans, which is what merging works on.
 */
@Composable
private fun GridPage(
    comic: Comic,
    images: PanelImageSource,
    selection: List<Span>,
    onToggle: (Span) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val cells = remember(comic) { gridPanels(comic.layout.grid, comic.pageHeight, comic.style) }
    val shapes = remember(comic) { panelShapes(comic.layout, comic.pageHeight, comic.style) }
    val latest = rememberUpdatedState(cells)

    Canvas(
        modifier = Modifier
            .aspectRatio(1f / comic.pageHeight)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // The page fills this canvas exactly, so a page unit is its width.
                    val viewport = PageViewport(0f, 0f, size.width.toFloat())
                    val point = viewport.toPage(offset)
                    latest.value.firstOrNull { it.shape.contains(point) }
                        ?.let { onToggle(it.span) }
                }
            },
    ) {
        val viewport = PageViewport(0f, 0f, size.width)
        shapes.forEachIndexed { index, shape ->
            val path = shape.toPath(viewport)
            val image = comic.panels.getOrNull(index)?.image
            val bitmap = image?.let { images.bitmapFor(it.sourceUri) }
            clipPath(path) {
                if (image == null || bitmap == null) {
                    drawPath(path, color = scheme.surfaceContainerLowest)
                } else {
                    val spec = imageDrawSpec(
                        panel = shape.bounds,
                        image = image,
                        imageAspect = bitmap.width.toFloat() / bitmap.height,
                        viewport = viewport,
                    )
                    withTransform({ rotate(spec.angleDegrees, spec.pivot) }) {
                        drawImage(
                            image = bitmap,
                            dstOffset = IntOffset(
                                spec.topLeft.x.roundToInt(),
                                spec.topLeft.y.roundToInt(),
                            ),
                            dstSize = IntSize(
                                spec.size.width.roundToInt(),
                                spec.size.height.roundToInt(),
                            ),
                        )
                    }
                }
            }
            drawPath(path, color = InkBlack, style = Stroke(width = 4f))
        }
        cells.filter { it.span in selection }.forEach { cell ->
            val path = cell.shape.toPath(viewport)
            drawPath(path, color = scheme.primary.copy(alpha = 0.3f))
            drawPath(path, color = scheme.primary, style = Stroke(width = 8f))
        }
    }
}

/** Only offered when the selection can actually take them, so they say what is possible. */
@Composable
private fun MergeActions(
    canMerge: Boolean,
    canUnmerge: Boolean,
    onMerge: () -> Unit,
    onUnmerge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!canMerge && !canUnmerge) return
    Box(modifier = modifier) {
        if (canMerge) {
            FloatingAction(label = "Merge", onClick = onMerge)
        } else {
            FloatingAction(label = "Unmerge", onClick = onUnmerge)
        }
    }
}

@Composable
private fun FloatingAction(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp,
            vertical = 6.dp,
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
