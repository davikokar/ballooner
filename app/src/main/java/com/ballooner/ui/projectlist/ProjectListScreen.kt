package com.ballooner.ui.projectlist

import android.graphics.BitmapFactory
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ballooner.domain.model.Project
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
        onDeleteProject = viewModel::deleteProject,
        onOpenSettings = onOpenSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    uiState: ProjectListUiState,
    onCreateProject: () -> Unit,
    onOpenProject: (Long) -> Unit,
    onDeleteProject: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ballooner") },
                actions = { OverflowMenu(onOpenSettings = onOpenSettings) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateProject) {
                Icon(Icons.Default.Add, contentDescription = "Create project")
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            when (uiState) {
                ProjectListUiState.Loading -> CircularProgressIndicator()
                ProjectListUiState.Empty -> Text("No projects yet. Tap + to start one.")
                is ProjectListUiState.Content -> ProjectList(
                    projects = uiState.projects,
                    onOpenProject = onOpenProject,
                    onDeleteProject = onDeleteProject,
                )
            }
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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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

@Composable
private fun ProjectRow(project: Project, onOpen: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProjectThumbnail(imageUri = project.imageUri, name = project.name)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = project.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = lastEditedLabel(project.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete ${project.name}")
            }
        }
    }
}

/** Square, center-cropped preview of the project's image, or its initial as a fallback. */
@Composable
private fun ProjectThumbnail(imageUri: String?, name: String) {
    val bitmap = rememberThumbnail(imageUri)
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
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
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
        onDeleteProject = {},
        onOpenSettings = {},
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
        onDeleteProject = {},
        onOpenSettings = {},
    )
}
