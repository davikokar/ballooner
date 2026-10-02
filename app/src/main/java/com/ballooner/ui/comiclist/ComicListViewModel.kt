package com.ballooner.ui.comiclist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ballooner.data.comic.ComicRepository
import com.ballooner.data.comic.SavedComic
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.PageSizing
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
) : ViewModel() {

    val uiState: StateFlow<ComicListUiState> = repository.observeComics()
        .map { comics -> if (comics.isEmpty()) ComicListUiState.Empty else ComicListUiState.Content(comics) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ComicListUiState.Loading,
        )

    /** Creates a comic with a default title and reports its id for navigation. */
    fun createComic(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val existing = repository.observeComics().first()
            // One panel, shaped by whatever image goes in it: the fewest decisions to start.
            val comic = Comic(
                name = "My Comic ${existing.size + 1}",
                sizing = PageSizing.FromImage,
            )
            onCreated(repository.createComic(comic))
        }
    }

    fun deleteComic(id: Long) {
        viewModelScope.launch { repository.deleteComic(id) }
    }

    fun renameComic(id: Long, name: String) {
        viewModelScope.launch { repository.renameComic(id, name) }
    }
}
