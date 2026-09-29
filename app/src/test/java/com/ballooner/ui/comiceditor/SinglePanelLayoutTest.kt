package com.ballooner.ui.comiceditor

import com.ballooner.data.comic.FakeComicRepository
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.ComicStyle
import com.ballooner.domain.comic.Cut
import com.ballooner.domain.comic.CutScope
import com.ballooner.domain.comic.Grid
import com.ballooner.domain.comic.Layout
import com.ballooner.domain.comic.NormalizedPoint
import com.ballooner.domain.comic.PageSizing
import com.ballooner.domain.comic.Panel
import com.ballooner.domain.comic.PanelImage
import com.ballooner.domain.comic.SQUARE_RATIO
import com.ballooner.domain.comic.Span
import com.ballooner.domain.comic.WIDE_RATIO
import com.ballooner.domain.comic.panelShapes
import com.ballooner.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SinglePanelLayoutTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val style = ComicStyle(pageMargin = 0f, gutter = 0f, borderThickness = 0f)

    private fun editorFor(initial: Comic) =
        ComicEditorViewModel(comicId = 1L, repository = FakeComicRepository(initial))

    private fun content(viewModel: ComicEditorViewModel) =
        viewModel.uiState.value as ComicEditorUiState.Content

    private fun comic(grid: Grid, cuts: List<Cut> = emptyList()) = Comic(
        sizing = PageSizing.Ratio(SQUARE_RATIO),
        style = style,
        layout = Layout(grid, cuts),
        panels = List(grid.panelSpans().size) { Panel() },
    )

    @Test
    fun `the layout step lands on the preset picker`() = runTest {
        val viewModel = editorFor(comic(Grid(rows = 2, columns = 2)))

        advanceUntilIdle()

        assertNull(content(viewModel).layoutKind)
    }

    @Test
    fun `a one panel comic is active on the single preset`() {
        assertEquals(LayoutKind.SINGLE, layoutKindOf(comic(Grid(1, 1))))
    }

    @Test
    fun `a single row comic is active on the strip preset`() {
        assertEquals(LayoutKind.STRIP, layoutKindOf(comic(Grid(rows = 1, columns = 3))))
    }

    @Test
    fun `a rows and columns comic is active on the grid preset`() {
        assertEquals(LayoutKind.GRID, layoutKindOf(comic(Grid(rows = 2, columns = 3))))
    }

    @Test
    fun `a cut comic is active on the custom preset`() {
        val cut = Cut(NormalizedPoint(0f, 0.5f), NormalizedPoint(1f, 0.5f), CutScope.WholePage)

        assertEquals(LayoutKind.CUSTOM, layoutKindOf(comic(Grid(1, 1), listOf(cut))))
    }

    @Test
    fun `a merged grid is still a grid`() {
        // Merging is something a grid does, so it must not tip the comic into another kind.
        val grid = Grid(rows = 2, columns = 2, spans = listOf(Span(0, 0, columnCount = 2)))

        assertEquals(LayoutKind.GRID, layoutKindOf(comic(grid)))
    }

    @Test
    fun `a merged strip is still a strip`() {
        val grid = Grid(rows = 1, columns = 4, spans = listOf(Span(0, 0, columnCount = 2)))

        assertEquals(LayoutKind.STRIP, layoutKindOf(comic(grid)))
    }

    @Test
    fun `going back to the picker throws away what the preset changed`() = runTest {
        val viewModel = editorFor(comic(Grid(rows = 2, columns = 2)))
        advanceUntilIdle()
        val before = content(viewModel).comic

        viewModel.selectLayoutKind(LayoutKind.SINGLE)
        advanceUntilIdle()
        viewModel.discardLayoutKind()
        advanceUntilIdle()

        assertNull(content(viewModel).layoutKind)
        assertEquals(before, content(viewModel).comic)
    }

    @Test
    fun `going back to the picker leaves the preset the comic already was active`() = runTest {
        val viewModel = editorFor(comic(Grid(rows = 2, columns = 2)))
        advanceUntilIdle()

        viewModel.selectLayoutKind(LayoutKind.SINGLE)
        advanceUntilIdle()
        viewModel.discardLayoutKind()
        advanceUntilIdle()

        assertEquals(LayoutKind.GRID, layoutKindOf(content(viewModel).comic))
    }

    @Test
    fun `going back to the picker throws away a shape chosen in the options`() = runTest {
        val viewModel = editorFor(comic(Grid(1, 1)))
        advanceUntilIdle()
        val before = content(viewModel).comic

        viewModel.selectLayoutKind(LayoutKind.SINGLE)
        viewModel.setSizing(PageSizing.Ratio(WIDE_RATIO))
        advanceUntilIdle()
        viewModel.discardLayoutKind()
        advanceUntilIdle()

        assertEquals(before, content(viewModel).comic)
    }

    @Test
    fun `going back to the picker leaves nothing of the preset to undo`() = runTest {
        val viewModel = editorFor(comic(Grid(rows = 2, columns = 2)))
        advanceUntilIdle()

        viewModel.selectLayoutKind(LayoutKind.SINGLE)
        advanceUntilIdle()
        viewModel.discardLayoutKind()
        advanceUntilIdle()

        assertFalse(content(viewModel).canUndo)
    }

    @Test
    fun `moving on to the images step keeps what the preset changed`() = runTest {
        val viewModel = editorFor(comic(Grid(rows = 2, columns = 2)))
        advanceUntilIdle()

        viewModel.selectLayoutKind(LayoutKind.SINGLE)
        advanceUntilIdle()
        viewModel.selectStep(EditorStep.PLACEMENT)
        advanceUntilIdle()

        assertEquals(LayoutKind.SINGLE, layoutKindOf(content(viewModel).comic))
    }

    @Test
    fun `leaving the layout step returns it to the picker`() = runTest {
        val viewModel = editorFor(comic(Grid(1, 1)))
        advanceUntilIdle()
        viewModel.selectLayoutKind(LayoutKind.SINGLE)
        advanceUntilIdle()

        viewModel.selectStep(EditorStep.PLACEMENT)
        viewModel.selectStep(EditorStep.LAYOUT)
        advanceUntilIdle()

        assertNull(content(viewModel).layoutKind)
    }

    @Test
    fun `choosing single leaves one panel shaped by its image`() = runTest {
        val viewModel = editorFor(comic(Grid(rows = 2, columns = 2)))
        advanceUntilIdle()

        viewModel.selectLayoutKind(LayoutKind.SINGLE)
        advanceUntilIdle()

        assertEquals(LayoutKind.SINGLE, content(viewModel).layoutKind)
        assertEquals(1, content(viewModel).comic.panels.size)
        assertEquals(PageSizing.FromImage, content(viewModel).comic.sizing)
    }

    @Test
    fun `opening the options of the kind a comic already is changes nothing`() = runTest {
        val viewModel = editorFor(comic(Grid(1, 1)))
        advanceUntilIdle()
        val before = content(viewModel).comic

        viewModel.selectLayoutKind(LayoutKind.SINGLE)
        advanceUntilIdle()

        assertEquals(LayoutKind.SINGLE, content(viewModel).layoutKind)
        assertEquals(before, content(viewModel).comic)
    }

    @Test
    fun `choosing a panel shape reshapes the page`() = runTest {
        val viewModel = editorFor(comic(Grid(1, 1)))
        advanceUntilIdle()

        viewModel.setSizing(PageSizing.Ratio(WIDE_RATIO))
        advanceUntilIdle()

        val panel = content(viewModel).comic.panelShapes().single().bounds
        assertEquals(WIDE_RATIO, panel.width / panel.height, 1e-4f)
    }

    @Test
    fun `reshaping the panel keeps its image and can be undone`() = runTest {
        val start = comic(Grid(1, 1)).copy(panels = listOf(Panel(PanelImage("kept"))))
        val viewModel = editorFor(start)
        advanceUntilIdle()

        viewModel.setSizing(PageSizing.Ratio(WIDE_RATIO))
        advanceUntilIdle()

        assertEquals("kept", content(viewModel).comic.panels.single().image?.sourceUri)
        assertTrue(content(viewModel).canUndo)
    }

    @Test
    fun `an auto shaped panel follows the image put into it`() = runTest {
        val viewModel = editorFor(comic(Grid(1, 1)))
        advanceUntilIdle()

        viewModel.setSizing(PageSizing.FromImage)
        viewModel.setPanelImage(0, "wide", sourceAspect = 2f)
        advanceUntilIdle()

        assertEquals(0.5f, content(viewModel).comic.pageHeight, 1e-4f)
    }
}
