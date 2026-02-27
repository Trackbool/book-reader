package com.trackbool.bookreader.di

import com.trackbool.bookreader.data.parser.content.DocumentContentParserFactoryImpl
import com.trackbool.bookreader.data.parser.content.EpubContentParser
import com.trackbool.bookreader.data.parser.content.PdfContentParser
import com.trackbool.bookreader.data.parser.metadata.DocumentMetadataParserFactoryImpl
import com.trackbool.bookreader.data.parser.metadata.EpubMetadataParser
import com.trackbool.bookreader.data.parser.metadata.PdfMetadataParser
import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.parser.content.DocumentContentParser
import com.trackbool.bookreader.domain.parser.content.DocumentContentParserFactory
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

    @Binds
    @Singleton
    abstract fun bindDocumentContentParserFactory(
        factoryImpl: DocumentContentParserFactoryImpl
    ): DocumentContentParserFactory

    companion object {
        @Provides
        @Singleton
        fun provideEpubMetadataParser(): EpubMetadataParser {
            return EpubMetadataParser()
        }

        @Provides
        @Singleton
        fun providePdfMetadataParser(): PdfMetadataParser {
            return PdfMetadataParser()
        }

        @Provides
        @Singleton
        fun provideMetadataParsers(
            epubParser: EpubMetadataParser,
            pdfParser: PdfMetadataParser
        ): Map<BookFileType, DocumentMetadataParser> {
            return mapOf(
                BookFileType.EPUB to epubParser,
                BookFileType.PDF to pdfParser
            )
        }

        @Provides
        @Singleton
        fun provideEpubContentParser(): EpubContentParser {
            return EpubContentParser()
        }

        @Provides
        @Singleton
        fun providePdfContentParser(): PdfContentParser {
            return PdfContentParser()
        }

        @Provides
        @Singleton
        fun provideContentParsers(
            epubParser: EpubContentParser,
            pdfParser: PdfContentParser
        ): Map<BookFileType, DocumentContentParser> {
            return mapOf(
                BookFileType.EPUB to epubParser,
                BookFileType.PDF to pdfParser
            )
        }
    }
}
