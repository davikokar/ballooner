package com.ballooner.ui.comiceditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ballooner.data.comic.ComicRepository
import com.ballooner.data.comic.ImportedImage
import com.ballooner.data.comic.PanelImageImporter
import com.ballooner.domain.comic.Balloon
import com.ballooner.domain.comic.BalloonScope
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.ComicStyle
import com.ballooner.domain.comic.Cut
import com.ballooner.domain.comic.CutScope
import com.ballooner.domain.comic.Grid
import com.ballooner.domain.comic.GridAxis
import com.ballooner.domain.comic.Layout
import com.ballooner.domain.comic.LayoutChange
import com.ballooner.domain.comic.MAX_BALLOON_TEXT_SIZE
import com.ballooner.domain.comic.MIN_BALLOON_TEXT_SIZE
import com.ballooner.domain.comic.NormalizedPoint
import com.ballooner.domain.comic.PagePoint
import com.ballooner.domain.comic.PageRect
import com.ballooner.domain.comic.PageSizing
import com.ballooner.domain.comic.PanelImage
import com.ballooner.domain.comic.PanelMatching
import com.ballooner.domain.comic.Span
import com.ballooner.domain.comic.centreOnPage
import com.ballooner.domain.comic.mergedFrom
import com.ballooner.domain.comic.panelIndex
import com.ballooner.domain.comic.panelShapes
import com.ballooner.domain.comic.resizedTo
import com.ballooner.domain.comic.straightened
import com.ballooner.domain.comic.transformed
import com.ballooner.domain.comic.unmergedAt
import com.ballooner.domain.comic.withBoundaryMoved
import com.ballooner.domain.comic.withCentreOnPage
import com.ballooner.domain.comic.withCut
import com.ballooner.domain.comic.withCutEndMoved
import com.ballooner.domain.comic.withLayout
import com.ballooner.domain.comic.withScope
import com.ballooner.domain.comic.withTailAt
import com.ballooner.domain.comic.withTailWidthAt
import com.ballooner.domain.model.BalloonFont
import com.ballooner.domain.model.BalloonType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the comic editor.
 *
 * The document is the single source of truth and every step edits a different part of it, so
 * moving between steps needs no saving, restoring, or discarding.
 */
