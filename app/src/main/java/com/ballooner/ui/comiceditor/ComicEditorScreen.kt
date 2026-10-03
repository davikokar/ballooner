package com.ballooner.ui.comiceditor

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.davide.seddio.ballooner.R
import com.ballooner.domain.comic.Balloon
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.ComicStyle
import com.ballooner.domain.comic.MAX_BALLOON_BORDER
import com.ballooner.domain.comic.MAX_BALLOON_TEXT_SIZE
import com.ballooner.domain.comic.MAX_CORNER_RADIUS
import com.ballooner.domain.comic.MIN_BALLOON_BORDER
import com.ballooner.domain.comic.MIN_BALLOON_TEXT_SIZE
import com.ballooner.domain.comic.PanelStyle
import com.ballooner.domain.comic.panelIndex
import com.ballooner.domain.comic.panelShapes
import com.ballooner.domain.comic.panelStyleAt
import com.ballooner.domain.model.BalloonFont
import com.ballooner.domain.model.BalloonType
import com.ballooner.ui.comic.ComicPage
import com.ballooner.ui.comic.PanelImageSource
import com.ballooner.ui.theme.label
import com.ballooner.ui.theme.toFontFamily
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** The comic editor: one page, three steps over it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicEditorScreen(
    state: ComicEditorUiState,
    images: PanelImageSource,
    actions: ComicEditorActions,
    modifier: Modifier = Modifier,
    onPickImage: (Int) -> Unit = {},
    onSave: () -> Unit = {},
    onShare: () -> Unit = {},
    sharing: Boolean = false,
) {
    var showOptions by rememberSaveable { mutableStateOf(false) }
    var showPanelOptions by rememberSaveable { mutableStateOf(false) }
    var showBalloonStyle by rememberSaveable { mutableStateOf(false) }
    // Saving is the end of the comic, which is the moment it is worth asking what it is called.
    var naming by rememberSaveable { mutableStateOf<String?>(null) }
    when (state) {
        ComicEditorUiState.Loading -> Box(modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator()
        }
        is ComicEditorUiState.Content -> Column(modifier = modifier.fillMaxSize()) {
            // A preset's options are a decision in progress: they are taken or dropped as a
            // whole, so going back throws them away where everywhere else simply steps back.
            val inLayoutOptions = state.step == EditorStep.LAYOUT && state.layoutKind != null
            val onPresetPicker = state.step == EditorStep.LAYOUT && state.layoutKind == null
            // An expanded preview is given the whole screen, chrome included; the preview's own
            // Collapse button is what gives it back.
            if (!state.expandedPreview) {
                EditorHeader(
                    step = state.step,
                    onSelectStep = actions::selectStep,
                    canUndo = state.canUndo,
                    // The picker changes nothing until a preset is opened, so there is nothing to undo.
                    onUndo = if (onPresetPicker) null else actions::undo,
                    onBack = when {
                        inLayoutOptions -> actions::discardLayoutKind
                        // Back into the Layout step means back to the options that were left, not to
                        // the picker: the preset has already been chosen.
                        state.step == EditorStep.PLACEMENT -> ({
                            actions.selectStep(EditorStep.LAYOUT)
                            actions.selectLayoutKind(layoutKindOf(state.comic))
                        })
                        state.step == EditorStep.BALLOONS -> ({ actions.selectStep(EditorStep.PLACEMENT) })
                        else -> null
                    },
                    onNext = when {
                        inLayoutOptions -> ({ actions.selectStep(EditorStep.PLACEMENT) })
                        // On the picker, moving on means opening the preset the comic already is.
                        state.step == EditorStep.LAYOUT ->
                            ({ actions.selectLayoutKind(layoutKindOf(state.comic)) })
                        state.step == EditorStep.PLACEMENT -> ({ actions.selectStep(EditorStep.BALLOONS) })
                        // The last step has nowhere to go but out, with the comic in hand — and
                        // a comic about to leave is one worth giving a title.
                        state.step == EditorStep.BALLOONS -> ({ naming = state.comic.name })
                        else -> null
                    },
                    nextLabel = stringResource(
                        if (state.step == EditorStep.BALLOONS) R.string.save else R.string.next,
                    ),
                    // A comic is only worth sending once it has its balloons on.
                    onShare = if (state.step == EditorStep.BALLOONS) onShare else null,
                    sharing = sharing,
                )
            }
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
                // The Panel flyout restyles one panel, so it waits until exactly one is chosen.
                val onPanelOptions = state.styledPanel()?.let { { showPanelOptions = true } }
                // A single panel is the whole comic, so its own preview says everything the
                // canvas would.
                if (state.step == EditorStep.LAYOUT && state.layoutKind == LayoutKind.SINGLE) {
                    SinglePanelShapeScreen(
                        sizing = state.comic.sizing,
                        panelRatio = state.comic.panelRatio(),
                        panel = state.comic.panels.firstOrNull(),
                        images = images,
                        style = state.comic.style,
                        expanded = state.expandedPreview,
                        onExpanded = actions::expandPreview,
                        onChange = actions::setSizing,
                        onBack = actions::discardLayoutKind,
                        onOptions = { showOptions = true },
                    )
                    return@Column
                }
                if (state.step == EditorStep.LAYOUT && state.layoutKind == LayoutKind.STRIP) {
                    StripLayoutScreen(
                        sizing = state.comic.sizing,
                        panelRatio = state.comic.panelRatio(),
                        grid = state.comic.layout.grid,
                        panels = state.comic.panels,
                        images = images,
                        style = state.comic.style,
                        selectedPanel = state.activePanel,
                        onChange = actions::setSizing,
                        onStrip = { across, count ->
                            if (across) actions.applyPreset(1, count) else actions.applyPreset(count, 1)
                        },
                        onSelectPanel = actions::selectPanel,
                        resize = remember(actions) {
                            PanelResize(
                                start = actions::startBoundaryDrag,
                                move = actions::moveBoundary,
                                end = actions::endBoundaryDrag,
                            )
                        },
                        expanded = state.expandedPreview,
                        onExpanded = actions::expandPreview,
                        onBack = actions::discardLayoutKind,
                        onOptions = { showOptions = true },
                        onPanelOptions = onPanelOptions,
                    )
                    return@Column
                }
                if (state.step == EditorStep.LAYOUT && state.layoutKind == LayoutKind.GRID) {
                    GridLayoutScreen(
                        comic = state.comic,
                        images = images,
                        selection = state.selection,
                        canMerge = state.canMerge,
                        canUnmerge = state.canUnmerge,
                        onChange = actions::setSizing,
                        onGrid = actions::applyPreset,
                        onToggleSelection = actions::toggleSelection,
                        onMerge = actions::mergeSelection,
                        onUnmerge = actions::unmergeSelection,
                        resize = remember(actions) {
                            PanelResize(
                                start = actions::startBoundaryDrag,
                                move = actions::moveBoundary,
                                end = actions::endBoundaryDrag,
                            )
                        },
                        onRowFree = actions::setRowFree,
                        expanded = state.expandedPreview,
                        onExpanded = actions::expandPreview,
                        onBack = actions::discardLayoutKind,
                        onOptions = { showOptions = true },
                        onPanelOptions = onPanelOptions,
                    )
                    return@Column
                }
                if (state.step == EditorStep.LAYOUT && state.layoutKind == LayoutKind.CUSTOM) {
                    CustomLayoutScreen(
                        comic = state.comic,
                        images = images,
                        canUndo = state.canUndo,
                        selectedPanel = state.activePanel,
                        onCut = actions::addCut,
                        onStartCutDrag = actions::startBoundaryDrag,
                        onMoveCutEnd = actions::moveCutEnd,
                        onEndCutDrag = actions::endCutDrag,
                        onSelectPanel = actions::selectPanel,
                        onUndo = actions::undo,
                        expanded = state.expandedPreview,
                        onExpanded = actions::expandPreview,
                        onBack = actions::discardLayoutKind,
                        onOptions = { showOptions = true },
                        onPanelOptions = onPanelOptions,
                    )
                    return@Column
                }
                // Hiding the handles is for looking at the panel undisturbed, so it lasts only as
                // long as the focused view it is offered in.
                var handlesHidden by remember(state.focusedPanel != null) { mutableStateOf(false) }
                val selectedBalloon = state.comic.balloons.firstOrNull { it.id == state.selectedBalloon }
                // The heading and what belongs to it are one block, so the workspace's spacing
                // falls below them rather than between them.
                Column {
                    StepTitle(
                        text = stringResource(
                            if (state.step == EditorStep.BALLOONS) BALLOON_STEP_TITLE else PLACEMENT_STEP_TITLE,
                        ),
                        handlesHidden = handlesHidden,
                        onToggleHandles = if (state.focusedPanel != null) {
                            ({ handlesHidden = !handlesHidden })
                        } else {
                            null
                        },
                    )
                    if (state.step == EditorStep.BALLOONS) {
                        BalloonTypeBar(
                            onAdd = { actions.addBalloon(it, state.activePanel) },
                            inPanel = selectedBalloon?.let { it.panelIndex != null },
                            onToggleScope = {
                                selectedBalloon?.let { actions.toggleBalloonScope(it.id) }
                            },
                        )
                    }
                }
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Page(
                        state = state,
                        images = images,
                        actions = actions,
                        onPickImage = onPickImage,
                        showHandles = !handlesHidden,
                        onEditBalloon = { showBalloonStyle = true },
                    )
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
                        // The panels and balloons carry their own handles, and the layout kinds
                        // each have a screen of their own and return above.
                        EditorStep.BALLOONS, EditorStep.PLACEMENT, EditorStep.LAYOUT -> Unit
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
    val styledPanel = content?.styledPanel()
    if (showPanelOptions && content != null && styledPanel != null) {
        PanelOptionsSheet(
            style = content.comic.panelStyleAt(styledPanel),
            onChange = { actions.setPanelStyle(styledPanel, it) },
            onDismiss = { showPanelOptions = false },
        )
    }
    val styled = content?.comic?.balloons?.firstOrNull { it.id == content.selectedBalloon }
    if (showBalloonStyle && styled != null) {
        BalloonStyleSheet(balloon = styled, actions = actions, onDismiss = { showBalloonStyle = false })
    }
    naming?.let { title ->
        ComicTitleDialog(
            title = title,
            onTitle = { naming = it },
            onSave = {
                actions.setName(title.trim())
                naming = null
                onSave()
            },
            onCancel = { naming = null },
        )
    }
}

/**
 * Asks what the comic is called, on its way out.
 *
 * The title is the name the comic is listed under and the name the exported file takes, so this
 * is the last chance to give it one before both are decided.
 */
@Composable
private fun ComicTitleDialog(
    title: String,
    onTitle: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.title_your_comic)) },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = onTitle,
                label = { Text(stringResource(R.string.title_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = title.isNotBlank()) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
    )
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
    canUndo: Boolean,
    onUndo: (() -> Unit)?,
    onBack: (() -> Unit)?,
    onNext: (() -> Unit)?,
    nextLabel: String,
    onShare: (() -> Unit)?,
    sharing: Boolean,
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
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Text(stringResource(R.string.back), style = MaterialTheme.typography.labelLarge)
                }
            }
            if (onNext != null) {
                Button(
                    onClick = onNext,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    Text(nextLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
            if (onShare != null) {
                // The spinner stands exactly where the button was, so nothing moves.
                if (sharing) {
                    Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                } else {
                    IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.share_as_png),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            // Keeps Undo at the far end whether or not the preset buttons are there.
            Spacer(modifier = Modifier.weight(1f))
            if (onUndo != null) {
                val undoLabel = stringResource(R.string.undo)
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    // The glyph is drawn, not written, so the button itself has to say what it is.
                    modifier = Modifier.size(36.dp).semantics { contentDescription = undoLabel },
                ) {
                    UndoGlyph(
                        tint = if (canUndo) scheme.onSurface else scheme.onSurface.copy(alpha = DISABLED),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/** An arrow curving back on itself: undo has no icon in the core set, so it is drawn. */
@Composable
private fun UndoGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val thickness = size.minDimension * 0.12f
        val radius = size.minDimension / 2f - thickness * 1.6f
        val centre = Offset(size.width / 2f, size.height / 2f)
        // Open at the lower left, which is where the head goes and where the arrow points.
        val from = 200f
        drawArc(
            color = tint,
            startAngle = from,
            sweepAngle = 215f,
            useCenter = false,
            topLeft = Offset(centre.x - radius, centre.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = thickness, cap = StrokeCap.Round),
        )
        val radians = Math.toRadians(from.toDouble())
        val tip = Offset(
            centre.x + radius * cos(radians).toFloat(),
            centre.y + radius * sin(radians).toFloat(),
        )
        // Back down the arc: the head points the way the arrow came from.
        val along = Offset(sin(radians).toFloat(), -cos(radians).toFloat())
        val across = Offset(-along.y, along.x)
        val length = thickness * 2.6f
        val width = thickness * 1.6f
        drawPath(
            path = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(tip.x - along.x * length + across.x * width, tip.y - along.y * length + across.y * width)
                lineTo(tip.x - along.x * length - across.x * width, tip.y - along.y * length - across.y * width)
                close()
            },
            color = tint,
        )
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
            Text(stringResource(R.string.comic_style), style = MaterialTheme.typography.headlineSmall)
            StyleControls(style, onChange)
        }
    }
}

/**
 * One panel's own frame, which overrides the comic style for that panel alone.
 *
 * Only the border and the corners are here: the gutter is the space between panels, so it can
 * never belong to one of them. What is set here lasts until the comic is restyled as a whole,
 * which has the last word and puts every panel back on the comic style.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PanelOptionsSheet(
    style: PanelStyle,
    onChange: (PanelStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.panel_style), style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stringResource(R.string.panel_style_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StyleSlider(stringResource(R.string.style_border), style.borderThickness, 0f, MAX_PANEL_BORDER) {
                onChange(style.copy(borderThickness = it))
            }
            StyleSlider(stringResource(R.string.style_corners), style.cornerRadius, 0f, MAX_CORNER_RADIUS) {
                onChange(style.copy(cornerRadius = it))
            }
        }
    }
}

/**
 * The one panel the Panel flyout styles, or null while that is not exactly one panel.
 *
 * Each preset already has a way of picking a panel out, so this reads the choice the user has
 * already made there rather than asking them to make it twice.
 */
private fun ComicEditorUiState.Content.styledPanel(): Int? = when (layoutKind) {
    // One panel is the whole comic, so styling it is styling the comic: the Options button
    // already does that, and a Panel flyout beside it would only say the same thing twice.
    LayoutKind.SINGLE -> null
    // The grid picks panels out by their cells, which is what merging already works on.
    LayoutKind.GRID -> selection.singleOrNull()?.let { panelIndexOfCell(comic, it) }
    LayoutKind.STRIP, LayoutKind.CUSTOM -> activePanel?.takeIf { it in comic.panels.indices }
    null -> null
}

@Composable
private fun Page(
    state: ComicEditorUiState.Content,
    images: PanelImageSource,
    actions: ComicEditorActions,
    onPickImage: (Int) -> Unit,
    showHandles: Boolean,
    onEditBalloon: () -> Unit,
) {
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
            // The selected balloon's words are typed into the page itself, not drawn on it.
            editingBalloon = state.selectedBalloon.takeIf { state.step == EditorStep.BALLOONS },
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
                onPickImage = onPickImage,
                showHandles = showHandles,
                importingPanels = state.importingPanels,
            )
        }
        if (state.step == EditorStep.BALLOONS) {
            BalloonStepOverlay(
                comic = state.comic,
                selectedBalloon = state.selectedBalloon,
                activePanel = state.activePanel,
                focus = focus,
                actions = actions,
                showHandles = showHandles,
                onEditBalloon = onEditBalloon,
            )
        }
    }
}

