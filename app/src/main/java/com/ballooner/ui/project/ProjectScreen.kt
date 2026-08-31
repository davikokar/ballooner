package com.ballooner.ui.project

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ballooner.domain.model.Balloon
import com.ballooner.domain.model.BalloonFont
import com.ballooner.domain.model.BalloonType
import com.ballooner.domain.model.ImagePlacement
import com.ballooner.domain.model.ImagePosition
import com.ballooner.domain.model.RectFraction
import com.ballooner.domain.model.availableImagePlacements
import com.ballooner.domain.model.defaultImagePlacement
import com.ballooner.domain.model.gridCell
import com.ballooner.domain.model.panelAt
import com.ballooner.domain.model.panelGridCells
import com.ballooner.ui.theme.AnimeAceFontFamily
import com.ballooner.ui.theme.InkBlack
import com.ballooner.ui.theme.balloonerTopAppBarColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.math.abs

@Composable
fun ProjectRoute(
    projectId: Long,
    autoOpenPicker: Boolean,
    onNavigateBack: () -> Unit,
    viewModel: ProjectViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProjectScreen(
        projectName = uiState.name,
        autoOpenPicker = autoOpenPicker,
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onRenameProject = viewModel::setProjectName,
        onImagePicked = viewModel::onImagePicked,
        onAddImage = viewModel::onAddImage,
        onAddBalloon = viewModel::addBalloon,
        onSelectBalloon = viewModel::selectBalloon,
        onCommitBalloon = viewModel::commitBalloon,
        onDeleteSelected = viewModel::deleteSelectedBalloon,
        onDeleteComic = { viewModel.deleteProject(onNavigateBack) },
        onDeleteImage = viewModel::onDeleteImage,
        onMoveImage = viewModel::onMoveImage,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(
    projectName: String,
    autoOpenPicker: Boolean,
    uiState: ProjectUiState,
    onNavigateBack: () -> Unit,
    onRenameProject: (String) -> Unit,
    onImagePicked: (String) -> Unit,
    onAddImage: (String, ImagePlacement) -> Unit,
    onAddBalloon: (BalloonType) -> Unit,
    onSelectBalloon: (Long?) -> Unit,
    onCommitBalloon: (Balloon) -> Unit,
    onDeleteSelected: () -> Unit,
    onDeleteComic: () -> Unit,
    onDeleteImage: (RectFraction) -> Unit,
    onMoveImage: (RectFraction, ImagePlacement) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val scope = rememberCoroutineScope()
    // Tracks whether the next picked image should replace the comic's image outright, or be
    // added alongside it (which then needs a position before it can be composed in).
    var addingImage by remember { mutableStateOf(false) }
    var pendingNewImageUri by remember { mutableStateOf<String?>(null) }
    // The Photo Picker shows albums/folders of images and can browse other locations.
    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        if (addingImage && uiState.hasImage) {
            pendingNewImageUri = uri.toString()
        } else {
            onImagePicked(uri.toString())
        }
    }
    val launchPicker = { addImage: Boolean ->
        addingImage = addImage
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    // Displayed image width in px, used to scale text to the exported resolution.
    var displayedWidth by remember { mutableStateOf(0) }
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png"),
    ) { uri: Uri? ->
        val imageUri = uiState.imageUri
        if (uri != null && imageUri != null) {
            scope.launch {
                val ok = exportComic(
                    context = context,
                    outUri = uri,
                    imageUri = imageUri,
                    balloons = uiState.balloons,
                    panels = uiState.panels,
                    displayedWidth = displayedWidth,
                    autoTextSize = uiState.autoTextSize,
                    textMeasurer = textMeasurer,
                    density = density,
                )
                Toast.makeText(context, if (ok) "Saved image" else "Save failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val onSave = { saveLauncher.launch("${projectName.ifBlank { "comic" }}.png") }
    // Edit mode shows the balloon controls; view mode shows the flat result.
    var editMode by remember { mutableStateOf(true) }
    // View-only rotation is hoisted so the toolbar and reset affordance share the same state.
    var rotation by remember { mutableStateOf(0f) }
    var selectedPanel by remember { mutableStateOf<RectFraction?>(null) }
    var focusedPanel by remember { mutableStateOf<RectFraction?>(null) }
    LaunchedEffect(uiState.panels) {
        if (selectedPanel !in uiState.panels) selectedPanel = null
        if (focusedPanel !in uiState.panels) focusedPanel = null
    }
    // A freshly created project jumps straight to image selection.
    LaunchedEffect(Unit) {
        if (autoOpenPicker) launchPicker(false)
    }
    // Keep a balloon selected in edit mode so the controls stay visible.
    LaunchedEffect(editMode, uiState.balloons, uiState.selectedBalloonId) {
        if (editMode && uiState.selectedBalloonId == null && uiState.balloons.isNotEmpty()) {
            onSelectBalloon(uiState.balloons.last().id)
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (editMode) {
                            EditableTitle(name = projectName, onRename = onRenameProject)
                        } else {
                            Text(
                                text = projectName.ifBlank { "Untitled" },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    },
                    colors = balloonerTopAppBarColors(),
                    navigationIcon = {
                        ComicChip(
                            onClick = onNavigateBack,
                            containerColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(start = 8.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                            )
                        }
                    },
                    actions = {
                        ProjectOverflowMenu(onDeleteComic = onDeleteComic)
                    },
                )
                // Thick ink border under the bar, the signature "hard-edged inking" look.
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(InkBlack))
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!uiState.hasImage) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ComicButton(text = "Open image", onClick = { launchPicker(false) })
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Toolbar(
                        editMode = editMode,
                        canFocusImage = uiState.panels.size > 1,
                        imageFocused = focusedPanel != null,
                        onToggleImageFocus = {
                            focusedPanel = imageFocusTarget(uiState.panels, selectedPanel, focusedPanel)
                            rotation = 0f
                        },
                        onRotate = {
                            focusedPanel = null
                            rotation = (rotation + 90f) % 360f
                        },
                        onChangeImage = { launchPicker(true) },
                        onSave = onSave,
                        onToggleMode = { editMode = it },
                    )
                    Editor(
                        imageUri = uiState.imageUri!!,
                        balloons = uiState.balloons,
                        selectedBalloonId = uiState.selectedBalloonId,
                        editMode = editMode,
                        hideFontSelector = uiState.hideFontSelector,
                        autoTextSize = uiState.autoTextSize,
                        rotation = rotation,
                        onRotationChange = { rotation = it },
                        onSelectBalloon = onSelectBalloon,
                        onCommitBalloon = onCommitBalloon,
                        onDeleteSelected = onDeleteSelected,
                        onAddBalloon = onAddBalloon,
                        onOpenImagePicker = { launchPicker(false) },
                        onLayerWidth = { displayedWidth = it },
                        panels = uiState.panels,
                        selectedPanel = selectedPanel,
                        onSelectPanel = { selectedPanel = it },
                        focusedPanel = focusedPanel,
                        onDeleteImage = onDeleteImage,
                        onMoveImage = onMoveImage,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (pendingNewImageUri != null) {
                ImagePositionDialog(
                    panels = uiState.panels,
                    onSelect = { placement ->
                        onAddImage(pendingNewImageUri!!, placement)
                        pendingNewImageUri = null
                    },
                    onDismiss = { pendingNewImageUri = null },
                )
            }
            if (uiState.isProcessingImage) {
                ImageProcessingOverlay()
            }
        }
    }
}

@Composable
private fun ImagePositionDialog(
    panels: List<RectFraction>,
    onSelect: (ImagePlacement) -> Unit,
    onDismiss: () -> Unit,
) {
    val placements = remember(panels) { availableImagePlacements(panels) }
    var snapped by remember(placements) {
        mutableStateOf(defaultImagePlacement(placements, panels))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add image") },
        text = {
            Column {
                Text("Drag the new panel next to any existing one, then tap Add.")
                Spacer(modifier = Modifier.height(12.dp))
                ImagePositionPicker(
                    panels = panels,
                    placements = placements,
                    snapped = snapped,
                    onSnappedChange = { snapped = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { snapped?.let(onSelect) },
                enabled = snapped != null,
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/**
 * A visual position picker: one uniformly-sized white box per existing image panel, plus a
 * yellow draggable box for the new image that snaps ("magnetic") to whichever side of the
 * existing panels it nears. Box size always shrinks to fit the picker with room to spare on
 * every side, so the new-image box never runs out of space to be dragged into.
 */
@Composable
private fun ImagePositionPicker(
    panels: List<RectFraction>,
    placements: List<ImagePlacement>,
    snapped: ImagePlacement?,
    onSnappedChange: (ImagePlacement?) -> Unit,
) {
    val density = LocalDensity.current
    val pickerSize = DpSize(280.dp, 220.dp)
    val pickerWidthPx = with(density) { pickerSize.width.toPx() }
    val pickerHeightPx = with(density) { pickerSize.height.toPx() }
    val panelCells = remember(panels) { panelGridCells(panels) }
    val targetCells = remember(placements, panelCells) {
        placements.associateWith { it.gridCell(panelCells) }
    }
    val allCells = panelCells.values + targetCells.values
    val minColumn = allCells.minOfOrNull { it.column } ?: -1
    val maxColumn = allCells.maxOfOrNull { it.column } ?: 1
    val minRow = allCells.minOfOrNull { it.row } ?: -1
    val maxRow = allCells.maxOfOrNull { it.row } ?: 1
    val columnCount = maxColumn - minColumn + 1
    val rowCount = maxRow - minRow + 1
    val marginPx = with(density) { 12.dp.toPx() }
    val gapPx = with(density) { 10.dp.toPx() }
    val cellPx = minOf(
        (pickerWidthPx - marginPx * 2 - gapPx * (columnCount - 1)) / columnCount,
        (pickerHeightPx - marginPx * 2 - gapPx * (rowCount - 1)) / rowCount,
        with(density) { 56.dp.toPx() },
    )
    val pitchPx = cellPx + gapPx
    val centerColumn = (minColumn + maxColumn) / 2f
    val centerRow = (minRow + maxRow) / 2f
    fun centerOf(cell: com.ballooner.domain.model.PanelGridCell) = Offset(
        (cell.column - centerColumn) * pitchPx,
        (cell.row - centerRow) * pitchPx,
    )

    val snapTargets = targetCells.mapValues { centerOf(it.value) }
    var dragOffset by remember { mutableStateOf<Offset?>(null) }
    LaunchedEffect(panels) { dragOffset = null }
    val displayOffset = dragOffset ?: snapped?.let { snapTargets[it] } ?: Offset.Zero
    val nearestSnap = snapTargets.entries.minByOrNull { (it.value - displayOffset).getDistance() }?.key
    val newBlockSize = DpSize(with(density) { cellPx.toDp() }, with(density) { cellPx.toDp() })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(pickerSize.height)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .border(2.dp, InkBlack, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.size(pickerSize)) {
            targetCells.forEach { (placement, _) ->
                val target = snapTargets.getValue(placement)
                val isActive = placement == (if (dragOffset == null) snapped else nearestSnap)
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (pickerWidthPx / 2f + target.x - cellPx / 2f).roundToInt(),
                                (pickerHeightPx / 2f + target.y - cellPx / 2f).roundToInt(),
                            )
                        }
                        .size(with(density) { cellPx.toDp() })
                        .dashedBorder(2.dp, InkBlack.copy(alpha = if (isActive) 0.8f else 0.35f), RoundedCornerShape(4.dp)),
                )
            }
            panelCells.forEach { (_, cell) ->
                val center = centerOf(cell)
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (pickerWidthPx / 2f + center.x - cellPx / 2f).roundToInt(),
                                (pickerHeightPx / 2f + center.y - cellPx / 2f).roundToInt(),
                            )
                        }
                        .size(with(density) { cellPx.toDp() })
                        .background(Color.White, RoundedCornerShape(4.dp))
                        .border(2.dp, InkBlack, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(BalloonerIcons.Image, contentDescription = null, tint = InkBlack)
                }
            }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (pickerWidthPx / 2f + displayOffset.x - with(density) { newBlockSize.width.toPx() } / 2f)
                                .roundToInt(),
                            (pickerHeightPx / 2f + displayOffset.y - with(density) { newBlockSize.height.toPx() } / 2f)
                                .roundToInt(),
                        )
                    }
                    .size(newBlockSize)
                    .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(4.dp))
                    .border(2.dp, InkBlack, RoundedCornerShape(4.dp))
                    .pointerInput(snapTargets, snapped) {
                        var gestureOffset = snapped?.let { snapTargets[it] } ?: Offset.Zero
                        detectDragGestures(
                            onDragStart = {
                                gestureOffset = snapped?.let { snapTargets[it] } ?: Offset.Zero
                                dragOffset = gestureOffset
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                gestureOffset += amount
                                dragOffset = gestureOffset
                            },
                            onDragEnd = {
                                onSnappedChange(snapTargets.minByOrNull { (it.value - gestureOffset).getDistance() }?.key)
                                dragOffset = null
                            },
                            onDragCancel = { dragOffset = null },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Add, contentDescription = "New image", tint = InkBlack)
            }
        }
    }
}

/** A blocking scrim + spinner shown while an image import/compose runs in the background. */
@Composable
private fun ImageProcessingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            // Swallow taps so the frozen-looking editor underneath can't be interacted with.
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(4.dp, InkBlack, RoundedCornerShape(8.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Adding image\u2026", color = InkBlack, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EditableTitle(name: String, onRename: (String) -> Unit) {
    var text by remember { mutableStateOf(name) }
    // Seed once the persisted name loads without clobbering in-progress edits.
    LaunchedEffect(name) {
        if (text.isBlank() && name.isNotBlank()) text = name
    }
    BasicTextField(
        value = text,
        onValueChange = {
            text = it
            onRename(it)
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.titleLarge.copy(
            color = Color.White,
            fontFamily = AnimeAceFontFamily,
            fontWeight = FontWeight.Bold,
        ),
        cursorBrush = SolidColor(Color.White),
        decorationBox = { inner ->
            if (text.isEmpty()) {
                Text(
                    text = "Title",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
            inner()
        },
    )
}

@Composable
private fun ProjectOverflowMenu(onDeleteComic: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = "More options")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Delete comic") },
            onClick = {
                expanded = false
                showConfirm = true
            },
        )
    }
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Delete comic?") },
            text = { Text("This permanently removes the comic and its image.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirm = false
                        onDeleteComic()
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun Toolbar(
    editMode: Boolean,
    canFocusImage: Boolean,
    imageFocused: Boolean,
    onToggleImageFocus: () -> Unit,
    onRotate: () -> Unit,
    onChangeImage: () -> Unit,
    onSave: () -> Unit,
    onToggleMode: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .drawBehind {
                drawLine(
                    color = InkBlack,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 4.dp.toPx(),
                )
            }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ComicButton(text = "Rotate", onClick = onRotate, icon = BalloonerIcons.Rotate, showLabel = false)
        ComicButton(
            text = if (imageFocused) "Show all images" else "Focus image",
            onClick = onToggleImageFocus,
            icon = BalloonerIcons.FocusImage,
            showLabel = false,
            enabled = canFocusImage,
            containerColor = if (imageFocused) MaterialTheme.colorScheme.tertiary else Color.White,
        )
        ModeToggle(editMode = editMode, onToggleMode = onToggleMode)
        if (editMode) {
            ComicButton(text = "Change image", onClick = onChangeImage, icon = BalloonerIcons.Image, showLabel = false)
        }
        ComicButton(
            text = "Save",
            onClick = onSave,
            icon = BalloonerIcons.Save,
            showLabel = false,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = Color.White,
        )
    }
}

/** The "Edit / View" segmented control, matching the comic panel button style. */
@Composable
private fun ModeToggle(editMode: Boolean, onToggleMode: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .background(Color.White, CircleShape)
            .border(4.dp, InkBlack, CircleShape)
            .clip(CircleShape),
    ) {
        ModeToggleSegment(text = "Edit", selected = editMode, onClick = { onToggleMode(true) })
        ModeToggleSegment(text = "View", selected = !editMode, onClick = { onToggleMode(false) })
    }
}

@Composable
private fun ModeToggleSegment(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            color = if (selected) Color.White else InkBlack,
            fontFamily = googleFontFamily("Space Grotesk"),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}

/** A bordered button with a solid offset shadow, the app's signature comic button style. */
@Composable
private fun ComicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    showLabel: Boolean = true,
    containerColor: Color = Color.White,
    contentColor: Color = InkBlack,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .background(containerColor, RoundedCornerShape(8.dp))
            .border(4.dp, InkBlack, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClickLabel = text, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            }
            if (showLabel) {
                Text(
                    text = text.uppercase(),
                    color = contentColor,
                    fontFamily = googleFontFamily("Space Grotesk"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

/** A small bordered chip button used for the top bar's back action. */
@Composable
private fun ComicChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(containerColor, RoundedCornerShape(8.dp))
            .border(2.dp, InkBlack, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

/** An icon button showing a stylized balloon that adds one of that type when tapped. */
@Composable
private fun BalloonTypeButton(type: BalloonType, onClick: () -> Unit) {
    val shape = if (type == BalloonType.CAPTION) RoundedCornerShape(8.dp) else CircleShape
    Box(
        modifier = Modifier
            .size(52.dp)
            .background(Color.White, shape)
            .then(
                if (type == BalloonType.WHISPER) {
                    Modifier.dashedBorder(3.dp, InkBlack, shape)
                } else {
                    Modifier.border(4.dp, InkBlack, shape)
                },
            )
            .clickable(onClickLabel = type.label(), onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(28.dp)) {
            // Captions have no tail, so their icon is a plain square instead of a balloon shape.
            val sample = if (type == BalloonType.CAPTION) {
                Balloon(
                    id = 0,
                    type = type,
                    centerX = 0.5f,
                    centerY = 0.5f,
                    width = 0.7f,
                    height = 0.7f,
                    tailLength = 0f,
                    cornerRoundness = 0f,
                )
            } else {
                Balloon(
                    id = 0,
                    type = type,
                    centerX = 0.5f,
                    centerY = 0.4f,
                    width = 0.74f,
                    height = 0.5f,
                    tailAngleDegrees = 110f,
                    tailLength = 0.2f,
                )
            }
            drawBalloon(sample, size, bodyColor = Color.Transparent, outlineColor = InkBlack)
        }
    }
}

/** A dashed ink border, used for the whisper balloon chip to hint at its dashed outline. */
private fun Modifier.dashedBorder(width: Dp, color: Color, shape: Shape): Modifier = drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = when (outline) {
        is androidx.compose.ui.graphics.Outline.Rectangle -> Path().apply { addRect(outline.rect) }
        is androidx.compose.ui.graphics.Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
        is androidx.compose.ui.graphics.Outline.Generic -> outline.path
    }
    val strokeWidthPx = width.toPx()
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidthPx,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(strokeWidthPx * 3, strokeWidthPx * 2)),
        ),
    )
}

@Composable
private fun Editor(
    imageUri: String,
    balloons: List<Balloon>,
    selectedBalloonId: Long?,
    editMode: Boolean,
    hideFontSelector: Boolean,
    autoTextSize: Boolean,
    rotation: Float,
    onRotationChange: (Float) -> Unit,
    onSelectBalloon: (Long?) -> Unit,
    onCommitBalloon: (Balloon) -> Unit,
    onDeleteSelected: () -> Unit,
    onAddBalloon: (BalloonType) -> Unit,
    onOpenImagePicker: () -> Unit,
    onLayerWidth: (Int) -> Unit,
    panels: List<RectFraction>,
    selectedPanel: RectFraction?,
    onSelectPanel: (RectFraction?) -> Unit,
    focusedPanel: RectFraction?,
    onDeleteImage: (RectFraction) -> Unit,
    onMoveImage: (RectFraction, ImagePlacement) -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageState = rememberImageState(imageUri)
    var layerSize by remember { mutableStateOf(IntSize.Zero) }
    // Available area for the image, used to refit it after a 90° rotation.
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var moveHandleOffset by remember { mutableStateOf(Offset.Zero) }
    var moveTarget by remember { mutableStateOf<ImagePlacement?>(null) }
    var showConfirmDeleteImage by remember { mutableStateOf(false) }
    LaunchedEffect(panels) {
        moveHandleOffset = Offset.Zero
        moveTarget = null
    }
    // Working copy of the selected balloon. Seeded when the selection changes and
    // kept across gestures so size / position / tail / text edits accumulate
    // instead of overwriting each other before Room has round-tripped a commit.
    val selectedPersisted = balloons.firstOrNull { it.id == selectedBalloonId }
    var live by remember(selectedBalloonId) { mutableStateOf(selectedPersisted) }
    val effective = balloons.map { b -> live?.takeIf { it.id == b.id } ?: b }
    val selected = effective.firstOrNull { it.id == selectedBalloonId }

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.background)
                .dotGridBackground(color = MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .onSizeChanged { containerSize = it }
                        .padding(16.dp),
                ) {
                    val availableWidth = maxWidth
                    val availableHeight = maxHeight
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                    when (val state = imageState) {
                        ImageResult.Loading -> Text("Loading image\u2026")
                        ImageResult.Failed -> Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("Couldn't load this image.")
                            ComicButton(text = "Choose image", onClick = onOpenImagePicker)
                        }
                        is ImageResult.Loaded -> {
                        val image = state.bitmap
                        // A quarter turn swaps width/height, so scale the layer to refill the space.
                        val quarterTurned = ((rotation / 90f).roundToInt() % 2) != 0
                        val fitScale = if (
                            quarterTurned && layerSize.width > 0 && layerSize.height > 0 &&
                            containerSize.width > 0 && containerSize.height > 0
                        ) {
                            minOf(
                                containerSize.width.toFloat() / layerSize.height,
                                containerSize.height.toFloat() / layerSize.width,
                            )
                        } else {
                            1f
                        }
                        // Fit the frame within the available space (letterboxed) with an exact
                        // size, reserving room below for the shape slider so tall images never
                        // push it off-screen.
                        val showShapeSlider = editMode && selected != null &&
                            (selected.type == BalloonType.SPEAK || selected.type == BalloonType.WHISPER)
                        val shapeSliderSpace = if (showShapeSlider) 8.dp + 24.dp else 0.dp
                        val viewport = focusedPanel ?: RectFraction(0f, 0f, 1f, 1f)
                        val viewportAspect = image.width * viewport.width / (image.height * viewport.height)
                        val fitWidth = minOf(availableWidth, (availableHeight - shapeSliderSpace) * viewportAspect)
                        val fitHeight = fitWidth / viewportAspect
                        val focusLayout = focusedPanel?.focusLayout(fitWidth.value, fitHeight.value)
                        Box(
                            modifier = Modifier
                                .size(fitWidth, fitHeight)
                                .then(if (focusedPanel != null) Modifier.clipToBounds() else Modifier),
                        ) {
                            Box(
                                modifier = Modifier
                                    .wrapContentSize(align = Alignment.TopStart, unbounded = true)
                                    .offset(
                                        x = (focusLayout?.offsetX ?: 0f).dp,
                                        y = (focusLayout?.offsetY ?: 0f).dp,
                                    )
                                    .requiredSize(
                                        width = (focusLayout?.contentWidth ?: fitWidth.value).dp,
                                        height = (focusLayout?.contentHeight ?: fitHeight.value).dp,
                                    )
                                    // No outer border here: each stored image already has its own
                                    // border baked in (see AppImageStore), so a group-level border
                                    // isn't drawn around composited panels.
                                    .graphicsLayer {
                                        scaleX = fitScale
                                        scaleY = fitScale
                                        rotationZ = rotation
                                    },
                            ) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .onSizeChanged {
                                        layerSize = it
                                        onLayerWidth(it.width)
                                    }
                                    .pointerInput(balloons, panels, editMode) {
                                        detectTapGestures(
                                            onTap = { offset ->
                                                onSelectPanel(null)
                                                if (!editMode) return@detectTapGestures
                                                val canvas = Size(size.width.toFloat(), size.height.toFloat())
                                                val hit = effective.lastOrNull { it.containsPoint(offset, canvas) }
                                                if (hit != null) onSelectBalloon(hit.id)
                                            },
                                            onLongPress = { offset ->
                                                if (!editMode || panels.size <= 1) return@detectTapGestures
                                                val u = offset.x / size.width
                                                val v = offset.y / size.height
                                                onSelectPanel(panels.panelAt(u, v))
                                                moveHandleOffset = Offset.Zero
                                                moveTarget = null
                                            },
                                        )
                                    },
                            ) {
                                Canvas(modifier = Modifier.matchParentSize()) {
                                    drawImage(image = image, dstSize = IntSize(size.width.toInt(), size.height.toInt()))
                                    effective.forEach { balloon ->
                                        val panel = panels.panelAt(balloon.centerX, balloon.centerY)
                                        clipToPanel(panel, size) {
                                            drawBalloon(balloon, size, bodyColor = Color.White, outlineColor = Color.Black)
                                        }
                                    }
                                }

                                val size = Size(layerSize.width.toFloat(), layerSize.height.toFloat())
                                if (size.width > 0f && size.height > 0f) {
                                    effective.forEach { balloon ->
                                        val panel = panels.panelAt(balloon.centerX, balloon.centerY)
                                        val bounds = panel?.balloonClipBounds(size)
                                        Box(
                                            modifier = if (bounds == null) {
                                                Modifier.matchParentSize()
                                            } else {
                                                Modifier
                                                    .offset { IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()) }
                                                    .size(
                                                        with(LocalDensity.current) { bounds.width.toDp() },
                                                        with(LocalDensity.current) { bounds.height.toDp() },
                                                    )
                                                    .clipToBounds()
                                            },
                                        ) {
                                            BalloonText(
                                                balloon = balloon,
                                                canvasSize = size,
                                                origin = bounds?.topLeft ?: Offset.Zero,
                                                editable = editMode,
                                                autoSize = autoTextSize,
                                                onTextChange = { newText ->
                                                    val current = live?.takeIf { it.id == balloon.id } ?: balloon
                                                    val updated = current.copy(text = newText)
                                                    if (selectedBalloonId == balloon.id) live = updated
                                                    onCommitBalloon(updated)
                                                },
                                                onFocused = { onSelectBalloon(balloon.id) },
                                            )
                                        }
                                    }
                                }

                                if (editMode) {
                                    selected?.let { sel ->
                                        Handles(
                                            balloon = sel,
                                            canvasSize = size,
                                            contentScale = fitScale,
                                            base = { live ?: sel },
                                            onLiveChange = { live = it },
                                            onCommit = { live?.let(onCommitBalloon) },
                                            onDelete = onDeleteSelected,
                                        )
                                    }
                                    val moveHighlightColor = MaterialTheme.colorScheme.tertiary
                                    moveTarget?.let { placement ->
                                        val target = placement.anchor
                                        Canvas(modifier = Modifier.matchParentSize()) {
                                            val left = target.left * size.width
                                            val top = target.top * size.height
                                            val right = (target.left + target.width) * size.width
                                            val bottom = (target.top + target.height) * size.height
                                            val (start, end) = when (placement.position) {
                                                ImagePosition.LEFT -> Offset(left, top) to Offset(left, bottom)
                                                ImagePosition.RIGHT -> Offset(right, top) to Offset(right, bottom)
                                                ImagePosition.TOP -> Offset(left, top) to Offset(right, top)
                                                ImagePosition.BOTTOM -> Offset(left, bottom) to Offset(right, bottom)
                                            }
                                            drawLine(
                                                color = moveHighlightColor,
                                                start = start,
                                                end = end,
                                                strokeWidth = 5.dp.toPx(),
                                            )
                                        }
                                    }
                                    selectedPanel?.let { pending ->
                                        ImageMoveHandle(
                                            centerPx = Offset(
                                                (pending.left + pending.width / 2f) * size.width,
                                                pending.top * size.height,
                                            ) + moveHandleOffset,
                                            contentScale = fitScale,
                                            onDrag = { delta ->
                                                moveHandleOffset += delta
                                                val center = Offset(
                                                    (pending.left + pending.width / 2f) * size.width,
                                                    pending.top * size.height,
                                                ) + moveHandleOffset
                                                val u = (center.x / size.width).coerceIn(0f, 0.999999f)
                                                val v = (center.y / size.height).coerceIn(0f, 0.999999f)
                                                moveTarget = panels.panelAt(u, v)?.let { target ->
                                                    ImagePlacement(target, target.dropPosition(u, v))
                                                }
                                            },
                                            onDragEnd = {
                                                moveTarget?.takeIf { it.anchor != pending }?.let {
                                                    onMoveImage(pending, it)
                                                }
                                                moveHandleOffset = Offset.Zero
                                                moveTarget = null
                                            },
                                        )
                                        ImageDeleteHandle(
                                            centerPx = Offset(
                                                (pending.left + pending.width) * size.width,
                                                pending.top * size.height,
                                            ),
                                            contentScale = fitScale,
                                            onTap = { showConfirmDeleteImage = true },
                                        )
                                    }
                                }
                            }
                            }
                        }
                        if (showShapeSlider) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(shapeSliderSpace)
                                    .background(MaterialTheme.colorScheme.background)
                                    .zIndex(1f),
                            ) {
                                Spacer(modifier = Modifier.height(8.dp))
                                ShapeSlider(
                                    roundness = selected.cornerRoundness,
                                    onChange = {
                                        val sel = selected
                                        live = (live ?: sel).copy(cornerRoundness = it)
                                    },
                                    onChangeFinished = { live?.let(onCommitBalloon) },
                                )
                            }
                        }
                        }
                    }
                    }
                }
                if (editMode && imageState is ImageResult.Loaded) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ComicKit(
                        selected = selected,
                        hideFontSelector = hideFontSelector,
                        autoTextSize = autoTextSize,
                        onAddBalloon = onAddBalloon,
                        onFontChange = {
                            val sel = selected ?: return@ComicKit
                            live = (live ?: sel).copy(font = it)
                            live?.let(onCommitBalloon)
                        },
                        onSizeChange = {
                            val sel = selected ?: return@ComicKit
                            live = (live ?: sel).copy(fontSize = it)
                        },
                        onSizeChangeFinished = { live?.let(onCommitBalloon) },
                    )
                }
            }
            if (imageState is ImageResult.Loaded && rotation != 0f) {
                ComicButton(
                    text = "Reset",
                    onClick = { onRotationChange(0f) },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    containerColor = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
    if (showConfirmDeleteImage) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteImage = false },
            title = { Text("Delete image?") },
            text = { Text("This permanently removes this image and any balloons on it.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedPanel?.let(onDeleteImage)
                        onSelectPanel(null)
                        showConfirmDeleteImage = false
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteImage = false }) { Text("Cancel") }
            },
        )
    }
}

