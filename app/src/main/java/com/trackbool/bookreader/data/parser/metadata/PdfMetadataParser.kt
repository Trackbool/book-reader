package com.trackbool.bookreader.data.parser.metadata

import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.DocumentMetadata
import com.trackbool.bookreader.domain.parser.metadata.DocumentMetadataParser
import java.io.File
import javax.inject.Inject

class PdfMetadataParser @Inject constructor() : DocumentMetadataParser {

    override fun parse(file: File): DocumentMetadata? {
        return null
    }

    override fun supports(fileType: BookFileType): Boolean {
        return fileType == BookFileType.PDF
    }
}
