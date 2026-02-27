package com.trackbool.bookreader.di

import android.content.Context
import com.trackbool.bookreader.data.local.BookDao
import com.trackbool.bookreader.data.repository.BookFileRepositoryImpl
import com.trackbool.bookreader.data.repository.BookRepositoryImpl
import com.trackbool.bookreader.data.repository.ChapterRepositoryImpl
import com.trackbool.bookreader.domain.repository.BookFileRepository
import com.trackbool.bookreader.domain.repository.BookRepository
import com.trackbool.bookreader.domain.repository.ChapterRepository
import com.trackbool.bookreader.domain.parser.content.DocumentContentParserFactory
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
        @ApplicationContext context: Context
    ): BookFileRepository {
        return BookFileRepositoryImpl(context)
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
        parserFactory: DocumentContentParserFactory
    ): ChapterRepository {
        return ChapterRepositoryImpl(parserFactory)
    }
}
