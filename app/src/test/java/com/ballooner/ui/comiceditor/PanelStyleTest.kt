package com.ballooner.ui.comiceditor

import com.ballooner.data.comic.FakeComicRepository
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.ComicStyle
import com.ballooner.domain.comic.Grid
import com.ballooner.domain.comic.Layout
import com.ballooner.domain.comic.PageSizing
import com.ballooner.domain.comic.Panel
import com.ballooner.domain.comic.PanelStyle
import com.ballooner.domain.comic.panelStyleAt
import com.ballooner.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** Covers a panel framed on its own, which overrides the comic style for that panel alone. */
@OptIn(ExperimentalCoroutinesApi::class)
class PanelStyleTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val comicStyle = ComicStyle(gutter = 0.02f, borderThickness = 0.005f, cornerRadius = 0.01f)

    private val comic = Comic(
        sizing = PageSizing.Ratio(0.5f),
        style = comicStyle,
        layout = Layout(Grid(rows = 1, columns = 2)),
        panels = List(2) { Panel() },
    )

    private fun editorFor(initial: Comic): Pair<ComicEditorViewModel, FakeComicRepository> {
        val repository = FakeComicRepository(initial)
        return ComicEditorViewModel(comicId = 1L, repository = repository) to repository
    }

    private fun content(viewModel: ComicEditorViewModel) =
        viewModel.uiState.value as ComicEditorUiState.Content

    @Test
    fun `a panel is framed by the comic style until it is given a frame of its own`() = runTest {
        assertEquals(comicStyle.panelStyle, comic.panelStyleAt(0))
    }

    @Test
    fun `a panel given its own frame is framed by it`() = runTest {
        val (viewModel, repository) = editorFor(comic)
        advanceUntilIdle()

        viewModel.setPanelStyle(1, PanelStyle(borderThickness = 0.02f, cornerRadius = 0.3f))
        advanceUntilIdle()

        viewModel.saveComic()
        advanceUntilIdle()

        val saved = repository.saved.value.getValue(1L)
        assertEquals(PanelStyle(0.02f, 0.3f), saved.panelStyleAt(1))
        // Its neighbour is untouched: the frame belongs to one panel, not to the pair.
        assertEquals(comicStyle.panelStyle, saved.panelStyleAt(0))
    }

    @Test
    fun `restyling the comic takes back every frame a panel was given`() = runTest {
        val (viewModel, _) = editorFor(comic)
        advanceUntilIdle()
        viewModel.setPanelStyle(0, PanelStyle(borderThickness = 0.02f, cornerRadius = 0.3f))
        viewModel.setPanelStyle(1, PanelStyle(borderThickness = 0f, cornerRadius = 0f))
        advanceUntilIdle()

        viewModel.setStyle(comicStyle.copy(borderThickness = 0.01f))
        advanceUntilIdle()

        assertEquals(listOf(null, null), content(viewModel).comic.panels.map { it.style })
    }

    @Test
    fun `a panel that is not there cannot be framed`() = runTest {
        val (viewModel, _) = editorFor(comic)
        advanceUntilIdle()

        viewModel.setPanelStyle(7, PanelStyle(borderThickness = 0.02f, cornerRadius = 0.3f))
        advanceUntilIdle()

        assertEquals(listOf(null, null), content(viewModel).comic.panels.map { it.style })
    }

    @Test
    fun `an unknown panel is framed by the comic style`() = runTest {
        assertEquals(comicStyle.panelStyle, comic.panelStyleAt(7))
        assertEquals(comicStyle.panelStyle, comic.panelStyleAt(null))
    }

    @Test
    fun `a panel keeps its own frame when the layout moves it`() = runTest {
        val (viewModel, _) = editorFor(comic)
        advanceUntilIdle()
        viewModel.setPanelStyle(0, PanelStyle(borderThickness = 0.02f, cornerRadius = 0.3f))
        advanceUntilIdle()

        // A cut reshapes the page without restyling it, so the frame travels with its panel.
        viewModel.applyPreset(rows = 2, columns = 2)
        advanceUntilIdle()

        val panels = content(viewModel).comic.panels
        assertEquals(PanelStyle(0.02f, 0.3f), panels[0].style)
        assertNull(panels[1].style)
    }
}
