package com.ballooner.ui.comiclist

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ballooner.data.comic.SavedComic
import com.ballooner.ui.comic.ComicThumbnail
import com.ballooner.ui.comic.PanelImageSource
import com.ballooner.ui.comic.THUMBNAIL_EDGE_PIXELS
import com.ballooner.ui.comic.rememberPanelImageSource
import com.ballooner.ui.comic.shareComicPng
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun ComicListRoute(
    onOpenComic: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ComicListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ComicListScreen(
        state = state,
        onOpenComic = onOpenComic,
        onCreateComic = { viewModel.createComic(onOpenComic) },
        onDeleteComic = viewModel::deleteComic,
        onRenameComic = viewModel::renameComic,
        onOpenSettings = onOpenSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicListScreen(
    state: ComicListUiState,
    onOpenComic: (Long) -> Unit,
    onCreateComic: () -> Unit,
    onDeleteComic: (Long) -> Unit,
    onRenameComic: (Long, String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<SavedComic?>(null) }
    var renaming by remember { mutableStateOf<Pair<Long, String>?>(null) }
    // Rendering a comic at full size takes a noticeable moment, so the comic says it is working.
    var sharing by remember { mutableStateOf<Long?>(null) }

    val comics = (state as? ComicListUiState.Content)?.comics.orEmpty()
    // One source for the whole list, so two comics sharing an image decode it once between them.
    val images = rememberPanelImageSource(
        sourceUris = comics.flatMapTo(mutableSetOf()) { saved ->
            saved.comic.panels.mapNotNull { it.image?.sourceUri }
        },
        maxEdge = THUMBNAIL_EDGE_PIXELS,
    )

    val context = LocalContext.current
    val density = LocalDensity.current
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ballooner") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateComic) {
                Icon(Icons.Default.Add, contentDescription = "Create comic")
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                ComicListUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                ComicListUiState.Empty -> Text(
                    text = "No comics yet. Tap + to start one.",
                    modifier = Modifier.align(Alignment.Center),
                )
                is ComicListUiState.Content -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.comics, key = { it.id }) { saved ->
                        ComicRow(
                            saved = saved,
                            images = images,
                            sharing = sharing == saved.id,
                            onOpen = { onOpenComic(saved.id) },
                            onRename = { renaming = saved.id to saved.comic.name },
                            onShare = {
                                sharing = saved.id
                                scope.launch {
                                    val shared = shareComicPng(
                                        context = context,
                                        comic = saved.comic,
                                        name = saved.comic.name.ifBlank { "Comic" },
                                        density = density,
                                        fontFamilyResolver = fontFamilyResolver,
                                    )
                                    sharing = null
                                    if (!shared) {
                                        Toast.makeText(
                                            context,
                                            "Could not share the comic",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                            },
                            onDelete = { pendingDelete = saved },
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { saved ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this comic?") },
            text = { Text("\"${saved.comic.name}\" will be removed for good.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteComic(saved.id)
                        pendingDelete = null
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Keep") } },
        )
    }

    renaming?.let { (id, title) ->
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Edit title") },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { renaming = id to it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRenameComic(id, title.trim())
                        renaming = null
                    },
                    enabled = title.isNotBlank(),
                ) {
                    Text("Save")
                }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ComicRow(
    saved: SavedComic,
    images: PanelImageSource,
    sharing: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ComicThumbnail(
                comic = saved.comic,
                images = images,
                modifier = Modifier
                    .size(THUMBNAIL_SIZE)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp)),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = saved.comic.name.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Edited ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(Date(saved.updatedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            ComicMenu(
                sharing = sharing,
                onRename = onRename,
                onShare = onShare,
                onDelete = onDelete,
            )
        }
    }
}

/** What can be done to a comic without opening it. */
@Composable
private fun ComicMenu(
    sharing: Boolean,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        if (sharing) {
            // In the button's place, so the row neither jumps nor offers a second go at it.
            Box(modifier = Modifier.size(SHARING_SPINNER_BOX), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(SHARING_SPINNER), strokeWidth = 2.dp)
            }
        } else {
            IconButton(onClick = { open = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Comic options")
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Edit title") },
                onClick = {
                    open = false
                    onRename()
                },
            )
            DropdownMenuItem(
                text = { Text("Share as PNG") },
                onClick = {
                    open = false
                    onShare()
                },
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    open = false
                    onDelete()
                },
            )
        }
    }
}

/** Every comic is listed at one size, whatever shape its page is. */
private val THUMBNAIL_SIZE = 64.dp

/** The spinner stands exactly where the options button was, so nothing moves. */
private val SHARING_SPINNER_BOX = 48.dp
private val SHARING_SPINNER = 20.dp
