package com.ballooner.data.comic

import com.ballooner.domain.comic.Comic
import kotlinx.coroutines.flow.Flow

/** One saved comic as the comic list needs it, without loading the whole document. */
data class ComicSummary(val id: Long, val name: String, val updatedAt: Long)

/** Stores comic documents. No comic is ever stored as pixels; only its description. */
interface ComicRepository {

    fun observeComic(id: Long): Flow<Comic?>

    fun observeComics(): Flow<List<ComicSummary>>

    suspend fun createComic(comic: Comic): Long

    suspend fun saveComic(id: Long, comic: Comic)

    suspend fun deleteComic(id: Long)
}
