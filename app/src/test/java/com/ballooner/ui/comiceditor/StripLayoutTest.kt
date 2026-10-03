package com.ballooner.ui.comiceditor

import com.ballooner.data.comic.FakeComicRepository
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.ComicStyle
import com.ballooner.domain.comic.Grid
import com.ballooner.domain.comic.GridAxis
import com.ballooner.domain.comic.GridLine
import com.ballooner.domain.comic.Layout
import com.ballooner.domain.comic.PageSizing
import com.ballooner.domain.comic.Panel
import com.ballooner.domain.comic.PanelImage
import com.ballooner.domain.comic.SQUARE_RATIO
import com.ballooner.domain.comic.TALL_RATIO
import com.ballooner.domain.comic.WIDE_RATIO
import com.ballooner.domain.comic.panelShapes
import com.ballooner.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StripLayoutTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val style = ComicStyle(gutter = 0f, borderThickness = 0f)

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
    fun `choosing strip starts a comic that is not one on four square panels across`() = runTest {
        val viewModel = editorFor(comic(Grid(1, 1)))
        advanceUntilIdle()

        viewModel.selectLayoutKind(LayoutKind.STRIP)
        advanceUntilIdle()

        assertEquals(1, grid(viewModel).rows)
        assertEquals(4, grid(viewModel).columns)
        assertEquals(PageSizing.Ratio(SQUARE_RATIO), content(viewModel).comic.sizing)
    }

    @Test
    fun `choosing strip leaves a comic that already is one alone`() = runTest {
        val viewModel = editorFor(comic(Grid(rows = 4, columns = 1)))
        advanceUntilIdle()
        val before = content(viewModel).comic

        viewModel.selectLayoutKind(LayoutKind.STRIP)
        advanceUntilIdle()

        assertEquals(before, content(viewModel).comic)
    }

    @Test
    fun `turning a strip down keeps its panel count`() = runTest {
        val viewModel = editorFor(comic(Grid(rows = 1, columns = 4)))
        advanceUntilIdle()

        viewModel.applyPreset(rows = 4, columns = 1)
        advanceUntilIdle()

        assertEquals(4, grid(viewModel).rows)
        assertEquals(1, grid(viewModel).columns)
        assertEquals(4, content(viewModel).comic.panels.size)
    }

    @Test
    fun `adding panels to a strip keeps the ones already there`() = runTest {
        val images = listOf(PanelImage("a"), PanelImage("b"))
        val viewModel = editorFor(comic(Grid(rows = 1, columns = 2), images))
        advanceUntilIdle()

        viewModel.applyPreset(rows = 1, columns = 4)
        advanceUntilIdle()

        val uris = content(viewModel).comic.panels.map { it.image?.sourceUri }
        assertEquals(listOf("a", "b", null, null), uris)
    }

    @Test
    fun `dragging a gutter widens one panel and narrows its neighbour`() = runTest {
        val viewModel = editorFor(comic(Grid(rows = 1, columns = 3)))
        advanceUntilIdle()

        viewModel.startBoundaryDrag()
        viewModel.moveBoundary(GridLine(GridAxis.COLUMN, 1), delta = 0.2f)
        viewModel.endBoundaryDrag()
        advanceUntilIdle()

        val weights = grid(viewModel).columnWeights
        assertEquals(1.6f, weights[0], 1e-4f)
        assertEquals(0.4f, weights[1], 1e-4f)
        assertEquals(1f, weights[2], 1e-4f)
    }

    @Test
    fun `choosing a shape puts dragged panels back to one size`() = runTest {
        val viewModel = editorFor(comic(Grid(rows = 1, columns = 3)))
        advanceUntilIdle()
        viewModel.startBoundaryDrag()
        viewModel.moveBoundary(GridLine(GridAxis.COLUMN, 1), delta = 0.2f)
        viewModel.endBoundaryDrag()

        viewModel.setSizing(PageSizing.Ratio(WIDE_RATIO))
        advanceUntilIdle()

        assertEquals(listOf(1f, 1f, 1f), grid(viewModel).columnWeights)
        assertEquals(PageSizing.Ratio(WIDE_RATIO), content(viewModel).comic.sizing)
    }

    @Test
    fun `every panel of a strip is the shape that was chosen`() {
        val comic = comic(Grid(rows = 1, columns = 3))
            .copy(sizing = PageSizing.Ratio(0.75f))

        val shapes = panelShapes(comic.layout, comic.pageHeight, comic.style)

        val ratios = shapes.map { it.bounds.width / it.bounds.height }
        ratios.forEach { assertEquals(0.75f, it, 1e-4f) }
    }

    @Test
    fun `an auto strip takes every panel's shape from the first image`() {
        val comic = comic(
            grid = Grid(rows = 1, columns = 3),
            images = listOf(PanelImage("wide", sourceAspect = 2f), PanelImage("tall", sourceAspect = 0.5f)),
        ).copy(sizing = PageSizing.FromImage)

        val shapes = panelShapes(comic.layout, comic.pageHeight, comic.style)

        // The second panel's own image is ignored: a strip has one shape, and the first sets it.
        val ratios = shapes.map { it.bounds.width / it.bounds.height }
        ratios.forEach { assertEquals(2f, it, 1e-4f) }
    }

    @Test
    fun `an auto strip down the page shapes its panels the same way`() {
        val comic = comic(
            grid = Grid(rows = 3, columns = 1),
            images = listOf(PanelImage("wide", sourceAspect = 2f)),
        ).copy(sizing = PageSizing.FromImage)

        val shapes = panelShapes(comic.layout, comic.pageHeight, comic.style)

        val ratios = shapes.map { it.bounds.width / it.bounds.height }
        ratios.forEach { assertEquals(2f, it, 1e-4f) }
    }

    @Test
    fun `a long strip still gets the shape it asked for`() {
        // The panel count is open-ended, so the page has to stretch as far as the panels need
        // rather than hit a limit and quietly reshape them.
        listOf(12, 24, 40).forEach { count ->
            val across = comic(Grid(rows = 1, columns = count))
                .copy(sizing = PageSizing.Ratio(WIDE_RATIO))
            val down = comic(Grid(rows = count, columns = 1))
                .copy(sizing = PageSizing.Ratio(TALL_RATIO))

            panelShapes(across.layout, across.pageHeight, across.style).forEach {
                assertEquals("$count across", WIDE_RATIO, it.bounds.width / it.bounds.height, 1e-3f)
            }
            panelShapes(down.layout, down.pageHeight, down.style).forEach {
                assertEquals("$count down", TALL_RATIO, it.bounds.width / it.bounds.height, 1e-3f)
            }
        }
    }
}
