package com.ballooner.data.comic

import com.ballooner.domain.comic.Comic
import kotlinx.coroutines.flow.Flow

/**
 * One saved comic as the list needs it: the document itself, so the list can draw the comic
 * rather than only name it, and the facts about the row that are not part of the document.
 */
data class SavedComic(val id: Long, val updatedAt: Long, val comic: Comic)

/** Stores comic documents. No comic is ever stored as pixels; only its description. */
interface ComicRepository {

    fun observeComic(id: Long): Flow<Comic?>

    fun observeComics(): Flow<List<SavedComic>>

    suspend fun createComic(comic: Comic): Long

    suspend fun saveComic(id: Long, comic: Comic)

    suspend fun renameComic(id: Long, name: String)

    suspend fun deleteComic(id: Long)
}