internal fun imageFocusTarget(
    panels: List<RectFraction>,
    selectedPanel: RectFraction?,
    focusedPanel: RectFraction?,
): RectFraction? = if (focusedPanel == null) selectedPanel ?: panels.firstOrNull() else null

internal data class FocusLayout(
    val contentWidth: Float,
    val contentHeight: Float,
    val offsetX: Float,
    val offsetY: Float,
)

internal fun RectFraction.focusLayout(viewportWidth: Float, viewportHeight: Float): FocusLayout {
    val contentWidth = viewportWidth / width
    val contentHeight = viewportHeight / height
    return FocusLayout(
        contentWidth = contentWidth,
        contentHeight = contentHeight,
        offsetX = -left * contentWidth,
        offsetY = -top * contentHeight,
    )
}

private fun RectFraction.dropPosition(x: Float, y: Float): ImagePosition {
    val horizontal = (x - (left + width / 2f)) / width
    val vertical = (y - (top + height / 2f)) / height
    return if (abs(horizontal) > abs(vertical)) {
        if (horizontal < 0f) ImagePosition.LEFT else ImagePosition.RIGHT
    } else {
        if (vertical < 0f) ImagePosition.TOP else ImagePosition.BOTTOM
    }
}

/** The bottom "comic kit" panel: balloon types plus the currently selected balloon's controls. */
@Composable
private fun ComicKit(
    selected: Balloon?,
    hideFontSelector: Boolean,
    autoTextSize: Boolean,
    onAddBalloon: (BalloonType) -> Unit,
    onFontChange: (BalloonFont) -> Unit,
    onSizeChange: (Float) -> Unit,
    onSizeChangeFinished: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(vertical = 12.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BalloonType.entries.forEach { type ->
                    BalloonTypeButton(type = type, onClick = { onAddBalloon(type) })
                }
            }
        }
        if (selected != null && (!hideFontSelector || !autoTextSize)) {
            Spacer(modifier = Modifier.height(12.dp))
            TextControls(
                font = selected.font,
                fontSize = selected.fontSize,
                showFontSelector = !hideFontSelector,
                showSizeSlider = !autoTextSize,
                onFontChange = onFontChange,
                onSizeChange = onSizeChange,
                onSizeChangeFinished = onSizeChangeFinished,
            )
        }
    }
}

