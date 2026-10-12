package com.ballooner.ui.comicviewer

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.toSize
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ballooner.domain.comic.Comic
import com.ballooner.ui.comic.ComicPage
import com.ballooner.ui.comic.PanelImageSource
import com.ballooner.ui.comic.rememberPanelImageSource
import com.davide.seddio.ballooner.R

/** Connects the Comic viewer screen to navigation and to the images the comic is made of. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicViewerRoute(
    onNavigateBack: () -> Unit,
    viewModel: ComicViewerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val comic = (state as? ComicViewerUiState.Content)?.comic
    val images = rememberPanelImageSource(
        sourceUris = comic?.panels?.mapNotNull { it.image?.sourceUri }?.toSet().orEmpty(),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = comic?.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.untitled),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        ComicViewerScreen(
            state = state,
            images = images,
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}

/**
 * The comic as it was made, with nothing over it: no steps, no handles, and nothing to edit.
 *
 * The whole page is shown to begin with, and pinching takes the reader closer to any part of it.
 */
@Composable
fun ComicViewerScreen(
    state: ComicViewerUiState,
    images: PanelImageSource,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (state) {
            ComicViewerUiState.Loading -> CircularProgressIndicator()
            is ComicViewerUiState.Content -> ZoomableComicPage(comic = state.comic, images = images)
        }
    }
}

@Composable
private fun ZoomableComicPage(comic: Comic, images: PanelImageSource) {
    var zoom by remember { mutableStateOf(PageZoom()) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            // Keyed on nothing, so a gesture that changes the zoom is not torn down mid-pinch.
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoomBy, _ ->
                    zoom = zoom.transformedBy(centroid, pan, zoomBy, size.toSize())
                }
            },
    ) {
        ComicPage(
            comic = comic,
            images = images,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = zoom.scale
                scaleY = zoom.scale
                translationX = zoom.offset.x
                translationY = zoom.offset.y
            },
        )
    }
}
