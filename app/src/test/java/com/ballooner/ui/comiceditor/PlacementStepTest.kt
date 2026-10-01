package com.ballooner.ui.comiceditor

import androidx.lifecycle.SavedStateHandle
import com.ballooner.data.comic.FakeComicRepository
import com.ballooner.data.comic.ImportedImage
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.ComicStyle
import com.ballooner.domain.comic.Grid
import com.ballooner.domain.comic.Layout
import com.ballooner.domain.comic.MAX_PANEL_ZOOM
import com.ballooner.domain.comic.MIN_PANEL_ZOOM
import com.ballooner.domain.comic.PageSizing
import com.ballooner.domain.comic.Panel
import com.ballooner.domain.comic.PanelImage
import com.ballooner.util.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
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

/** Covers the Placement step: choosing a panel, filling it, and fitting the image inside it. */
@OptIn(ExperimentalCoroutinesApi::class)
class PlacementStepTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val style = ComicStyle(gutter = 0f, borderThickness = 0f)

    private fun comic(withImages: Boolean = true) = Comic(
        // Two panels side by side, each held at half as wide as it is tall, so the page is square.
        sizing = PageSizing.Ratio(0.5f),
        style = style,
        layout = Layout(Grid(rows = 1, columns = 2)),
        panels = List(2) { Panel(if (withImages) PanelImage("image$it") else null) },
    )

    /** A row of panels, each either empty or already holding the named image. */
    private fun strip(images: List<String?>) = Comic(
        sizing = PageSizing.Ratio(0.5f),
        style = style,
        layout = Layout(Grid(rows = 1, columns = images.size)),
        panels = images.map { uri -> Panel(uri?.let { PanelImage(it) }) },
    )

    private fun editorFor(initial: Comic): Pair<ComicEditorViewModel, FakeComicRepository> {
        val repository = FakeComicRepository(initial)
        return ComicEditorViewModel(comicId = 1L, repository = repository) to repository
    }

    /** An editor whose imports do not finish until [gate] is completed. */
    private fun slowImportEditor(initial: Comic, gate: CompletableDeferred<Unit>) =
        ComicEditorViewModel(
            savedStateHandle = SavedStateHandle(mapOf(COMIC_ID_KEY to 1L)),
            repository = FakeComicRepository(initial),
            imageImporter = {
                gate.await()
                ImportedImage(it, null)
            },
        )

    private fun content(viewModel: ComicEditorViewModel) =
        viewModel.uiState.value as ComicEditorUiState.Content

    private fun imageOf(viewModel: ComicEditorViewModel, index: Int) =
        content(viewModel).comic.panels[index].image

    @Test
    fun `choosing a panel makes it the one being placed`() = runTest {
        val (viewModel, _) = editorFor(comic())
        advanceUntilIdle()

        viewModel.selectPanel(1)

        assertEquals(1, content(viewModel).activePanel)
    }

    @Test
    fun `double tapping an empty panel asks for an image`() {
        val opening = panelOpening(strip(listOf(null, "image1")), index = 0, focused = false)

        assertEquals(PanelOpening.PICK_IMAGE, opening)
    }

    @Test
    fun `double tapping a filled panel opens it up`() {
        val opening = panelOpening(comic(), index = 1, focused = false)

        assertEquals(PanelOpening.FOCUS, opening)
    }

    @Test
    fun `double tapping the panel that is open gives the page back`() {
        val opening = panelOpening(comic(), index = 1, focused = true)

        assertEquals(PanelOpening.UNFOCUS, opening)
    }

    @Test
    fun `an empty panel that is open gives the page back rather than asking for an image`() {
        // It is the only way out: an empty panel is offered no collapse handle.
        val opening = panelOpening(strip(listOf(null)), index = 0, focused = true)

        assertEquals(PanelOpening.UNFOCUS, opening)
    }

    @Test
    fun `the only panel of a comic cannot be opened up`() {
        val opening = panelOpening(strip(listOf("image0")), index = 0, focused = false)

        assertEquals(PanelOpening.NOTHING, opening)
    }

    @Test
    fun `choosing a panel that is not there selects nothing`() = runTest {
        val (viewModel, _) = editorFor(comic())
        advanceUntilIdle()

        viewModel.selectPanel(9)

        assertNull(content(viewModel).activePanel)
    }

    @Test
    fun `arriving at the step starts on the first panel`() = runTest {
        val (viewModel, _) = editorFor(comic())
        advanceUntilIdle()

        viewModel.selectStep(EditorStep.PLACEMENT)

        assertEquals(0, content(viewModel).activePanel)
    }

    @Test
    fun `going back to the layout forgets which panel was being placed`() = runTest {
        val (viewModel, _) = editorFor(comic())
        advanceUntilIdle()
        viewModel.selectPanel(1)

        viewModel.selectStep(EditorStep.LAYOUT)

        assertNull(content(viewModel).activePanel)
    }

    @Test
    fun `an image dropped into a panel starts out filling it`() = runTest {
        val (viewModel, repository) = editorFor(comic(withImages = false))
        advanceUntilIdle()

        viewModel.setPanelImage(0, "file://picked")
        advanceUntilIdle()

        val placed = imageOf(viewModel, 0)
        assertNotNull(placed)
        assertEquals("file://picked", placed!!.sourceUri)
        assertEquals(MIN_PANEL_ZOOM, placed.zoom, TOLERANCE)
        assertEquals(0f, placed.angleDegrees, TOLERANCE)
        assertEquals("file://picked", repository.saved.value.getValue(1L).panels[0].image?.sourceUri)
    }

    @Test
    fun `the first picked image lands in the panel that was asked for`() = runTest {
        val (viewModel, _) = editorFor(strip(listOf(null, null, null, null)))
        advanceUntilIdle()

        viewModel.importPanelImages(2, listOf("file://a"))
        advanceUntilIdle()

        assertEquals("file://a", imageOf(viewModel, 2)!!.sourceUri)
        assertNull(imageOf(viewModel, 0))
    }

    @Test
    fun `the images picked after the first fill the panels that follow it`() = runTest {
        val (viewModel, _) = editorFor(strip(listOf(null, null, null, null)))
        advanceUntilIdle()

        viewModel.importPanelImages(1, listOf("file://a", "file://b", "file://c"))
        advanceUntilIdle()

        assertEquals("file://a", imageOf(viewModel, 1)!!.sourceUri)
        assertEquals("file://b", imageOf(viewModel, 2)!!.sourceUri)
        assertEquals("file://c", imageOf(viewModel, 3)!!.sourceUri)
    }

    @Test
    fun `filling the page leaves the panels that already have an image alone`() = runTest {
        val (viewModel, _) = editorFor(strip(listOf(null, "kept", null, null)))
        advanceUntilIdle()

        viewModel.importPanelImages(0, listOf("file://a", "file://b", "file://c"))
        advanceUntilIdle()

        assertEquals("kept", imageOf(viewModel, 1)!!.sourceUri)
        assertEquals("file://b", imageOf(viewModel, 2)!!.sourceUri)
        assertEquals("file://c", imageOf(viewModel, 3)!!.sourceUri)
    }

    @Test
    fun `filling the page carries on from the last panel round to the first`() = runTest {
        val (viewModel, _) = editorFor(strip(listOf(null, null, null, null)))
        advanceUntilIdle()

        viewModel.importPanelImages(3, listOf("file://a", "file://b"))
        advanceUntilIdle()

        assertEquals("file://a", imageOf(viewModel, 3)!!.sourceUri)
        assertEquals("file://b", imageOf(viewModel, 0)!!.sourceUri)
    }

    @Test
    fun `more images than there are panels for leaves the extra ones behind`() = runTest {
        val (viewModel, _) = editorFor(strip(listOf(null, null)))
        advanceUntilIdle()

        viewModel.importPanelImages(0, listOf("file://a", "file://b", "file://c"))
        advanceUntilIdle()

        assertEquals(2, content(viewModel).comic.panels.size)
        assertEquals("file://b", imageOf(viewModel, 1)!!.sourceUri)
    }

    @Test
    fun `a panel waiting on its picked image says so`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val viewModel = slowImportEditor(strip(listOf(null, null)), gate)
        advanceUntilIdle()

        viewModel.importPanelImages(0, listOf("file://a", "file://b"))
        advanceUntilIdle()

        assertEquals(setOf(0, 1), content(viewModel).importingPanels)
    }

    @Test
    fun `a panel stops waiting once its picked image has been copied in`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val viewModel = slowImportEditor(strip(listOf(null, null)), gate)
        advanceUntilIdle()
        viewModel.importPanelImages(0, listOf("file://a", "file://b"))
        advanceUntilIdle()

        gate.complete(Unit)
        advanceUntilIdle()

        assertTrue(content(viewModel).importingPanels.isEmpty())
    }

    @Test
    fun `one trip to the picker counts as one undo`() = runTest {
        val (viewModel, _) = editorFor(strip(listOf(null, null, null, null)))
        advanceUntilIdle()
        viewModel.importPanelImages(0, listOf("file://a", "file://b", "file://c"))
        advanceUntilIdle()

        viewModel.undo()
        advanceUntilIdle()

        assertTrue(content(viewModel).comic.panels.all { it.image == null })
        assertFalse(content(viewModel).canUndo)
    }

    @Test
    fun `removing an image leaves the panel empty but still there`() = runTest {
        val (viewModel, _) = editorFor(comic())
        advanceUntilIdle()

        viewModel.setPanelImage(0, null)
        advanceUntilIdle()

        assertNull(imageOf(viewModel, 0))
        assertEquals(2, content(viewModel).comic.panels.size)
    }

    @Test
    fun `replacing an image starts its placement again`() = runTest {
        val turned = PanelImage("image0", zoom = 3f, angleDegrees = 40f)
        val (viewModel, _) = editorFor(comic().let { it.copy(panels = listOf(Panel(turned), it.panels[1])) })
        advanceUntilIdle()

        viewModel.setPanelImage(0, "file://other")
        advanceUntilIdle()

        assertEquals(MIN_PANEL_ZOOM, imageOf(viewModel, 0)!!.zoom, TOLERANCE)
        assertEquals(0f, imageOf(viewModel, 0)!!.angleDegrees, TOLERANCE)
    }

    @Test
    fun `pinching zooms the image in its panel`() = runTest {
        val (viewModel, repository) = editorFor(comic())
        advanceUntilIdle()

        viewModel.startPlacementGesture()
        viewModel.transformPanelImage(0, imageAspect = 1f, zoomBy = 2f)
        viewModel.endPlacementGesture(0, imageAspect = 1f)
        advanceUntilIdle()

        assertEquals(2f, imageOf(viewModel, 0)!!.zoom, TOLERANCE)
        assertEquals(2f, repository.saved.value.getValue(1L).panels[0].image!!.zoom, TOLERANCE)
    }

    @Test
    fun `a whole pinch counts as one undo`() = runTest {
        val (viewModel, _) = editorFor(comic())
        advanceUntilIdle()

        viewModel.startPlacementGesture()
        repeat(5) { viewModel.transformPanelImage(0, imageAspect = 1f, zoomBy = 1.1f) }
        viewModel.endPlacementGesture(0, imageAspect = 1f)
        advanceUntilIdle()
        assertTrue(content(viewModel).canUndo)

        viewModel.undo()
        advanceUntilIdle()

        assertEquals(MIN_PANEL_ZOOM, imageOf(viewModel, 0)!!.zoom, TOLERANCE)
        assertFalse(content(viewModel).canUndo)
    }

    @Test
    fun `an image can never be zoomed out past covering its panel`() = runTest {
        val (viewModel, _) = editorFor(comic())
        advanceUntilIdle()

        viewModel.startPlacementGesture()
        repeat(10) { viewModel.transformPanelImage(0, imageAspect = 1f, zoomBy = 0.5f) }
        viewModel.endPlacementGesture(0, imageAspect = 1f)
        advanceUntilIdle()

        assertEquals(MIN_PANEL_ZOOM, imageOf(viewModel, 0)!!.zoom, TOLERANCE)
    }

    @Test
    fun `zooming stops at the maximum`() = runTest {
        val (viewModel, _) = editorFor(comic())
        advanceUntilIdle()

        viewModel.startPlacementGesture()
        repeat(20) { viewModel.transformPanelImage(0, imageAspect = 1f, zoomBy = 2f) }
        viewModel.endPlacementGesture(0, imageAspect = 1f)
        advanceUntilIdle()

        assertEquals(MAX_PANEL_ZOOM, imageOf(viewModel, 0)!!.zoom, TOLERANCE)
    }

    @Test
    fun `dragging a zoomed image moves what is shown`() = runTest {
        val (viewModel, _) = editorFor(comic())
        advanceUntilIdle()

        viewModel.startPlacementGesture()
        viewModel.transformPanelImage(0, imageAspect = 1f, zoomBy = 2f)
        viewModel.transformPanelImage(0, imageAspect = 1f, panX = 0.05f)
        viewModel.endPlacementGesture(0, imageAspect = 1f)
        advanceUntilIdle()

        assertTrue(imageOf(viewModel, 0)!!.centre.u < 0.5f)
    }

    @Test
    fun `twisting turns the image and snaps near a quarter turn`() = runTest {
        val (viewModel, _) = editorFor(comic())
        advanceUntilIdle()

        viewModel.startPlacementGesture()
        // A real twist arrives a degree at a time, so the magnet must wait for the fingers to lift.
        repeat(88) { viewModel.transformPanelImage(0, imageAspect = 1f, rotateBy = 1f) }
        assertEquals(88f, imageOf(viewModel, 0)!!.angleDegrees, TOLERANCE)
        viewModel.endPlacementGesture(0, imageAspect = 1f)
        advanceUntilIdle()

        assertEquals(90f, imageOf(viewModel, 0)!!.angleDegrees, TOLERANCE)
    }

    @Test
    fun `placing one panel leaves the others alone`() = runTest {
        val (viewModel, _) = editorFor(comic())
        advanceUntilIdle()
        val before = imageOf(viewModel, 1)

        viewModel.startPlacementGesture()
        viewModel.transformPanelImage(0, imageAspect = 1f, zoomBy = 3f, rotateBy = 20f)
        viewModel.endPlacementGesture(0, imageAspect = 1f)
        advanceUntilIdle()

        assertEquals(before, imageOf(viewModel, 1))
    }

    @Test
    fun `placing an empty panel does nothing`() = runTest {
        val (viewModel, _) = editorFor(comic(withImages = false))
        advanceUntilIdle()

        viewModel.startPlacementGesture()
        viewModel.transformPanelImage(0, imageAspect = 1f, zoomBy = 2f)
        viewModel.endPlacementGesture(0, imageAspect = 1f)
        advanceUntilIdle()

        assertNull(imageOf(viewModel, 0))
        assertFalse(content(viewModel).canUndo)
    }

    @Test
    fun `the layout step is unaffected by image placement`() = runTest {
        val (viewModel, _) = editorFor(comic())
        advanceUntilIdle()
        val layout = content(viewModel).comic.layout

        viewModel.startPlacementGesture()
        viewModel.transformPanelImage(0, imageAspect = 1f, zoomBy = 2f, rotateBy = 33f)
        viewModel.endPlacementGesture(0, imageAspect = 1f)
        advanceUntilIdle()

        assertEquals(layout, content(viewModel).comic.layout)
    }

    @Test
    fun `style changes never add or remove panels`() = runTest {
        val (viewModel, repository) = editorFor(comic())
        advanceUntilIdle()

        viewModel.setStyle(ComicStyle(gutter = 0.05f, borderThickness = 0.01f))
        advanceUntilIdle()

        assertEquals(2, content(viewModel).comic.panels.size)
        assertEquals(0.05f, repository.saved.value.getValue(1L).style.gutter, TOLERANCE)
        assertEquals("image0", imageOf(viewModel, 0)?.sourceUri)
    }
}

private const val TOLERANCE = 1e-4f
