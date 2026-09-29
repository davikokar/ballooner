package com.ballooner.ui.comiceditor

import com.ballooner.data.comic.FakeComicRepository
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.ComicStyle
import com.ballooner.domain.comic.Grid
import com.ballooner.domain.comic.Layout
import com.ballooner.domain.comic.PageSizing
import com.ballooner.domain.comic.Panel
import com.ballooner.domain.comic.PanelImage
import com.ballooner.domain.comic.SQUARE_RATIO
import com.ballooner.domain.comic.TALL_RATIO
import com.ballooner.domain.comic.panelShapes
import com.ballooner.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GridLayoutTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val style = ComicStyle(pageMargin = 0f, gutter = 0f, borderThickness = 0f)

    private fun comic(grid: Grid, images: List<PanelImage?> = emptyList()) = Comic(
        sizing = PageSizing.Ratio(SQUARE_RATIO),
        style = style,
        layout = Layout(grid),
        panels = List(grid.panelSpans().size) { Panel(images.getOrNull(it)) },
    )

    private fun editorFor(initial: Comic) =
        ComicEditorViewModel(comicId = 1L, repository = FakeComicRepository(initial))

    private fun content(viewModel: ComicEditorViewModel) =
        viewModel.uiState.value as ComicEditorUiState.Content

    private fun grid(viewModel: ComicEditorViewModel) = content(viewModel).comic.layout.grid

    @Test
    fun `choosing grid starts a comic that is not one on two by two`() = runTest {
        val viewModel = editorFor(comic(Grid(1, 1)))
        advanceUntilIdle()

        viewModel.selectLayoutKind(LayoutKind.GRID)
        advanceUntilIdle()

        assertEquals(2, grid(viewModel).rows)
        assertEquals(2, grid(viewModel).columns)
        assertEquals(4, content(viewModel).comic.panels.size)
    }

    @Test
    fun `choosing grid leaves a comic that already is one alone`() = runTest {
        val viewModel = editorFor(comic(Grid(rows = 3, columns = 4)))
        advanceUntilIdle()
        val before = content(viewModel).comic

        viewModel.selectLayoutKind(LayoutKind.GRID)
        advanceUntilIdle()

        assertEquals(before, content(viewModel).comic)
    }

    @Test
    fun `adding a row keeps the panels already there`() = runTest {
        val images = listOf(PanelImage("a"), PanelImage("b"), PanelImage("c"), PanelImage("d"))
        val viewModel = editorFor(comic(Grid(rows = 2, columns = 2), images))
        advanceUntilIdle()

        viewModel.applyPreset(rows = 3, columns = 2)
        advanceUntilIdle()

        val uris = content(viewModel).comic.panels.map { it.image?.sourceUri }
        assertEquals(listOf("a", "b", "c", "d", null, null), uris)
    }

    @Test
    fun `every cell of a grid is the shape that was chosen`() {
        val comic = comic(Grid(rows = 3, columns = 4)).copy(sizing = PageSizing.Ratio(TALL_RATIO))

        val shapes = panelShapes(comic.layout, comic.pageHeight, comic.style)

        assertEquals(12, shapes.size)
        shapes.forEach {
            assertEquals(TALL_RATIO, it.bounds.width / it.bounds.height, 1e-3f)
        }
    }

    @Test
    fun `an auto grid takes every cell's shape from the first image`() {
        val comic = comic(
            grid = Grid(rows = 2, columns = 3),
            images = listOf(PanelImage("wide", sourceAspect = 2f), PanelImage("tall", sourceAspect = 0.5f)),
        ).copy(sizing = PageSizing.FromImage)

        val shapes = panelShapes(comic.layout, comic.pageHeight, comic.style)

        shapes.forEach { assertEquals(2f, it.bounds.width / it.bounds.height, 1e-3f) }
    }

    @Test
    fun `a large grid still gets the shape it asked for`() {
        val comic = comic(Grid(rows = 8, columns = 6)).copy(sizing = PageSizing.Ratio(TALL_RATIO))

        val shapes = panelShapes(comic.layout, comic.pageHeight, comic.style)

        assertEquals(48, shapes.size)
        shapes.forEach {
            assertEquals(TALL_RATIO, it.bounds.width / it.bounds.height, 1e-3f)
        }
    }

    @Test
    fun `the gutter does not change the shape the cells come out`() {
        val comic = comic(Grid(rows = 3, columns = 3)).copy(
            sizing = PageSizing.Ratio(SQUARE_RATIO),
            style = ComicStyle(pageMargin = 0.03f, gutter = 0.04f, borderThickness = 0f),
        )

        val shapes = panelShapes(comic.layout, comic.pageHeight, comic.style)

        shapes.forEach {
            assertEquals(SQUARE_RATIO, it.bounds.width / it.bounds.height, 1e-3f)
        }
    }
}
