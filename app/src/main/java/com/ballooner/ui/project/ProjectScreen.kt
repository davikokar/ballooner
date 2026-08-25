package com.ballooner.ui.project

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ballooner.domain.model.Balloon
import com.ballooner.domain.model.BalloonType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProjectRoute(
    projectId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ProjectViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProjectScreen(
        title = "Project #$projectId",
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onImagePicked = viewModel::onImagePicked,
        onAddBalloon = viewModel::addBalloon,
        onSelectBalloon = viewModel::selectBalloon,
        onMoveSelected = viewModel::moveSelectedBy,
        onDeleteSelected = viewModel::deleteSelectedBalloon,
        onTypeChange = viewModel::setType,
        onTextChange = viewModel::setText,
        onSizeChange = viewModel::setSize,
        onTailAngleChange = viewModel::setTailAngle,
        onTailLengthChange = viewModel::setTailLength,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(
    title: String,
    uiState: ProjectUiState,
    onNavigateBack: () -> Unit,
    onImagePicked: (String) -> Unit,
    onAddBalloon: (BalloonType) -> Unit,
    onSelectBalloon: (Long?) -> Unit,
    onMoveSelected: (Float, Float) -> Unit,
    onDeleteSelected: () -> Unit,
    onTypeChange: (BalloonType) -> Unit,
    onTextChange: (String) -> Unit,
    onSizeChange: (Float, Float) -> Unit,
    onTailAngleChange: (Float) -> Unit,
    onTailLengthChange: (Float) -> Unit,
) {
    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) onImagePicked(uri.toString())
    }
    val launchPicker = {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.hasImage) {
                        IconButton(onClick = { launchPicker() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Change image")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!uiState.hasImage) {
                NoImage(onOpenImage = { launchPicker() })
            } else {
                Editor(
                    imageUri = uiState.imageUri!!,
                    balloons = uiState.balloons,
                    selectedBalloonId = uiState.selectedBalloonId,
                    onSelectBalloon = onSelectBalloon,
                    onMoveSelected = onMoveSelected,
                    modifier = Modifier.weight(1f),
                )
                Controls(
                    selected = uiState.selectedBalloon,
                    onAddBalloon = onAddBalloon,
                    onDeleteSelected = onDeleteSelected,
                    onTypeChange = onTypeChange,
                    onTextChange = onTextChange,
                    onSizeChange = onSizeChange,
                    onTailAngleChange = onTailAngleChange,
                    onTailLengthChange = onTailLengthChange,
                )
            }
        }
    }
}

@Composable
private fun NoImage(onOpenImage: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Button(onClick = onOpenImage) {
            Text(text = "Open image")
        }
    }
}

@Composable
private fun Editor(
    imageUri: String,
    balloons: List<Balloon>,
    selectedBalloonId: Long?,
    onSelectBalloon: (Long?) -> Unit,
    onMoveSelected: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val image = rememberImageBitmap(imageUri)
    val textMeasurer = rememberTextMeasurer()
    val bodyColor = Color.White
    val outlineColor = Color.Black
    val selectionColor = MaterialTheme.colorScheme.primary
    val textColor = Color.Black

    Box(modifier = modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
        if (image == null) {
            Text("Loading image\u2026")
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(image.width.toFloat() / image.height.toFloat())
                    .pointerInput(balloons) {
                        detectTapGestures { offset ->
                            val canvas = Size(size.width.toFloat(), size.height.toFloat())
                            val hit = balloons.lastOrNull { it.containsPoint(offset, canvas) }
                            onSelectBalloon(hit?.id)
                        }
                    }
                    .pointerInput(balloons, selectedBalloonId) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val canvas = Size(size.width.toFloat(), size.height.toFloat())
                                val hit = balloons.lastOrNull { it.containsPoint(offset, canvas) }
                                if (hit != null) onSelectBalloon(hit.id)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onMoveSelected(dragAmount.x / size.width, dragAmount.y / size.height)
                            },
                        )
                    },
            ) {
                drawImage(
                    image = image,
                    dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                )
                balloons.forEach { balloon ->
                    drawBalloon(
                        balloon = balloon,
                        canvasSize = size,
                        isSelected = balloon.id == selectedBalloonId,
                        textMeasurer = textMeasurer,
                        bodyColor = bodyColor,
                        outlineColor = outlineColor,
                        selectionColor = selectionColor,
                        textColor = textColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun Controls(
    selected: Balloon?,
    onAddBalloon: (BalloonType) -> Unit,
    onDeleteSelected: () -> Unit,
    onTypeChange: (BalloonType) -> Unit,
    onTextChange: (String) -> Unit,
    onSizeChange: (Float, Float) -> Unit,
    onTailAngleChange: (Float) -> Unit,
    onTailLengthChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(onClick = { onAddBalloon(BalloonType.SPEAK) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text(text = "Add balloon", modifier = Modifier.padding(start = 8.dp))
        }

        if (selected == null) {
            Text(
                text = "Tap a balloon to edit it, or add a new one.",
                style = MaterialTheme.typography.bodyMedium,
            )
            return@Column
        }

        Text(text = "Type", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BalloonType.entries.forEach { type ->
                FilterChip(
                    selected = selected.type == type,
                    onClick = { onTypeChange(type) },
                    label = { Text(type.label()) },
                )
            }
        }

        OutlinedTextField(
            value = selected.text,
            onValueChange = onTextChange,
            label = { Text("Text") },
            modifier = Modifier.fillMaxWidth(),
        )

        LabeledSlider(
            label = "Width",
            value = selected.width,
            valueRange = 0.1f..1f,
            onValueChange = { onSizeChange(it, selected.height) },
        )
        LabeledSlider(
            label = "Height",
            value = selected.height,
            valueRange = 0.1f..1f,
            onValueChange = { onSizeChange(selected.width, it) },
        )
        LabeledSlider(
            label = "Tail position",
            value = selected.tailAngleDegrees,
            valueRange = 0f..360f,
            onValueChange = onTailAngleChange,
        )
        LabeledSlider(
            label = "Tail length",
            value = selected.tailLength,
            valueRange = 0f..0.4f,
            onValueChange = onTailLengthChange,
        )

        OutlinedButton(onClick = onDeleteSelected, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Text(text = "Delete balloon", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}

private fun BalloonType.label(): String = when (this) {
    BalloonType.SPEAK -> "Speak"
    BalloonType.THINK -> "Think"
    BalloonType.WHISPER -> "Whisper"
    BalloonType.YELL -> "Yell"
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
        title = "Project #1",
        uiState = ProjectUiState(),
        onNavigateBack = {},
        onImagePicked = {},
        onAddBalloon = {},
        onSelectBalloon = {},
        onMoveSelected = { _, _ -> },
        onDeleteSelected = {},
        onTypeChange = {},
        onTextChange = {},
        onSizeChange = { _, _ -> },
        onTailAngleChange = {},
        onTailLengthChange = {},
    )
}
