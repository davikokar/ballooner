package com.ballooner.ui.comicviewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ballooner.data.comic.ComicRepository
import com.ballooner.ui.comiceditor.COMIC_ID_KEY
import com.ballooner.ui.comiceditor.NEW_COMIC_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Reads one saved comic so it can be looked at. Nothing here changes it. */
@HiltViewModel
class ComicViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: ComicRepository,
) : ViewModel() {

    private val comicId: Long = savedStateHandle.get<Long>(COMIC_ID_KEY) ?: NEW_COMIC_ID

    val uiState: StateFlow<ComicViewerUiState> = repository.observeComic(comicId)
        .map { comic -> if (comic == null) ComicViewerUiState.Loading else ComicViewerUiState.Content(comic) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ComicViewerUiState.Loading,
        )
}
