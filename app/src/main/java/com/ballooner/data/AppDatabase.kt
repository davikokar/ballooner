package com.ballooner.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ballooner.data.project.ProjectDao
import com.ballooner.data.project.ProjectEntity

@Database(
    entities = [ProjectEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
}
