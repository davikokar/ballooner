package com.ballooner.data.panel

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PanelDao {
    @Query("SELECT * FROM panel WHERE projectId = :projectId ORDER BY id ASC")
    fun observeByProject(projectId: Long): Flow<List<PanelEntity>>

    @Insert
    suspend fun insertAll(panels: List<PanelEntity>)

    @Query("DELETE FROM panel WHERE projectId = :projectId")
    suspend fun deleteByProject(projectId: Long)

    /** Atomically swaps a project's panels for a freshly computed layout. */
    @Transaction
    suspend fun replaceAll(projectId: Long, panels: List<PanelEntity>) {
        deleteByProject(projectId)
        insertAll(panels)
    }
}
