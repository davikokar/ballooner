package com.ballooner.data.comic

import com.ballooner.domain.comic.Comic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomComicRepository @Inject constructor(
    private val dao: ComicDao,
) : ComicRepository {

    override fun observeComic(id: Long): Flow<Comic?> =
        dao.observeComic(id).map { parts -> parts?.toDomain() }

    override fun observeComics(): Flow<List<SavedComic>> =
        dao.observeComics().map { comics ->
            comics.map {
                SavedComic(
                    id = it.comic.id,
                    updatedAt = it.comic.updatedAt,
                    comic = it.toDomain(),
                    coverUri = it.comic.coverUri,
                )
            }
        }

    override suspend fun createComic(comic: Comic): Long {
        val now = System.currentTimeMillis()
        val id = dao.insertComic(comic.toParts(id = 0, createdAt = now, updatedAt = now).comic)
        dao.saveComic(comic.toParts(id = id, createdAt = now, updatedAt = now))
        return id
    }

    override suspend fun saveComic(id: Long, comic: Comic) {
        val createdAt = dao.createdAt(id) ?: return
        dao.saveComic(
            comic.toParts(
                id = id,
                createdAt = createdAt,
                updatedAt = System.currentTimeMillis(),
                // The document says nothing about the cover, so the row keeps the one it has.
                coverUri = dao.coverUri(id),
            ),
        )
    }

    override suspend fun setCover(id: Long, sourceUri: String?) =
        dao.setCover(id, sourceUri, System.currentTimeMillis())

    override suspend fun renameComic(id: Long, name: String) =
        dao.renameComic(id, name, System.currentTimeMillis())

    override suspend fun deleteComic(id: Long) = dao.deleteComic(id)
}
