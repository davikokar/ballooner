package com.ballooner.data.comic

import com.ballooner.domain.comic.Comic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class FakeComicRepository(initial: Comic = Comic()) : ComicRepository {

    private val comics = MutableStateFlow(mapOf(1L to initial))
    private var nextId = 2L
    private var covers = emptyMap<Long, String>()

    /** What the editor has written, so tests can check that edits are persisted. */
    val saved: StateFlow<Map<Long, Comic>> = comics

    override fun observeComic(id: Long) = comics.map { it[id] }

    override fun observeComics() = comics.map { all ->
        all.map { (id, comic) -> SavedComic(id = id, updatedAt = id, comic = comic, coverUri = covers[id]) }
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

    override suspend fun renameComic(id: Long, name: String) {
        val comic = comics.value[id] ?: return
        comics.value = comics.value + (id to comic.copy(name = name))
    }

    override suspend fun setCover(id: Long, sourceUri: String?) {
        covers = if (sourceUri == null) covers - id else covers + (id to sourceUri)
    }

    override suspend fun deleteComic(id: Long) {
        comics.value = comics.value - id
    }
}
