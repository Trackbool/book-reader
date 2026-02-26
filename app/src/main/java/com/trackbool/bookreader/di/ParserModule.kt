package com.trackbool.bookreader.di

import android.content.Context
import com.trackbool.bookreader.data.parser.DocumentParserFactoryImpl
import com.trackbool.bookreader.data.parser.EpubParser
import com.trackbool.bookreader.data.parser.PdfParser
import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.parser.DocumentParser
import com.trackbool.bookreader.domain.parser.DocumentParserFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ParserModule {

    @Binds
    @Singleton
    abstract fun bindDocumentParserFactory(
        factoryImpl: DocumentParserFactoryImpl
    ): DocumentParserFactory

    companion object {
        @Provides
        @Singleton
        fun provideEpubParser(
            @ApplicationContext context: Context
        ): EpubParser {
            return EpubParser(context)
        }

        @Provides
        @Singleton
        fun providePdfParser(): PdfParser {
            return PdfParser()
        }

        @Provides
        @Singleton
        fun provideParsers(
            epubParser: EpubParser,
            pdfParser: PdfParser
        ): Map<BookFileType, DocumentParser> {
            return mapOf(
                BookFileType.EPUB to epubParser,
                BookFileType.PDF to pdfParser
            )
        }
    }
}
