package com.ballooner.di

import android.content.Context
import androidx.room.Room
import com.ballooner.data.AppDatabase
import com.ballooner.data.project.ProjectDao
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
        Room.databaseBuilder(context, AppDatabase::class.java, "ballooner.db").build()

    @Provides
    fun provideProjectDao(database: AppDatabase): ProjectDao = database.projectDao()
}
