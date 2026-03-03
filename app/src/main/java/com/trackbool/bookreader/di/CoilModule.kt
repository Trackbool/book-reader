package com.trackbool.bookreader.di

import android.content.Context
import coil.ImageLoader
import com.trackbool.bookreader.ui.EpubImageFetcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoilModule {

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(EpubImageFetcher.Factory())
                // add(PdfImageFetcher.Factory())
            }
            .build()
    }
}