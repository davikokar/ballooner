package com.ballooner.ui.comiceditor

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ballooner.R
import com.ballooner.ui.comic.rememberPanelImageSource
import com.ballooner.ui.comic.shareComicPng
import kotlinx.coroutines.launch

/** Connects the comic editor to navigation, image picking, and the rest of the app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicEditorRoute(
    onNavigateBack: () -> Unit,
    onOpenSettings: () -> Unit,
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
    val untitled = stringResource(R.string.untitled)
    // Rendering a comic at full size takes a noticeable moment, so the button says it is working.
    var sharing by remember { mutableStateOf(false) }

    val saveComic = {
        viewModel.saveComic { Toast.makeText(context, R.string.comic_saved, Toast.LENGTH_SHORT).show() }
    }

    val shareComic = {
        // Read at the moment of sharing rather than from this composition: the title dialog sets
        // the name and saves in one go, before any recomposition could refresh a captured one.
        val comic = (viewModel.uiState.value as? ComicEditorUiState.Content)?.comic
        if (comic != null) {
            sharing = true
            scope.launch {
                val shared = shareComicPng(
                    context = context,
                    comic = comic,
                    name = comic.name.ifBlank { untitled },
                    density = density,
                    fontFamilyResolver = fontFamilyResolver,
                )
                sharing = false
                if (!shared) {
                    Toast.makeText(context, R.string.share_comic_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Leaving with work the database has not seen is the one moment it can be lost, so it asks.
    var leaving by remember { mutableStateOf(false) }
    val leave = {
        if (content?.unsaved == true) leaving = true else onNavigateBack()
    }
    BackHandler(enabled = content?.unsaved == true) { leaving = true }

    Scaffold(
        topBar = {
            // The expanded Panel preview takes the whole screen, so the bar stands aside for it.
            if (content?.expandedPreview != true) {
                EditorTopBar(
                    name = content?.comic?.name.orEmpty(),
                    onName = viewModel::setName,
                    onNavigateBack = leave,
                    onOpenSettings = onOpenSettings,
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
            onShare = shareComic,
            sharing = sharing,
        )
    }

    if (leaving) {
        AlertDialog(
            onDismissRequest = { leaving = false },
            title = { Text(stringResource(R.string.keep_changes_title)) },
            text = { Text(stringResource(R.string.keep_changes_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        leaving = false
                        viewModel.saveComic { onNavigateBack() }
                    },
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        leaving = false
                        onNavigateBack()
                    },
                ) {
                    Text(stringResource(R.string.discard))
                }
            },
        )
    }
}

/** The comic's own bar: its name, the way out, and the way to the app's settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    name: String,
    onName: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenSettings: () -> Unit,
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        },
        actions = {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
            }
        },
    )
}
