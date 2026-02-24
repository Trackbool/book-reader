package com.trackbool.bookreader.di

import com.trackbool.bookreader.data.local.FileManager
import com.trackbool.bookreader.domain.repository.BookRepository
import com.trackbool.bookreader.domain.usecase.AddBookUseCase
import com.trackbool.bookreader.domain.usecase.DeleteBookUseCase
import com.trackbool.bookreader.domain.usecase.GetAllBooksUseCase
import com.trackbool.bookreader.domain.usecase.GetBooksInProgressUseCase
import com.trackbool.bookreader.domain.usecase.GetCompletedBooksUseCase
import com.trackbool.bookreader.domain.usecase.ImportBookUseCase
import com.trackbool.bookreader.domain.usecase.UpdateBookProgressUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {

    @Provides
    @ViewModelScoped
    fun provideGetAllBooksUseCase(
        repository: BookRepository
    ): GetAllBooksUseCase {
        return GetAllBooksUseCase(repository)
    }

    @Provides
    @ViewModelScoped
    fun provideAddBookUseCase(
        repository: BookRepository
    ): AddBookUseCase {
        return AddBookUseCase(repository)
    }

    @Provides
    @ViewModelScoped
    fun provideUpdateBookProgressUseCase(
        repository: BookRepository
    ): UpdateBookProgressUseCase {
        return UpdateBookProgressUseCase(repository)
    }

    @Provides
    @ViewModelScoped
    fun provideDeleteBookUseCase(
        repository: BookRepository
    ): DeleteBookUseCase {
        return DeleteBookUseCase(repository)
    }

    @Provides
    @ViewModelScoped
    fun provideImportBookUseCase(
        repository: BookRepository,
        fileManager: FileManager
    ): ImportBookUseCase {
        return ImportBookUseCase(repository, fileManager)
    }

    @Provides
    @ViewModelScoped
    fun provideGetBooksInProgressUseCase(
        repository: BookRepository
    ): GetBooksInProgressUseCase {
        return GetBooksInProgressUseCase(repository)
    }

    @Provides
    @ViewModelScoped
    fun provideGetCompletedBooksUseCase(
        repository: BookRepository
    ): GetCompletedBooksUseCase {
        return GetCompletedBooksUseCase(repository)
    }
}
