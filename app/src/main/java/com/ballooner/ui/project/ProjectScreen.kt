package com.ballooner.ui.project

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ballooner.domain.model.Balloon
import com.ballooner.domain.model.BalloonFont
import com.ballooner.domain.model.BalloonType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

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
        onAddBalloon = viewModel::addBalloon,
        onSelectBalloon = viewModel::selectBalloon,
        onCommitBalloon = viewModel::commitBalloon,
        onDeleteSelected = viewModel::deleteSelectedBalloon,
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
    onAddBalloon: (BalloonType) -> Unit,
    onSelectBalloon: (Long?) -> Unit,
    onCommitBalloon: (Balloon) -> Unit,
    onDeleteSelected: () -> Unit,
) {
    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) onImagePicked(uri.toString())
    }
    val launchPicker = {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    // Edit mode shows the balloon controls; view mode shows the flat result.
    var editMode by remember { mutableStateOf(true) }
    // A freshly created project jumps straight to image selection.
    LaunchedEffect(Unit) {
        if (autoOpenPicker) launchPicker()
    }
    // Keep a balloon selected in edit mode so the controls stay visible.
    LaunchedEffect(editMode, uiState.balloons, uiState.selectedBalloonId) {
        if (editMode && uiState.selectedBalloonId == null && uiState.balloons.isNotEmpty()) {
            onSelectBalloon(uiState.balloons.last().id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (editMode) {
                        EditableTitle(name = projectName, onRename = onRenameProject)
                    } else {
                        Text(projectName.ifBlank { "Untitled" })
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.hasImage && editMode) {
                        IconButton(onClick = { launchPicker() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Change image")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!uiState.hasImage) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = { launchPicker() }) { Text("Open image") }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Toolbar(
                        editMode = editMode,
                        onAddBalloon = onAddBalloon,
                        onToggleMode = { editMode = !editMode },
                    )
                    Editor(
                        imageUri = uiState.imageUri!!,
                        balloons = uiState.balloons,
                        selectedBalloonId = uiState.selectedBalloonId,
                        editMode = editMode,
                        onSelectBalloon = onSelectBalloon,
                        onCommitBalloon = onCommitBalloon,
                        onDeleteSelected = onDeleteSelected,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
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
        textStyle = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { inner ->
            if (text.isEmpty()) {
                Text(
                    text = "Title",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            inner()
        },
    )
}

@Composable
private fun Toolbar(
    editMode: Boolean,
    onAddBalloon: (BalloonType) -> Unit,
    onToggleMode: () -> Unit,
) {
    var type by remember { mutableStateOf(BalloonType.SPEAK) }
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (editMode) {
            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(type.label())
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    BalloonType.entries.forEach { entry ->
                        DropdownMenuItem(
                            text = { Text(entry.label()) },
                            onClick = {
                                type = entry
                                expanded = false
                            },
                        )
                    }
                }
            }
            Button(onClick = { onAddBalloon(type) }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(text = "Add", modifier = Modifier.padding(start = 4.dp))
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onToggleMode) {
            Icon(
                imageVector = if (editMode) Icons.Default.Done else Icons.Default.Edit,
                contentDescription = if (editMode) "Switch to view mode" else "Switch to edit mode",
            )
        }
    }
}

@Composable
private fun Editor(
    imageUri: String,
    balloons: List<Balloon>,
    selectedBalloonId: Long?,
    editMode: Boolean,
    onSelectBalloon: (Long?) -> Unit,
    onCommitBalloon: (Balloon) -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val image = rememberImageBitmap(imageUri)
    var layerSize by remember { mutableStateOf(IntSize.Zero) }
    // Working copy of the selected balloon. Seeded when the selection changes and
    // kept across gestures so size / position / tail / text edits accumulate
    // instead of overwriting each other before Room has round-tripped a commit.
    val selectedPersisted = balloons.firstOrNull { it.id == selectedBalloonId }
    var live by remember(selectedBalloonId) { mutableStateOf(selectedPersisted) }
    val effective = balloons.map { b -> live?.takeIf { it.id == b.id } ?: b }
    val selected = effective.firstOrNull { it.id == selectedBalloonId }

    Column(modifier = modifier.fillMaxSize().padding(8.dp)) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            if (image == null) {
                Text("Loading image\u2026")
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(image.width.toFloat() / image.height.toFloat())
                        .onSizeChanged { layerSize = it }
                        .pointerInput(balloons, editMode) {
                            detectTapGestures { offset ->
                                if (!editMode) return@detectTapGestures
                                val canvas = Size(size.width.toFloat(), size.height.toFloat())
                                val hit = effective.lastOrNull { it.containsPoint(offset, canvas) }
                                if (hit != null) onSelectBalloon(hit.id)
                            }
                        },
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawImage(image = image, dstSize = IntSize(size.width.toInt(), size.height.toInt()))
                        effective.forEach { balloon ->
                            drawBalloon(balloon, size, bodyColor = Color.White, outlineColor = Color.Black)
                        }
                    }

                    val size = Size(layerSize.width.toFloat(), layerSize.height.toFloat())
                    if (size.width > 0f && size.height > 0f) {
                        effective.forEach { balloon ->
                            BalloonText(
                                balloon = balloon,
                                canvasSize = size,
                                editable = editMode,
                                onTextChange = { newText ->
                                    val current = live?.takeIf { it.id == balloon.id } ?: balloon
                                    val updated = current.copy(text = newText)
                                    if (selectedBalloonId == balloon.id) live = updated
                                    onCommitBalloon(updated)
                                },
                                onFocused = { onSelectBalloon(balloon.id) },
                            )
                        }

                        if (editMode) {
                            selected?.let { sel ->
                                Handles(
                                    balloon = sel,
                                    canvasSize = size,
                                    base = { live ?: sel },
                                    onLiveChange = { live = it },
                                    onCommit = { live?.let(onCommitBalloon) },
                                    onDelete = onDeleteSelected,
                                )
                            }
                        }
                    }
                }
            }
        }

        selected?.let { sel ->
            if (!editMode) return@let
            TextControls(
                font = sel.font,
                fontSize = sel.fontSize,
                onFontChange = {
                    live = (live ?: sel).copy(font = it)
                    live?.let(onCommitBalloon)
                },
                onSizeChange = { live = (live ?: sel).copy(fontSize = it) },
                onSizeChangeFinished = { live?.let(onCommitBalloon) },
            )
            if (sel.type == BalloonType.SPEAK || sel.type == BalloonType.WHISPER) {
                ShapeSlider(
                    roundness = sel.cornerRoundness,
                    onChange = { live = (live ?: sel).copy(cornerRoundness = it) },
                    onChangeFinished = { live?.let(onCommitBalloon) },
                )
            }
        }
    }
}

@Composable
private fun TextControls(
    font: BalloonFont,
    fontSize: Float,
    onFontChange: (BalloonFont) -> Unit,
    onSizeChange: (Float) -> Unit,
    onSizeChangeFinished: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        var expanded by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(font.label())
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
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
        Text(text = "Size", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = fontSize,
            onValueChange = onSizeChange,
            onValueChangeFinished = onSizeChangeFinished,
            valueRange = 8f..48f,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ShapeSlider(
    roundness: Float,
    onChange: (Float) -> Unit,
    onChangeFinished: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "Shape", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = roundness,
            onValueChange = onChange,
            onValueChangeFinished = onChangeFinished,
            valueRange = 0f..1f,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BalloonText(
    balloon: Balloon,
    canvasSize: Size,
    editable: Boolean,
    onTextChange: (String) -> Unit,
    onFocused: () -> Unit,
) {
    val density = LocalDensity.current
    val left = balloon.centerX * canvasSize.width - balloon.width * canvasSize.width / 2f
    val top = balloon.centerY * canvasSize.height - balloon.height * canvasSize.height / 2f
    val widthDp = with(density) { (balloon.width * canvasSize.width).toDp() }
    val heightDp = with(density) { (balloon.height * canvasSize.height).toDp() }
    val innerPadding = if (balloon.type == BalloonType.YELL) 18.dp else 12.dp

    var text by remember(balloon.id) { mutableStateOf(balloon.text) }

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
                fontSize = balloon.fontSize.sp,
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

@Composable
private fun Handles(
    balloon: Balloon,
    canvasSize: Size,
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

    // Tail handle (drag the tip to set both direction and length).
    DragHandle(
        centerPx = balloon.tailTip(canvasSize),
        sizeDp = 28.dp,
        color = Color(0xFF00C9B1),
        shape = CircleShape,
        keyId = balloon.id,
        onDrag = { d ->
            val b = base()
            val target = b.tailTip(canvasSize) + d
            onLiveChange(b.tailAtPoint(target, canvasSize))
        },
        onDragEnd = onCommit,
    )

    // Tail-width handle (drag sideways to set the tail thickness). Not for Think,
    // whose tail is made of bubbles.
    if (balloon.type != BalloonType.THINK) {
        DragHandle(
            centerPx = balloon.tailBaseHandle(canvasSize),
            sizeDp = 24.dp,
            color = Color(0xFF2ECC71),
            shape = CircleShape,
            keyId = balloon.id,
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
        onTap = onDelete,
    ) {
        Text(text = "\u00D7", color = Color.White)
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
            .border(2.dp, Color.White, shape)
            .background(color, shape)
            .pointerInput(keyId) {
                detectDragGestures(
                    onDragEnd = onDragEnd,
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
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
    onTap: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val halfPx = with(density) { (sizeDp / 2).toPx() }
    Box(
        modifier = Modifier
            .offset { IntOffset((centerPx.x - halfPx).roundToInt(), (centerPx.y - halfPx).roundToInt()) }
            .size(sizeDp)
            .border(2.dp, Color.White, CircleShape)
            .background(color, CircleShape)
            .pointerInput(Unit) { detectTapGestures { onTap() } },
        contentAlignment = Alignment.Center,
    ) { content() }
}

private fun BalloonType.label(): String = when (this) {
    BalloonType.SPEAK -> "Speak"
    BalloonType.THINK -> "Think"
    BalloonType.WHISPER -> "Whisper"
    BalloonType.YELL -> "Yell"
}

private fun BalloonFont.label(): String = when (this) {
    BalloonFont.DEFAULT -> "Default"
    BalloonFont.SANS_SERIF -> "Sans serif"
    BalloonFont.SERIF -> "Serif"
    BalloonFont.MONOSPACE -> "Fixed width"
    BalloonFont.CURSIVE -> "Cursive"
    BalloonFont.WIDE -> "Wide"
    BalloonFont.NARROW -> "Narrow"
    BalloonFont.COMIC_SANS_MS -> "Comic Sans MS"
    BalloonFont.GARAMOND -> "Garamond"
    BalloonFont.GEORGIA -> "Georgia"
    BalloonFont.TAHOMA -> "Tahoma"
    BalloonFont.TREBUCHET -> "Trebuchet"
    BalloonFont.VERDANA -> "Verdana"
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
}

@Composable
private fun rememberImageBitmap(uri: String?): ImageBitmap? {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = null, uri) {
        value = uri?.let { loadBitmap(context, it) }
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
        onAddBalloon = {},
        onSelectBalloon = {},
        onCommitBalloon = {},
        onDeleteSelected = {},
    )
}
