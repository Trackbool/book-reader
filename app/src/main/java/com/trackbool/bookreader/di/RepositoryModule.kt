package com.trackbool.bookreader.di

import android.content.Context
import com.trackbool.bookreader.data.local.BookDao
import com.trackbool.bookreader.data.repository.BookFileRepositoryImpl
import com.trackbool.bookreader.data.repository.BookRepositoryImpl
import com.trackbool.bookreader.data.repository.BookContentRepositoryImpl
import com.trackbool.bookreader.domain.parser.metadata.BookMetadataParserFactory
import com.trackbool.bookreader.domain.repository.BookFileRepository
import com.trackbool.bookreader.domain.repository.BookRepository
import com.trackbool.bookreader.domain.repository.BookContentRepository
import com.trackbool.bookreader.domain.parser.content.BookContentParserFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideBookFileRepository(
        @ApplicationContext context: Context,
        bookMetadataParserFactory: BookMetadataParserFactory
    ): BookFileRepository {
        return BookFileRepositoryImpl(context, bookMetadataParserFactory)
    }

    @Provides
    @Singleton
    fun provideBookRepository(
        bookDao: BookDao
    ): BookRepository {
        return BookRepositoryImpl(bookDao)
    }

    @Provides
    @Singleton
    fun provideChapterRepository(
        parserFactory: BookContentParserFactory
    ): BookContentRepository {
        return BookContentRepositoryImpl(parserFactory)
    }
}
