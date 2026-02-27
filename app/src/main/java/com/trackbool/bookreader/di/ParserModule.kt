package com.trackbool.bookreader.di

import com.trackbool.bookreader.data.parser.DocumentMetadataParserFactoryImpl
import com.trackbool.bookreader.data.parser.EpubMetadataParser
import com.trackbool.bookreader.data.parser.PdfMetadataParser
import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.parser.metadata.DocumentMetadataParser
import com.trackbool.bookreader.domain.parser.metadata.DocumentMetadataParserFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ParserModule {

    @Binds
    @Singleton
    abstract fun bindDocumentParserFactory(
        factoryImpl: DocumentMetadataParserFactoryImpl
    ): DocumentMetadataParserFactory

    companion object {
        @Provides
        @Singleton
        fun provideEpubParser(): EpubMetadataParser {
            return EpubMetadataParser()
        }

        @Provides
        @Singleton
        fun providePdfParser(): PdfMetadataParser {
            return PdfMetadataParser()
        }

        @Provides
        @Singleton
        fun provideParsers(
            epubParser: EpubMetadataParser,
            pdfParser: PdfMetadataParser
        ): Map<BookFileType, DocumentMetadataParser> {
            return mapOf(
                BookFileType.EPUB to epubParser,
                BookFileType.PDF to pdfParser
            )
        }
    }
}