/** A step's own heading, carrying the toggle that takes the handles off a focused panel. */
@Composable
private fun StepTitle(text: String, handlesHidden: Boolean, onToggleHandles: (() -> Unit)?) {
    val handlesLabel = stringResource(
        if (handlesHidden) R.string.show_panel_handles else R.string.hide_panel_handles,
    )
    StepHeading(text) {
        // Only worth offering over a focused panel, where the handles sit on the work itself.
        if (onToggleHandles != null) {
            IconButton(
                onClick = onToggleHandles,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
                    .size(32.dp)
                    .semantics { contentDescription = handlesLabel },
            ) {
                EyeGlyph(hidden = handlesHidden, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** An eye, open or struck through: the core icon set has none. */
@Composable
private fun EyeGlyph(hidden: Boolean, tint: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val thickness = size.minDimension * 0.1f
        val middle = size.height / 2f
        val lens = Path().apply {
            moveTo(size.width * 0.06f, middle)
            quadraticTo(size.width / 2f, size.height * 0.08f, size.width * 0.94f, middle)
            quadraticTo(size.width / 2f, size.height * 0.92f, size.width * 0.06f, middle)
            close()
        }
        drawPath(lens, tint, style = Stroke(width = thickness, join = StrokeJoin.Round))
        drawCircle(tint, radius = size.minDimension * 0.14f, center = Offset(size.width / 2f, middle))
        if (hidden) {
            drawLine(
                color = tint,
                start = Offset(size.width * 0.1f, size.height * 0.9f),
                end = Offset(size.width * 0.9f, size.height * 0.1f),
                strokeWidth = thickness * 1.2f,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun StepSwitch(step: EditorStep, onSelect: (EditorStep) -> Unit, modifier: Modifier = Modifier) {
    PillSwitch(
        options = EditorStep.entries,
        selected = step,
        label = { index, entry -> stringResource(R.string.step_label, index + 1, stringResource(entry.label)) },
        onSelect = onSelect,
        modifier = modifier,
    )
}

/** A flat segmented pill: the active segment is lifted out in paper, not filled with colour. */
@Composable
private fun <T> PillSwitch(
    options: List<T>,
    selected: T,
    label: @Composable (Int, T) -> String,
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

/** How the selected balloon is lettered, kept in a sheet so it never crowds the page. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BalloonStyleSheet(
    balloon: Balloon,
    actions: ComicEditorActions,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        // The index, not the title: the title is translated, and a saved title would not survive
        // a language change.
        var tab by rememberSaveable { mutableIntStateOf(TEXT_TAB) }
        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.balloon_style), style = MaterialTheme.typography.headlineSmall)
            // The same pill the Step switch is, so a tab reads the same wherever it is.
            PillSwitch(
                options = BALLOON_STYLE_TABS,
                selected = tab,
                label = { _, title -> stringResource(title) },
                onSelect = { tab = it },
            )
            when (tab) {
                TEXT_TAB -> BalloonTextStyle(balloon, actions)
                else -> BalloonShapeStyle(balloon, actions)
            }
        }
    }
}

/** The Text tab: what the words are set in, and how big. */
@Composable
private fun BalloonTextStyle(balloon: Balloon, actions: ComicEditorActions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Named in the same column the sliders below put their labels in, so the whole tab
            // reads down one edge.
            Text(
                text = stringResource(R.string.balloon_font),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(STYLE_LABEL_WIDTH),
            )
            FontDropdown(
                font = balloon.font,
                onChange = { actions.setBalloonFont(balloon.id, it) },
                modifier = Modifier.weight(1f),
            )
            Checkbox(
                checked = balloon.autoSize,
                onCheckedChange = { actions.setBalloonAutoSize(balloon.id, it) },
                modifier = Modifier.size(32.dp),
            )
            Text(stringResource(R.string.balloon_autosize), style = MaterialTheme.typography.labelSmall)
        }
        StyleSlider(
            label = stringResource(R.string.balloon_size),
            value = balloon.fontSize,
            from = MIN_BALLOON_TEXT_SIZE,
            to = MAX_BALLOON_TEXT_SIZE,
            // The balloon is choosing for itself, so there is nothing here to choose.
            enabled = !balloon.autoSize,
        ) {
            actions.setBalloonTextSize(balloon.id, it)
        }
    }
}

/** The Balloon tab: the outline the words are drawn inside. */
@Composable
private fun BalloonShapeStyle(balloon: Balloon, actions: ComicEditorActions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Only the rounded shapes have a roundness worth changing.
        if (balloon.type == BalloonType.SPEAK || balloon.type == BalloonType.WHISPER) {
            StyleSlider(stringResource(R.string.balloon_shape), balloon.cornerRoundness, 0f, 1f) {
                actions.setBalloonRoundness(balloon.id, it)
            }
        }
        StyleSlider(
            label = stringResource(R.string.balloon_border_size),
            value = balloon.borderThickness,
            from = MIN_BALLOON_BORDER,
            to = MAX_BALLOON_BORDER,
            // The panel borders are deciding, so there is nothing here to decide.
            enabled = !balloon.matchPanelBorder,
        ) {
            actions.setBalloonBorderThickness(balloon.id, it)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Checkbox(
                checked = balloon.matchPanelBorder,
                onCheckedChange = { actions.setBalloonMatchPanelBorder(balloon.id, it) },
                modifier = Modifier.size(32.dp),
            )
            Text(stringResource(R.string.balloon_match_panel_border), style = MaterialTheme.typography.labelSmall)
        }
    }
}

private val BALLOON_STYLE_TABS = listOf(R.string.balloon_tab_text, R.string.balloon_tab_balloon)

/** The balloon's typeface, each choice shown in the letters it would set the words in. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontDropdown(
    font: BalloonFont,
    onChange: (BalloonFont) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = open,
        onExpandedChange = { open = it },
        modifier = modifier,
    ) {
        // A bordered line rather than a text field: nothing is typed here, and a field's label
        // and padding cost more height than the whole row is worth.
        Row(
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(BorderStroke(1.dp, scheme.outlineVariant), RoundedCornerShape(4.dp))
                .padding(start = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = font.label(),
                style = MaterialTheme.typography.labelLarge,
                fontFamily = font.toFontFamily(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = open)
        }
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            BalloonFont.entries.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(choice.label(), fontFamily = choice.toFontFamily()) },
                    onClick = {
                        onChange(choice)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun StyleControls(style: ComicStyle, onChange: (ComicStyle) -> Unit) {
    Column {
        // There is no margin control: the page margin is the gutter, so every gap in the comic is
        // the same width whichever side of a panel it is on.
        StyleSlider(stringResource(R.string.style_gutter), style.gutter, 0f, 0.1f) {
            onChange(style.copy(gutter = it))
        }
        StyleSlider(stringResource(R.string.style_border), style.borderThickness, 0f, MAX_PANEL_BORDER) {
            onChange(style.copy(borderThickness = it))
        }
        StyleSlider(stringResource(R.string.style_corners), style.cornerRadius, 0f, MAX_CORNER_RADIUS) {
            onChange(style.copy(cornerRadius = it))
        }
    }
}

/** The heaviest a panel border may be, whether the comic or one panel is asking for it. */
private const val MAX_PANEL_BORDER = 0.02f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StyleSlider(
    label: String,
    value: Float,
    from: Float,
    to: Float,
    enabled: Boolean = true,
    onChange: (Float) -> Unit,
) {
    val current = value.coerceIn(from, to)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(STYLE_LABEL_WIDTH))
        Slider(
            value = current,
            onValueChange = onChange,
            valueRange = from..to,
            enabled = enabled,
            modifier = Modifier.weight(1f).height(20.dp),
            // No thumb at all: the filled track says where the value is, and a thumb would only
            // cost height.
            thumb = {},
            track = { FilledTrack((current - from) / (to - from), enabled) },
        )
        // Every comic style distance is a fraction of the page width, so the number that means
        // something to the reader is what percentage of the page it takes.
        Text(
            text = percentOfPage(current),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 8.dp).width(52.dp),
        )
    }
}

/** A page-width fraction written the way the control reads it out: "1.5%" of the page's width. */
internal fun percentOfPage(value: Float): String =
    "${(value * 1000f).roundToInt() / 10f}%"

/** Wide enough for the longest label a style control carries ("Border size"). */
private val STYLE_LABEL_WIDTH = 76.dp

/** The Balloon style sheet's first tab, named once so the sheet can switch on it. */
private val TEXT_TAB = R.string.balloon_tab_text

@Composable
private fun LayoutChangeDialog(warning: LayoutChangeWarning, onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.change_layout_title)) },
        text = { Text(warning.describe()) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.change)) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.keep)) } },
    )
}

