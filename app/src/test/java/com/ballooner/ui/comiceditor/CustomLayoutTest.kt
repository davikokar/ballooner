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
import com.ballooner.domain.comic.panelShapes
import com.ballooner.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CustomLayoutTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val style = ComicStyle(pageMargin = 0f, gutter = 0f, borderThickness = 0f)

    private fun comic(grid: Grid = Grid(1, 1), cuts: List<Cut> = emptyList()): Comic {
        val layout = Layout(grid, cuts)
        val bare = Comic(sizing = PageSizing.Ratio(SQUARE_RATIO), style = style, layout = layout)
        // Cuts make panels too, so the list has to match what the layout really produces.
        val count = panelShapes(layout, bare.pageHeight, style).size
        return bare.copy(panels = List(count) { Panel() })
    }

    private fun editorFor(initial: Comic) =
        ComicEditorViewModel(comicId = 1L, repository = FakeComicRepository(initial))

    private fun content(viewModel: ComicEditorViewModel) =
        viewModel.uiState.value as ComicEditorUiState.Content

    @Test
    fun `choosing custom starts from one whole panel`() = runTest {
        val viewModel = editorFor(comic(Grid(rows = 2, columns = 2)))
        advanceUntilIdle()

        viewModel.selectLayoutKind(LayoutKind.CUSTOM)
        advanceUntilIdle()

        assertEquals(LayoutKind.CUSTOM, content(viewModel).layoutKind)
        assertEquals(1, content(viewModel).comic.panels.size)
    }

    @Test
    fun `choosing custom leaves a comic that is already cut alone`() = runTest {
        val cut = Cut(NormalizedPoint(0f, 0.5f), NormalizedPoint(1f, 0.5f), CutScope.WholePage)
        val viewModel = editorFor(comic(cuts = listOf(cut)))
        advanceUntilIdle()
        val before = content(viewModel).comic

        viewModel.selectLayoutKind(LayoutKind.CUSTOM)
        advanceUntilIdle()

        assertEquals(before, content(viewModel).comic)
    }

    @Test
    fun `dragging one end of a cut swings it about the other`() = runTest {
        val cut = Cut(NormalizedPoint(0f, 0.5f), NormalizedPoint(1f, 0.5f), CutScope.WholePage)
        val viewModel = editorFor(comic(cuts = listOf(cut)))
        advanceUntilIdle()

        viewModel.startBoundaryDrag()
        viewModel.moveCutEnd(index = 0, start = false, to = NormalizedPoint(1f, 0.8f))
        viewModel.endCutDrag()
        advanceUntilIdle()

        val moved = content(viewModel).comic.layout.cuts.single()
        assertEquals(NormalizedPoint(0f, 0.5f), moved.a)
        assertEquals(NormalizedPoint(1f, 0.8f), moved.b)
        assertEquals(2, content(viewModel).comic.panels.size)
    }

    @Test
    fun `a whole drag of a cut end is one undo step`() = runTest {
        val cut = Cut(NormalizedPoint(0f, 0.5f), NormalizedPoint(1f, 0.5f), CutScope.WholePage)
        val viewModel = editorFor(comic(cuts = listOf(cut)))
        advanceUntilIdle()

        viewModel.startBoundaryDrag()
        listOf(0.55f, 0.6f, 0.7f, 0.8f).forEach {
            viewModel.moveCutEnd(index = 0, start = false, to = NormalizedPoint(1f, it))
        }
        viewModel.endCutDrag()
        advanceUntilIdle()
        viewModel.undo()
        advanceUntilIdle()

        assertEquals(NormalizedPoint(1f, 0.5f), content(viewModel).comic.layout.cuts.single().b)
    }

    @Test
    fun `dragging a handle keeps each image with its own panel`() = runTest {
        // Swinging a cut reshapes panels and can reorder them, so the images have to be carried
        // across rather than left sitting at their old index.
        val cut = Cut(NormalizedPoint(0f, 0.5f), NormalizedPoint(1f, 0.5f), CutScope.WholePage)
        val filled = comic(cuts = listOf(cut))
            .copy(panels = listOf(Panel(PanelImage("top")), Panel(PanelImage("bottom"))))
        val viewModel = editorFor(filled)
        advanceUntilIdle()

        viewModel.startBoundaryDrag()
        viewModel.moveCutEnd(index = 0, start = false, to = NormalizedPoint(1f, 0.62f))
        viewModel.endCutDrag()
        advanceUntilIdle()

        val uris = content(viewModel).comic.panels.map { it.image?.sourceUri }
        assertEquals(listOf("top", "bottom"), uris)
    }

    @Test
    fun `a drag that would discard an image is refused`() = runTest {
        // Both ends sit off the page, so swinging one can sweep the line clear of it entirely.
        // A cut's ends only give it a direction, so an end left on the page can never do that.
        val cut = Cut(
            a = NormalizedPoint(0.5f, 3f),
            b = NormalizedPoint(0.5f, 4f),
            scope = CutScope.WholePage,
        )
        val filled = comic(cuts = listOf(cut))
            .copy(panels = listOf(Panel(PanelImage("left")), Panel(PanelImage("right"))))
        val viewModel = editorFor(filled)
        advanceUntilIdle()
        assertEquals(2, content(viewModel).comic.panels.size)

        viewModel.startBoundaryDrag()
        viewModel.moveCutEnd(index = 0, start = false, to = NormalizedPoint(3f, 4f))
        viewModel.endCutDrag()
        advanceUntilIdle()

        assertEquals(2, content(viewModel).comic.panels.size)
        assertEquals(NormalizedPoint(0.5f, 4f), content(viewModel).comic.layout.cuts.single().b)
    }

    @Test
    fun `a cut end cannot be dragged onto its other end`() = runTest {
        val cut = Cut(NormalizedPoint(0f, 0.5f), NormalizedPoint(1f, 0.5f), CutScope.WholePage)
        val viewModel = editorFor(comic(cuts = listOf(cut)))
        advanceUntilIdle()

        viewModel.startBoundaryDrag()
        viewModel.moveCutEnd(index = 0, start = true, to = NormalizedPoint(1f, 0.5f))
        viewModel.endCutDrag()
        advanceUntilIdle()

        assertEquals(NormalizedPoint(0f, 0.5f), content(viewModel).comic.layout.cuts.single().a)
    }

    @Test
    fun `a line drawn across the page splits the panel it was drawn on`() = runTest {
        val viewModel = editorFor(comic())
        advanceUntilIdle()

        viewModel.addCut(
            from = NormalizedPoint(0f, 0.5f),
            to = NormalizedPoint(1f, 0.5f),
            scope = CutScope.AtPoint(NormalizedPoint(0f, 0.5f)),
        )
        advanceUntilIdle()

        assertEquals(2, content(viewModel).comic.panels.size)
    }

    @Test
    fun `a second line cuts one of the pieces the first one left`() = runTest {
        val viewModel = editorFor(comic())
        advanceUntilIdle()
        viewModel.addCut(
            from = NormalizedPoint(0f, 0.5f),
            to = NormalizedPoint(1f, 0.5f),
            scope = CutScope.AtPoint(NormalizedPoint(0.5f, 0.25f)),
        )
        advanceUntilIdle()

        // Anchored in the lower piece, so only that one is divided.
        viewModel.addCut(
            from = NormalizedPoint(0.5f, 0.5f),
            to = NormalizedPoint(0.5f, 1f),
            scope = CutScope.AtPoint(NormalizedPoint(0.5f, 0.75f)),
        )
        advanceUntilIdle()

        assertEquals(3, content(viewModel).comic.panels.size)
    }

    @Test
    fun `a diagonal line cuts just as well as a straight one`() = runTest {
        val viewModel = editorFor(comic())
        advanceUntilIdle()

        viewModel.addCut(
            from = NormalizedPoint(0f, 0.2f),
            to = NormalizedPoint(1f, 0.8f),
            scope = CutScope.AtPoint(NormalizedPoint(0.5f, 0.5f)),
        )
        advanceUntilIdle()

        assertEquals(2, content(viewModel).comic.panels.size)
    }

    @Test
    fun `a line that separates nothing is not kept`() = runTest {
        val viewModel = editorFor(comic())
        advanceUntilIdle()

        // Along the very edge, so there is nothing on one side of it.
        viewModel.addCut(
            from = NormalizedPoint(0f, 0f),
            to = NormalizedPoint(1f, 0f),
            scope = CutScope.AtPoint(NormalizedPoint(0.5f, 0.5f)),
        )
        advanceUntilIdle()

        assertEquals(1, content(viewModel).comic.panels.size)
        assertEquals(emptyList<Cut>(), content(viewModel).comic.layout.cuts)
    }

    @Test
    fun `a cut can be undone`() = runTest {
        val viewModel = editorFor(comic())
        advanceUntilIdle()
        viewModel.addCut(
            from = NormalizedPoint(0f, 0.5f),
            to = NormalizedPoint(1f, 0.5f),
            scope = CutScope.AtPoint(NormalizedPoint(0.5f, 0.25f)),
        )
        advanceUntilIdle()
        assertTrue(content(viewModel).canUndo)

        viewModel.undo()
        advanceUntilIdle()

        assertEquals(1, content(viewModel).comic.panels.size)
        assertEquals(emptyList<Cut>(), content(viewModel).comic.layout.cuts)
    }

    @Test
    fun `a cut comic opens on the custom preset`() {
        val cut = Cut(NormalizedPoint(0f, 0.5f), NormalizedPoint(1f, 0.5f), CutScope.WholePage)

        assertEquals(LayoutKind.CUSTOM, layoutKindOf(comic(cuts = listOf(cut))))
    }
}
