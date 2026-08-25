package com.ballooner.data.project

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM project ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Insert
    suspend fun insert(project: ProjectEntity): Long

    @Query("DELETE FROM project WHERE id = :id")
    suspend fun deleteById(id: Long)
}
