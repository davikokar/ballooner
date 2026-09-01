package com.ballooner.ui.project

import android.content.ClipData
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
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
import androidx.compose.ui.res.stringResource
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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ballooner.R
import com.ballooner.domain.model.Balloon
import com.ballooner.domain.model.BalloonFont
import com.ballooner.domain.model.BalloonType
import com.ballooner.domain.model.ImagePlacement
import com.ballooner.domain.model.ImagePosition
import com.ballooner.domain.model.RectFraction
import com.ballooner.domain.model.availableImagePlacements
import com.ballooner.domain.model.defaultImagePlacement
import com.ballooner.domain.model.edgeImagePlacements
import com.ballooner.domain.model.panelAt
import com.ballooner.domain.model.targetRect
import com.ballooner.ui.theme.AnimeAceFontFamily
import com.ballooner.ui.theme.InkBlack
import com.ballooner.ui.theme.balloonerTopAppBarColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun ProjectRoute(
    projectId: Long,
    autoOpenPicker: Boolean,
    onNavigateBack: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ProjectViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProjectScreen(
        projectName = uiState.name,
        autoOpenPicker = autoOpenPicker,
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onOpenSettings = onOpenSettings,
        onRenameProject = viewModel::setProjectName,
        onInitialImagesPicked = viewModel::onInitialImagesPicked,
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
    onOpenSettings: () -> Unit,
    onRenameProject: (String) -> Unit,
    onInitialImagesPicked: (List<String>) -> Unit,
    onImagePicked: (String) -> Unit,
    onAddImage: (String, ImagePlacement) -> Unit,
    onAddBalloon: (BalloonType, RectFraction?) -> Unit,
    onSelectBalloon: (Long?) -> Unit,
    onCommitBalloon: (Balloon) -> Unit,
    onDeleteSelected: () -> Unit,
    onDeleteComic: () -> Unit,
    onDeleteImage: (RectFraction) -> Unit,
    onMoveImage: (RectFraction, RectFraction) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val scope = rememberCoroutineScope()
    // Tracks whether the next picked image should replace the comic's image outright, or be
    // added alongside it (which then needs a position before it can be composed in).
    var addingImage by remember { mutableStateOf(false) }
    var pendingNewImageUri by remember { mutableStateOf<String?>(null) }
    var pendingImagePlacement by remember { mutableStateOf<ImagePlacement?>(null) }
    val pickInitialMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(),
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) onInitialImagesPicked(uris.map(Uri::toString))
    }
    val launchInitialPicker = {
        pickInitialMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    // The Photo Picker shows albums/folders of images and can browse other locations.
    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri == null) {
            pendingImagePlacement = null
            return@rememberLauncherForActivityResult
        }
        if (addingImage && uiState.hasImage) {
            val placement = pendingImagePlacement
            if (placement == null) {
                pendingNewImageUri = uri.toString()
            } else {
                onAddImage(uri.toString(), placement)
                pendingImagePlacement = null
            }
        } else {
            onImagePicked(uri.toString())
        }
    }
    val launchPicker = { addImage: Boolean, placement: ImagePlacement? ->
        addingImage = addImage
        pendingImagePlacement = placement
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
                    imageUri = imageUri,
                    balloons = uiState.balloons,
                    panels = uiState.panels,
                    displayedWidth = displayedWidth,
                    autoTextSize = uiState.autoTextSize,
                    textMeasurer = textMeasurer,
                    density = density,
                    compressFormat = Bitmap.CompressFormat.PNG,
                    openOutputStream = { context.contentResolver.openOutputStream(uri) },
                )
                Toast.makeText(context, if (ok) R.string.saved_image else R.string.save_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val onSave = { saveLauncher.launch("${projectName.ifBlank { "comic" }}.png") }
    val onShare = {
        val imageUri = uiState.imageUri
        if (imageUri != null) {
            scope.launch {
                val shareFile = withContext(Dispatchers.IO) {
                    File(context.cacheDir, "shared").apply { mkdirs() }
                        .resolve(shareFileName(projectName))
                }
                val ok = exportComic(
                    context = context,
                    imageUri = imageUri,
                    balloons = uiState.balloons,
                    panels = uiState.panels,
                    displayedWidth = displayedWidth,
                    autoTextSize = uiState.autoTextSize,
                    textMeasurer = textMeasurer,
                    density = density,
                    compressFormat = Bitmap.CompressFormat.JPEG,
                    openOutputStream = { shareFile.outputStream() },
                )
                if (ok) {
                    val shared = runCatching {
                        val shareUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            shareFile,
                        )
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/jpeg"
                            putExtra(Intent.EXTRA_STREAM, shareUri)
                            clipData = ClipData.newRawUri("Comic", shareUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.share_comic)))
                    }.isSuccess
                    if (!shared) Toast.makeText(context, R.string.share_failed, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, R.string.share_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
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
        if (autoOpenPicker) launchInitialPicker()
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
                                text = projectName.ifBlank { stringResource(R.string.untitled) },
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
                                contentDescription = stringResource(R.string.back),
                                tint = Color.White,
                            )
                        }
                    },
                    actions = {
                        ProjectOverflowMenu(
                            canShare = uiState.hasImage,
                            onDeleteComic = onDeleteComic,
                            onShareComic = onShare,
                            onOpenSettings = onOpenSettings,
                        )
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
                    ComicButton(text = stringResource(R.string.open_images), onClick = launchInitialPicker)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Toolbar(
                        editMode = editMode,
                        canRotate = rotationTarget(uiState.panels, selectedPanel, focusedPanel) != null,
                        canFocusImage = uiState.panels.size > 1,
                        imageFocused = focusedPanel != null,
                        onToggleImageFocus = {
                            focusedPanel = imageFocusTarget(uiState.panels, selectedPanel, focusedPanel)
                            rotation = 0f
                        },
                        onRotate = {
                            rotationTarget(uiState.panels, selectedPanel, focusedPanel)?.let { target ->
                                if (uiState.panels.size > 1) {
                                    focusedPanel = target
                                    selectedPanel = null
                                }
                                rotation = (rotation + 90f) % 360f
                            }
                        },
                        onChangeImage = { launchPicker(true, null) },
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
                        onSelectBalloon = onSelectBalloon,
                        onCommitBalloon = onCommitBalloon,
                        onDeleteSelected = onDeleteSelected,
                        onAddBalloon = { type ->
                            onAddBalloon(type, focusedPanel ?: selectedPanel ?: uiState.panels.firstOrNull())
                        },
                        onOpenImagePicker = { launchPicker(false, null) },
                        onAddImageAt = { placement -> launchPicker(true, placement) },
                        onLayerWidth = { displayedWidth = it },
                        panels = uiState.panels,
                        selectedPanel = selectedPanel,
                        onSelectPanel = { selectedPanel = it },
                        focusedPanel = focusedPanel,
                        onFocusPanel = {
                            focusedPanel = it
                            selectedPanel = null
                            rotation = 0f
                        },
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
        title = { Text(stringResource(R.string.add_panel)) },
        text = {
            Column {
                Text(stringResource(R.string.add_panel_instructions))
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
            ) { Text(stringResource(R.string.add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
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
    val targets = remember(placements) { placements.associateWith { it.targetRect() } }
    val allRects = panels + targets.values
    val minLeft = allRects.minOfOrNull { it.left } ?: 0f
    val minTop = allRects.minOfOrNull { it.top } ?: 0f
    val maxRight = allRects.maxOfOrNull { it.left + it.width } ?: 1f
    val maxBottom = allRects.maxOfOrNull { it.top + it.height } ?: 1f
    val marginPx = with(density) { 12.dp.toPx() }
    val scale = minOf(
        (pickerWidthPx - marginPx * 2) / (maxRight - minLeft).coerceAtLeast(0.01f),
        (pickerHeightPx - marginPx * 2) / (maxBottom - minTop).coerceAtLeast(0.01f),
    )
    fun topLeft(rect: RectFraction) = Offset(
        marginPx + (rect.left - minLeft) * scale,
        marginPx + (rect.top - minTop) * scale,
    )
    fun rectSize(rect: RectFraction) = DpSize(
        with(density) { (rect.width * scale).toDp() },
        with(density) { (rect.height * scale).toDp() },
    )
    fun centerOf(rect: RectFraction) = topLeft(rect) + Offset(rect.width * scale / 2f, rect.height * scale / 2f)

    val snapTargets = targets.mapValues { centerOf(it.value) }
    var dragOffset by remember { mutableStateOf<Offset?>(null) }
    LaunchedEffect(panels) { dragOffset = null }
    val displayOffset = dragOffset ?: snapped?.let { snapTargets[it] } ?: Offset(pickerWidthPx / 2f, pickerHeightPx / 2f)
    val nearestSnap = snapTargets.entries.minByOrNull { (it.value - displayOffset).getDistance() }?.key
    val displayPlacement = if (dragOffset == null) snapped else nearestSnap
    val newBlockRect = displayPlacement?.let { targets[it] } ?: panels.firstOrNull()
    val newBlockSize = newBlockRect?.let(::rectSize) ?: DpSize(48.dp, 48.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(pickerSize.height)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .border(2.dp, InkBlack, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.size(pickerSize)) {
            targets.forEach { (placement, rect) ->
                val isActive = placement == (if (dragOffset == null) snapped else nearestSnap)
                Box(
                    modifier = Modifier
                        .offset {
                            val offset = topLeft(rect)
                            IntOffset(
                                offset.x.roundToInt(),
                                offset.y.roundToInt(),
                            )
                        }
                        .size(rectSize(rect))
                        .dashedBorder(2.dp, InkBlack.copy(alpha = if (isActive) 0.8f else 0.35f), RoundedCornerShape(4.dp)),
                )
            }
            panels.forEach { panel ->
                Box(
                    modifier = Modifier
                        .offset {
                            val offset = topLeft(panel)
                            IntOffset(
                                offset.x.roundToInt(),
                                offset.y.roundToInt(),
                            )
                        }
                        .size(rectSize(panel))
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
                                (displayOffset.x - with(density) { newBlockSize.width.toPx() } / 2f)
                                .roundToInt(),
                            (displayOffset.y - with(density) { newBlockSize.height.toPx() } / 2f)
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
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_panel), tint = InkBlack)
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
            Text(stringResource(R.string.adding_panel), color = InkBlack, fontWeight = FontWeight.Bold)
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
                    text = stringResource(R.string.title_hint),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
            inner()
        },
    )
}

@Composable
private fun ProjectOverflowMenu(
    canShare: Boolean,
    onDeleteComic: () -> Unit,
    onShareComic: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.delete_comic)) },
            onClick = {
                expanded = false
                showConfirm = true
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.share_comic)) },
            enabled = canShare,
            onClick = {
                expanded = false
                onShareComic()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.settings_title)) },
            onClick = {
                expanded = false
                onOpenSettings()
            },
        )
    }
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(stringResource(R.string.delete_comic_title)) },
            text = { Text(stringResource(R.string.delete_comic_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirm = false
                        onDeleteComic()
                    },
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun Toolbar(
    editMode: Boolean,
    canRotate: Boolean,
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
        ComicButton(
            text = stringResource(R.string.rotate),
            onClick = onRotate,
            icon = BalloonerIcons.Rotate,
            showLabel = false,
            enabled = canRotate,
        )
        ComicButton(
            text = stringResource(if (imageFocused) R.string.show_all_panels else R.string.focus_panel),
            onClick = onToggleImageFocus,
            icon = BalloonerIcons.FocusImage,
            showLabel = false,
            enabled = canFocusImage,
            containerColor = if (imageFocused) MaterialTheme.colorScheme.tertiary else Color.White,
        )
        ModeToggle(editMode = editMode, onToggleMode = onToggleMode)
        ComicButton(
            text = stringResource(R.string.add_panel),
            onClick = onChangeImage,
            icon = BalloonerIcons.ImageAdd,
            showLabel = false,
            enabled = editMode,
        )
        ComicButton(
            text = stringResource(R.string.save),
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
        ModeToggleSegment(text = stringResource(R.string.edit), selected = editMode, onClick = { onToggleMode(true) })
        ModeToggleSegment(text = stringResource(R.string.view), selected = !editMode, onClick = { onToggleMode(false) })
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
    onSelectBalloon: (Long?) -> Unit,
    onCommitBalloon: (Balloon) -> Unit,
    onDeleteSelected: () -> Unit,
    onAddBalloon: (BalloonType) -> Unit,
    onOpenImagePicker: () -> Unit,
    onAddImageAt: (ImagePlacement) -> Unit,
    onLayerWidth: (Int) -> Unit,
    panels: List<RectFraction>,
    selectedPanel: RectFraction?,
    onSelectPanel: (RectFraction?) -> Unit,
    focusedPanel: RectFraction?,
    onFocusPanel: (RectFraction) -> Unit,
    onDeleteImage: (RectFraction) -> Unit,
    onMoveImage: (RectFraction, RectFraction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageState = rememberImageState(imageUri)
    var layerSize by remember { mutableStateOf(IntSize.Zero) }
    var moveHandleOffset by remember { mutableStateOf(Offset.Zero) }
    var showConfirmDeleteImage by remember { mutableStateOf(false) }
    var comicKitExpanded by remember { mutableStateOf(true) }
    LaunchedEffect(panels) {
        moveHandleOffset = Offset.Zero
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
                        .padding(16.dp),
                ) {
                    val availableWidth = maxWidth
                    val availableHeight = maxHeight
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                    when (val state = imageState) {
                        ImageResult.Loading -> Text(stringResource(R.string.loading_image))
                        ImageResult.Failed -> Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(stringResource(R.string.image_load_failed))
                            ComicButton(text = stringResource(R.string.choose_image), onClick = onOpenImagePicker)
                        }
                        is ImageResult.Loaded -> {
                        val image = state.bitmap
                        val quarterTurned = ((rotation / 90f).roundToInt() % 2) != 0
                        // Fit the frame within the available space (letterboxed) with an exact
                        // size, reserving room below for the shape slider so tall images never
                        // push it off-screen.
                        val showShapeSlider = editMode && selected != null &&
                            (selected.type == BalloonType.SPEAK || selected.type == BalloonType.WHISPER)
                        val shapeSliderSpace = if (showShapeSlider) 8.dp + 24.dp else 0.dp
                        val viewport = focusedPanel ?: RectFraction(0f, 0f, 1f, 1f)
                        val unrotatedAspect = image.width * viewport.width / (image.height * viewport.height)
                        val viewportAspect = if (quarterTurned) 1f / unrotatedAspect else unrotatedAspect
                        val fitWidth = minOf(availableWidth, (availableHeight - shapeSliderSpace) * viewportAspect)
                        val fitHeight = fitWidth / viewportAspect
                        val normalAspect = image.width.toFloat() / image.height
                        val normalContentWidth = minOf(
                            availableWidth,
                            (availableHeight - shapeSliderSpace) * normalAspect,
                        ).value
                        val focusLayout = if (focusedPanel != null || rotation != 0f) {
                            viewport.focusLayout(fitWidth.value, fitHeight.value, quarterTurned)
                        } else {
                            null
                        }
                        val textScale = balloonTextScale(
                            currentContentWidth = focusLayout?.contentWidth ?: fitWidth.value,
                            normalContentWidth = normalContentWidth,
                        )
                        val rotationOrigin = TransformOrigin(
                            pivotFractionX = viewport.left + viewport.width / 2f,
                            pivotFractionY = viewport.top + viewport.height / 2f,
                        )
                        Box(
                            modifier = Modifier.size(fitWidth, fitHeight),
                        ) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .then(if (focusedPanel != null || rotation != 0f) Modifier.clipToBounds() else Modifier),
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
                                        rotationZ = rotation
                                        transformOrigin = rotationOrigin
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
                                            },
                                        )
                                    },
                            ) {
                                val size = Size(layerSize.width.toFloat(), layerSize.height.toFloat())
                                if (size.width > 0f && size.height > 0f) {
                                    val movingPanel = selectedPanel?.takeIf {
                                        focusedPanel == null && moveHandleOffset != Offset.Zero
                                    }
                                    val previewPanel = movingPanel?.copy(
                                        left = movingPanel.left + moveHandleOffset.x / size.width,
                                        top = movingPanel.top + moveHandleOffset.y / size.height,
                                    )
                                    val displayPanels = if (movingPanel != null && previewPanel != null) {
                                        panels.map { if (it == movingPanel) previewPanel else it }
                                    } else {
                                        panels
                                    }

                                    Canvas(modifier = Modifier.matchParentSize()) {
                                        drawImage(image = image, dstSize = IntSize(size.width.toInt(), size.height.toInt()))
                                        if (movingPanel != null && previewPanel != null) {
                                            drawRect(
                                                color = Color.Transparent,
                                                topLeft = Offset(movingPanel.left * size.width, movingPanel.top * size.height),
                                                size = Size(movingPanel.width * size.width, movingPanel.height * size.height),
                                                blendMode = BlendMode.Clear,
                                            )
                                            drawImage(
                                                image = image,
                                                srcOffset = IntOffset(
                                                    (movingPanel.left * image.width).roundToInt(),
                                                    (movingPanel.top * image.height).roundToInt(),
                                                ),
                                                srcSize = IntSize(
                                                    (movingPanel.width * image.width).roundToInt(),
                                                    (movingPanel.height * image.height).roundToInt(),
                                                ),
                                                dstOffset = IntOffset(
                                                    (previewPanel.left * size.width).roundToInt(),
                                                    (previewPanel.top * size.height).roundToInt(),
                                                ),
                                                dstSize = IntSize(
                                                    (previewPanel.width * size.width).roundToInt(),
                                                    (previewPanel.height * size.height).roundToInt(),
                                                ),
                                            )
                                        }
                                        effective.forEach { balloon ->
                                            val displayBalloon = if (movingPanel?.contains(
                                                    balloon.centerX,
                                                    balloon.centerY,
                                                ) == true && previewPanel != null
                                            ) {
                                                balloon.translatedBetween(movingPanel, previewPanel)
                                            } else {
                                                balloon
                                            }
                                            val panel = displayPanels.ownerPanel(
                                                displayBalloon.centerX,
                                                displayBalloon.centerY,
                                            )
                                            clipToPanel(panel, size) {
                                                drawBalloon(
                                                    displayBalloon,
                                                    size,
                                                    bodyColor = Color.White,
                                                    outlineColor = Color.Black,
                                                )
                                            }
                                        }
                                    }
                                    effective.forEach { balloon ->
                                        val displayBalloon = if (movingPanel?.contains(
                                                balloon.centerX,
                                                balloon.centerY,
                                            ) == true && previewPanel != null
                                        ) {
                                            balloon.translatedBetween(movingPanel, previewPanel)
                                        } else {
                                            balloon
                                        }
                                        val panel = displayPanels.ownerPanel(
                                            displayBalloon.centerX,
                                            displayBalloon.centerY,
                                        )
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
                                                balloon = displayBalloon,
                                                canvasSize = size,
                                                origin = bounds?.topLeft ?: Offset.Zero,
                                                editable = editMode,
                                                autoSize = autoTextSize,
                                                contentScale = textScale,
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
                                    selectedPanel?.takeIf { focusedPanel == null }?.let { pending ->
                                        ImageMoveHandle(
                                            centerPx = Offset(
                                                (pending.left + pending.width / 2f) * size.width,
                                                pending.top * size.height,
                                            ) + moveHandleOffset,
                                            contentScale = 1f,
                                            onDrag = { delta ->
                                                moveHandleOffset += delta
                                            },
                                            onDragEnd = {
                                                if (moveHandleOffset != Offset.Zero) {
                                                    onMoveImage(
                                                        pending,
                                                        pending.copy(
                                                            left = pending.left + moveHandleOffset.x / size.width,
                                                            top = pending.top + moveHandleOffset.y / size.height,
                                                        ),
                                                    )
                                                }
                                                moveHandleOffset = Offset.Zero
                                            },
                                        )
                                        ImageDeleteHandle(
                                            centerPx = Offset(
                                                (pending.left + pending.width) * size.width,
                                                pending.top * size.height,
                                            ),
                                            contentScale = 1f,
                                            onTap = { showConfirmDeleteImage = true },
                                        )
                                    }
                                    if (showAddPanelHandles(focusedPanel, selectedPanel)) {
                                        edgeImagePlacements(panels).forEach { placement ->
                                            val anchor = placement.anchor
                                            val center = when (placement.position) {
                                                ImagePosition.RIGHT -> Offset(
                                                    (anchor.left + anchor.width) * size.width,
                                                    (anchor.top + anchor.height / 2f) * size.height,
                                                )
                                                ImagePosition.BOTTOM -> Offset(
                                                    (anchor.left + anchor.width / 2f) * size.width,
                                                    (anchor.top + anchor.height) * size.height,
                                                )
                                                else -> return@forEach
                                            }
                                            ImageAddEdgeButton(
                                                centerPx = center,
                                                position = placement.position,
                                                onClick = { onAddImageAt(placement) },
                                            )
                                        }
                                    }
                                }
                            }
                            }
                            }
                            if (editMode && selected != null && layerSize.width > 0 && layerSize.height > 0) {
                                val handleCanvasSize = Size(layerSize.width.toFloat(), layerSize.height.toFloat())
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
                                        .graphicsLayer {
                                            rotationZ = rotation
                                            transformOrigin = rotationOrigin
                                        },
                                ) {
                                    val ownerBounds = panels
                                        .ownerPanel(selected.centerX, selected.centerY)
                                        ?.panelBounds(handleCanvasSize)
                                    Handles(
                                        balloon = selected,
                                        canvasSize = handleCanvasSize,
                                        imageBounds = ownerBounds,
                                        contentScale = 1f,
                                        base = { live ?: selected },
                                        onLiveChange = { live = it },
                                        onCommit = { live?.let(onCommitBalloon) },
                                        onDelete = onDeleteSelected,
                                    )
                                }
                            }
                            if (focusedPanel != null) {
                                FocusNavigation(
                                    adjacentPanels = adjacentPanels(panels, focusedPanel),
                                    onFocusPanel = onFocusPanel,
                                    modifier = Modifier.matchParentSize(),
                                )
                                if (selectedPanel == focusedPanel) {
                                    val density = LocalDensity.current
                                    ImageDeleteHandle(
                                        centerPx = Offset(
                                            with(density) { fitWidth.toPx() },
                                            0f,
                                        ),
                                        contentScale = 1f,
                                        onTap = { showConfirmDeleteImage = true },
                                    )
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
                    ComicKit(
                        expanded = comicKitExpanded,
                        onToggleExpanded = { comicKitExpanded = !comicKitExpanded },
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
        }
    }
    if (showConfirmDeleteImage) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteImage = false },
            title = { Text(stringResource(R.string.delete_panel_title)) },
            text = { Text(stringResource(R.string.delete_panel_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedPanel?.let(onDeleteImage)
                        onSelectPanel(null)
                        showConfirmDeleteImage = false
                    },
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteImage = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

internal fun imageFocusTarget(
    panels: List<RectFraction>,
    selectedPanel: RectFraction?,
    focusedPanel: RectFraction?,
): RectFraction? = if (focusedPanel == null) selectedPanel ?: panels.firstOrNull() else null

internal fun showAddPanelHandles(
    focusedPanel: RectFraction?,
    selectedPanel: RectFraction?,
): Boolean = focusedPanel == null && selectedPanel == null

internal fun rotationTarget(
    panels: List<RectFraction>,
    selectedPanel: RectFraction?,
    focusedPanel: RectFraction?,
): RectFraction? = focusedPanel ?: selectedPanel ?: panels.singleOrNull()

internal fun List<RectFraction>.ownerPanel(x: Float, y: Float): RectFraction? =
    panelAt(x, y) ?: minByOrNull { panel ->
        val dx = x - (panel.left + panel.width / 2f)
        val dy = y - (panel.top + panel.height / 2f)
        dx * dx + dy * dy
    }

internal data class FocusLayout(
    val contentWidth: Float,
    val contentHeight: Float,
    val offsetX: Float,
    val offsetY: Float,
)

internal fun RectFraction.focusLayout(
    viewportWidth: Float,
    viewportHeight: Float,
    quarterTurned: Boolean = false,
): FocusLayout {
    val contentWidth = (if (quarterTurned) viewportHeight else viewportWidth) / width
    val contentHeight = (if (quarterTurned) viewportWidth else viewportHeight) / height
    return FocusLayout(
        contentWidth = contentWidth,
        contentHeight = contentHeight,
        offsetX = viewportWidth / 2f - (left + width / 2f) * contentWidth,
        offsetY = viewportHeight / 2f - (top + height / 2f) * contentHeight,
    )
}

internal fun adjacentPanels(
    panels: List<RectFraction>,
    focusedPanel: RectFraction,
): Map<ImagePosition, RectFraction> {
    val focusedCenterX = focusedPanel.left + focusedPanel.width / 2f
    val focusedCenterY = focusedPanel.top + focusedPanel.height / 2f
    return ImagePosition.entries.mapNotNull { position ->
        val nearest = panels.asSequence().filter { it != focusedPanel }.filter { panel ->
            val centerX = panel.left + panel.width / 2f
            val centerY = panel.top + panel.height / 2f
            when (position) {
                ImagePosition.LEFT -> centerX < focusedCenterX
                ImagePosition.RIGHT -> centerX > focusedCenterX
                ImagePosition.TOP -> centerY < focusedCenterY
                ImagePosition.BOTTOM -> centerY > focusedCenterY
            }
        }.minByOrNull { panel ->
            val dx = panel.left + panel.width / 2f - focusedCenterX
            val dy = panel.top + panel.height / 2f - focusedCenterY
            when (position) {
                ImagePosition.LEFT, ImagePosition.RIGHT -> dx * dx + dy * dy * 2f
                ImagePosition.TOP, ImagePosition.BOTTOM -> dy * dy + dx * dx * 2f
            }
        }
        nearest?.let { position to it }
    }.toMap()
}

@Composable
private fun FocusNavigation(
    adjacentPanels: Map<ImagePosition, RectFraction>,
    onFocusPanel: (RectFraction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.zIndex(2f)) {
        adjacentPanels.forEach { (position, panel) ->
            val alignment = when (position) {
                ImagePosition.LEFT -> Alignment.CenterStart
                ImagePosition.RIGHT -> Alignment.CenterEnd
                ImagePosition.TOP -> Alignment.TopCenter
                ImagePosition.BOTTOM -> Alignment.BottomCenter
            }
            val icon = when (position) {
                ImagePosition.LEFT -> Icons.Default.KeyboardArrowLeft
                ImagePosition.RIGHT -> Icons.Default.KeyboardArrowRight
                ImagePosition.TOP -> Icons.Default.KeyboardArrowUp
                ImagePosition.BOTTOM -> Icons.Default.KeyboardArrowDown
            }
            val edgeOffset = focusNavigationOffset(position)
            val description = stringResource(R.string.show_panel_direction, position.label())
            Box(
                modifier = Modifier
                    .align(alignment)
                    .offset(x = edgeOffset.x, y = edgeOffset.y)
                    .size(30.dp)
                    .background(Color(0xFFFFD21F), CircleShape)
                    .border(2.dp, InkBlack, CircleShape)
                    .clickable(onClickLabel = description) {
                        onFocusPanel(panel)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = description,
                    tint = InkBlack,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

internal fun focusNavigationOffset(position: ImagePosition): DpOffset = when (position) {
    ImagePosition.LEFT -> DpOffset((-15).dp, 0.dp)
    ImagePosition.RIGHT -> DpOffset(15.dp, 0.dp)
    ImagePosition.TOP -> DpOffset(0.dp, (-15).dp)
    ImagePosition.BOTTOM -> DpOffset(0.dp, 15.dp)
}

private fun Balloon.translatedBetween(from: RectFraction, to: RectFraction): Balloon = copy(
    centerX = centerX + to.left - from.left,
    centerY = centerY + to.top - from.top,
)

/** The bottom "comic kit" panel: balloon types plus the currently selected balloon's controls. */
@Composable
private fun ComicKit(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
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
            .padding(bottom = if (expanded) 12.dp else 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .offset(y = (-12).dp)
                .size(width = 52.dp, height = 28.dp)
                .background(Color(0xFFFFD21F), RoundedCornerShape(8.dp))
                .border(3.dp, InkBlack, RoundedCornerShape(8.dp))
                .clickable(
                    onClickLabel = stringResource(
                        if (expanded) R.string.collapse_balloon_tools else R.string.expand_balloon_tools,
                    ),
                    onClick = onToggleExpanded,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                contentDescription = stringResource(
                    if (expanded) R.string.collapse_balloon_tools else R.string.expand_balloon_tools,
                ),
                tint = InkBlack,
                modifier = Modifier.size(22.dp),
            )
        }
        if (expanded) {
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
                ComicFieldLabel(stringResource(R.string.font_style))
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
                ComicFieldLabel(stringResource(R.string.text_size))
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
    contentScale: Float,
    onTextChange: (String) -> Unit,
    onFocused: () -> Unit,
) {
    val density = LocalDensity.current
    val left = balloon.centerX * canvasSize.width - balloon.width * canvasSize.width / 2f - origin.x
    val top = balloon.centerY * canvasSize.height - balloon.height * canvasSize.height / 2f - origin.y
    val widthDp = with(density) { (balloon.width * canvasSize.width).toDp() }
    val heightDp = with(density) { (balloon.height * canvasSize.height).toDp() }
    val textArea = balloonTextAreaPx(
        type = balloon.type,
        cornerRoundness = balloon.cornerRoundness,
        boxWidth = balloon.width * canvasSize.width,
        boxHeight = balloon.height * canvasSize.height,
        contentScale = contentScale,
    )
    val textAreaWidthDp = with(density) { textArea.width.toDp() }
    val textAreaHeightDp = with(density) { textArea.height.toDp() }

    var text by remember(balloon.id) { mutableStateOf(balloon.text) }

    val availableWidth = textArea.width.toInt().coerceAtLeast(1)
    val availableHeight = textArea.height.toInt().coerceAtLeast(1)
    val effectiveFontSize = if (autoSize) {
        rememberAutoFontSize(text, balloon.font, availableWidth, availableHeight, contentScale)
    } else {
        scaledBalloonTextDimension(balloon.fontSize, contentScale)
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
            .size(widthDp, heightDp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(textAreaWidthDp, textAreaHeightDp)
                .clipToBounds(),
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
                .onFocusChanged { if (it.isFocused) onFocused() },
            )
        }
    }
}

/** Largest font (in sp) whose text fits within the available box, for auto text sizing. */
@Composable
private fun rememberAutoFontSize(
    text: String,
    font: BalloonFont,
    availableWidth: Int,
    availableHeight: Int,
    contentScale: Float,
): Float {
    val measurer = rememberTextMeasurer()
    return remember(text, font, availableWidth, availableHeight, contentScale) {
        if (availableWidth <= 0 || availableHeight <= 0) {
            scaledBalloonTextDimension(AUTO_MIN_FONT_SIZE, contentScale)
        } else {
            // A blank balloon still gets a caret sized to the box via a sample glyph.
            autoFitFontSize(
                text.ifBlank { "A" },
                font,
                availableWidth,
                availableHeight,
                measurer,
                contentScale,
            )
        }
    }
}

private fun autoFitFontSize(
    text: String,
    font: BalloonFont,
    maxWidth: Int,
    maxHeight: Int,
    measurer: TextMeasurer,
    contentScale: Float = 1f,
): Float {
    val minFontSize = scaledBalloonTextDimension(AUTO_MIN_FONT_SIZE, contentScale)
    val maxFontSize = scaledBalloonTextDimension(AUTO_MAX_FONT_SIZE, contentScale)
    val step = scaledBalloonTextDimension(1f, contentScale)
    var best = minFontSize
    var candidate = minFontSize
    while (candidate <= maxFontSize) {
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
            candidate += step
        } else {
            break
        }
    }
    return best
}

private const val AUTO_MIN_FONT_SIZE = 8f
private const val AUTO_MAX_FONT_SIZE = 96f

internal fun balloonTextScale(currentContentWidth: Float, normalContentWidth: Float): Float =
    if (currentContentWidth > 0f && normalContentWidth > 0f) currentContentWidth / normalContentWidth else 1f

internal fun scaledBalloonTextDimension(baseDimension: Float, contentScale: Float): Float =
    baseDimension * contentScale.coerceAtLeast(0f)

internal data class BalloonTextArea(val width: Float, val height: Float)

internal fun balloonTextAreaPx(
    type: BalloonType,
    cornerRoundness: Float,
    boxWidth: Float,
    boxHeight: Float,
    contentScale: Float,
): BalloonTextArea {
    val gap = scaledBalloonTextDimension(if (type == BalloonType.CAPTION) 2f else 3f, contentScale)
    val (shapeWidth, shapeHeight) = when (type) {
        BalloonType.YELL -> {
            val safeFraction = 0.82f / sqrt(2f)
            boxWidth * safeFraction to boxHeight * safeFraction
        }
        BalloonType.THINK -> boxWidth * 0.68f to boxHeight * 0.68f
        BalloonType.CAPTION -> boxWidth to boxHeight
        BalloonType.SPEAK, BalloonType.WHISPER -> {
            val radius = cornerRoundness.coerceIn(0f, 1f) * minOf(boxWidth, boxHeight) / 2f
            val cornerInset = radius * (1f - 1f / sqrt(2f))
            boxWidth - 2f * cornerInset to boxHeight - 2f * cornerInset
        }
    }
    return BalloonTextArea(
        width = (shapeWidth - 2f * gap).coerceAtLeast(1f),
        height = (shapeHeight - 2f * gap).coerceAtLeast(1f),
    )
}

@Composable
private fun Handles(
    balloon: Balloon,
    canvasSize: Size,
    imageBounds: Rect?,
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

    // Move handle (top-center).
    val moveHandleCenter = Offset(center.x, center.y - halfY)
    DragHandle(
        centerPx = visibleHandleCenter(moveHandleCenter, imageBounds),
        sizeDp = 26.dp,
        color = MaterialTheme.colorScheme.tertiary,
        shape = RoundedCornerShape(6.dp),
        borderColor = InkBlack,
        keyId = balloon.id,
        alpha = handleAlpha(moveHandleCenter, imageBounds),
        contentScale = contentScale,
        onDrag = { d ->
            val b = base()
            onLiveChange(b.copy(centerX = b.centerX + d.x / w, centerY = b.centerY + d.y / h))
        },
        onDragEnd = onCommit,
    ) {
        Icon(
            imageVector = BalloonerIcons.Move,
            contentDescription = stringResource(R.string.move_balloon),
            tint = InkBlack,
            modifier = Modifier.size(18.dp),
        )
    }

    // Resize handles (four corners).
    val corners = listOf(
        Corner(Offset(center.x - halfX, center.y - halfY), -1f, -1f),
        Corner(Offset(center.x + halfX, center.y - halfY), 1f, -1f),
        Corner(Offset(center.x - halfX, center.y + halfY), -1f, 1f),
        Corner(Offset(center.x + halfX, center.y + halfY), 1f, 1f),
    )
    corners.forEach { corner ->
        DragHandle(
            centerPx = visibleHandleCenter(corner.pos, imageBounds),
            sizeDp = 22.dp,
            color = MaterialTheme.colorScheme.secondary,
            shape = CircleShape,
            keyId = balloon.id,
            alpha = handleAlpha(corner.pos, imageBounds),
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
        val tailTip = balloon.tailTip(canvasSize)
        DragHandle(
            centerPx = visibleHandleCenter(tailTip, imageBounds),
            sizeDp = 28.dp,
            color = Color(0xFF00C9B1),
            shape = CircleShape,
            keyId = balloon.id,
            alpha = handleAlpha(tailTip, imageBounds),
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
        val tailBaseHandle = balloon.tailBaseHandle(canvasSize)
        DragHandle(
            centerPx = visibleHandleCenter(tailBaseHandle, imageBounds),
            sizeDp = 24.dp,
            color = Color(0xFF2ECC71),
            shape = CircleShape,
            keyId = balloon.id,
            alpha = handleAlpha(tailBaseHandle, imageBounds),
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
    val deleteHandleCenter = Offset(center.x + halfX, center.y - halfY)
    TapHandle(
        centerPx = visibleHandleCenter(deleteHandleCenter, imageBounds),
        sizeDp = 26.dp,
        color = Color(0xFFE8325A),
        alpha = handleAlpha(deleteHandleCenter, imageBounds),
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
            contentDescription = stringResource(R.string.delete_panel),
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
            .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(6.dp))
            .border(2.dp, InkBlack, RoundedCornerShape(6.dp))
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
            imageVector = BalloonerIcons.Move,
            contentDescription = stringResource(R.string.move_panel),
            tint = InkBlack,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Adds an image directly beside the panel edge where this button is shown. */
@Composable
private fun ImageAddEdgeButton(centerPx: Offset, position: ImagePosition, onClick: () -> Unit) {
    val density = LocalDensity.current
    val halfPx = with(density) { 15.dp.toPx() }
    val description = stringResource(R.string.add_panel_direction, position.label())
    Box(
        modifier = Modifier
            .offset { IntOffset((centerPx.x - halfPx).roundToInt(), (centerPx.y - halfPx).roundToInt()) }
            .size(30.dp)
            .zIndex(2f)
            .background(MaterialTheme.colorScheme.tertiary, CircleShape)
            .border(2.dp, InkBlack, CircleShape)
            .clickable(
                onClickLabel = description,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = description,
            tint = InkBlack,
            modifier = Modifier.size(20.dp),
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
    borderColor: Color = Color.White,
    keyId: Long,
    alpha: Float,
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
            .alpha(alpha)
            .graphicsLayer {
                scaleX = fixedControlScale(contentScale)
                scaleY = fixedControlScale(contentScale)
            }
            .border(2.dp, borderColor, shape)
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
    alpha: Float,
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
            .alpha(alpha)
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

internal fun handleAlpha(center: Offset, imageBounds: Rect?): Float =
    if (imageBounds == null || imageBounds.contains(center)) 1f else 0.55f

internal fun visibleHandleCenter(center: Offset, imageBounds: Rect?): Offset =
    imageBounds?.let { bounds ->
        Offset(
            x = center.x.coerceIn(bounds.left, bounds.right),
            y = center.y.coerceIn(bounds.top, bounds.bottom),
        )
    } ?: center

@Composable
private fun BalloonType.label(): String = stringResource(when (this) {
    BalloonType.SPEAK -> R.string.balloon_type_speak
    BalloonType.THINK -> R.string.balloon_type_think
    BalloonType.WHISPER -> R.string.balloon_type_whisper
    BalloonType.YELL -> R.string.balloon_type_yell
    BalloonType.CAPTION -> R.string.balloon_type_caption
})

@Composable
private fun ImagePosition.label(): String = stringResource(when (this) {
    ImagePosition.LEFT -> R.string.direction_left
    ImagePosition.RIGHT -> R.string.direction_right
    ImagePosition.TOP -> R.string.direction_top
    ImagePosition.BOTTOM -> R.string.direction_bottom
})

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

/** Renders the image + balloons (with text) at native resolution and writes the encoded result. */
private suspend fun exportComic(
    context: Context,
    imageUri: String,
    balloons: List<Balloon>,
    panels: List<RectFraction>,
    displayedWidth: Int,
    autoTextSize: Boolean,
    textMeasurer: TextMeasurer,
    density: Density,
    compressFormat: Bitmap.CompressFormat,
    openOutputStream: () -> OutputStream?,
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
            openOutputStream()?.use { stream ->
                output.asAndroidBitmap().compress(compressFormat, 100, stream)
            } ?: return@runCatching false
            true
        }.getOrDefault(false)
    }
}

internal fun shareFileName(projectName: String): String {
    val safeName = projectName
        .trim()
        .replace(Regex("[^\\p{L}\\p{N}._-]+"), "_")
        .trim('.', '_')
        .take(80)
    return "${safeName.ifBlank { "comic" }}.jpg"
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

internal fun RectFraction.panelBounds(canvasSize: Size): Rect = Rect(
    left = left * canvasSize.width,
    top = top * canvasSize.height,
    right = (left + width) * canvasSize.width,
    bottom = (top + height) * canvasSize.height,
)

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
    val textArea = balloonTextAreaPx(
        type = balloon.type,
        cornerRoundness = balloon.cornerRoundness,
        boxWidth = boxW,
        boxHeight = boxH,
        contentScale = scale,
    )
    val maxW = textArea.width.toInt().coerceAtLeast(1)
    val maxH = textArea.height.toInt().coerceAtLeast(1)
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
        onOpenSettings = {},
        onRenameProject = {},
        onInitialImagesPicked = {},
        onImagePicked = {},
        onAddImage = { _, _ -> },
        onAddBalloon = { _, _ -> },
        onSelectBalloon = {},
        onCommitBalloon = {},
        onDeleteSelected = {},
        onDeleteComic = {},
        onDeleteImage = {},
        onMoveImage = { _, _ -> },
    )
}
