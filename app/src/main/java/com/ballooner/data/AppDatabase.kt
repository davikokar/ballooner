package com.ballooner.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ballooner.data.comic.ComicBalloonEntity
import com.ballooner.data.comic.ComicCutEntity
import com.ballooner.data.comic.ComicDao
import com.ballooner.data.comic.ComicEntity
import com.ballooner.data.comic.ComicPanelEntity
import com.ballooner.data.comic.ComicSpanEntity
import com.ballooner.data.comic.FloatListConverter

@Database(
    entities = [
        ComicEntity::class,
        ComicSpanEntity::class,
        ComicCutEntity::class,
        ComicPanelEntity::class,
        ComicBalloonEntity::class,
    ],
    version = 10,
    exportSchema = true,
)
@TypeConverters(FloatListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun comicDao(): ComicDao
}
