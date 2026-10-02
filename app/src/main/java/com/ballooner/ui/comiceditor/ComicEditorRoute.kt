package com.ballooner.ui.comiceditor

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.unit.IntSize
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ballooner.ui.comic.comicPixelWidth
import com.ballooner.ui.comic.loadImagesForExport
import com.ballooner.ui.comic.rememberPanelImageSource
import com.ballooner.ui.comic.renderComic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PNG_TYPE = "image/png"

/** Connects the comic editor to navigation, image picking, and the rest of the app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicEditorRoute(
    onNavigateBack: () -> Unit,
    viewModel: ComicEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val content = state as? ComicEditorUiState.Content
    val sourceUris = content?.comic?.panels?.mapNotNull { it.image?.sourceUri }?.toSet().orEmpty()
    val images = rememberPanelImageSource(sourceUris)
    val actions = remember(viewModel) { viewModel.asActions() }

    // Which panel the picker was opened for, so its result knows where to land.
    var pickingFor by remember { mutableStateOf<Int?>(null) }
    // Picking is plural: one trip can fill the whole page. How many the picker offers is left to
    // the platform, and anything picked beyond the panels there are to fill is simply not used.
    val pickContract = remember { ActivityResultContracts.PickMultipleVisualMedia() }
    val picker = rememberLauncherForActivityResult(pickContract) { uris ->
        val panel = pickingFor
        pickingFor = null
        if (uris.isNotEmpty() && panel != null) {
            viewModel.importPanelImages(panel, uris.map { it.toString() })
        }
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val scope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(PNG_TYPE)) { target ->
        val comic = content?.comic
        if (target == null || comic == null) return@rememberLauncherForActivityResult
        exporting = true
        scope.launch {
            // Export decodes its own, larger copies: the editing ones are deliberately small.
            val full = loadImagesForExport(context, sourceUris)
            val written = withContext(Dispatchers.Default) {
                runCatching {
                    val bitmap = renderComic(
                        comic = comic,
                        images = full,
                        pixelWidth = comicPixelWidth(comic) { uri ->
                            full.bitmapFor(uri)?.let { IntSize(it.width, it.height) }
                        },
                        density = density,
                        fontFamilyResolver = fontFamilyResolver,
                    ).asAndroidBitmap()
                    context.contentResolver.openOutputStream(target)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    } ?: false
                }.getOrDefault(false)
            }
            exporting = false
            Toast.makeText(
                context,
                if (written) "Comic saved" else "Could not save the comic",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val saveComic = {
        // Read at the moment of saving rather than from this composition: the title dialog sets
        // the name and exports in one go, before any recomposition could refresh a captured one.
        val named = (viewModel.uiState.value as? ComicEditorUiState.Content)?.comic?.name
        exporter.launch("${named.orEmpty().ifBlank { "comic" }}.png")
    }

    Scaffold(
        topBar = {
            // The expanded Panel preview takes the whole screen, so the bar stands aside for it.
            if (content?.expandedPreview != true) {
                EditorTopBar(
                    name = content?.comic?.name.orEmpty(),
                    onName = viewModel::setName,
                    onNavigateBack = onNavigateBack,
                    onSave = saveComic,
                    canSave = content != null && !exporting,
                )
            }
        },
    ) { padding ->
        ComicEditorScreen(
            state = state,
            images = images,
            actions = actions,
            modifier = Modifier.fillMaxSize().padding(padding),
            onPickImage = { panel ->
                pickingFor = panel
                picker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onSave = saveComic,
        )
    }
}

/** The comic's own bar: its name, the way out, and the way to a PNG. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    name: String,
    onName: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onSave: () -> Unit,
    canSave: Boolean,
) {
    TopAppBar(
        title = {
            TextField(
                value = name,
                onValueChange = onName,
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = onSave, enabled = canSave) {
                Icon(Icons.Default.Share, contentDescription = "Save as PNG")
            }
        },
    )
}
