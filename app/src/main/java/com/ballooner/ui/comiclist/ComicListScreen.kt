package com.ballooner.ui.comiclist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ballooner.data.comic.ComicSummary
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
    onOpenSettings: () -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<ComicSummary?>(null) }

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
                    items(state.comics, key = { it.id }) { comic ->
                        ComicRow(
                            comic = comic,
                            onOpen = { onOpenComic(comic.id) },
                            onLongPress = { pendingDelete = comic },
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { comic ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this comic?") },
            text = { Text("\"${comic.name}\" will be removed for good.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteComic(comic.id)
                        pendingDelete = null
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Keep") } },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ComicRow(comic: ComicSummary, onOpen: () -> Unit, onLongPress: () -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(comic.name.ifBlank { "Untitled" }, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Edited ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(Date(comic.updatedAt))}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
