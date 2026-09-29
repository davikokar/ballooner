package com.ballooner.ui.comiclist

import com.ballooner.data.comic.FakeComicRepository
import com.ballooner.domain.comic.PageSizing
import com.ballooner.ui.comiceditor.LayoutKind
import com.ballooner.ui.comiceditor.layoutKindOf
import com.ballooner.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
}
