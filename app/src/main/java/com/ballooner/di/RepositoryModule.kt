package com.ballooner.di

import com.ballooner.data.comic.AppComicNamer
import com.ballooner.data.comic.AppPanelImageImporter
import com.ballooner.data.comic.ComicNamer
import com.ballooner.data.comic.ComicRepository
import com.ballooner.data.comic.PanelImageImporter
import com.ballooner.data.comic.RoomComicRepository
import com.ballooner.data.image.AppImageStore
import com.ballooner.data.image.ImageStore
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
    abstract fun bindComicRepository(impl: RoomComicRepository): ComicRepository

    @Binds
    @Singleton
    abstract fun bindPanelImageImporter(impl: AppPanelImageImporter): PanelImageImporter

    @Binds
    @Singleton
    abstract fun bindImageStore(impl: AppImageStore): ImageStore

    @Binds
    @Singleton
    abstract fun bindComicNamer(impl: AppComicNamer): ComicNamer
}
