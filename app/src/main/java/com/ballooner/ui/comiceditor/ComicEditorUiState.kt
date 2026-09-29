package com.ballooner.ui.comiceditor

import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.Span

/** The three stages of making a comic. Each one edits a different part of the document. */
enum class EditorStep { LAYOUT, PLACEMENT, BALLOONS }

/**
 * The kind of layout the Layout step is offering choices for.
 *
 * This is how the user thinks about a layout rather than how the document stores it, so it is
 * read back off the comic instead of being saved.
 */
enum class LayoutKind { SINGLE, STRIP, GRID, CUSTOM }

/** The kind [comic] already is, which is the one the picker marks as active. */
fun layoutKindOf(comic: Comic): LayoutKind {
    val grid = comic.layout.grid
    return when {
        // Merging is a grid operation, so a merged grid is still a grid. Only a cut, which no
        // grid can make, takes a comic out of the kind it was built as.
        comic.layout.cuts.isNotEmpty() -> LayoutKind.CUSTOM
        grid.rows == 1 && grid.columns == 1 -> LayoutKind.SINGLE
        grid.rows == 1 || grid.columns == 1 -> LayoutKind.STRIP
        else -> LayoutKind.GRID
    }
}

/** What a drag on the page does while the Layout step is active. */
enum class LayoutTool {
    /** Tap panels to select them, and drag a grid line to change proportions. */
    SELECT,

    /** Drag to trace a cut across the whole page. */
    CUT_PAGE,

    /** Drag to trace a cut through the single panel the drag starts in. */
    CUT_PANEL,
}

/** What a layout change is about to throw away, shown for confirmation before it is applied. */
data class LayoutChangeWarning(val removedImages: Int, val removedBalloons: Int)

sealed interface ComicEditorUiState {

    data object Loading : ComicEditorUiState

    data class Content(
        val comic: Comic,
        val step: EditorStep,
        val tool: LayoutTool = LayoutTool.SELECT,
        /**
         * The layout kind whose options are open, or null while the Layout step is showing the
         * preset picker.
         */
        val layoutKind: LayoutKind? = null,
        /** Grid panels picked out in the Layout step, as the cells they cover. */
        val selection: List<Span> = emptyList(),
        /** The panel being placed in the Placement step, by index in reading order. */
        val activePanel: Int? = null,
        /**
         * The panel filling the canvas for closer work, by index. This is view state: it is never
         * saved with the comic and changes nothing in it.
         */
        val focusedPanel: Int? = null,
        /** The balloon being edited in the Balloon step, by id. */
        val selectedBalloon: Long? = null,
        val canMerge: Boolean = false,
        val canUnmerge: Boolean = false,
        val canUndo: Boolean = false,
        val warning: LayoutChangeWarning? = null,
    ) : ComicEditorUiState
}
