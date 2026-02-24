package com.trackbool.bookreader.di

import android.content.Context
import com.trackbool.bookreader.data.local.BookDao
import com.trackbool.bookreader.data.local.BookDatabase
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
    fun provideBookDatabase(
        @ApplicationContext context: Context
    ): BookDatabase {
        return BookDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideBookDao(database: BookDatabase): BookDao {
        return database.bookDao()
    }
}
