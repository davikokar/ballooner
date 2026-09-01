package com.ballooner.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ballooner.data.balloon.BalloonDao
import com.ballooner.data.balloon.BalloonEntity
import com.ballooner.data.panel.PanelDao
import com.ballooner.data.panel.PanelEntity
import com.ballooner.data.project.ProjectDao
import com.ballooner.data.project.ProjectEntity

@Database(
    entities = [ProjectEntity::class, BalloonEntity::class, PanelEntity::class],
    version = 6,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun balloonDao(): BalloonDao
    abstract fun panelDao(): PanelDao
}
