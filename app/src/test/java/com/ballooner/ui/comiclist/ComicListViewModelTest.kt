package com.ballooner.ui.comiclist

import com.ballooner.data.comic.FakeComicRepository
import com.ballooner.data.comic.ImportedImage
import com.ballooner.data.comic.SavedComic
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.PageSizing
import com.ballooner.domain.comic.Panel
import com.ballooner.domain.comic.PanelImage
import com.ballooner.ui.comiceditor.LayoutKind
import com.ballooner.ui.comiceditor.NEW_COMIC_ID
import com.ballooner.ui.comiceditor.layoutKindOf
import com.ballooner.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ComicListViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `creating a comic writes nothing until the editor saves it`() = runTest {
        val repository = FakeComicRepository()
        val viewModel = ComicListViewModel(repository)
        val before = repository.saved.value
        var opened: Long? = null

        viewModel.createComic { opened = it }
        advanceUntilIdle()

        // Abandoning a comic that was never saved must leave nothing behind, so nothing is made.
        assertEquals(NEW_COMIC_ID, opened)
        assertEquals(before, repository.saved.value)
    }

    @Test
    fun `a duplicate is the same comic under a copied name`() = runTest {
        val original = Comic(name = "Caper", panels = List(1) { Panel(PanelImage("picture")) })
        val repository = FakeComicRepository(original)
        val viewModel = ComicListViewModel(repository)

        viewModel.duplicateComic(SavedComic(id = 1L, updatedAt = 0L, comic = original))
        advanceUntilIdle()

        val copy = repository.saved.value.values.last()
        assertEquals("Caper copy", copy.name)
        // The copy points at the same picture: an image is a file the app already holds.
        assertEquals("picture", copy.panels.single().image?.sourceUri)
    }

    @Test
    fun `a duplicate keeps the cover the original was listed under`() = runTest {
        val original = Comic(name = "Caper")
        val repository = FakeComicRepository(original)
        val viewModel = ComicListViewModel(repository)

        viewModel.duplicateComic(SavedComic(id = 1L, updatedAt = 0L, comic = original, coverUri = "cover"))
        advanceUntilIdle()

        val listed = repository.observeComics().first()
        assertEquals("cover", listed.single { it.comic.name == "Caper copy" }.coverUri)
    }

    @Test
    fun `a chosen cover is copied into the app before it is kept`() = runTest {
        val repository = FakeComicRepository()
        val viewModel = ComicListViewModel(
            repository = repository,
            imageImporter = { ImportedImage("copy-of-$it", null) },
        )

        viewModel.setCover(1L, "borrowed")
        advanceUntilIdle()

        assertEquals("copy-of-borrowed", repository.observeComics().first().single().coverUri)
    }
}
