package com.ballooner.ui.comiceditor

import com.ballooner.data.comic.FakeComicRepository
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.Grid
import com.ballooner.domain.comic.Layout
import com.ballooner.domain.comic.PageSizing
import com.ballooner.domain.comic.Panel
import com.ballooner.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** The editor works on a copy and the database only hears about it when the comic is saved. */
@OptIn(ExperimentalCoroutinesApi::class)
class SavingTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val stored = Comic(
        name = "Stored",
        sizing = PageSizing.Ratio(1f),
        layout = Layout(Grid(rows = 1, columns = 2)),
        panels = List(2) { Panel() },
    )

    private fun editorFor(
        comicId: Long = 1L,
        initial: Comic = stored,
    ): Pair<ComicEditorViewModel, FakeComicRepository> {
        val repository = FakeComicRepository(initial)
        return ComicEditorViewModel(comicId = comicId, repository = repository) to repository
    }

    private fun content(viewModel: ComicEditorViewModel) =
        viewModel.uiState.value as ComicEditorUiState.Content

    @Test
    fun `an edit leaves the stored comic alone`() = runTest {
        val (viewModel, repository) = editorFor()
        advanceUntilIdle()

        viewModel.setName("Renamed")
        advanceUntilIdle()

        assertEquals("Renamed", content(viewModel).comic.name)
        assertEquals(stored, repository.saved.value.getValue(1L))
    }

    @Test
    fun `saving hands the edited comic to the database`() = runTest {
        val (viewModel, repository) = editorFor()
        advanceUntilIdle()

        viewModel.setName("Renamed")
        viewModel.saveComic()
        advanceUntilIdle()

        assertEquals("Renamed", repository.saved.value.getValue(1L).name)
    }

    @Test
    fun `an edit marks the comic unsaved until it is saved`() = runTest {
        val (viewModel, _) = editorFor()
        advanceUntilIdle()
        assertFalse(content(viewModel).unsaved)

        viewModel.setName("Renamed")
        advanceUntilIdle()
        assertTrue(content(viewModel).unsaved)

        viewModel.saveComic()
        advanceUntilIdle()
        assertFalse(content(viewModel).unsaved)
    }

    @Test
    fun `undoing back to the stored comic clears the unsaved mark`() = runTest {
        val (viewModel, _) = editorFor()
        advanceUntilIdle()

        viewModel.applyPreset(rows = 3, columns = 3)
        advanceUntilIdle()
        assertTrue(content(viewModel).unsaved)

        viewModel.undo()
        advanceUntilIdle()

        assertFalse(content(viewModel).unsaved)
    }

    @Test
    fun `a comic that was never opened from the list starts unsaved and empty-handed`() = runTest {
        val (viewModel, repository) = editorFor(comicId = NEW_COMIC_ID)
        val before = repository.saved.value
        advanceUntilIdle()

        // It is named after how many comics there already are, and shaped by the image to come.
        assertEquals("My Comic 2", content(viewModel).comic.name)
        assertEquals(PageSizing.FromImage, content(viewModel).comic.sizing)
        assertEquals(LayoutKind.SINGLE, layoutKindOf(content(viewModel).comic))
        assertTrue(content(viewModel).unsaved)
        assertEquals(before, repository.saved.value)
    }

    @Test
    fun `saving a new comic for the first time gives it a row of its own`() = runTest {
        val (viewModel, repository) = editorFor(comicId = NEW_COMIC_ID)
        advanceUntilIdle()

        var savedId: Long? = null
        viewModel.saveComic { savedId = it }
        advanceUntilIdle()

        assertEquals(2, repository.saved.value.size)
        assertEquals("My Comic 2", repository.saved.value.getValue(savedId!!).name)
        assertFalse(content(viewModel).unsaved)
    }

    @Test
    fun `saving a new comic twice does not make a second comic`() = runTest {
        val (viewModel, repository) = editorFor(comicId = NEW_COMIC_ID)
        advanceUntilIdle()

        viewModel.saveComic()
        advanceUntilIdle()
        viewModel.setName("Renamed")
        viewModel.saveComic()
        advanceUntilIdle()

        assertEquals(2, repository.saved.value.size)
        assertTrue(repository.saved.value.values.any { it.name == "Renamed" })
    }
}
