package com.ballooner.ui.comiceditor

import com.ballooner.data.comic.FakeComicRepository
import com.ballooner.domain.comic.Balloon
import com.ballooner.domain.comic.BalloonScope
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.ComicStyle
import com.ballooner.domain.comic.Grid
import com.ballooner.domain.comic.Layout
import com.ballooner.domain.comic.MAX_BALLOON_BORDER
import com.ballooner.domain.comic.MAX_BALLOON_TEXT_SIZE
import com.ballooner.domain.comic.MIN_BALLOON_BORDER
import com.ballooner.domain.comic.MIN_BALLOON_TEXT_SIZE
import com.ballooner.domain.comic.MIN_TAIL_WIDTH
import com.ballooner.domain.comic.NormalizedPoint
import com.ballooner.domain.comic.PageSizing
import com.ballooner.domain.comic.Panel
import com.ballooner.domain.comic.Span
import com.ballooner.domain.comic.WIDE_RATIO
import com.ballooner.domain.comic.panelIndex
import com.ballooner.domain.model.BalloonFont
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

/** Covers the Balloon step: adding, selecting, editing, and scoping balloons. */
@OptIn(ExperimentalCoroutinesApi::class)
class BalloonStepTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val style = ComicStyle(gutter = 0f, borderThickness = 0f)

    private fun comic(balloons: List<Balloon> = emptyList()) = Comic(
        // Two panels side by side, each held at half as wide as it is tall, so the page is square.
        sizing = PageSizing.Ratio(0.5f),
        style = style,
        layout = Layout(Grid(rows = 1, columns = 2)),
        panels = List(2) { Panel() },
        balloons = balloons,
    )

    private fun editorFor(initial: Comic): Pair<ComicEditorViewModel, FakeComicRepository> {
        val repository = FakeComicRepository(initial)
        return ComicEditorViewModel(comicId = 1L, repository = repository) to repository
    }

    private fun content(viewModel: ComicEditorViewModel) =
        viewModel.uiState.value as ComicEditorUiState.Content

    private fun balloons(viewModel: ComicEditorViewModel) = content(viewModel).comic.balloons

    @Test
    fun `arriving at the step starts on the first panel`() = runTest {
        val (viewModel, _) = editorFor(comic())
        advanceUntilIdle()

        viewModel.selectStep(EditorStep.BALLOONS)

        assertEquals(0, content(viewModel).activePanel)
    }

    @Test
    fun `adding a balloon puts it in the chosen panel and selects it`() = runTest {
        val (viewModel, repository) = editorFor(comic())
        advanceUntilIdle()

        viewModel.addBalloon(BalloonType.SPEAK, panelIndex = 1)
        advanceUntilIdle()

        val added = balloons(viewModel).single()
        assertEquals(BalloonScope.Panel(1), added.scope)
        assertEquals(added.id, content(viewModel).selectedBalloon)
        assertEquals(1, repository.saved.value.getValue(1L).balloons.size)
    }

    @Test
    fun `adding a balloon to the comic gives it no panel`() = runTest {
        val (viewModel, _) = editorFor(comic())
        advanceUntilIdle()

        viewModel.addBalloon(BalloonType.CAPTION, panelIndex = null)
        advanceUntilIdle()

        assertEquals(BalloonScope.Comic, balloons(viewModel).single().scope)
    }

    @Test
    fun `balloon ids rise and are never reused`() = runTest {
        val (viewModel, _) = editorFor(comic())
        advanceUntilIdle()

        viewModel.addBalloon(BalloonType.SPEAK, 0)
        viewModel.addBalloon(BalloonType.THINK, 0)
        val second = balloons(viewModel).last().id
        viewModel.deleteBalloon(second)
        viewModel.addBalloon(BalloonType.YELL, 0)
        advanceUntilIdle()

        assertEquals(listOf(1L, 3L), balloons(viewModel).map { it.id })
    }

    @Test
    fun `a caption starts with no tail and square corners`() = runTest {
        val (viewModel, _) = editorFor(comic())
        advanceUntilIdle()

        viewModel.addBalloon(BalloonType.CAPTION, 0)
        advanceUntilIdle()

        val caption = balloons(viewModel).single()
        assertEquals(0f, caption.tailLength, TOLERANCE)
        assertEquals(0f, caption.cornerRoundness, TOLERANCE)
    }

    @Test
    fun `deleting a balloon clears the selection`() = runTest {
        val (viewModel, _) = editorFor(comic())
        advanceUntilIdle()
        viewModel.addBalloon(BalloonType.SPEAK, 0)
        val id = balloons(viewModel).single().id

        viewModel.deleteBalloon(id)
        advanceUntilIdle()

        assertEquals(emptyList<Balloon>(), balloons(viewModel))
        assertNull(content(viewModel).selectedBalloon)
    }

    @Test
    fun `selecting a balloon that is gone selects nothing`() = runTest {
        val (viewModel, _) = editorFor(comic())
        advanceUntilIdle()

        viewModel.selectBalloon(42)

        assertNull(content(viewModel).selectedBalloon)
    }

    @Test
    fun `typing into a balloon is saved`() = runTest {
        val (viewModel, repository) = editorFor(comic())
        advanceUntilIdle()
        viewModel.addBalloon(BalloonType.SPEAK, 0)
        val id = balloons(viewModel).single().id

        viewModel.setBalloonText(id, "Pow!")
        advanceUntilIdle()

        assertEquals("Pow!", balloons(viewModel).single().text)
        assertEquals("Pow!", repository.saved.value.getValue(1L).balloons.single().text)
    }

    @Test
    fun `moving a balloon records its place relative to its panel`() = runTest {
        val balloon = Balloon(id = 1, type = BalloonType.SPEAK, scope = BalloonScope.Panel(1))
        val (viewModel, _) = editorFor(comic(listOf(balloon)))
        advanceUntilIdle()

        // Panel 1 is the right half of a square page, so this is its centre.
        viewModel.moveBalloon(1, x = 0.75f, y = 0.5f)
        advanceUntilIdle()

        val moved = balloons(viewModel).single()
        assertEquals(0.5f, moved.centre.u, TOLERANCE)
        assertEquals(0.5f, moved.centre.v, TOLERANCE)
    }

    @Test
    fun `resizing a balloon keeps its centre`() = runTest {
        val balloon = Balloon(
            id = 1,
            type = BalloonType.SPEAK,
            scope = BalloonScope.Panel(0),
            centre = NormalizedPoint(0.5f, 0.5f),
        )
        val (viewModel, _) = editorFor(comic(listOf(balloon)))
        advanceUntilIdle()

        // Panel 0's centre is at (0.25, 0.5); dragging the corner a tenth out each way.
        viewModel.resizeBalloon(1, x = 0.35f, y = 0.6f)
        advanceUntilIdle()

        val resized = balloons(viewModel).single()
        assertEquals(0.2f, resized.width, TOLERANCE)
        assertEquals(0.2f, resized.height, TOLERANCE)
        assertEquals(0.5f, resized.centre.u, TOLERANCE)
    }

    @Test
    fun `aiming the tail points it at the target`() = runTest {
        val balloon = Balloon(
            id = 1,
            type = BalloonType.SPEAK,
            scope = BalloonScope.Panel(0),
            centre = NormalizedPoint(0.5f, 0.5f),
        )
        val (viewModel, _) = editorFor(comic(listOf(balloon)))
        advanceUntilIdle()

        // Straight down from the panel centre at (0.25, 0.5).
        viewModel.pointBalloonTail(1, x = 0.25f, y = 0.9f)
        advanceUntilIdle()

        assertEquals(90f, balloons(viewModel).single().tailAngleDegrees, 0.5f)
        assertTrue(balloons(viewModel).single().tailLength > 0f)
    }

    @Test
    fun `a whole balloon drag counts as one undo`() = runTest {
        val balloon = Balloon(id = 1, type = BalloonType.SPEAK, scope = BalloonScope.Panel(0))
        val (viewModel, _) = editorFor(comic(listOf(balloon)))
        advanceUntilIdle()

        viewModel.startBalloonGesture()
        viewModel.moveBalloon(1, 0.2f, 0.3f)
        viewModel.moveBalloon(1, 0.3f, 0.4f)
        viewModel.moveBalloon(1, 0.4f, 0.5f)
        viewModel.endBalloonGesture()
        advanceUntilIdle()

        viewModel.undo()
        advanceUntilIdle()

        assertEquals(balloon, balloons(viewModel).single())
        assertFalse(content(viewModel).canUndo)
    }

    @Test
    fun `freeing a balloon from its panel does not move it on the page`() = runTest {
        val balloon = Balloon(
            id = 1,
            type = BalloonType.SPEAK,
            scope = BalloonScope.Panel(1),
            centre = NormalizedPoint(0.25f, 0.75f),
        )
        val (viewModel, _) = editorFor(comic(listOf(balloon)))
        advanceUntilIdle()

        viewModel.toggleBalloonScope(1)
        advanceUntilIdle()

        val freed = balloons(viewModel).single()
        assertEquals(BalloonScope.Comic, freed.scope)
        // Panel 1 spans x 0.5..1, so a quarter across it is 0.625 of the page.
        assertEquals(0.625f, freed.centre.u, TOLERANCE)
        assertEquals(0.75f, freed.centre.v, TOLERANCE)
    }

    @Test
    fun `putting a comic balloon back in a panel picks the one it sits over`() = runTest {
        val balloon = Balloon(
            id = 1,
            type = BalloonType.SPEAK,
            scope = BalloonScope.Comic,
            centre = NormalizedPoint(0.75f, 0.5f),
        )
        val (viewModel, _) = editorFor(comic(listOf(balloon)))
        advanceUntilIdle()

        viewModel.toggleBalloonScope(1)
        advanceUntilIdle()

        val owned = balloons(viewModel).single()
        assertEquals(1, owned.panelIndex)
        assertEquals(0.5f, owned.centre.u, TOLERANCE)
    }

    @Test
    fun `a comic balloon over no panel cannot be put in one`() = runTest {
        val gutterStyle = ComicStyle(gutter = 0.2f, borderThickness = 0f)
        val balloon = Balloon(
            id = 1,
            type = BalloonType.SPEAK,
            scope = BalloonScope.Comic,
            centre = NormalizedPoint(0.02f, 0.02f),
        )
        val (viewModel, _) = editorFor(comic(listOf(balloon)).copy(style = gutterStyle))
        advanceUntilIdle()

        viewModel.toggleBalloonScope(1)
        advanceUntilIdle()

        assertEquals(BalloonScope.Comic, balloons(viewModel).single().scope)
    }

    @Test
    fun `a panel balloon follows its panel when the layout changes`() = runTest {
        val balloon = Balloon(id = 1, type = BalloonType.SPEAK, scope = BalloonScope.Panel(1))
        val (viewModel, _) = editorFor(comic(listOf(balloon)))
        advanceUntilIdle()

        viewModel.toggleSelection(Span(0, 0))
        viewModel.toggleSelection(Span(0, 1))
        viewModel.mergeSelection()
        advanceUntilIdle()

        assertEquals(1, balloons(viewModel).size)
        assertNotNull(balloons(viewModel).single().panelIndex)
    }

    @Test
    fun `balloons are untouched by the other steps`() = runTest {
        val balloon = Balloon(id = 1, type = BalloonType.SPEAK, scope = BalloonScope.Panel(0))
        val (viewModel, _) = editorFor(comic(listOf(balloon)))
        advanceUntilIdle()

        viewModel.setStyle(ComicStyle(gutter = 0.04f, borderThickness = 0.01f))
        viewModel.setSizing(PageSizing.Ratio(WIDE_RATIO))
        advanceUntilIdle()

        assertEquals(balloon, balloons(viewModel).single())
    }

    @Test
    fun `choosing a font changes only that balloon`() = runTest {
        val balloons = listOf(
            Balloon(id = 1, type = BalloonType.SPEAK, scope = BalloonScope.Panel(0)),
            Balloon(id = 2, type = BalloonType.SPEAK, scope = BalloonScope.Panel(1)),
        )
        val (viewModel, repository) = editorFor(comic(balloons))
        advanceUntilIdle()

        viewModel.setBalloonFont(1, BalloonFont.MONOSPACE)
        advanceUntilIdle()

        assertEquals(BalloonFont.MONOSPACE, balloons(viewModel).first { it.id == 1L }.font)
        assertEquals(balloons[1].font, balloons(viewModel).first { it.id == 2L }.font)
        assertEquals(BalloonFont.MONOSPACE, repository.saved.value.getValue(1L).balloons.first().font)
    }

    @Test
    fun `text size is kept within what can be read`() = runTest {
        val balloon = Balloon(id = 1, type = BalloonType.SPEAK, scope = BalloonScope.Panel(0))
        val (viewModel, _) = editorFor(comic(listOf(balloon)))
        advanceUntilIdle()

        viewModel.setBalloonTextSize(1, 99f)
        advanceUntilIdle()
        assertEquals(MAX_BALLOON_TEXT_SIZE, balloons(viewModel).single().fontSize, TOLERANCE)

        viewModel.setBalloonTextSize(1, 0f)
        advanceUntilIdle()
        assertEquals(MIN_BALLOON_TEXT_SIZE, balloons(viewModel).single().fontSize, TOLERANCE)
    }

    @Test
    fun `roundness runs from square to fully rounded`() = runTest {
        val balloon = Balloon(id = 1, type = BalloonType.SPEAK, scope = BalloonScope.Panel(0))
        val (viewModel, _) = editorFor(comic(listOf(balloon)))
        advanceUntilIdle()

        viewModel.setBalloonRoundness(1, 0.4f)
        advanceUntilIdle()
        assertEquals(0.4f, balloons(viewModel).single().cornerRoundness, TOLERANCE)

        viewModel.setBalloonRoundness(1, 5f)
        advanceUntilIdle()
        assertEquals(1f, balloons(viewModel).single().cornerRoundness, TOLERANCE)
    }

    @Test
    fun `border size is kept within what can be drawn`() = runTest {
        val balloon = Balloon(id = 1, type = BalloonType.SPEAK, scope = BalloonScope.Panel(0))
        val (viewModel, _) = editorFor(comic(listOf(balloon)))
        advanceUntilIdle()

        viewModel.setBalloonBorderThickness(1, 99f)
        advanceUntilIdle()
        assertEquals(MAX_BALLOON_BORDER, balloons(viewModel).single().borderThickness, TOLERANCE)

        viewModel.setBalloonBorderThickness(1, -1f)
        advanceUntilIdle()
        assertEquals(MIN_BALLOON_BORDER, balloons(viewModel).single().borderThickness, TOLERANCE)
    }

    @Test
    fun `a balloon matching the panel border is outlined as thickly as the panels`() = runTest {
        val balloon = Balloon(
            id = 1,
            type = BalloonType.SPEAK,
            scope = BalloonScope.Panel(0),
            borderThickness = 0.02f,
        )
        val panelStyle = ComicStyle(borderThickness = 0.007f)

        assertEquals(0.007f, balloon.borderWidth(panelStyle), TOLERANCE)
        assertEquals(0.02f, balloon.copy(matchPanelBorder = false).borderWidth(panelStyle), TOLERANCE)
    }

    @Test
    fun `dragging the tail base widens the tail`() = runTest {
        val balloon = Balloon(
            id = 1,
            type = BalloonType.SPEAK,
            scope = BalloonScope.Panel(0),
            centre = NormalizedPoint(0.5f, 0.5f),
            tailAngleDegrees = 90f,
        )
        val (viewModel, _) = editorFor(comic(listOf(balloon)))
        advanceUntilIdle()

        // The tail points straight down, so its base widens sideways from the panel centre.
        viewModel.setBalloonTailWidth(1, x = 0.15f, y = 0.5f)
        advanceUntilIdle()

        assertTrue(balloons(viewModel).single().tailWidth > balloon.tailWidth)
    }

    @Test
    fun `the tail cannot be narrowed away completely`() = runTest {
        val balloon = Balloon(
            id = 1,
            type = BalloonType.SPEAK,
            scope = BalloonScope.Panel(0),
            centre = NormalizedPoint(0.5f, 0.5f),
            tailAngleDegrees = 90f,
        )
        val (viewModel, _) = editorFor(comic(listOf(balloon)))
        advanceUntilIdle()

        viewModel.setBalloonTailWidth(1, x = 0.25f, y = 0.5f)
        advanceUntilIdle()

        assertTrue(balloons(viewModel).single().tailWidth >= MIN_TAIL_WIDTH)
    }
}

private const val TOLERANCE = 1e-4f
