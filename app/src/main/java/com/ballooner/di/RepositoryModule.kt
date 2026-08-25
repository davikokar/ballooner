package com.ballooner.di

import com.ballooner.data.project.ProjectRepository
import com.ballooner.data.project.RoomProjectRepository
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
}
