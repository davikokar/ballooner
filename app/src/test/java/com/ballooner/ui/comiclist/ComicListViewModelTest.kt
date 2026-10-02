package com.ballooner.ui.comiclist

import com.ballooner.data.comic.FakeComicRepository
import com.ballooner.data.comic.ImportedImage
import com.ballooner.data.comic.SavedComic
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.PageSizing
import com.ballooner.domain.comic.Panel
import com.ballooner.domain.comic.PanelImage
import com.ballooner.ui.comiceditor.LayoutKind
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
    fun `a new comic is one panel shaped by the image that will fill it`() = runTest {
        val repository = FakeComicRepository()
        val viewModel = ComicListViewModel(repository)
        var created: Long? = null

        viewModel.createComic { created = it }
        advanceUntilIdle()

        val comic = repository.saved.value.getValue(created!!)
        assertEquals(LayoutKind.SINGLE, layoutKindOf(comic))
        assertEquals(PageSizing.FromImage, comic.sizing)
        assertEquals(1, comic.panels.size)
    }

    @Test
    fun `a new comic is named after how many there already are`() = runTest {
        val repository = FakeComicRepository()
        val viewModel = ComicListViewModel(repository)
        var created: Long? = null

        viewModel.createComic { created = it }
        advanceUntilIdle()

        assertEquals("My Comic 2", repository.saved.value.getValue(created!!).name)
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
