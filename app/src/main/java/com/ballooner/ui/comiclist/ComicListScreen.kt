package com.ballooner.ui.comiclist

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ballooner.R
import com.ballooner.data.comic.SavedComic
import com.ballooner.ui.comic.ComicThumbnail
import com.ballooner.ui.comic.PNG_TYPE
import com.ballooner.ui.comic.PanelImageSource
import com.ballooner.ui.comic.THUMBNAIL_EDGE_PIXELS
import com.ballooner.ui.comic.exportComicPng
import com.ballooner.ui.comic.rememberPanelImageSource
import com.ballooner.ui.comic.shareComicPng
import kotlinx.coroutines.launch

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
        onDuplicateComic = viewModel::duplicateComic,
        onCover = viewModel::setCover,
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
    onDuplicateComic: (SavedComic) -> Unit,
    onCover: (Long, String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<SavedComic?>(null) }
    var renaming by remember { mutableStateOf<Pair<Long, String>?>(null) }
    // Rendering a comic at full size takes a noticeable moment, so the comic says it is working.
    var busy by remember { mutableStateOf<Long?>(null) }
    // Which comic the picker was opened for, so its result knows where to land.
    var coveringFor by remember { mutableStateOf<Long?>(null) }
    // Which comic the export was started for, so the chosen document knows what to hold.
    var exportingFor by remember { mutableStateOf<SavedComic?>(null) }

    val comics = (state as? ComicListUiState.Content)?.comics.orEmpty()
    // One source for the whole list, so two comics sharing an image decode it once between them.
    val images = rememberPanelImageSource(
        sourceUris = comics.flatMapTo(mutableSetOf()) { saved ->
            saved.comic.panels.mapNotNull { it.image?.sourceUri } + listOfNotNull(saved.coverUri)
        },
        maxEdge = THUMBNAIL_EDGE_PIXELS,
    )

    val pickContract = remember { ActivityResultContracts.PickVisualMedia() }
    val coverPicker = rememberLauncherForActivityResult(pickContract) { picked ->
        val id = coveringFor
        coveringFor = null
        if (picked != null && id != null) onCover(id, picked.toString())
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val scope = rememberCoroutineScope()
    val untitled = stringResource(R.string.untitled)

    // Built once: a contract made per composition re-registers the launcher behind it.
    val exportContract = remember { ActivityResultContracts.CreateDocument(PNG_TYPE) }
    val exporter = rememberLauncherForActivityResult(exportContract) { target ->
        val saved = exportingFor
        exportingFor = null
        if (target != null && saved != null) {
            busy = saved.id
            scope.launch {
                val written = exportComicPng(
                    context = context,
                    comic = saved.comic,
                    target = target,
                    density = density,
                    fontFamilyResolver = fontFamilyResolver,
                )
                busy = null
                Toast.makeText(
                    context,
                    if (written) R.string.comic_exported else R.string.comic_export_failed,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BalloonerLogo(size = TITLE_LOGO_SIZE)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.app_name))
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateComic) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_comic))
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                ComicListUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                ComicListUiState.Empty -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BalloonerLogo(size = EMPTY_LOGO_SIZE, alpha = EMPTY_LOGO_ALPHA)
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.comics_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                is ComicListUiState.Content -> LazyVerticalGrid(
                    columns = GridCells.Fixed(TILES_ACROSS),
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.comics, key = { it.id }) { saved ->
                        ComicTile(
                            saved = saved,
                            images = images,
                            busy = busy == saved.id,
                            onOpen = { onOpenComic(saved.id) },
                            onRename = { renaming = saved.id to saved.comic.name },
                            onDuplicate = { onDuplicateComic(saved) },
                            onCover = {
                                coveringFor = saved.id
                                coverPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            onShare = {
                                busy = saved.id
                                scope.launch {
                                    val shared = shareComicPng(
                                        context = context,
                                        comic = saved.comic,
                                        name = saved.comic.name.ifBlank { untitled },
                                        density = density,
                                        fontFamilyResolver = fontFamilyResolver,
                                    )
                                    busy = null
                                    if (!shared) {
                                        Toast.makeText(
                                            context,
                                            R.string.share_comic_failed,
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                            },
                            onExport = {
                                exportingFor = saved
                                exporter.launch("${saved.comic.name.ifBlank { untitled }}.png")
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
            title = { Text(stringResource(R.string.delete_comic_title)) },
            text = { Text(stringResource(R.string.delete_comic_message, saved.comic.name.ifBlank { untitled })) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteComic(saved.id)
                        pendingDelete = null
                    },
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.keep)) } },
        )
    }

    renaming?.let { (id, title) ->
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(stringResource(R.string.edit_title)) },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { renaming = id to it },
                    label = { Text(stringResource(R.string.title_hint)) },
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
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/**
 * One comic as the list shows it: its picture, then what it is called and what can be done to it.
 *
 * The picture is the cover when one has been chosen and the comic itself otherwise, and either
 * way it is cropped to the tile rather than letterboxed, so every tile is the same shape.
 */
@Composable
private fun ComicTile(
    saved: SavedComic,
    images: PanelImageSource,
    busy: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onCover: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(TILE_ASPECT)
            .clickable(onClick = onOpen),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            val cover = saved.coverUri?.let { images.bitmapFor(it) }
            if (cover != null) {
                Image(
                    bitmap = cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                ComicThumbnail(
                    comic = saved.comic,
                    images = images,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = saved.comic.name.ifBlank { stringResource(R.string.untitled) },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            ComicMenu(
                busy = busy,
                onRename = onRename,
                onDuplicate = onDuplicate,
                onCover = onCover,
                onShare = onShare,
                onExport = onExport,
                onDelete = onDelete,
            )
        }
    }
}

/** What can be done to a comic without opening it. */
@Composable
private fun ComicMenu(
    busy: Boolean,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onCover: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        if (busy) {
            // In the button's place, so the tile neither jumps nor offers a second go at it.
            Box(modifier = Modifier.size(BUSY_SPINNER_BOX), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(BUSY_SPINNER), strokeWidth = 2.dp)
            }
        } else {
            IconButton(onClick = { open = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.comic_options))
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit_title)) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = {
                    open = false
                    onRename()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit_cover)) },
                leadingIcon = { PictureGlyph(MaterialTheme.colorScheme.onSurfaceVariant) },
                onClick = {
                    open = false
                    onCover()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.duplicate)) },
                leadingIcon = { CopyGlyph(MaterialTheme.colorScheme.onSurfaceVariant) },
                onClick = {
                    open = false
                    onDuplicate()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.share_as_png)) },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                onClick = {
                    open = false
                    onShare()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.export_as_png)) },
                leadingIcon = { DownloadGlyph(MaterialTheme.colorScheme.onSurfaceVariant) },
                onClick = {
                    open = false
                    onExport()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete)) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                onClick = {
                    open = false
                    onDelete()
                },
            )
        }
    }
}

