package com.ballooner.ui.comiceditor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.ComicStyle
import com.ballooner.domain.comic.MAX_BALLOON_TEXT_SIZE
import com.ballooner.domain.comic.MIN_BALLOON_TEXT_SIZE
import com.ballooner.domain.comic.panelIndex
import com.ballooner.domain.comic.panelShapes
import com.ballooner.domain.model.BalloonFont
import com.ballooner.domain.model.BalloonType
import com.ballooner.ui.comic.ComicPage
import com.ballooner.ui.comic.PanelImageSource
import com.ballooner.ui.theme.toFontFamily

/** The comic editor: one page, three steps over it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicEditorScreen(
    state: ComicEditorUiState,
    images: PanelImageSource,
    actions: ComicEditorActions,
    modifier: Modifier = Modifier,
    onPickImage: (Int) -> Unit = {},
) {
    var showOptions by rememberSaveable { mutableStateOf(false) }
    when (state) {
        ComicEditorUiState.Loading -> Box(modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator()
        }
        is ComicEditorUiState.Content -> Column(modifier = modifier.fillMaxSize()) {
            EditorHeader(
                step = state.step,
                onSelectStep = actions::selectStep,
                onOptions = { showOptions = true },
            )
            // The workspace sits on its own ground so the chrome above it reads as a separate
            // surface rather than as the top of the canvas.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Before a preset is opened the picker is the whole step: there is nothing to
                // look at on the canvas that the cards do not already say.
                if (state.step == EditorStep.LAYOUT && state.layoutKind == null) {
                    LayoutPresetPicker(
                        active = layoutKindOf(state.comic),
                        onSelect = actions::selectLayoutKind,
                    )
                    return@Column
                }
                // A single panel is the whole comic, so its own preview says everything the
                // canvas would.
                if (state.step == EditorStep.LAYOUT && state.layoutKind == LayoutKind.SINGLE) {
                    SinglePanelShapeScreen(
                        sizing = state.comic.sizing,
                        panelRatio = state.comic.panelRatio(),
                        image = state.comic.panels.firstOrNull()?.image,
                        images = images,
                        onChange = actions::setSizing,
                        onBack = actions::closeLayoutKind,
                    )
                    return@Column
                }
                if (state.step == EditorStep.LAYOUT && state.layoutKind == LayoutKind.STRIP) {
                    val grid = state.comic.layout.grid
                    StripLayoutScreen(
                        sizing = state.comic.sizing,
                        panelRatio = state.comic.panelRatio(),
                        horizontal = grid.rows == 1,
                        panelCount = maxOf(grid.rows, grid.columns),
                        panels = state.comic.panels.map { it.image },
                        images = images,
                        onChange = actions::setSizing,
                        onStrip = { across, count ->
                            if (across) actions.applyPreset(1, count) else actions.applyPreset(count, 1)
                        },
                        onBack = actions::closeLayoutKind,
                    )
                    return@Column
                }
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Page(state, images, actions)
                }
                // The controls scroll rather than squeezing the page, which is being edited.
                Column(
                    modifier = Modifier
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    when (state.step) {
                        EditorStep.LAYOUT -> LayoutStepControls(state, actions)
                        EditorStep.PLACEMENT -> PlacementStepControls(state, actions, onPickImage)
                        EditorStep.BALLOONS -> BalloonStepControls(state, actions)
                    }
                }
            }
        }
    }
    val warning = (state as? ComicEditorUiState.Content)?.warning
    if (warning != null) {
        LayoutChangeDialog(warning, actions::confirmLayoutChange, actions::cancelLayoutChange)
    }
    val content = state as? ComicEditorUiState.Content
    if (showOptions && content != null) {
        ComicOptionsSheet(
            style = content.comic.style,
            onChange = actions::setStyle,
            onDismiss = { showOptions = false },
        )
    }
}

/**
 * The editor's chrome: which step is being worked on, and the actions that apply whichever it is.
 *
 * It sits on its own paper surface, above the workspace rather than on it.
 */
@Composable
private fun EditorHeader(
    step: EditorStep,
    onSelectStep: (EditorStep) -> Unit,
    onOptions: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        StepSwitch(step, onSelectStep, modifier = Modifier.padding(bottom = 8.dp))
        HorizontalDivider(color = scheme.surfaceContainerHigh)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onOptions, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Options",
                    tint = scheme.onSurface,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** Comic-wide styling, kept off the controls area so it never crowds the step being worked on. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComicOptionsSheet(
    style: ComicStyle,
    onChange: (ComicStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Comic style", style = MaterialTheme.typography.headlineSmall)
            StyleControls(style, onChange)
        }
    }
}

