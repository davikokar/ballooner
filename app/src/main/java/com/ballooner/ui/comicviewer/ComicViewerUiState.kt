package com.ballooner.ui.comicviewer

import com.ballooner.domain.comic.Comic

/** What the Comic viewer screen is showing. */
sealed interface ComicViewerUiState {

    data object Loading : ComicViewerUiState

    data class Content(val comic: Comic) : ComicViewerUiState
}
