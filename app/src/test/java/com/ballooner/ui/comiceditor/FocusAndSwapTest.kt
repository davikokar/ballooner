package com.ballooner.ui.comiceditor

import com.ballooner.data.comic.FakeComicRepository
import com.ballooner.domain.comic.Balloon
import com.ballooner.domain.comic.BalloonScope
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.ComicStyle
import com.ballooner.domain.comic.Grid
import com.ballooner.domain.comic.Layout
import com.ballooner.domain.comic.NormalizedPoint
import com.ballooner.domain.comic.PageSizing
import com.ballooner.domain.comic.Panel
import com.ballooner.domain.comic.PanelImage
import com.ballooner.domain.comic.SQUARE_RATIO
import com.ballooner.domain.model.BalloonType
import com.ballooner.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** Covers focusing one panel for closer work, and carrying an image from one panel to another. */
@OptIn(ExperimentalCoroutinesApi::class)
class FocusAndSwapTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val style = ComicStyle(pageMargin = 0f, gutter = 0f, borderThickness = 0f)

    private fun comic(panels: Int = 4, withImages: Boolean = true) = Comic(
        // A two by two grid of square panels, which makes the page square too.
        sizing = PageSizing.Ratio(SQUARE_RATIO),
        style = style,
        layout = Layout(Grid(rows = 2, columns = 2)),
        panels = List(panels) { Panel(if (withImages) PanelImage("image$it") else null) },
    )

    private fun editorFor(initial: Comic): Pair<ComicEditorViewModel, FakeComicRepository> {
        val repository = FakeComicRepository(initial)
        return ComicEditorViewModel(comicId = 1L, repository = repository) to repository
    }

    private fun content(viewModel: ComicEditorViewModel) =
        viewModel.uiState.value as ComicEditorUiState.Content

    private fun uris(viewModel: ComicEditorViewModel) =
        content(viewModel).comic.panels.map { it.image?.sourceUri }

    @Test
    fun `focusing a panel also makes it the one being worked on`() = runTest {
        val (viewModel, _) = editorFor(comic())
        advanceUntilIdle()

        viewModel.focusPanel(2)

        assertEquals(2, content(viewModel).focusedPanel)
        assertEquals(2, content(viewModel).activePanel)
    }

    @Test
    fun `focus changes nothing about the comic`() = runTest {
        val (viewModel, repository) = editorFor(comic())
        advanceUntilIdle()
        val before = content(viewModel).comic

        viewModel.focusPanel(1)
        viewModel.focusNeighbour(forward = true)
        viewModel.focusPanel(null)
        advanceUntilIdle()

        assertEquals(before, content(viewModel).comic)
        assertEquals(before, repository.saved.value.getValue(1L))
        assertFalse(content(viewModel).canUndo)
    }

    @Test
    fun `focusing a panel that is not there focuses nothing`() = runTest {
        val (viewModel, _) = editorFor(comic())
        advanceUntilIdle()

        viewModel.focusPanel(9)

        assertNull(content(viewModel).focusedPanel)
    }

    @Test
    fun `stepping forward moves to the next panel and wraps around`() = runTest {
        val (viewModel, _) = editorFor(comic())
        advanceUntilIdle()
        viewModel.focusPanel(2)

        viewModel.focusNeighbour(forward = true)
        assertEquals(3, content(viewModel).focusedPanel)

        viewModel.focusNeighbour(forward = true)
        assertEquals(0, content(viewModel).focusedPanel)
    }

    @Test
    fun `stepping back wraps around the other way`() = runTest {
        val (viewModel, _) = editorFor(comic())
        advanceUntilIdle()
        viewModel.focusPanel(0)

        viewModel.focusNeighbour(forward = false)

        assertEquals(3, content(viewModel).focusedPanel)
    }

    @Test
    fun `stepping does nothing when no panel is focused`() = runTest {
        val (viewModel, _) = editorFor(comic())
        advanceUntilIdle()

        viewModel.focusNeighbour(forward = true)

        assertNull(content(viewModel).focusedPanel)
    }

    @Test
    fun `leaving the step drops focus`() = runTest {
        val (viewModel, _) = editorFor(comic())
        advanceUntilIdle()
        viewModel.focusPanel(1)

        viewModel.selectStep(EditorStep.BALLOONS)

        assertNull(content(viewModel).focusedPanel)
    }

    @Test
    fun `stepping to another panel drops the balloon selection`() = runTest {
        val balloon = Balloon(id = 1, type = BalloonType.SPEAK, scope = BalloonScope.Panel(0))
        val (viewModel, _) = editorFor(comic().copy(balloons = listOf(balloon)))
        advanceUntilIdle()
        viewModel.selectStep(EditorStep.BALLOONS)
        viewModel.focusPanel(0)
        viewModel.selectBalloon(1)

        viewModel.focusNeighbour(forward = true)

        assertNull(content(viewModel).selectedBalloon)
    }

    @Test
    fun `carrying an image to another panel trades their places`() = runTest {
        val (viewModel, repository) = editorFor(comic())
        advanceUntilIdle()

        viewModel.swapPanelImages(from = 0, to = 3)
        advanceUntilIdle()

        assertEquals(listOf("image3", "image1", "image2", "image0"), uris(viewModel))
        assertEquals("image3", repository.saved.value.getValue(1L).panels[0].image?.sourceUri)
    }

    @Test
    fun `an image carried onto an empty panel leaves the first one empty`() = runTest {
        val comic = comic().let { it.copy(panels = listOf(it.panels[0], Panel(), it.panels[2], it.panels[3])) }
        val (viewModel, _) = editorFor(comic)
        advanceUntilIdle()

        viewModel.swapPanelImages(from = 0, to = 1)
        advanceUntilIdle()

        assertNull(uris(viewModel)[0])
        assertEquals("image0", uris(viewModel)[1])
    }

    @Test
    fun `a swap keeps each image's own fit`() = runTest {
        val first = PanelImage("image0", NormalizedPoint(0.3f, 0.3f), zoom = 2f, angleDegrees = 20f)
        val second = PanelImage("image1", NormalizedPoint(0.7f, 0.7f), zoom = 1.5f, angleDegrees = -10f)
        val comic = comic().let { it.copy(panels = listOf(Panel(first), Panel(second), it.panels[2], it.panels[3])) }
        val (viewModel, _) = editorFor(comic)
        advanceUntilIdle()

        viewModel.swapPanelImages(from = 0, to = 1)
        advanceUntilIdle()

        assertEquals(second, content(viewModel).comic.panels[0].image)
        assertEquals(first, content(viewModel).comic.panels[1].image)
    }

    @Test
    fun `a swap can be undone in one step`() = runTest {
        val (viewModel, _) = editorFor(comic())
        advanceUntilIdle()
        viewModel.swapPanelImages(0, 3)
        advanceUntilIdle()

        viewModel.undo()
        advanceUntilIdle()

        assertEquals(listOf("image0", "image1", "image2", "image3"), uris(viewModel))
    }

    @Test
    fun `carrying an image back onto itself changes nothing`() = runTest {
        val (viewModel, _) = editorFor(comic())
        advanceUntilIdle()

        viewModel.swapPanelImages(from = 2, to = 2)
        advanceUntilIdle()

        assertEquals(listOf("image0", "image1", "image2", "image3"), uris(viewModel))
        assertFalse(content(viewModel).canUndo)
    }

    @Test
    fun `swapping two empty panels does nothing`() = runTest {
        val (viewModel, _) = editorFor(comic(withImages = false))
        advanceUntilIdle()

        viewModel.swapPanelImages(from = 0, to = 1)
        advanceUntilIdle()

        assertFalse(content(viewModel).canUndo)
    }

    @Test
    fun `swapping with a panel that is not there does nothing`() = runTest {
        val (viewModel, _) = editorFor(comic())
        advanceUntilIdle()

        viewModel.swapPanelImages(from = 0, to = 9)
        advanceUntilIdle()

        assertEquals(listOf("image0", "image1", "image2", "image3"), uris(viewModel))
    }

    @Test
    fun `a swap leaves the layout and balloons alone`() = runTest {
        val balloon = Balloon(id = 1, type = BalloonType.SPEAK, scope = BalloonScope.Panel(0))
        val (viewModel, _) = editorFor(comic().copy(balloons = listOf(balloon)))
        advanceUntilIdle()
        val layout = content(viewModel).comic.layout

        viewModel.swapPanelImages(0, 1)
        advanceUntilIdle()

        assertEquals(layout, content(viewModel).comic.layout)
        assertEquals(listOf(balloon), content(viewModel).comic.balloons)
    }
}