@Composable
private fun Page(state: ComicEditorUiState.Content, images: PanelImageSource, actions: ComicEditorActions) {
    val shapes = remember(state.comic) {
        panelShapes(state.comic.layout, state.comic.pageHeight, state.comic.style)
    }
    val focus = state.focusedPanel?.let { shapes.getOrNull(it) }
    Box(modifier = Modifier.fillMaxSize()) {
        ComicPage(
            comic = state.comic,
            images = images,
            modifier = Modifier.fillMaxSize(),
            // Later steps own the images and balloons, so the Layout step shows them faded.
            imageAlpha = if (state.step == EditorStep.LAYOUT) DIMMED else 1f,
            balloonAlpha = if (state.step == EditorStep.BALLOONS) 1f else DIMMED,
            focus = focus,
        )
        if (state.step == EditorStep.LAYOUT) {
            LayoutStepOverlay(
                comic = state.comic,
                tool = state.tool,
                selection = state.selection,
                actions = actions,
            )
        }
        if (state.step == EditorStep.PLACEMENT) {
            PlacementStepOverlay(
                comic = state.comic,
                activePanel = state.activePanel,
                focus = focus,
                images = images,
                actions = actions,
            )
        }
        if (state.step == EditorStep.BALLOONS) {
            BalloonStepOverlay(
                comic = state.comic,
                selectedBalloon = state.selectedBalloon,
                focus = focus,
                actions = actions,
            )
        }
        if (state.focusedPanel != null) {
            FocusNavigation(actions, modifier = Modifier.fillMaxSize())
        }
    }
}

