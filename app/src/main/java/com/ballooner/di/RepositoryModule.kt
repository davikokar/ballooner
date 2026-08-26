package com.ballooner.di

import com.ballooner.data.balloon.BalloonRepository
import com.ballooner.data.balloon.RoomBalloonRepository
import com.ballooner.data.image.AppImageStore
import com.ballooner.data.image.ImageStore
import com.ballooner.data.project.ProjectRepository
import com.ballooner.data.project.RoomProjectRepository
import com.ballooner.data.settings.AppSettingsRepository
import com.ballooner.data.settings.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProjectRepository(impl: RoomProjectRepository): ProjectRepository

    @Binds
    @Singleton
    abstract fun bindBalloonRepository(impl: RoomBalloonRepository): BalloonRepository

    @Binds
    @Singleton
    abstract fun bindImageStore(impl: AppImageStore): ImageStore

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: AppSettingsRepository): SettingsRepository
}
