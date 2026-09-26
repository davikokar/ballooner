package com.ballooner.data.comic

import com.ballooner.domain.comic.Comic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class FakeComicRepository(initial: Comic = Comic()) : ComicRepository {

    private val comics = MutableStateFlow(mapOf(1L to initial))
    private var nextId = 2L

    /** What the editor has written, so tests can check that edits are persisted. */
    val saved: StateFlow<Map<Long, Comic>> = comics

    override fun observeComic(id: Long) = comics.map { it[id] }

    override fun observeComics() = comics.map { all ->
        all.map { (id, comic) -> ComicSummary(id = id, name = comic.name, updatedAt = id) }
    }

    override suspend fun createComic(comic: Comic): Long {
        val id = nextId++
        comics.value = comics.value + (id to comic)
        return id
    }

    override suspend fun saveComic(id: Long, comic: Comic) {
        if (id !in comics.value) return
        comics.value = comics.value + (id to comic)
    }

    override suspend fun deleteComic(id: Long) {
        comics.value = comics.value - id
    }
}