/** A framed picture, drawn because material-icons-core has no image icon. */
@Composable
private fun PictureGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(MENU_GLYPH_SIZE)) {
        val stroke = size.minDimension * 0.08f
        val frame = size.minDimension - stroke
        val left = (size.width - frame) / 2f
        val top = (size.height - frame) / 2f
        drawRoundRect(
            color = tint,
            topLeft = Offset(left, top),
            size = Size(frame, frame),
            cornerRadius = CornerRadius(frame * 0.16f),
            style = Stroke(width = stroke),
        )
        drawCircle(
            color = tint,
            radius = frame * 0.1f,
            center = Offset(left + frame * 0.32f, top + frame * 0.32f),
        )
        // A hill rising to the frame's edge, so the picture reads as a picture at 24dp.
        val base = top + frame * 0.78f
        drawPath(
            path = Path().apply {
                moveTo(left + frame * 0.12f, base)
                lineTo(left + frame * 0.46f, top + frame * 0.38f)
                lineTo(left + frame * 0.88f, base)
                close()
            },
            color = tint,
        )
    }
}

/** An arrow coming down onto a shelf: material-icons-core has no download icon. */
@Composable
private fun DownloadGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(MENU_GLYPH_SIZE)) {
        val stroke = size.minDimension * 0.08f
        val centre = size.width / 2f
        val head = size.height * 0.56f
        val wing = size.minDimension * 0.18f
        drawLine(
            color = tint,
            start = Offset(centre, size.height * 0.16f),
            end = Offset(centre, head),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawPath(
            path = Path().apply {
                moveTo(centre - wing, head - wing)
                lineTo(centre, head)
                lineTo(centre + wing, head - wing)
            },
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawLine(
            color = tint,
            start = Offset(size.width * 0.2f, size.height * 0.82f),
            end = Offset(size.width * 0.8f, size.height * 0.82f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/** Two pages, one behind the other: the shape every platform uses for "duplicate". */
@Composable
private fun CopyGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(MENU_GLYPH_SIZE)) {
        val stroke = size.minDimension * 0.08f
        val page = Size(size.minDimension * 0.62f, size.minDimension * 0.74f)
        val corner = CornerRadius(page.width * 0.16f)
        val shift = size.minDimension * 0.18f
        drawRoundRect(
            color = tint,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = page,
            cornerRadius = corner,
            style = Stroke(width = stroke),
        )
        // The front page is filled, so the two never read as one dense grid of lines.
        drawRoundRect(
            color = tint,
            topLeft = Offset(shift + stroke / 2f, shift + stroke / 2f),
            size = page,
            cornerRadius = corner,
        )
    }
}

/** The app's own mark. Decorative wherever it is used, so it is never announced. */
@Composable
private fun BalloonerLogo(size: Dp, alpha: Float = 1f) {
    Image(
        painter = painterResource(R.drawable.ic_ballooner_logo),
        contentDescription = null,
        modifier = Modifier.size(size),
        alpha = alpha,
    )
}

private val MENU_GLYPH_SIZE = 24.dp

/** Beside the title, and faint behind the invitation to start a comic. */
private val TITLE_LOGO_SIZE = 28.dp
private val EMPTY_LOGO_SIZE = 150.dp
private const val EMPTY_LOGO_ALPHA = 0.25f

/** Two tiles to a row, each standing taller than it is wide, as a comic page does. */
private const val TILES_ACROSS = 2
private const val TILE_ASPECT = 2f / 3f

/** The spinner stands exactly where the options button was, so nothing moves. */
private val BUSY_SPINNER_BOX = 48.dp
private val BUSY_SPINNER = 20.dp
