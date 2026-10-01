package com.ballooner.di

import android.content.Context
import androidx.room.Room
import com.ballooner.data.AppDatabase
import com.ballooner.data.MIGRATION_10_11
import com.ballooner.data.MIGRATION_1_2
import com.ballooner.data.MIGRATION_2_3
import com.ballooner.data.MIGRATION_3_4
import com.ballooner.data.MIGRATION_4_5
import com.ballooner.data.MIGRATION_5_6
import com.ballooner.data.MIGRATION_6_7
import com.ballooner.data.MIGRATION_7_8
import com.ballooner.data.MIGRATION_8_9
import com.ballooner.data.MIGRATION_9_10
import com.ballooner.data.comic.ComicDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "ballooner.db")
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
            )
            .build()

    @Provides
    fun provideComicDao(database: AppDatabase): ComicDao = database.comicDao()
}
