package com.ballooner.data.comic

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** A comic and everything that hangs off it, loaded in one shot. */
data class ComicWithParts(
    @Embedded val comic: ComicEntity,
    @Relation(parentColumn = "id", entityColumn = "comicId")
    val spans: List<ComicSpanEntity>,
    @Relation(parentColumn = "id", entityColumn = "comicId")
    val cuts: List<ComicCutEntity>,
    @Relation(parentColumn = "id", entityColumn = "comicId")
    val panels: List<ComicPanelEntity>,
    @Relation(parentColumn = "id", entityColumn = "comicId")
    val balloons: List<ComicBalloonEntity>,
)

@Dao
interface ComicDao {

    @Transaction
    @Query("SELECT * FROM comic WHERE id = :id")
    fun observeComic(id: Long): Flow<ComicWithParts?>

    @Query("SELECT * FROM comic ORDER BY updatedAt DESC")
    fun observeComics(): Flow<List<ComicEntity>>

    @Query("SELECT createdAt FROM comic WHERE id = :id")
    suspend fun createdAt(id: Long): Long?

    @Insert
    suspend fun insertComic(comic: ComicEntity): Long

    @Update
    suspend fun updateComic(comic: ComicEntity)

    @Query("DELETE FROM comic WHERE id = :id")
    suspend fun deleteComic(id: Long)

    @Query("UPDATE comic SET name = :name, updatedAt = :updatedAt WHERE id = :id")
    suspend fun renameComic(id: Long, name: String, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpans(spans: List<ComicSpanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCuts(cuts: List<ComicCutEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPanels(panels: List<ComicPanelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBalloons(balloons: List<ComicBalloonEntity>)

    @Query("DELETE FROM comic_span WHERE comicId = :comicId")
    suspend fun deleteSpans(comicId: Long)

    @Query("DELETE FROM comic_cut WHERE comicId = :comicId")
    suspend fun deleteCuts(comicId: Long)

    @Query("DELETE FROM comic_panel WHERE comicId = :comicId")
    suspend fun deletePanels(comicId: Long)

    @Query("DELETE FROM comic_balloon WHERE comicId = :comicId")
    suspend fun deleteBalloons(comicId: Long)

    /**
     * Writes a whole document at once. The document is the unit of change, so replacing its parts
     * wholesale is both simpler and less error-prone than diffing them.
     */
    @Transaction
    suspend fun saveComic(parts: ComicWithParts) {
        updateComic(parts.comic)
        deleteSpans(parts.comic.id)
        deleteCuts(parts.comic.id)
        deletePanels(parts.comic.id)
        deleteBalloons(parts.comic.id)
        insertSpans(parts.spans)
        insertCuts(parts.cuts)
        insertPanels(parts.panels)
        insertBalloons(parts.balloons)
    }
}
