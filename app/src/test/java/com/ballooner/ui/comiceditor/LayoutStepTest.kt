package com.ballooner.ui.comiceditor

import com.ballooner.data.comic.FakeComicRepository
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.ComicStyle
import com.ballooner.domain.comic.CutScope
import com.ballooner.domain.comic.Grid
import com.ballooner.domain.comic.GridAxis
import com.ballooner.domain.comic.Layout
import com.ballooner.domain.comic.NormalizedPoint
import com.ballooner.domain.comic.PageSizing
import com.ballooner.domain.comic.Panel
import com.ballooner.domain.comic.PanelImage
import com.ballooner.domain.comic.Span
import com.ballooner.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Covers the Layout step's two drag-driven tools: moving a grid line and tracing a cut. */
@OptIn(ExperimentalCoroutinesApi::class)
class LayoutStepTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val style = ComicStyle(pageMargin = 0f, gutter = 0f, borderThickness = 0f)

    private fun comic(rows: Int, columns: Int, withImages: Boolean = false) = Comic(
        // Holding a panel at its share of a square page keeps the page square for any grid.
        sizing = PageSizing.Ratio(rows.toFloat() / columns),
        style = style,
        layout = Layout(Grid(rows = rows, columns = columns)),
        panels = List(rows * columns) { Panel(if (withImages) PanelImage("image$it") else null) },
    )

    private fun editorFor(initial: Comic): Pair<ComicEditorViewModel, FakeComicRepository> {
        val repository = FakeComicRepository(initial)
        return ComicEditorViewModel(comicId = 1L, repository = repository) to repository
    }

    private fun content(viewModel: ComicEditorViewModel) =
        viewModel.uiState.value as ComicEditorUiState.Content

    @Test
    fun `dragging a grid line changes the panel proportions`() = runTest {
        val (viewModel, repository) = editorFor(comic(1, 2))
        advanceUntilIdle()

        viewModel.startBoundaryDrag()
        viewModel.moveBoundary(GridAxis.COLUMN, index = 1, delta = 0.25f)
        viewModel.endBoundaryDrag()
        advanceUntilIdle()

        assertEquals(listOf(1.5f, 0.5f), content(viewModel).comic.layout.grid.columnWeights)
        assertEquals(listOf(1.5f, 0.5f), repository.saved.value.getValue(1L).layout.grid.columnWeights)
    }

    @Test
    fun `a whole drag counts as one undo, not one per step`() = runTest {
        val (viewModel, _) = editorFor(comic(1, 2))
        advanceUntilIdle()

        viewModel.startBoundaryDrag()
        viewModel.moveBoundary(GridAxis.COLUMN, index = 1, delta = 0.1f)
        viewModel.moveBoundary(GridAxis.COLUMN, index = 1, delta = 0.2f)
        viewModel.moveBoundary(GridAxis.COLUMN, index = 1, delta = 0.3f)
        viewModel.endBoundaryDrag()
        advanceUntilIdle()
        assertTrue(content(viewModel).canUndo)

        viewModel.undo()
        advanceUntilIdle()

        assertEquals(listOf(1f, 1f), content(viewModel).comic.layout.grid.columnWeights)
        assertFalse(content(viewModel).canUndo)
    }

    @Test
    fun `each drag step is measured from where the drag began`() = runTest {
        val (viewModel, _) = editorFor(comic(1, 2))
        advanceUntilIdle()

        viewModel.startBoundaryDrag()
        viewModel.moveBoundary(GridAxis.COLUMN, index = 1, delta = 0.1f)
        viewModel.moveBoundary(GridAxis.COLUMN, index = 1, delta = 0.25f)
        viewModel.endBoundaryDrag()
        advanceUntilIdle()

        // Not 0.1 + 0.25: the second report replaces the first rather than adding to it.
        assertEquals(listOf(1.5f, 0.5f), content(viewModel).comic.layout.grid.columnWeights)
    }

    @Test
    fun `a drag that changes nothing adds no undo step`() = runTest {
        val (viewModel, _) = editorFor(comic(1, 2))
        advanceUntilIdle()

        viewModel.startBoundaryDrag()
        viewModel.endBoundaryDrag()
        advanceUntilIdle()

        assertFalse(content(viewModel).canUndo)
    }

    @Test
    fun `moving a boundary without starting a drag does nothing`() = runTest {
        val (viewModel, _) = editorFor(comic(1, 2))
        advanceUntilIdle()

        viewModel.moveBoundary(GridAxis.COLUMN, index = 1, delta = 0.25f)
        advanceUntilIdle()

        assertEquals(listOf(1f, 1f), content(viewModel).comic.layout.grid.columnWeights)
    }

    @Test
    fun `a page cut splits every panel it crosses`() = runTest {
        val (viewModel, repository) = editorFor(comic(2, 1))
        advanceUntilIdle()

        viewModel.addCut(NormalizedPoint(0.5f, 0f), NormalizedPoint(0.5f, 1f), CutScope.WholePage)
        advanceUntilIdle()

        assertEquals(4, content(viewModel).comic.panels.size)
        assertEquals(4, repository.saved.value.getValue(1L).panels.size)
    }

    @Test
    fun `a panel cut splits only the panel it was traced in`() = runTest {
        val (viewModel, _) = editorFor(comic(2, 1))
        advanceUntilIdle()

        viewModel.addCut(
            from = NormalizedPoint(0.5f, 0.2f),
            to = NormalizedPoint(0.5f, 0.4f),
            scope = CutScope.AtPoint(NormalizedPoint(0.5f, 0.2f)),
        )
        advanceUntilIdle()

        assertEquals(3, content(viewModel).comic.panels.size)
    }

    @Test
    fun `a diagonal cut is allowed`() = runTest {
        val (viewModel, _) = editorFor(comic(1, 1))
        advanceUntilIdle()

        viewModel.addCut(NormalizedPoint(0f, 0.2f), NormalizedPoint(1f, 0.8f), CutScope.WholePage)
        advanceUntilIdle()

        assertEquals(2, content(viewModel).comic.panels.size)
    }

    @Test
    fun `a cut that separates nothing is not stored`() = runTest {
        val (viewModel, _) = editorFor(comic(1, 1))
        advanceUntilIdle()

        // Anchored outside the page, so it has no panel to split.
        viewModel.addCut(
            from = NormalizedPoint(2f, 2f),
            to = NormalizedPoint(3f, 3f),
            scope = CutScope.AtPoint(NormalizedPoint(2f, 2f)),
        )
        advanceUntilIdle()

        assertEquals(emptyList<Any>(), content(viewModel).comic.layout.cuts)
        assertEquals(1, content(viewModel).comic.panels.size)
    }

    @Test
    fun `cutting a panel that holds an image asks first`() = runTest {
        val (viewModel, _) = editorFor(comic(1, 1, withImages = true))
        advanceUntilIdle()

        viewModel.addCut(NormalizedPoint(0.5f, 0f), NormalizedPoint(0.5f, 1f), CutScope.WholePage)
        advanceUntilIdle()

        // Splitting keeps the image in the first piece, so nothing is lost and nothing is asked.
        assertEquals(2, content(viewModel).comic.panels.size)
        assertEquals("image0", content(viewModel).comic.panels[0].image?.sourceUri)
    }

    @Test
    fun `undo removes a cut`() = runTest {
        val (viewModel, _) = editorFor(comic(1, 1))
        advanceUntilIdle()
        viewModel.addCut(NormalizedPoint(0.5f, 0f), NormalizedPoint(0.5f, 1f), CutScope.WholePage)
        advanceUntilIdle()

        viewModel.undo()
        advanceUntilIdle()

        assertEquals(emptyList<Any>(), content(viewModel).comic.layout.cuts)
        assertEquals(1, content(viewModel).comic.panels.size)
    }

    @Test
    fun `choosing a tool clears the selection`() = runTest {
        val (viewModel, _) = editorFor(comic(2, 2))
        advanceUntilIdle()
        viewModel.toggleSelection(Span(0, 0))

        viewModel.selectTool(LayoutTool.CUT_PAGE)

        assertEquals(LayoutTool.CUT_PAGE, content(viewModel).tool)
        assertEquals(emptyList<Span>(), content(viewModel).selection)
    }

    @Test
    fun `cuts and merges stack up on the same page`() = runTest {
        val (viewModel, _) = editorFor(comic(2, 2))
        advanceUntilIdle()

        viewModel.addCut(
            from = NormalizedPoint(0.75f, 0.6f),
            to = NormalizedPoint(0.9f, 0.9f),
            scope = CutScope.AtPoint(NormalizedPoint(0.75f, 0.75f)),
        )
        advanceUntilIdle()

        assertEquals(5, content(viewModel).comic.panels.size)
        assertNotNull(content(viewModel).comic.layout.cuts.singleOrNull())
    }
}
