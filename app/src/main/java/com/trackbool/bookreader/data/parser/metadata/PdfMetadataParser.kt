package com.trackbool.bookreader.data.parser.metadata

import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.BookMetadata
import com.trackbool.bookreader.domain.parser.metadata.BookMetadataParser
import java.io.File
import javax.inject.Inject

class PdfMetadataParser @Inject constructor() : BookMetadataParser {

    override fun parse(file: File): BookMetadata? {
        return null
    }

    override fun supports(fileType: BookFileType): Boolean {
        return fileType == BookFileType.PDF
    }
}