/** Edge buttons for stepping between panels, and back out, without leaving focus. */
@Composable
private fun FocusNavigation(actions: ComicEditorActions, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { actions.focusNeighbour(forward = false) },
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Text("\u2039")
        }
        OutlinedButton(
            onClick = { actions.focusNeighbour(forward = true) },
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Text("\u203a")
        }
        OutlinedButton(
            onClick = { actions.focusPanel(null) },
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Text("Show all", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun StepSwitch(step: EditorStep, onSelect: (EditorStep) -> Unit, modifier: Modifier = Modifier) {
    PillSwitch(
        options = EditorStep.entries,
        selected = step,
        label = { index, entry -> "${index + 1}. ${entry.label}" },
        onSelect = onSelect,
        modifier = modifier,
    )
}

/** A flat segmented pill: the active segment is lifted out in paper, not filled with colour. */
@Composable
private fun <T> PillSwitch(
    options: List<T>,
    selected: T,
    label: (Int, T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(scheme.surfaceContainer)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { index, entry ->
            val active = entry == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (active) scheme.surfaceContainerLowest else Color.Transparent)
                    .clickable { onSelect(entry) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(index, entry),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) scheme.primary else scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LayoutStepControls(state: ComicEditorUiState.Content, actions: ComicEditorActions) {
    val kind = state.layoutKind ?: return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextButton(onClick = actions::closeLayoutKind) { Text("Presets") }
            Text("/", style = MaterialTheme.typography.bodyMedium)
            Text(kind.label, style = MaterialTheme.typography.titleMedium)
        }
        MultiPanelControls(state, actions)
        OutlinedButton(onClick = actions::undo, enabled = state.canUndo) { Text("Undo") }
    }
}

@Composable
private fun MultiPanelControls(state: ComicEditorUiState.Content, actions: ComicEditorActions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PillSwitch(
            options = LayoutTool.entries,
            selected = state.tool,
            label = { _, entry -> entry.label },
            onSelect = actions::selectTool,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LayoutPresets.forEach { preset ->
                OutlinedButton(onClick = { actions.applyPreset(preset.rows, preset.columns) }) {
                    Text(preset.label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = actions::mergeSelection, enabled = state.canMerge) { Text("Merge") }
            Button(onClick = actions::unmergeSelection, enabled = state.canUnmerge) { Text("Unmerge") }
        }
    }
}

@Composable
private fun PlacementStepControls(
    state: ComicEditorUiState.Content,
    actions: ComicEditorActions,
    onPickImage: (Int) -> Unit,
) {
    val panel = state.activePanel
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (panel == null) {
            Text("Tap a panel to place its image", style = MaterialTheme.typography.bodySmall)
            return@Column
        }
        Text("Pinch, drag, and twist to fit the image", style = MaterialTheme.typography.bodySmall)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = { onPickImage(panel) }) {
                Text(if (state.comic.panels[panel].image == null) "Add image" else "Replace")
            }
            OutlinedButton(
                onClick = { actions.focusPanel(if (state.focusedPanel == null) panel else null) },
            ) {
                Text(if (state.focusedPanel == null) "Focus" else "Show all")
            }
            OutlinedButton(
                onClick = { actions.setPanelImage(panel, null) },
                enabled = state.comic.panels[panel].image != null,
            ) {
                Text("Remove")
            }
            OutlinedButton(onClick = actions::undo, enabled = state.canUndo) { Text("Undo") }
        }
        Text("Press and hold a panel to carry its image to another", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun BalloonStepControls(state: ComicEditorUiState.Content, actions: ComicEditorActions) {
    val selected = state.comic.balloons.firstOrNull { it.id == state.selectedBalloon }
    // Focusing follows the balloon being lettered, falling back to the last panel tapped.
    val panelToFocus = selected?.panelIndex ?: state.activePanel
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BalloonType.entries.forEach { type ->
                OutlinedButton(onClick = { actions.addBalloon(type, state.activePanel ?: 0) }) {
                    Text(type.name.lowercase(), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { actions.focusPanel(if (state.focusedPanel == null) panelToFocus else null) },
                enabled = state.focusedPanel != null || panelToFocus != null,
            ) {
                Text(if (state.focusedPanel == null) "Focus" else "Show all")
            }
        }
        if (selected == null) {
            Text("Tap a balloon to edit it", style = MaterialTheme.typography.bodySmall)
            return@Column
        }
        OutlinedTextField(
            value = selected.text,
            onValueChange = { actions.setBalloonText(selected.id, it) },
            label = { Text("Text") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BalloonFont.entries.forEach { font ->
                OutlinedButton(
                    onClick = { actions.setBalloonFont(selected.id, font) },
                    enabled = font != selected.font,
                ) {
                    Text(
                        text = font.name.lowercase().replace('_', ' '),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = font.toFontFamily(),
                    )
                }
            }
        }
        StyleSlider(
            label = "Size",
            value = selected.fontSize,
            from = MIN_BALLOON_TEXT_SIZE,
            to = MAX_BALLOON_TEXT_SIZE,
        ) {
            actions.setBalloonTextSize(selected.id, it)
        }
        // Only the rounded shapes have a roundness worth changing.
        if (selected.type == BalloonType.SPEAK || selected.type == BalloonType.WHISPER) {
            StyleSlider("Shape", selected.cornerRoundness, 0f, 1f) {
                actions.setBalloonRoundness(selected.id, it)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { actions.toggleBalloonScope(selected.id) }) {
                Text(if (selected.panelIndex == null) "Put in panel" else "Free on page")
            }
            OutlinedButton(onClick = { actions.deleteBalloon(selected.id) }) { Text("Delete") }
            OutlinedButton(onClick = actions::undo, enabled = state.canUndo) { Text("Undo") }
        }
    }
}

@Composable
private fun StyleControls(style: ComicStyle, onChange: (ComicStyle) -> Unit) {
    Column {
        StyleSlider("Margin", style.pageMargin, 0f, 0.12f) { onChange(style.copy(pageMargin = it)) }
        StyleSlider("Gutter", style.gutter, 0f, 0.1f) { onChange(style.copy(gutter = it)) }
        StyleSlider("Border", style.borderThickness, 0f, 0.02f) { onChange(style.copy(borderThickness = it)) }
        StyleSlider("Corners", style.cornerRadius, 0f, 0.05f) { onChange(style.copy(cornerRadius = it)) }
    }
}

@Composable
private fun StyleSlider(label: String, value: Float, from: Float, to: Float, onChange: (Float) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(56.dp))
        Slider(
            value = value.coerceIn(from, to),
            onValueChange = onChange,
            valueRange = from..to,
            // Material's defaults take the track from the secondary role, which is crimson here
            // and reads as an error rather than as a measurement.
            colors = SliderDefaults.colors(
                thumbColor = scheme.primary,
                activeTrackColor = scheme.primary,
                inactiveTrackColor = scheme.surfaceContainerHighest,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LayoutChangeDialog(warning: LayoutChangeWarning, onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Change the layout?") },
        text = { Text(warning.describe()) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Change") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Keep") } },
    )
}

internal fun LayoutChangeWarning.describe(): String {
    val parts = buildList {
        if (removedImages > 0) add("$removedImages image${plural(removedImages)}")
        if (removedBalloons > 0) add("$removedBalloons balloon${plural(removedBalloons)}")
    }
    return "This removes ${parts.joinToString(" and ")}."
}

private fun plural(count: Int) = if (count == 1) "" else "s"

private val EditorStep.label: String
    get() = when (this) {
        EditorStep.LAYOUT -> "Layout"
        EditorStep.PLACEMENT -> "Images"
        EditorStep.BALLOONS -> "Balloons"
    }

private val LayoutTool.label: String
    get() = when (this) {
        LayoutTool.SELECT -> "Select"
        LayoutTool.CUT_PAGE -> "Cut page"
        LayoutTool.CUT_PANEL -> "Cut panel"
    }

private val LayoutKind.label: String
    get() = when (this) {
        LayoutKind.SINGLE -> "Single"
        LayoutKind.STRIP -> "Strip"
        LayoutKind.GRID -> "Grid"
        LayoutKind.CUSTOM -> "Custom"
    }

private data class LayoutPreset(val label: String, val rows: Int, val columns: Int)

private val LayoutPresets = listOf(
    LayoutPreset("1", 1, 1),
    LayoutPreset("1x2", 1, 2),
    LayoutPreset("1x3", 1, 3),
    LayoutPreset("2x2", 2, 2),
    LayoutPreset("3x3", 3, 3),
)

private const val DIMMED = 0.35f

/** The shape of one panel as the comic will really draw it, which is what a preview must show. */
private fun Comic.panelRatio(): Float {
    val panel = panelShapes().firstOrNull()?.bounds ?: return 1f
    return if (panel.height > 0f) panel.width / panel.height else 1f
}
