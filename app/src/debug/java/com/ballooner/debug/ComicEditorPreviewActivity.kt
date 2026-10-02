package com.ballooner.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ballooner.data.comic.ComicRepository
import com.ballooner.data.comic.SavedComic
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.ComicStyle
import com.ballooner.domain.comic.Grid
import com.ballooner.domain.comic.Layout
import com.ballooner.domain.comic.PageSizing
import com.ballooner.domain.comic.TALL_RATIO
import com.ballooner.domain.comic.Panel
import com.ballooner.domain.comic.PanelImage
import com.ballooner.ui.comic.PanelImageSource
import com.ballooner.ui.comiceditor.ComicEditorScreen
import com.ballooner.ui.comiceditor.ComicEditorViewModel
import com.ballooner.ui.comiceditor.asActions
import com.ballooner.ui.theme.BalloonerTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Debug-only host for the comic editor, backed by an in-memory comic.
 *
 * It exists so the editor can be used on a device before navigation is switched over to it, and
 * is not part of the shipped app.
 */
class ComicEditorPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BalloonerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    EditorHost()
                }
            }
        }
    }
}

@Composable
private fun EditorHost() {
    val bitmap = remember { checkerboard() }
    val images = remember(bitmap) { PanelImageSource { bitmap } }
    val viewModel = remember { ComicEditorViewModel(comicId = 1L, repository = InMemoryComicRepository()) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ComicEditorScreen(
        state = state,
        images = images,
        actions = remember(viewModel) { viewModel.asActions() },
        modifier = Modifier.fillMaxSize(),
        // A real picker belongs to the app; this host just fills the panel with the test image.
        onPickImage = { index -> viewModel.setPanelImage(index, "sample") },
    )
}

private class InMemoryComicRepository : ComicRepository {

    private val comic = MutableStateFlow(
        Comic(
            name = "Debug comic",
            sizing = PageSizing.Ratio(TALL_RATIO),
            style = ComicStyle(gutter = 0.025f, borderThickness = 0.006f),
            layout = Layout(Grid(rows = 3, columns = 2)),
            panels = List(6) { index -> Panel(if (index % 2 == 0) PanelImage("sample") else null) },
        ),
    )

    override fun observeComic(id: Long) = comic.map { it }
    override fun observeComics() = comic.map { listOf(SavedComic(1L, 0L, it)) }
    override suspend fun createComic(comic: Comic) = 1L
    override suspend fun saveComic(id: Long, comic: Comic) {
        this.comic.value = comic
    }
    override suspend fun renameComic(id: Long, name: String) {
        comic.value = comic.value.copy(name = name)
    }
    override suspend fun setCover(id: Long, sourceUri: String?) = Unit
    override suspend fun deleteComic(id: Long) = Unit
}

/** A grid with a marked top-left corner, so pan, zoom, and rotation are obvious at a glance. */
internal fun checkerboard(): ImageBitmap {
    val size = 512
    val cell = size / 8
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        for (x in 0 until size) {
            val dark = ((x / cell) + (y / cell)) % 2 == 0
            val corner = x < cell * 2 && y < cell
            pixels[y * size + x] = when {
                corner -> 0xFFD32F2F.toInt()
                dark -> 0xFF5C6BC0.toInt()
                else -> 0xFFE8EAF6.toInt()
            }
        }
    }
    return android.graphics.Bitmap
        .createBitmap(pixels, size, size, android.graphics.Bitmap.Config.ARGB_8888)
        .asImageBitmap()
}
