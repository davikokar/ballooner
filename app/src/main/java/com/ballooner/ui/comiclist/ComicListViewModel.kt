package com.ballooner.ui.comiclist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ballooner.data.comic.ComicNamer
import com.ballooner.data.comic.ComicRepository
import com.ballooner.data.comic.ImportedImage
import com.ballooner.data.comic.PanelImageImporter
import com.ballooner.data.comic.SavedComic
import com.ballooner.data.comic.TestComicNamer
import com.ballooner.domain.comic.Comic
import com.ballooner.ui.comiceditor.NEW_COMIC_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ComicListUiState {
    data object Loading : ComicListUiState
    data object Empty : ComicListUiState
    data class Content(val comics: List<SavedComic>) : ComicListUiState
}

@HiltViewModel
class ComicListViewModel @Inject constructor(
    private val repository: ComicRepository,
    private val imageImporter: PanelImageImporter,
    private val namer: ComicNamer,
) : ViewModel() {

    /**
     * Used by tests, whose uris are already local, so importing them is a no-op.
     */
    constructor(repository: ComicRepository) :
        this(repository, PanelImageImporter { ImportedImage(it, null) }, TestComicNamer)

    val uiState: StateFlow<ComicListUiState> = repository.observeComics()
        .map { comics -> if (comics.isEmpty()) ComicListUiState.Empty else ComicListUiState.Content(comics) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ComicListUiState.Loading,
        )

    /**
     * Opens a comic that does not exist yet.
     *
     * Nothing is written: a new comic has no row until the editor saves it, so one that is
     * abandoned leaves nothing behind (ADR-0011).
     */
    fun createComic(onCreated: (Long) -> Unit) = onCreated(NEW_COMIC_ID)

    fun deleteComic(id: Long) {
        viewModelScope.launch { repository.deleteComic(id) }
    }

    fun renameComic(id: Long, name: String) {
        viewModelScope.launch { repository.renameComic(id, name) }
    }

    /**
     * Copies a comic, cover and all.
     *
     * The copy points at the same image files as the original: a panel image is a reference to a
     * picture the app already holds, so copying the document copies what it refers to.
     */
    fun duplicateComic(saved: SavedComic) {
        viewModelScope.launch {
            val copy = saved.comic.copy(name = namer.copyName(saved.comic.name))
            val id = repository.createComic(copy)
            saved.coverUri?.let { repository.setCover(id, it) }
        }
    }

    /** A picked image is only borrowed, so a copy of it is taken before it becomes the cover. */
    fun setCover(id: Long, sourceUri: String) {
        viewModelScope.launch {
            val imported = imageImporter.import(sourceUri) ?: return@launch
            repository.setCover(id, imported.uri)
        }
    }
}
