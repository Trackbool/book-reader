package com.trackbool.bookreader.data.parser.text

import com.trackbool.bookreader.data.parser.content.EpubContentParser
import com.trackbool.bookreader.data.parser.content.PdfContentParser
import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.parser.text.TextParser
import com.trackbool.bookreader.domain.parser.text.TextParserFactory
import javax.inject.Inject

class TextParserFactoryImpl @Inject constructor() : TextParserFactory {

    override fun getParser(fileType: BookFileType): TextParser? {
        return when (fileType) {
            BookFileType.EPUB -> EpubTextParser()
            BookFileType.PDF -> null
            else -> null
        }
    }
}
