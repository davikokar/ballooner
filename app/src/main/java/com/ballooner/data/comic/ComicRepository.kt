package com.ballooner.data.comic

import com.ballooner.domain.comic.Comic
import kotlinx.coroutines.flow.Flow

/**
 * One saved comic as the list needs it: the document itself, so the list can draw the comic
 * rather than only name it, and the facts about the row that are not part of the document.
 */
data class SavedComic(
    val id: Long,
    val updatedAt: Long,
    val comic: Comic,
    /** The picture chosen to stand for it in the list, or null to show the comic itself. */
    val coverUri: String? = null,
)

/** Stores comic documents. No comic is ever stored as pixels; only its description. */
interface ComicRepository {

    fun observeComic(id: Long): Flow<Comic?>

    fun observeComics(): Flow<List<SavedComic>>

    suspend fun createComic(comic: Comic): Long

    suspend fun saveComic(id: Long, comic: Comic)

    suspend fun renameComic(id: Long, name: String)

    /** Chooses the picture the comic is listed under, or null to list the comic itself. */
    suspend fun setCover(id: Long, sourceUri: String?)

    suspend fun deleteComic(id: Long)
}
