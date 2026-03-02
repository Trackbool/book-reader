package com.trackbool.bookreader.ui.parser

import com.trackbool.bookreader.domain.model.BookFileType
import javax.inject.Inject

class BookContentRenderParserFactoryImpl @Inject constructor(
    private val epubRenderParser: EpubContentRenderParser
) : BookContentRenderParserFactory {

    override fun getParser(fileType: BookFileType): BookContentRenderParser? {
        return when (fileType) {
            BookFileType.EPUB -> epubRenderParser
            BookFileType.PDF -> null
            BookFileType.NONE -> null
        }
    }
}
