package com.ballooner.ui.comiceditor

import com.ballooner.data.comic.FakeComicRepository
import com.ballooner.domain.comic.Balloon
import com.ballooner.domain.comic.BalloonScope
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.ComicStyle
import com.ballooner.domain.comic.Grid
import com.ballooner.domain.comic.Layout
import com.ballooner.domain.comic.PageSizing
import com.ballooner.domain.comic.Panel
import com.ballooner.domain.comic.PanelImage
import com.ballooner.domain.comic.SQUARE_RATIO
import com.ballooner.domain.comic.Span
import com.ballooner.domain.comic.WIDE_RATIO
import com.ballooner.domain.model.BalloonType
import com.ballooner.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ComicEditorViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val style = ComicStyle(gutter = 0f, borderThickness = 0f)

    private fun comic(rows: Int, columns: Int, withImages: Boolean = false): Comic {
        val panelCount = rows * columns
        return Comic(
            name = "Test",
            sizing = PageSizing.Ratio(SQUARE_RATIO),
            style = style,
            layout = Layout(Grid(rows = rows, columns = columns)),
            panels = List(panelCount) { Panel(if (withImages) PanelImage("image$it") else null) },
        )
    }

    private fun editorFor(initial: Comic): Pair<ComicEditorViewModel, FakeComicRepository> {
        val repository = FakeComicRepository(initial)
        return ComicEditorViewModel(comicId = 1L, repository = repository) to repository
    }

    private fun content(viewModel: ComicEditorViewModel) =
        viewModel.uiState.value as ComicEditorUiState.Content

    @Test
    fun `the editor starts on the layout step once the comic has loaded`() = runTest {
        val (viewModel, _) = editorFor(comic(2, 2))

        assertEquals(ComicEditorUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        assertEquals(EditorStep.LAYOUT, content(viewModel).step)
        assertEquals(4, content(viewModel).comic.panels.size)
    }

    @Test
    fun `changing step keeps the document untouched`() = runTest {
        val (viewModel, _) = editorFor(comic(2, 2, withImages = true))
        advanceUntilIdle()
        val before = content(viewModel).comic

        viewModel.selectStep(EditorStep.PLACEMENT)

        assertEquals(EditorStep.PLACEMENT, content(viewModel).step)
        assertEquals(before, content(viewModel).comic)
    }

    @Test
    fun `selecting two adjacent panels allows merging them`() = runTest {
        val (viewModel, _) = editorFor(comic(2, 2))
        advanceUntilIdle()

        viewModel.toggleSelection(Span(0, 0))
        viewModel.toggleSelection(Span(0, 1))

        assertTrue(content(viewModel).canMerge)
        assertFalse(content(viewModel).canUnmerge)
    }

    @Test
    fun `an L-shaped selection cannot be merged`() = runTest {
        val (viewModel, _) = editorFor(comic(2, 2))
        advanceUntilIdle()

        viewModel.toggleSelection(Span(0, 0))
        viewModel.toggleSelection(Span(0, 1))
        viewModel.toggleSelection(Span(1, 0))

        assertFalse(content(viewModel).canMerge)
    }

    @Test
    fun `tapping a selected panel deselects it`() = runTest {
        val (viewModel, _) = editorFor(comic(2, 2))
        advanceUntilIdle()

        viewModel.toggleSelection(Span(0, 0))
        viewModel.toggleSelection(Span(0, 0))

        assertEquals(emptyList<Span>(), content(viewModel).selection)
    }

    @Test
    fun `merging empty panels applies immediately and clears the selection`() = runTest {
        val (viewModel, repository) = editorFor(comic(2, 2))
        advanceUntilIdle()
        viewModel.toggleSelection(Span(0, 0))
        viewModel.toggleSelection(Span(0, 1))

        viewModel.mergeSelection()
        advanceUntilIdle()

        assertEquals(3, content(viewModel).comic.panels.size)
        assertEquals(emptyList<Span>(), content(viewModel).selection)
        assertNull(content(viewModel).warning)
        assertEquals(3, repository.saved.value.getValue(1L).panels.size)
    }

    @Test
    fun `merging panels that hold images asks before discarding them`() = runTest {
        val (viewModel, repository) = editorFor(comic(2, 2, withImages = true))
        advanceUntilIdle()
        viewModel.toggleSelection(Span(0, 0))
        viewModel.toggleSelection(Span(0, 1))

        viewModel.mergeSelection()
        advanceUntilIdle()

        val warning = content(viewModel).warning
        assertNotNull(warning)
        assertEquals(1, warning!!.removedImages)
        assertEquals(0, warning.removedBalloons)
        assertEquals(4, content(viewModel).comic.panels.size)
        assertEquals(4, repository.saved.value.getValue(1L).panels.size)
    }

    @Test
    fun `confirming a destructive change applies it`() = runTest {
        val (viewModel, repository) = editorFor(comic(2, 2, withImages = true))
        advanceUntilIdle()
        viewModel.toggleSelection(Span(0, 0))
        viewModel.toggleSelection(Span(0, 1))
        viewModel.mergeSelection()

        viewModel.confirmLayoutChange()
        advanceUntilIdle()

        assertNull(content(viewModel).warning)
        assertEquals(3, content(viewModel).comic.panels.size)
        assertEquals("image0", content(viewModel).comic.panels[0].image?.sourceUri)
        assertEquals(3, repository.saved.value.getValue(1L).panels.size)
    }

    @Test
    fun `cancelling a destructive change leaves the comic alone`() = runTest {
        val (viewModel, _) = editorFor(comic(2, 2, withImages = true))
        advanceUntilIdle()
        val before = content(viewModel).comic
        viewModel.toggleSelection(Span(0, 0))
        viewModel.toggleSelection(Span(0, 1))
        viewModel.mergeSelection()

        viewModel.cancelLayoutChange()
        advanceUntilIdle()

        assertNull(content(viewModel).warning)
        assertEquals(before, content(viewModel).comic)
    }

    @Test
    fun `a merged panel can be unmerged`() = runTest {
        val merged = Grid(rows = 2, columns = 2, spans = listOf(Span(0, 0, rowCount = 1, columnCount = 2)))
        val (viewModel, _) = editorFor(comic(2, 2).copy(layout = Layout(merged), panels = List(3) { Panel() }))
        advanceUntilIdle()

        viewModel.toggleSelection(Span(0, 0, rowCount = 1, columnCount = 2))
        assertTrue(content(viewModel).canUnmerge)
        viewModel.unmergeSelection()
        advanceUntilIdle()

        assertEquals(4, content(viewModel).comic.panels.size)
    }

    @Test
    fun `a preset pairs panels off in reading order`() = runTest {
        val (viewModel, _) = editorFor(comic(1, 4, withImages = true))
        advanceUntilIdle()

        viewModel.applyPreset(rows = 2, columns = 2)
        advanceUntilIdle()

        assertEquals(
            listOf("image0", "image1", "image2", "image3"),
            content(viewModel).comic.panels.map { it.image?.sourceUri },
        )
    }

    @Test
    fun `a smaller preset warns before dropping panels`() = runTest {
        val (viewModel, _) = editorFor(comic(1, 4, withImages = true))
        advanceUntilIdle()

        viewModel.applyPreset(rows = 1, columns = 2)
        advanceUntilIdle()

        assertEquals(2, content(viewModel).warning?.removedImages)
    }

    @Test
    fun `changing the page sizing keeps every panel and its image`() = runTest {
        val (viewModel, _) = editorFor(comic(2, 2, withImages = true))
        advanceUntilIdle()

        viewModel.setSizing(PageSizing.Ratio(WIDE_RATIO))
        advanceUntilIdle()

        assertEquals(PageSizing.Ratio(WIDE_RATIO), content(viewModel).comic.sizing)
        assertEquals(4, content(viewModel).comic.panels.count { it.image != null })
    }

    @Test
    fun `undo reverses the last layout change`() = runTest {
        val (viewModel, repository) = editorFor(comic(2, 2))
        advanceUntilIdle()
        viewModel.applyPreset(rows = 3, columns = 3)
        advanceUntilIdle()
        assertEquals(9, content(viewModel).comic.panels.size)

        viewModel.undo()
        advanceUntilIdle()

        assertEquals(4, content(viewModel).comic.panels.size)
        assertEquals(4, repository.saved.value.getValue(1L).panels.size)
        assertFalse(content(viewModel).canUndo)
    }

    @Test
    fun `undo with nothing to reverse does nothing`() = runTest {
        val (viewModel, _) = editorFor(comic(2, 2))
        advanceUntilIdle()

        viewModel.undo()
        advanceUntilIdle()

        assertEquals(4, content(viewModel).comic.panels.size)
    }

    @Test
    fun `a panel balloon follows its panel through a merge`() = runTest {
        val withBalloon = comic(2, 2).copy(
            balloons = listOf(Balloon(id = 1, type = BalloonType.SPEAK, scope = BalloonScope.Panel(0))),
        )
        val (viewModel, _) = editorFor(withBalloon)
        advanceUntilIdle()
        viewModel.toggleSelection(Span(0, 0))
        viewModel.toggleSelection(Span(0, 1))

        viewModel.mergeSelection()
        advanceUntilIdle()

        assertEquals(BalloonScope.Panel(0), content(viewModel).comic.balloons.single().scope)
    }

    @Test
    fun `switching step clears the layout selection`() = runTest {
        val (viewModel, _) = editorFor(comic(2, 2))
        advanceUntilIdle()
        viewModel.toggleSelection(Span(0, 0))

        viewModel.selectStep(EditorStep.BALLOONS)

        assertEquals(emptyList<Span>(), content(viewModel).selection)
    }
}
