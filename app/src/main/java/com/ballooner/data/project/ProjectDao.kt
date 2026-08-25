package com.ballooner.data.project

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM project ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM project WHERE id = :id")
    fun observeById(id: Long): Flow<ProjectEntity?>

    @Insert
    suspend fun insert(project: ProjectEntity): Long

    @Query("UPDATE project SET imageUri = :uri WHERE id = :id")
    suspend fun updateImageUri(id: Long, uri: String?)

    @Query("UPDATE project SET name = :name WHERE id = :id")
    suspend fun updateName(id: Long, name: String)

    @Query("DELETE FROM project WHERE id = :id")
    suspend fun deleteById(id: Long)
}
