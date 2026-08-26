package com.ballooner.ui.projectlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ballooner.domain.model.Project

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
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = project.name, style = MaterialTheme.typography.titleMedium)
                if (project.description.isNotBlank()) {
                    Text(
                        text = project.description,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete ${project.name}")
            }
        }
    }
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