/** A faint dot grid mimicking newsprint texture, drawn behind the editor's canvas area. */
private fun Modifier.dotGridBackground(
    color: Color,
    spacing: Dp = 16.dp,
    dotRadius: Dp = 1.5.dp,
): Modifier = drawBehind {
    val step = spacing.toPx()
    val radius = dotRadius.toPx()
    var y = 0f
    while (y < size.height) {
        var x = 0f
        while (x < size.width) {
            drawCircle(color = color, radius = radius, center = Offset(x, y))
            x += step
        }
        y += step
    }
}

@Composable
private fun TextControls(
    font: BalloonFont,
    fontSize: Float,
    showFontSelector: Boolean,
    showSizeSlider: Boolean,
    onFontChange: (BalloonFont) -> Unit,
    onSizeChange: (Float) -> Unit,
    onSizeChangeFinished: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showFontSelector) {
            Column(modifier = Modifier.weight(1f)) {
                ComicFieldLabel("Font style")
                var expanded by remember { mutableStateOf(false) }
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(4.dp, InkBlack, RoundedCornerShape(8.dp))
                            .clickable { expanded = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(font.label(), color = InkBlack)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = InkBlack)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        BalloonFont.entries.forEach { entry ->
                            DropdownMenuItem(
                                text = { Text(entry.label()) },
                                onClick = {
                                    onFontChange(entry)
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
        }
        if (showSizeSlider) {
            Column(modifier = Modifier.weight(1f)) {
                ComicFieldLabel("Text size")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(4.dp, InkBlack, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Slider(
                        value = fontSize,
                        onValueChange = onSizeChange,
                        onValueChangeFinished = onSizeChangeFinished,
                        valueRange = 8f..48f,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShapeSlider(
    roundness: Float,
    onChange: (Float) -> Unit,
    onChangeFinished: () -> Unit,
) {
    Slider(
        value = roundness,
        onValueChange = onChange,
        onValueChangeFinished = onChangeFinished,
        valueRange = 0f..1f,
        modifier = Modifier.fillMaxWidth().height(24.dp),
    )
}

@Composable
private fun ComicFieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = Color.White,
        fontFamily = googleFontFamily("Space Grotesk"),
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}


@Composable
private fun BalloonText(
    balloon: Balloon,
    canvasSize: Size,
    origin: Offset,
    editable: Boolean,
    autoSize: Boolean,
    onTextChange: (String) -> Unit,
    onFocused: () -> Unit,
) {
    val density = LocalDensity.current
    val left = balloon.centerX * canvasSize.width - balloon.width * canvasSize.width / 2f - origin.x
    val top = balloon.centerY * canvasSize.height - balloon.height * canvasSize.height / 2f - origin.y
    val widthDp = with(density) { (balloon.width * canvasSize.width).toDp() }
    val heightDp = with(density) { (balloon.height * canvasSize.height).toDp() }
    val innerPadding = when (balloon.type) {
        BalloonType.YELL -> 24.dp
        // Captions are a plain rectangle, so the text can sit almost flush with the border.
        BalloonType.CAPTION -> 2.dp
        else -> 18.dp
    }

    var text by remember(balloon.id) { mutableStateOf(balloon.text) }

    val padPx = with(density) { innerPadding.toPx() }
    val availableWidth = (balloon.width * canvasSize.width - 2 * padPx).toInt().coerceAtLeast(1)
    val availableHeight = (balloon.height * canvasSize.height - 2 * padPx).toInt().coerceAtLeast(1)
    val effectiveFontSize = if (autoSize) {
        rememberAutoFontSize(text, balloon.font, availableWidth, availableHeight)
    } else {
        balloon.fontSize
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
            .size(widthDp, heightDp),
        contentAlignment = Alignment.Center,
    ) {
        BasicTextField(
            value = text,
            onValueChange = {
                if (!editable) return@BasicTextField
                text = it
                onTextChange(it)
            },
            readOnly = !editable,
            textStyle = TextStyle(
                color = Color.Black,
                fontSize = effectiveFontSize.sp,
                fontFamily = balloon.font.toFontFamily(),
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .onFocusChanged { if (it.isFocused) onFocused() },
        )
    }
}

/** Largest font (in sp) whose text fits within the available box, for auto text sizing. */
@Composable
private fun rememberAutoFontSize(
    text: String,
    font: BalloonFont,
    availableWidth: Int,
    availableHeight: Int,
): Float {
    val measurer = rememberTextMeasurer()
    return remember(text, font, availableWidth, availableHeight) {
        if (availableWidth <= 0 || availableHeight <= 0) {
            AUTO_MIN_FONT_SIZE
        } else {
            // A blank balloon still gets a caret sized to the box via a sample glyph.
            autoFitFontSize(text.ifBlank { "A" }, font, availableWidth, availableHeight, measurer)
        }
    }
}

private fun autoFitFontSize(
    text: String,
    font: BalloonFont,
    maxWidth: Int,
    maxHeight: Int,
    measurer: TextMeasurer,
): Float {
    var best = AUTO_MIN_FONT_SIZE
    var candidate = AUTO_MIN_FONT_SIZE
    while (candidate <= AUTO_MAX_FONT_SIZE) {
        val measured = measurer.measure(
            text = text,
            style = TextStyle(
                fontSize = candidate.sp,
                fontFamily = font.toFontFamily(),
                textAlign = TextAlign.Center,
            ),
            constraints = Constraints(maxWidth = maxWidth),
        )
        if (measured.size.height <= maxHeight && measured.size.width <= maxWidth) {
            best = candidate
            candidate += 1f
        } else {
            break
        }
    }
    return best
}

private const val AUTO_MIN_FONT_SIZE = 8f
private const val AUTO_MAX_FONT_SIZE = 96f

@Composable
private fun Handles(
    balloon: Balloon,
    canvasSize: Size,
    contentScale: Float,
    base: () -> Balloon,
    onLiveChange: (Balloon) -> Unit,
    onCommit: () -> Unit,
    onDelete: () -> Unit,
) {
    val w = canvasSize.width
    val h = canvasSize.height
    val center = balloon.bodyCenter(canvasSize)
    val halfX = balloon.width * w / 2f
    val halfY = balloon.height * h / 2f
    val selectionColor = MaterialTheme.colorScheme.primary

    // Move handle (top-center).
    DragHandle(
        centerPx = Offset(center.x, center.y - halfY),
        sizeDp = 26.dp,
        color = selectionColor,
        shape = RoundedCornerShape(6.dp),
        keyId = balloon.id,
        contentScale = contentScale,
        onDrag = { d ->
            val b = base()
            onLiveChange(b.copy(centerX = b.centerX + d.x / w, centerY = b.centerY + d.y / h))
        },
        onDragEnd = onCommit,
    )

    // Resize handles (four corners).
    val corners = listOf(
        Corner(Offset(center.x - halfX, center.y - halfY), -1f, -1f),
        Corner(Offset(center.x + halfX, center.y - halfY), 1f, -1f),
        Corner(Offset(center.x - halfX, center.y + halfY), -1f, 1f),
        Corner(Offset(center.x + halfX, center.y + halfY), 1f, 1f),
    )
    corners.forEach { corner ->
        DragHandle(
            centerPx = corner.pos,
            sizeDp = 22.dp,
            color = Color(0xFFE8325A),
            shape = CircleShape,
            keyId = balloon.id,
            contentScale = contentScale,
            onDrag = { d ->
                val b = base()
                val dxf = d.x / w
                val dyf = d.y / h
                onLiveChange(
                    b.copy(
                        width = (b.width + corner.signX * dxf).coerceAtLeast(0.1f),
                        height = (b.height + corner.signY * dyf).coerceAtLeast(0.1f),
                        centerX = b.centerX + dxf / 2f,
                        centerY = b.centerY + dyf / 2f,
                    ),
                )
            },
            onDragEnd = onCommit,
        )
    }

    // Tail handle (drag the tip to set both direction and length). Captions have no tail.
    if (balloon.type != BalloonType.CAPTION) {
        DragHandle(
            centerPx = balloon.tailTip(canvasSize),
            sizeDp = 28.dp,
            color = Color(0xFF00C9B1),
            shape = CircleShape,
            keyId = balloon.id,
            contentScale = contentScale,
            onDrag = { d ->
                val b = base()
                val target = b.tailTip(canvasSize) + d
                onLiveChange(b.tailAtPoint(target, canvasSize))
            },
            onDragEnd = onCommit,
        )
    }

    // Tail-width handle (drag sideways to set the tail thickness). Not for Think,
    // whose tail is made of bubbles, or Caption, which has no tail.
    if (balloon.type != BalloonType.THINK && balloon.type != BalloonType.CAPTION) {
        DragHandle(
            centerPx = balloon.tailBaseHandle(canvasSize),
            sizeDp = 24.dp,
            color = Color(0xFF2ECC71),
            shape = CircleShape,
            keyId = balloon.id,
            contentScale = contentScale,
            onDrag = { d ->
                val b = base()
                val target = b.tailBaseHandle(canvasSize) + d
                onLiveChange(b.tailWidthFromPoint(target, canvasSize))
            },
            onDragEnd = onCommit,
        )
    }

    // Delete handle (top-right).
    TapHandle(
        centerPx = Offset(center.x + halfX, center.y - halfY),
        sizeDp = 26.dp,
        color = Color(0xFFE8325A),
        contentScale = contentScale,
        onTap = onDelete,
    ) {
        Text(text = "\u00D7", color = Color.White)
    }
}

/** Delete badge for a whole image panel, styled to match the comic list's delete button. */
@Composable
private fun ImageDeleteHandle(centerPx: Offset, contentScale: Float, onTap: () -> Unit) {
    val density = LocalDensity.current
    val halfPx = with(density) { 16.dp.toPx() }
    Box(
        modifier = Modifier
            .offset { IntOffset((centerPx.x - halfPx).roundToInt(), (centerPx.y - halfPx).roundToInt()) }
            .size(32.dp)
            .graphicsLayer {
                scaleX = fixedControlScale(contentScale)
                scaleY = fixedControlScale(contentScale)
            }
            .background(MaterialTheme.colorScheme.secondary, CircleShape)
            .border(2.dp, InkBlack, CircleShape)
            .pointerInput(Unit) { detectTapGestures { onTap() } },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Delete image",
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Drag handle for moving an image panel, positioned at its top-center edge. */
@Composable
private fun ImageMoveHandle(
    centerPx: Offset,
    contentScale: Float,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    val density = LocalDensity.current
    val halfPx = with(density) { 16.dp.toPx() }
    Box(
        modifier = Modifier
            .offset { IntOffset((centerPx.x - halfPx).roundToInt(), (centerPx.y - halfPx).roundToInt()) }
            .size(32.dp)
            .graphicsLayer {
                scaleX = fixedControlScale(contentScale)
                scaleY = fixedControlScale(contentScale)
            }
            .background(MaterialTheme.colorScheme.tertiary, CircleShape)
            .border(2.dp, InkBlack, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd,
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount / contentScale)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Move image",
            tint = InkBlack,
            modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = 90f },
        )
    }
}

private data class Corner(val pos: Offset, val signX: Float, val signY: Float)

@Composable
private fun DragHandle(
    centerPx: Offset,
    sizeDp: Dp,
    color: Color,
    shape: Shape,
    keyId: Long,
    contentScale: Float,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    content: @Composable () -> Unit = {},
) {
    val density = LocalDensity.current
    val halfPx = with(density) { (sizeDp / 2).toPx() }
    Box(
        modifier = Modifier
            .offset { IntOffset((centerPx.x - halfPx).roundToInt(), (centerPx.y - halfPx).roundToInt()) }
            .size(sizeDp)
            .graphicsLayer {
                scaleX = fixedControlScale(contentScale)
                scaleY = fixedControlScale(contentScale)
            }
            .border(2.dp, Color.White, shape)
            .background(color, shape)
            .pointerInput(keyId) {
                detectDragGestures(
                    onDragEnd = onDragEnd,
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount / contentScale)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun TapHandle(
    centerPx: Offset,
    sizeDp: Dp,
    color: Color,
    contentScale: Float,
    onTap: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val halfPx = with(density) { (sizeDp / 2).toPx() }
    Box(
        modifier = Modifier
            .offset { IntOffset((centerPx.x - halfPx).roundToInt(), (centerPx.y - halfPx).roundToInt()) }
            .size(sizeDp)
            .graphicsLayer {
                scaleX = fixedControlScale(contentScale)
                scaleY = fixedControlScale(contentScale)
            }
            .border(2.dp, Color.White, CircleShape)
            .background(color, CircleShape)
            .pointerInput(Unit) { detectTapGestures { onTap() } },
        contentAlignment = Alignment.Center,
    ) { content() }
}

internal fun fixedControlScale(contentScale: Float): Float =
    if (contentScale > 0f) 1f / contentScale else 1f

private fun BalloonType.label(): String = when (this) {
    BalloonType.SPEAK -> "Speak"
    BalloonType.THINK -> "Think"
    BalloonType.WHISPER -> "Whisper"
    BalloonType.YELL -> "Yell"
    BalloonType.CAPTION -> "Caption"
}

private fun BalloonFont.toFontFamily(): FontFamily = when (this) {
    BalloonFont.DEFAULT -> FontFamily.Default
    BalloonFont.SANS_SERIF -> FontFamily.SansSerif
    BalloonFont.SERIF -> FontFamily.Serif
    BalloonFont.MONOSPACE -> FontFamily.Monospace
    BalloonFont.CURSIVE -> FontFamily.Cursive
    BalloonFont.WIDE -> googleFontFamily("Michroma")
    BalloonFont.NARROW -> googleFontFamily("Archivo Narrow")
    BalloonFont.COMIC_SANS_MS -> googleFontFamily("Comic Neue")
    BalloonFont.GARAMOND -> googleFontFamily("EB Garamond")
    BalloonFont.GEORGIA -> googleFontFamily("Gelasio")
    BalloonFont.TAHOMA -> googleFontFamily("PT Sans")
    BalloonFont.TREBUCHET -> googleFontFamily("Fira Sans")
    BalloonFont.VERDANA -> googleFontFamily("Noto Sans")
    BalloonFont.ANIME_ACE -> AnimeAceFontFamily
}

private sealed interface ImageResult {
    data object Loading : ImageResult
    data class Loaded(val bitmap: ImageBitmap) : ImageResult
    data object Failed : ImageResult
}

@Composable
private fun rememberImageState(uri: String?): ImageResult {
    val context = LocalContext.current
    return produceState<ImageResult>(ImageResult.Loading, uri) {
        value = if (uri == null) {
            ImageResult.Loading
        } else {
            loadBitmap(context, uri)?.let(ImageResult::Loaded) ?: ImageResult.Failed
        }
    }.value
}

private suspend fun loadBitmap(context: Context, uri: String): ImageBitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(Uri.parse(uri)).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }

/** Renders the image + balloons (with text) to a PNG at native resolution and writes it to [outUri]. */
private suspend fun exportComic(
    context: Context,
    outUri: Uri,
    imageUri: String,
    balloons: List<Balloon>,
    panels: List<RectFraction>,
    displayedWidth: Int,
    autoTextSize: Boolean,
    textMeasurer: TextMeasurer,
    density: Density,
): Boolean {
    val source = loadBitmap(context, imageUri) ?: return false
    val width = source.width
    val height = source.height
    // The screen shows fixed-sp text over a scaled-down image, so scale text up to native size.
    val scale = if (displayedWidth > 0) width.toFloat() / displayedWidth else 1f
    val output = ImageBitmap(width, height)
    val canvas = Canvas(output)
    val size = Size(width.toFloat(), height.toFloat())
    CanvasDrawScope().draw(density, LayoutDirection.Ltr, canvas, size) {
        drawImage(source)
        balloons.forEach { balloon ->
            clipToPanel(panels.panelAt(balloon.centerX, balloon.centerY), size) {
                drawBalloon(balloon, size, bodyColor = Color.White, outlineColor = Color.Black)
            }
        }
        balloons.forEach { balloon ->
            clipToPanel(panels.panelAt(balloon.centerX, balloon.centerY), size) {
                drawExportText(balloon, size, textMeasurer, scale, autoTextSize)
            }
        }
    }
    return withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(outUri)?.use { stream ->
                output.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
            } ?: return@runCatching false
            true
        }.getOrDefault(false)
    }
}

internal fun RectFraction.balloonClipBounds(canvasSize: Size): Rect {
    val panelWidth = width * canvasSize.width
    val panelHeight = height * canvasSize.height
    val border = maxOf(minOf(panelWidth, panelHeight) * 0.006f, 3f)
    return Rect(
        left = left * canvasSize.width + border,
        top = top * canvasSize.height + border,
        right = (left + width) * canvasSize.width - border,
        bottom = (top + height) * canvasSize.height - border,
    )
}

private fun DrawScope.clipToPanel(panel: RectFraction?, canvasSize: Size, draw: DrawScope.() -> Unit) {
    if (panel == null) {
        draw()
        return
    }
    val bounds = panel.balloonClipBounds(canvasSize)
    clipRect(bounds.left, bounds.top, bounds.right, bounds.bottom, block = draw)
}

private fun DrawScope.drawExportText(
    balloon: Balloon,
    canvasSize: Size,
    textMeasurer: TextMeasurer,
    scale: Float,
    autoSize: Boolean,
) {
    if (balloon.text.isBlank()) return
    val boxW = balloon.width * canvasSize.width
    val boxH = balloon.height * canvasSize.height
    // Mirrors BalloonText's inner padding so exported text wraps the same way it did on screen.
    val paddingFraction = if (balloon.type == BalloonType.CAPTION) 0.97f else 0.74f
    val maxW = (boxW * paddingFraction).toInt().coerceAtLeast(1)
    val maxH = (boxH * paddingFraction).toInt().coerceAtLeast(1)
    val fontSize = if (autoSize) {
        autoFitFontSize(balloon.text, balloon.font, maxW, maxH, textMeasurer)
    } else {
        balloon.fontSize * scale
    }
    val result = textMeasurer.measure(
        text = balloon.text,
        style = TextStyle(
            color = Color.Black,
            fontSize = fontSize.sp,
            fontFamily = balloon.font.toFontFamily(),
            textAlign = TextAlign.Center,
        ),
        constraints = Constraints(maxWidth = maxW, maxHeight = maxH),
    )
    val cx = balloon.centerX * canvasSize.width
    val cy = balloon.centerY * canvasSize.height
    drawText(result, topLeft = Offset(cx - result.size.width / 2f, cy - result.size.height / 2f))
}

@Preview
@Composable
private fun ProjectScreenNoImagePreview() {
    ProjectScreen(
        projectName = "My Comic 1",
        autoOpenPicker = false,
        uiState = ProjectUiState(),
        onNavigateBack = {},
        onRenameProject = {},
        onImagePicked = {},
        onAddImage = { _, _ -> },
        onAddBalloon = {},
        onSelectBalloon = {},
        onCommitBalloon = {},
        onDeleteSelected = {},
        onDeleteComic = {},
        onDeleteImage = {},
        onMoveImage = { _, _ -> },
    )
}
