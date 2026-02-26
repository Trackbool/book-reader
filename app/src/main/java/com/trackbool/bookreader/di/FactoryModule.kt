package com.trackbool.bookreader.di

import com.trackbool.bookreader.data.source.BookSourceFactory
import com.trackbool.bookreader.data.source.AndroidBookSourceFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FactoryModule {

    @Binds
    @Singleton
    abstract fun bindBookSourceFactory(
        factory: AndroidBookSourceFactory
    ): BookSourceFactory
}
