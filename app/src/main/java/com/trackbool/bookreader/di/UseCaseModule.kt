package com.trackbool.bookreader.di

import com.trackbool.bookreader.domain.repository.BookFileRepository
import com.trackbool.bookreader.domain.repository.BookRepository
import com.trackbool.bookreader.domain.usecase.AddBooksUseCase
import com.trackbool.bookreader.domain.usecase.DeleteBooksUseCase
import com.trackbool.bookreader.domain.usecase.GetAllBooksUseCase
import com.trackbool.bookreader.domain.usecase.GetBooksInProgressUseCase
import com.trackbool.bookreader.domain.usecase.GetCompletedBooksUseCase
import com.trackbool.bookreader.domain.usecase.ImportBooksUseCase
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
    fun provideAddBooksUseCase(
        repository: BookRepository
    ): AddBooksUseCase {
        return AddBooksUseCase(repository)
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
    fun provideDeleteBooksUseCase(
        repository: BookRepository,
        bookFileRepository: BookFileRepository
    ): DeleteBooksUseCase {
        return DeleteBooksUseCase(repository, bookFileRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideImportBooksUseCase(
        bookFileRepository: BookFileRepository
    ): ImportBooksUseCase {
        return ImportBooksUseCase(bookFileRepository)
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
