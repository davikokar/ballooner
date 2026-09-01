package com.ballooner.ui.projectlist

import android.graphics.BitmapFactory
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ballooner.domain.model.Project
import com.ballooner.ui.project.BalloonerIcons
import com.ballooner.ui.project.googleFontFamily
import com.ballooner.ui.theme.AnimeAceFontFamily
import com.ballooner.ui.theme.InkBlack
import com.ballooner.ui.theme.balloonerTopAppBarColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProjectListRoute(
    onOpenProject: (Long) -> Unit,
    onCreatedProject: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ProjectListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProjectListScreen(
        uiState = uiState,
        onCreateProject = { viewModel.createProject(onCreatedProject) },
        onOpenProject = onOpenProject,
        onOpenSettings = onOpenSettings,
        onDeleteProject = viewModel::deleteProject,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    uiState: ProjectListUiState,
    onCreateProject: () -> Unit,
    onOpenProject: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onDeleteProject: (Long) -> Unit,
) {
    Scaffold(
        topBar = { ProjectListTopBar(onOpenSettings = onOpenSettings) },
        floatingActionButton = { ComicFab(onClick = onCreateProject) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .dotGridBackground(),
            contentAlignment = Alignment.Center,
        ) {
            when (uiState) {
                ProjectListUiState.Loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                ProjectListUiState.Empty -> EmptyState(onCreateProject = onCreateProject)
                is ProjectListUiState.Content -> ProjectList(
                    projects = uiState.projects,
                    onOpenProject = onOpenProject,
                    onDeleteProject = onDeleteProject,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectListTopBar(onOpenSettings: () -> Unit) {
    Column {
        TopAppBar(
            title = {
                Text(
                    text = "Ballooner",
                    color = Color.White,
                    fontFamily = AnimeAceFontFamily,
                    fontSize = 24.sp,
                    maxLines = 1,
                )
            },
            colors = balloonerTopAppBarColors(),
            actions = { OverflowMenu(onOpenSettings = onOpenSettings) },
        )
        // Thick ink border under the bar, the signature "hard-edged inking" look.
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(InkBlack))
    }
}

@Composable
private fun EmptyState(onCreateProject: () -> Unit) {
    NeoBrutalPanel(modifier = Modifier.padding(32.dp)) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = BalloonerIcons.Balloon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(64.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No comics yet!",
                fontFamily = googleFontFamily("Bricolage Grotesque"),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Time to start your heroic journey. Grab a pen and let's go!",
                fontFamily = googleFontFamily("Hanken Grotesk"),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(20.dp))
            ComicButton(
                text = "Create comic",
                onClick = onCreateProject,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ProjectList(
    projects: List<Project>,
    onOpenProject: (Long) -> Unit,
    onDeleteProject: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        items(projects, key = { it.id }) { project ->
            ProjectRow(
                project = project,
                onOpen = { onOpenProject(project.id) },
                onDelete = { onDeleteProject(project.id) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProjectRow(project: Project, onOpen: () -> Unit, onDelete: () -> Unit) {
    var showDeleteButton by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        NeoBrutalPanel(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { if (showDeleteButton) showDeleteButton = false else onOpen() },
                    onLongClick = { showDeleteButton = true },
                ),
        ) {
            ProjectThumbnail(imageUri = project.imageUri, name = project.name)
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = project.name.ifBlank { "Untitled" }.uppercase(),
                    fontFamily = AnimeAceFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lastEditedLabel(project.updatedAt),
                    fontFamily = googleFontFamily("Hanken Grotesk"),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (showDeleteButton) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp)
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.secondary, CircleShape)
                    .border(2.dp, InkBlack, CircleShape)
                    .clickable { showConfirm = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete comic",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Delete comic?") },
            text = { Text("This permanently removes the comic and its panels.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirm = false
                        showDeleteButton = false
                        onDelete()
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

/** Large, wide, center-cropped preview of the project's image, or its initial as a fallback. */
@Composable
private fun ProjectThumbnail(imageUri: String?, name: String) {
    val bitmap = rememberThumbnail(imageUri)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            // The thick ink line separating the image header from the card body.
            .drawBehind {
                drawLine(
                    color = InkBlack,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 4.dp.toPx(),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                fontFamily = googleFontFamily("Luckiest Guy"),
                fontSize = 40.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A comic "panel": a bordered surface with a solid black offset shadow instead of a
 * soft elevation shadow, the signature look of the app's neo-brutalist comic style.
 */
@Composable
private fun NeoBrutalPanel(
    modifier: Modifier = Modifier,
    shadowOffset: Dp = 8.dp,
    borderWidth: Dp = 4.dp,
    backgroundColor: Color = Color.White,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .background(InkBlack),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .border(borderWidth, InkBlack),
            content = content,
        )
    }
}

/** A bold, bordered call-to-action button matching the comic panel style. */
@Composable
private fun ComicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondary,
    contentColor: Color = Color.White,
) {
    Box(
        modifier = modifier
            .background(containerColor)
            .border(BorderStroke(4.dp, InkBlack))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            color = contentColor,
            fontFamily = googleFontFamily("Space Grotesk"),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
    }
}

/** A circular "+" button with a solid offset shadow, replacing the default Material FAB look. */
@Composable
private fun ComicFab(onClick: () -> Unit) {
    Box(modifier = Modifier.size(64.dp)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .background(InkBlack, CircleShape),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                .border(4.dp, InkBlack, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create project", tint = InkBlack)
        }
    }
}

/** A faint dot grid mimicking newsprint texture, drawn behind the screen's content. */
private fun Modifier.dotGridBackground(
    color: Color = Color(0xFFDCD9D9),
    spacing: Dp = 24.dp,
    dotRadius: Dp = 1.dp,
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
private fun rememberThumbnail(uri: String?): ImageBitmap? {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = null, uri) {
        value = if (uri == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(uri)).use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }.value
}

private fun lastEditedLabel(timestamp: Long): String {
    val relative = DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    )
    return "Last edited $relative"
}

@Composable
private fun OverflowMenu(onOpenSettings: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = "More options")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Settings") },
            onClick = {
                expanded = false
                onOpenSettings()
            },
        )
    }
}

@Preview
@Composable
private fun ProjectListEmptyPreview() {
    ProjectListScreen(
        uiState = ProjectListUiState.Empty,
        onCreateProject = {},
        onOpenProject = {},
        onOpenSettings = {},
        onDeleteProject = {},
    )
}

@Preview
@Composable
private fun ProjectListContentPreview() {
    ProjectListScreen(
        uiState = ProjectListUiState.Content(
            projects = listOf(
                Project(id = 1, name = "Space Cats", description = "A feline space opera", createdAt = 0),
                Project(id = 2, name = "Coffee Run", description = "", createdAt = 0),
            ),
        ),
        onCreateProject = {},
        onOpenProject = {},
        onOpenSettings = {},
        onDeleteProject = {},
    )
}