@HiltViewModel
class ComicEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ComicRepository,
    private val imageImporter: PanelImageImporter,
) : ViewModel() {

    /**
     * Used by tests and the debug host, which supply a comic directly rather than by route. Their
     * uris are already local, so importing them is a no-op.
     */
    constructor(comicId: Long, repository: ComicRepository) :
        this(SavedStateHandle(mapOf(COMIC_ID_KEY to comicId)), repository, PanelImageImporter { ImportedImage(it, null) })

    private val comicId: Long = savedStateHandle.get<Long>(COMIC_ID_KEY) ?: 0L

    private val state = MutableStateFlow<ComicEditorUiState>(ComicEditorUiState.Loading)
    val uiState: StateFlow<ComicEditorUiState> = state.asStateFlow()

    private val undoStack = ArrayDeque<Comic>()
    private var pending: Comic? = null
    private var beforeDrag: Comic? = null

    // Only ever counts up, so deleting a balloon cannot hand its id to a different one while the
    // undo stack and the selection still refer to it.
    private var nextBalloonId = 1L

    init {
        viewModelScope.launch {
            val comic = repository.observeComic(comicId).first() ?: Comic()
            nextBalloonId = (comic.balloons.maxOfOrNull { it.id } ?: 0L) + 1L
            state.value = ComicEditorUiState.Content(comic = comic, step = EditorStep.LAYOUT)
        }
    }

    fun selectStep(step: EditorStep) = updateContent {
        it.copy(
            step = step,
            layoutKind = null,
            selection = emptyList(),
            activePanel = null,
            focusedPanel = null,
        )
    }

    /** Fills the canvas with one panel. Focus is a way of looking, not a change to the comic. */
    fun focusPanel(index: Int?) = updateContent { content ->
        val focused = index?.takeIf { it in content.comic.panels.indices }
        content.copy(focusedPanel = focused, activePanel = focused ?: content.activePanel)
    }

    /** Steps to the next or previous panel without leaving focus. */
    fun focusNeighbour(forward: Boolean) = updateContent { content ->
        val count = content.comic.panels.size
        if (count == 0) return@updateContent content
        val current = content.focusedPanel ?: return@updateContent content
        val next = (current + if (forward) 1 else -1).mod(count)
        content.copy(focusedPanel = next, activePanel = next, selectedBalloon = null)
    }

    /**
     * Exchanges the images of two panels.
     *
     * Each image keeps its own zoom and turn, and because zoom counts in multiples of what covers
     * a panel, both still cover after landing in a differently shaped frame.
     */
    fun swapPanelImages(from: Int, to: Int) {
        val content = contentOrNull() ?: return
        if (from == to) return
        val panels = content.comic.panels
        if (from !in panels.indices || to !in panels.indices) return
        if (panels[from].image == null && panels[to].image == null) return
        val swapped = panels.toMutableList()
        swapped[from] = panels[to]
        swapped[to] = panels[from]
        commit(content.comic.copy(panels = swapped))
    }

    /**
     * Adds a balloon to [panelIndex], or to the comic itself when that is null.
     *
     * Ids rise with each balloon and are never reused, so they double as the drawing order and
     * survive a round trip through storage unchanged.
     */
    fun addBalloon(type: BalloonType, panelIndex: Int?) {
        val content = contentOrNull() ?: return
        val comic = content.comic
        val id = nextBalloonId++
        val balloon = Balloon(
            id = id,
            type = type,
            scope = panelIndex?.let { BalloonScope.Panel(it) } ?: BalloonScope.Comic,
            tailLength = if (type == BalloonType.CAPTION) 0f else 0.12f,
            cornerRoundness = if (type == BalloonType.CAPTION) 0f else 1f,
        )
        commit(comic.copy(balloons = comic.balloons + balloon))
        updateContent { it.copy(selectedBalloon = id) }
    }

    fun selectBalloon(id: Long?) = updateContent { content ->
        content.copy(selectedBalloon = id?.takeIf { wanted -> content.comic.balloons.any { it.id == wanted } })
    }

    fun deleteBalloon(id: Long) {
        val content = contentOrNull() ?: return
        if (content.comic.balloons.none { it.id == id }) return
        commit(content.comic.copy(balloons = content.comic.balloons.filterNot { it.id == id }))
        updateContent { it.copy(selectedBalloon = null) }
    }

    fun setBalloonText(id: Long, text: String) = editBalloon(id, undoable = false) { it.copy(text = text) }

    fun setBalloonFont(id: Long, font: BalloonFont) = editBalloon(id, undoable = true) { it.copy(font = font) }

    fun setBalloonTextSize(id: Long, size: Float) = editBalloon(id, undoable = false) {
        it.copy(fontSize = size.coerceIn(MIN_BALLOON_TEXT_SIZE, MAX_BALLOON_TEXT_SIZE))
    }

    fun setBalloonRoundness(id: Long, roundness: Float) = editBalloon(id, undoable = false) {
        it.copy(cornerRoundness = roundness.coerceIn(0f, 1f))
    }

    fun setBalloonTailWidth(id: Long, x: Float, y: Float) =
        editBalloonInPlace(id) { balloon, panel, pageHeight ->
            balloon.withTailWidthAt(PagePoint(x, y), panel, pageHeight)
        }

    /** Moves the balloon so its centre lands on the given page point. */
    fun moveBalloon(id: Long, x: Float, y: Float) = editBalloonInPlace(id) { balloon, panel, pageHeight ->
        balloon.withCentreOnPage(PagePoint(x, y), panel, pageHeight)
    }

    fun resizeBalloon(id: Long, x: Float, y: Float) = editBalloonInPlace(id) { balloon, panel, pageHeight ->
        balloon.resizedTo(PagePoint(x, y), panel, pageHeight)
    }

    fun pointBalloonTail(id: Long, x: Float, y: Float) = editBalloonInPlace(id) { balloon, panel, pageHeight ->
        balloon.withTailAt(PagePoint(x, y), panel, pageHeight)
    }

    /** Switches a balloon between belonging to its panel and belonging to the whole comic. */
    fun toggleBalloonScope(id: Long) {
        val content = contentOrNull() ?: return
        val comic = content.comic
        val balloon = comic.balloons.firstOrNull { it.id == id } ?: return
        val shapes = panelShapes(comic.layout, comic.pageHeight, comic.style)
        val currentPanel = balloon.panelIndex?.let { shapes.getOrNull(it)?.bounds }
        val newScope = if (balloon.scope is BalloonScope.Comic) {
            val owner = shapes.indexOfFirst {
                it.contains(balloon.centreOnPage(null, comic.pageHeight))
            }
            if (owner < 0) return
            BalloonScope.Panel(owner)
        } else {
            BalloonScope.Comic
        }
        val newPanel = (newScope as? BalloonScope.Panel)?.let { shapes.getOrNull(it.panelIndex)?.bounds }
        val moved = balloon.withScope(newScope, currentPanel, newPanel, comic.pageHeight)
        commit(comic.copy(balloons = comic.balloons.map { if (it.id == id) moved else it }))
    }

    fun startBalloonGesture() {
        beforeDrag = contentOrNull()?.comic
    }

    fun endBalloonGesture() = endDragAsOneUndoStep()

    private fun editBalloonInPlace(id: Long, transform: (Balloon, PageRect?, Float) -> Balloon) {
        val content = contentOrNull() ?: return
        val comic = content.comic
        val balloon = comic.balloons.firstOrNull { it.id == id } ?: return
        val panel = balloon.panelIndex
            ?.let { panelShapes(comic.layout, comic.pageHeight, comic.style).getOrNull(it)?.bounds }
        val moved = transform(balloon, panel, comic.pageHeight)
        if (moved == balloon) return
        commit(comic.copy(balloons = comic.balloons.map { if (it.id == id) moved else it }), undoable = false)
    }

    private fun editBalloon(id: Long, undoable: Boolean, transform: (Balloon) -> Balloon) {
        val content = contentOrNull() ?: return
        val comic = content.comic
        if (comic.balloons.none { it.id == id }) return
        commit(
            comic.copy(balloons = comic.balloons.map { if (it.id == id) transform(it) else it }),
            undoable = undoable,
        )
    }

    fun selectTool(tool: LayoutTool) = updateContent { it.copy(tool = tool).withSelection(emptyList()) }

    /** Chooses the panel the Placement step works on. */
    fun selectPanel(index: Int?) = updateContent { content ->
        content.copy(activePanel = index?.takeIf { it in content.comic.panels.indices })
    }

    fun setPanelImage(index: Int, sourceUri: String?, sourceAspect: Float? = null) {
        val content = contentOrNull() ?: return
        val panel = content.comic.panels.getOrNull(index) ?: return
        val replaced = panel.copy(image = sourceUri?.let { PanelImage(it, sourceAspect = sourceAspect) })
        commit(content.comic.copy(panels = content.comic.panels.toMutableList().also { it[index] = replaced }))
    }

    /** Takes a copy of a picked image before putting it in a panel. */
    fun importPanelImage(index: Int, pickedUri: String) {
        viewModelScope.launch {
            val imported = imageImporter.import(pickedUri) ?: return@launch
            setPanelImage(index, imported.uri, imported.aspect)
        }
    }

    /** Remembers the image before a pinch begins, so the whole gesture is one undoable change. */
    fun startPlacementGesture() {
        beforeDrag = contentOrNull()?.comic
    }

    fun transformPanelImage(
        index: Int,
        imageAspect: Float,
        panX: Float = 0f,
        panY: Float = 0f,
        zoomBy: Float = 1f,
        rotateBy: Float = 0f,
    ) {
        val content = contentOrNull() ?: return
        val comic = content.comic
        val panel = comic.panels.getOrNull(index) ?: return
        val image = panel.image ?: return
        val bounds = panelShapes(comic.layout, comic.pageHeight, comic.style)
            .getOrNull(index)?.bounds ?: return
        val moved = image.transformed(bounds, imageAspect, panX, panY, zoomBy, rotateBy)
        if (moved == image) return
        val panels = comic.panels.toMutableList().also { it[index] = panel.copy(image = moved) }
        commit(comic.copy(panels = panels), undoable = false)
    }

    /** Lets the quarter-turn magnet bite, then closes the gesture as one undoable change. */
    fun endPlacementGesture(index: Int, imageAspect: Float) {
        val content = contentOrNull()
        val comic = content?.comic
        val panel = comic?.panels?.getOrNull(index)
        val image = panel?.image
        val bounds = comic?.let {
            panelShapes(it.layout, it.pageHeight, it.style).getOrNull(index)?.bounds
        }
        if (image != null && bounds != null) {
            val straight = image.straightened(bounds, imageAspect)
            if (straight != image) {
                val panels = comic.panels.toMutableList()
                    .also { it[index] = panel.copy(image = straight) }
                commit(comic.copy(panels = panels), undoable = false)
            }
        }
        endDragAsOneUndoStep()
    }

    /** Adds or removes a grid panel from the Layout step selection. */
    fun toggleSelection(span: Span) = updateContent { content ->
        val selection = if (span in content.selection) content.selection - span else content.selection + span
        content.withSelection(selection)
    }

    fun clearSelection() = updateContent { it.withSelection(emptyList()) }

    fun mergeSelection() {
        val content = contentOrNull() ?: return
        val merged = content.comic.layout.grid.mergedFrom(content.selection) ?: return
        applyLayout(content.comic.layout.copy(grid = merged), PanelMatching.BY_OVERLAP)
    }

    fun unmergeSelection() {
        val content = contentOrNull() ?: return
        val span = content.selection.singleOrNull() ?: return
        val grid = content.comic.layout.grid.unmergedAt(span.firstRow, span.firstColumn)
        applyLayout(content.comic.layout.copy(grid = grid), PanelMatching.BY_OVERLAP)
    }

    /** Switches to a preset layout, pairing old and new panels off in reading order. */
    fun applyPreset(rows: Int, columns: Int) {
        applyLayout(Layout(Grid(rows = rows, columns = columns)), PanelMatching.BY_INDEX)
    }

    /**
     * Opens one kind of layout's options, starting the comic on that kind when it is not already.
     * A comic that is already the chosen kind is left exactly as it is.
     */
    fun selectLayoutKind(kind: LayoutKind) {
        updateContent { it.copy(layoutKind = kind).withSelection(emptyList()) }
        val comic = contentOrNull()?.comic ?: return
        if (layoutKindOf(comic) == kind) return
        when (kind) {
            LayoutKind.SINGLE -> applyPreset(rows = 1, columns = 1)
            LayoutKind.STRIP -> applyPreset(rows = 1, columns = DEFAULT_STRIP_PANELS)
            LayoutKind.GRID -> applyPreset(rows = DEFAULT_GRID_SIDE, columns = DEFAULT_GRID_SIDE)
            // A freely drawn layout starts from one whole panel and is cut up from there.
            LayoutKind.CUSTOM -> applyPreset(rows = 1, columns = 1)
        }
    }

    /** Returns to the preset picker, leaving the comic as it is. */
    fun closeLayoutKind() = updateContent {
        it.copy(layoutKind = null).withSelection(emptyList())
    }

    fun setRowWeights(weights: List<Float>) = withGrid { it.copy(rowWeights = weights) }

    fun setColumnWeights(weights: List<Float>) = withGrid { it.copy(columnWeights = weights) }

    /**
     * Changes what gives the page its height.
     *
     * The page is shaped by the reference panel, so this reshapes every panel at once but never
     * changes how many there are.
     */
    fun setSizing(sizing: PageSizing) {
        val content = contentOrNull() ?: return
        commit(content.comic.copy(sizing = sizing))
    }

    /** Remembers the layout before a drag begins, so the whole drag counts as one change. */
    fun startBoundaryDrag() {
        beforeDrag = contentOrNull()?.comic
    }

    /** [delta] is the distance dragged since the drag began, as a fraction of the content extent. */
    fun moveBoundary(axis: GridAxis, index: Int, delta: Float) {
        val origin = beforeDrag ?: return
        val grid = origin.layout.grid.withBoundaryMoved(axis, index, delta)
        commit(origin.copy(layout = origin.layout.copy(grid = grid)), undoable = false)
    }

    fun endBoundaryDrag() = endDragAsOneUndoStep()

    /**
     * Drags one end of a cut, swinging the line about its other end.
     *
     * Every step is matched against the layout the drag began from, because swinging a cut
     * reshapes panels and can reorder them: without matching, the images would swap panels under
     * the finger. A step that would discard anything is skipped rather than staged, since a drag
     * is no place to raise a confirmation.
     */
    fun moveCutEnd(index: Int, start: Boolean, to: NormalizedPoint) {
        val origin = beforeDrag ?: return
        val layout = origin.layout.withCutEndMoved(index, start, to)
        if (layout == origin.layout) return
        val change = origin.withLayout(layout, PanelMatching.BY_OVERLAP)
        if (change.isDestructive) return
        commit(change.comic, undoable = false)
    }

    fun endCutDrag() = endDragAsOneUndoStep()

    private fun endDragAsOneUndoStep() {
        val origin = beforeDrag ?: return
        beforeDrag = null
        val content = contentOrNull() ?: return
        if (content.comic == origin) return
        undoStack.addLast(origin)
        if (undoStack.size > UNDO_LIMIT) undoStack.removeFirst()
        state.value = content.copy(canUndo = true)
    }

    fun setStyle(style: ComicStyle) {
        val content = contentOrNull() ?: return
        // Style moves and resizes every panel but never changes how many there are.
        commit(content.comic.copy(style = style), undoable = false)
    }

    /**
     * Adds a cut traced from [from] to [to]. A cut that separates nothing is dropped rather than
     * stored, so a stray drag cannot leave an invisible line behind.
     */
    fun addCut(from: NormalizedPoint, to: NormalizedPoint, scope: CutScope) {
        val content = contentOrNull() ?: return
        val comic = content.comic
        val layout = comic.layout.withCut(Cut(from, to, scope))
        val before = panelShapes(comic.layout, comic.pageHeight, comic.style).size
        val after = panelShapes(layout, comic.pageHeight, comic.style).size
        if (after <= before) return
        applyLayout(layout, PanelMatching.BY_OVERLAP)
    }

    fun setName(name: String) {
        val content = contentOrNull() ?: return
        commit(content.comic.copy(name = name), undoable = false)
    }

    fun confirmLayoutChange() {
        val comic = pending ?: return
        pending = null
        commit(comic)
        updateContent { it.copy(warning = null).withSelection(emptyList()) }
    }

    fun cancelLayoutChange() {
        pending = null
        updateContent { it.copy(warning = null) }
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        save(previous)
        updateContent { it.copy(comic = previous, canUndo = undoStack.isNotEmpty()).withSelection(emptyList()) }
    }

    private fun withGrid(transform: (Grid) -> Grid) {
        val content = contentOrNull() ?: return
        val layout = content.comic.layout
        applyLayout(layout.copy(grid = transform(layout.grid)), PanelMatching.BY_OVERLAP)
    }

    private fun applyLayout(layout: Layout, matching: PanelMatching) {
        val content = contentOrNull() ?: return
        val change: LayoutChange = content.comic.withLayout(layout, matching)
        if (change.isDestructive) {
            pending = change.comic
            updateContent {
                it.copy(warning = LayoutChangeWarning(change.removedImages, change.removedBalloons))
            }
            return
        }
        commit(change.comic)
        updateContent { it.withSelection(emptyList()) }
    }

    private fun commit(comic: Comic, undoable: Boolean = true) {
        val content = contentOrNull() ?: return
        if (content.comic == comic) return
        if (undoable) {
            undoStack.addLast(content.comic)
            if (undoStack.size > UNDO_LIMIT) undoStack.removeFirst()
        }
        state.value = content.copy(comic = comic, canUndo = undoStack.isNotEmpty())
        save(comic)
    }

    private fun save(comic: Comic) {
        viewModelScope.launch { repository.saveComic(comicId, comic) }
    }

    private fun contentOrNull() = state.value as? ComicEditorUiState.Content

    private inline fun updateContent(transform: (ComicEditorUiState.Content) -> ComicEditorUiState.Content) {
        val content = contentOrNull() ?: return
        state.value = transform(content)
    }
}

/** Recomputes which merge actions the current selection allows. */
private fun ComicEditorUiState.Content.withSelection(selection: List<Span>): ComicEditorUiState.Content =
    copy(
        selection = selection,
        canMerge = selection.size >= 2 && comic.layout.grid.mergedFrom(selection) != null,
        canUnmerge = selection.singleOrNull()?.let { it.rowCount > 1 || it.columnCount > 1 } ?: false,
    )

private const val UNDO_LIMIT = 50

/** What a strip starts as, matching the arrangement its preset card shows. */
private const val DEFAULT_STRIP_PANELS = 3

/** What a grid starts as, matching the arrangement its preset card shows. */
private const val DEFAULT_GRID_SIDE = 2

/** The navigation argument naming which comic the editor opens. */
const val COMIC_ID_KEY = "comicId"