@Composable
internal fun LayoutChangeWarning.describe(): String {
    val images = pluralStringResource(R.plurals.removed_images, removedImages, removedImages)
    val balloons = pluralStringResource(R.plurals.removed_balloons, removedBalloons, removedBalloons)
    val lost = when {
        removedImages > 0 && removedBalloons > 0 -> stringResource(R.string.joined_pair, images, balloons)
        removedImages > 0 -> images
        else -> balloons
    }
    return stringResource(R.string.change_layout_message, lost)
}

private val EditorStep.label: Int
    @StringRes get() = when (this) {
        EditorStep.LAYOUT -> R.string.step_layout
        EditorStep.PLACEMENT -> R.string.step_images
        EditorStep.BALLOONS -> R.string.step_balloons
    }

private const val DIMMED = 0.35f

/** The Placement step's heading, which names the step the way the Layout step's breadcrumb does. */
private val PLACEMENT_STEP_TITLE = R.string.placement_step_title

/** The Balloon step's heading. */
private val BALLOON_STEP_TITLE = R.string.balloon_step_title

/** How far a control fades when there is nothing for it to do. */
private const val DISABLED = 0.38f

/** The shape of one panel as the comic will really draw it, which is what a preview must show. */
private fun Comic.panelRatio(): Float {
    val panel = panelShapes().firstOrNull()?.bounds ?: return 1f
    return if (panel.height > 0f) panel.width / panel.height else 1f
}
