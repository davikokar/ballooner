package com.ballooner.data.balloon

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BalloonDao {
    @Query("SELECT * FROM balloon WHERE projectId = :projectId ORDER BY id ASC")
    fun observeByProject(projectId: Long): Flow<List<BalloonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(balloon: BalloonEntity): Long

    @Query("DELETE FROM balloon WHERE id = :id")
    suspend fun deleteById(id: Long)
}
